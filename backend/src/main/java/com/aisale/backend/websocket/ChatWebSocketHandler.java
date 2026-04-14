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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    private final Map<Long, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        String username = (String) session.getAttributes().get("username");
        
        if (userId != null) {
            sessionUserMap.put(session.getId(), userId);
            userSessionMap.put(userId, session);
            log.info("WebSocket connected: userId={}, username={}, sessionId={}", userId, username, session.getId());
        } else {
            log.warn("WebSocket connection rejected: no userId in session");
            session.close(new CloseStatus(1008, "Not accepted"));
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
            log.info("Received message from userId {}: {}", senderId, payload);
            
            ChatMessageRequest request = objectMapper.readValue(payload, ChatMessageRequest.class);
            
            // chatService.sendMessage() 内部会保存消息并通过 sendMessageToUser 推送给接收方
            ChatMessageResponse response = chatService.sendMessage(senderId, request.getReceiverId(), request.getContent());
            
            // 只给发送方回传消息确认
            String responseJson = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(responseJson));
            
        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage());
            session.sendMessage(new TextMessage("{\"error\":\"" + e.getMessage() + "\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = sessionUserMap.remove(session.getId());
        if (userId != null) {
            userSessionMap.remove(userId);
        }
        log.info("WebSocket disconnected: sessionId={}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error: sessionId={}, error={}", session.getId(), exception.getMessage());
    }

    public void sendMessageToUser(Long userId, ChatMessageResponse message) {
        WebSocketSession session = userSessionMap.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                log.error("Failed to send message to userId {}: {}", userId, e.getMessage());
            }
        }
    }
}