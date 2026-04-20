package com.aisale.backend.service;

import com.aisale.backend.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageConsumer {

    private final SSESessionManager sseSessionManager;

    /**
     * 监听聊天消息队列
     */
    @RabbitListener(queues = RabbitMQConfig.CHAT_QUEUE)
    public void handleChatMessage(ChatMessageProducer.ChatMessagePayload payload) {
        Long targetUserId = payload.getTargetUserId();
        Object message = payload.getMessage();

        log.info("Received message from RabbitMQ: targetUserId={}", targetUserId);

        try {
            // 通过 SSE 推送给在线用户
            sseSessionManager.sendToUser(targetUserId, message);
            log.debug("Message delivered via SSE: targetUserId={}", targetUserId);
        } catch (Exception e) {
            // 异常时 ACK 消息（不重新入队），用户可以从数据库获取历史消息
            log.error("Failed to deliver message via SSE: targetUserId={}", targetUserId, e);
        }
    }
}