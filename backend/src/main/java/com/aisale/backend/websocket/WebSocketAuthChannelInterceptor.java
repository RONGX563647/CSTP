package com.aisale.backend.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor == null) {
            return message;
        }
        
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            
            if (sessionAttributes != null && sessionAttributes.containsKey("userId")) {
                Long userId = (Long) sessionAttributes.get("userId");
                String username = (String) sessionAttributes.get("username");
                
                accessor.setUser(new WebSocketPrincipal(userId, username));
                log.info("STOMP CONNECT: userId={}, username={}", userId, username);
            } else {
                log.warn("STOMP CONNECT rejected: no user info in session");
                return null;
            }
        }
        
        return message;
    }
}