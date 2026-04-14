package com.aisale.backend.service;

import com.aisale.backend.dto.ChatMessageResponse;
import com.aisale.backend.entity.ChatMessage;
import com.aisale.backend.entity.User;
import com.aisale.backend.repository.ChatMessageRepository;
import com.aisale.backend.repository.UserRepository;
import com.aisale.backend.websocket.ChatWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ApplicationContext applicationContext;

    private ChatWebSocketHandler webSocketHandler;

    private ChatWebSocketHandler getWebSocketHandler() {
        if (webSocketHandler == null) {
            webSocketHandler = applicationContext.getBean(ChatWebSocketHandler.class);
        }
        return webSocketHandler;
    }

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
        log.info("Message saved: id={}, from={}, to={}", savedMessage.getId(), senderId, receiverId);

        ChatMessageResponse response = ChatMessageResponse.fromEntity(savedMessage, sender.getUsername(), receiver.getUsername());

        // 推送消息给接收方
        getWebSocketHandler().sendMessageToUser(receiverId, response);
        log.info("Message sent to receiver {} via WebSocket", receiverId);

        return response;
    }

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

    @Transactional
    public void markAsRead(Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("消息不存在"));
        message.setIsRead(true);
        chatMessageRepository.save(message);
    }

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