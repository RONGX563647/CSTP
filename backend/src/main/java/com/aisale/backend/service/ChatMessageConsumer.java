package com.aisale.backend.service;

import com.aisale.backend.config.RabbitMQConfig;
import com.aisale.backend.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageConsumer {

    private final SSESessionManager sseSessionManager;

    @RabbitListener(queues = RabbitMQConfig.CHAT_QUEUE)
    public void handleChatMessage(ChatMessageProducer.ChatMessagePayload payload) {
        Long targetUserId = payload.getTargetUserId();
        ChatMessageResponse message = payload.getMessage();

        log.info("Received message from RabbitMQ: targetUserId={}, messageId={}", 
            targetUserId, message != null ? message.getId() : "null");

        try {
            sseSessionManager.sendToUser(targetUserId, message);
            log.debug("Message delivered via SSE: targetUserId={}", targetUserId);
        } catch (Exception e) {
            log.error("Failed to deliver message via SSE: targetUserId={}", targetUserId, e);
        }
    }
}