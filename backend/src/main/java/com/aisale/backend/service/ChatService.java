package com.aisale.backend.service;

import com.aisale.backend.dto.ChatMessageResponse;
import com.aisale.backend.dto.ChatPartnerResponse;
import com.aisale.backend.entity.ChatMessage;
import com.aisale.backend.entity.User;
import com.aisale.backend.repository.ChatMessageRepository;
import com.aisale.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatMessageProducer chatMessageProducer;

    @Transactional
    public ChatMessageResponse sendMessage(Long senderId, Long receiverId, String content) {
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new RuntimeException("发送者不存在"));
        User receiver = userRepository.findById(receiverId)
            .orElseThrow(() -> new RuntimeException("接收者不存在"));

        ChatMessage message = ChatMessage.builder()
            .senderId(senderId)
            .receiverId(receiverId)
            .content(content)
            .messageType(ChatMessage.MessageType.TEXT)
            .isRead(false)
            .build();

        ChatMessage savedMessage = chatMessageRepository.saveAndFlush(message);
        log.info("Message saved: id={}, from={}({}), to={}({})",
            savedMessage.getId(), senderId, sender.getUsername(), receiverId, receiver.getUsername());

        ChatMessageResponse response = ChatMessageResponse.fromEntity(savedMessage, sender.getUsername(), receiver.getUsername());
        log.info("Created response: senderId={}, senderName={}, receiverId={}, receiverName={}",
            response.getSenderId(), response.getSenderName(), response.getReceiverId(), response.getReceiverName());

        // 通过 RabbitMQ 推送给接收方和发送方
        chatMessageProducer.sendChatMessage(receiverId, response);
        chatMessageProducer.sendChatMessage(senderId, response);

        return response;
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getConversation(Long userId1, Long userId2) {
        List<ChatMessage> messages = chatMessageRepository.findConversation(userId1, userId2);
        return messages.stream()
            .map(msg -> {
                String senderName = userRepository.findById(msg.getSenderId()).map(User::getUsername).orElse("Unknown");
                String receiverName = userRepository.findById(msg.getReceiverId()).map(User::getUsername).orElse("Unknown");
                return ChatMessageResponse.fromEntity(msg, senderName, receiverName);
            })
            .collect(Collectors.toList());
    }

    public List<Long> getChatPartners(Long userId) {
        return chatMessageRepository.findChatPartners(userId);
    }

    @Transactional(readOnly = true)
    public List<ChatPartnerResponse> getChatPartnersDetail(Long currentUserId) {
        List<Long> partnerIds = chatMessageRepository.findAllPartnerIds(currentUserId);
        List<ChatPartnerResponse> partners = new ArrayList<>();

        for (Long partnerId : partnerIds) {
            User partner = userRepository.findById(partnerId).orElse(null);
            if (partner == null) continue;

            ChatMessage latestMsg = chatMessageRepository.findLatestMessage(currentUserId, partnerId, PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);
            int unreadCount = chatMessageRepository.countUnreadFromPartner(partnerId, currentUserId);

            ChatPartnerResponse response = ChatPartnerResponse.builder()
                .userId(partnerId)
                .username(partner.getUsername())
                .avatar(partner.getAvatar())
                .lastMessage(latestMsg != null ? latestMsg.getContent() : null)
                .lastMessageTime(latestMsg != null ? latestMsg.getCreatedAt().toString() : null)
                .unreadCount(unreadCount)
                .build();
            partners.add(response);
        }

        partners.sort(Comparator.comparing(
            (ChatPartnerResponse p) -> p.getLastMessageTime() == null ? "" : p.getLastMessageTime()
        ).reversed());

        return partners;
    }

    @Transactional
    public void markAsRead(Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("消息不存在"));
        message.setIsRead(true);
        chatMessageRepository.save(message);
    }

    @Transactional
    public void markConversationAsRead(Long currentUserId, Long partnerId) {
        chatMessageRepository.markConversationAsRead(partnerId, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getUnreadMessages(Long userId) {
        List<ChatMessage> messages = chatMessageRepository.findByReceiverIdAndIsReadFalse(userId);
        return messages.stream()
            .map(msg -> {
                String senderName = userRepository.findById(msg.getSenderId()).map(User::getUsername).orElse("Unknown");
                String receiverName = userRepository.findById(msg.getReceiverId()).map(User::getUsername).orElse("Unknown");
                return ChatMessageResponse.fromEntity(msg, senderName, receiverName);
            })
            .collect(Collectors.toList());
    }
}