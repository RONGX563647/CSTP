package com.aisale.backend.websocket;

import com.aisale.backend.dto.ChatMessageRequest;
import com.aisale.backend.dto.ChatMessageResponse;
import com.aisale.backend.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    // session.id -> userId
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    // userId -> 该用户的所有活跃 WebSocket 会话（支持多标签页）
    private final Map<Long, List<WebSocketSession>> userSessionsMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");

        if (userId != null) {
            sessionUserMap.put(session.getId(), userId);
            userSessionsMap.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(session);
            log.info("WebSocket connected: userId={}, username={}, sessionId={}", userId, username, session.getId());
        } else {
            log.warn("WebSocket connection rejected: no userId in session attributes");
            session.close(new CloseStatus(1008, "Not authenticated"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long senderId = sessionUserMap.get(session.getId());
        if (senderId == null) {
            log.warn("Message from unknown session: {}", session.getId());
            return;
        }

        try {
            String payload = message.getPayload();
            log.info("Received WebSocket message from userId {}: {}", senderId, payload);

            ChatMessageRequest request = objectMapper.readValue(payload, ChatMessageRequest.class);

            // 参数校验
            if (request.getReceiverId() == null) {
                session.sendMessage(new TextMessage("{\"error\":\"receiverId is required\"}"));
                return;
            }
            if (request.getContent() == null || request.getContent().isBlank()) {
                session.sendMessage(new TextMessage("{\"error\":\"content is required\"}"));
                return;
            }
            // 不能给自己发消息
            if (request.getReceiverId().equals(senderId)) {
                session.sendMessage(new TextMessage("{\"error\":\"cannot send message to yourself\"}"));
                return;
            }

            // 通过 ChatService 发送消息（保存到数据库 + 通过 Spring Event 推送 WebSocket）
            // 注意：返回值主要用于日志记录，实际推送由事件机制完成
            @SuppressWarnings("unused")
            ChatMessageResponse response = chatService.sendMessage(senderId, request.getReceiverId(), request.getContent());
            log.debug("Message sent successfully, response id: {}", response != null ? response.getId() : "null");

        } catch (Exception e) {
            log.error("Error handling WebSocket message from userId {}: {}", senderId, e.getMessage());
            try {
                String errorMsg = e.getMessage() != null ? e.getMessage().replace("\"", "'") : "Unknown error";
                session.sendMessage(new TextMessage("{\"error\":\"" + errorMsg + "\"}"));
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = sessionUserMap.remove(session.getId());
        if (userId != null) {
            List<WebSocketSession> sessions = userSessionsMap.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessionsMap.remove(userId);
                }
            }
        }
        log.info("WebSocket disconnected: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: sessionId={}, error={}", session.getId(), exception.getMessage());
    }

    /**
     * 向指定用户的所有活跃 WebSocket 会话推送消息
     */
    public void sendMessageToUser(Long userId, ChatMessageResponse message) {
        List<WebSocketSession> sessions = userSessionsMap.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("No active WebSocket sessions for userId {}, message stored for later", userId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            log.info("Sending WebSocket message to userId {}: {}", userId, json);
            TextMessage textMessage = new TextMessage(json);

            int successCount = 0;
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(textMessage);
                        successCount++;
                    } catch (Exception e) {
                        log.error("Failed to send WebSocket message to userId {} sessionId {}: {}",
                            userId, session.getId(), e.getMessage());
                    }
                }
            }
            log.info("WebSocket message sent to {} sessions for userId {}", successCount, userId);
        } catch (Exception e) {
            log.error("Failed to serialize message for userId {}: {}", userId, e.getMessage());
        }
    }

    /**
     * 检查用户是否有活跃的 WebSocket 连接
     */
    public boolean isUserOnline(Long userId) {
        List<WebSocketSession> sessions = userSessionsMap.get(userId);
        return sessions != null && sessions.stream().anyMatch(WebSocketSession::isOpen);
    }
}
