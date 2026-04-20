package com.aisale.backend.service;

import com.aisale.backend.config.RabbitMQConfig;
import com.aisale.backend.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送聊天消息到 RabbitMQ
     */
    public void sendChatMessage(Long targetUserId, ChatMessageResponse message) {
        try {
            ChatMessagePayload payload = new ChatMessagePayload(targetUserId, message);
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                RabbitMQConfig.CHAT_ROUTING_KEY,
                payload
            );
            log.info("Message sent to RabbitMQ: targetUserId={}, messageId={}",
                targetUserId, message.getId());
        } catch (Exception e) {
            log.error("Failed to send message to RabbitMQ: {}", e.getMessage());
        }
    }

    /**
     * 消息载体（内部类）
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ChatMessagePayload {
        private Long targetUserId;
        private ChatMessageResponse message;
    }
}