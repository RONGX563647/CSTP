package com.aisale.backend.websocket;

import com.aisale.backend.dto.ChatMessageRequest;
import com.aisale.backend.dto.ChatMessageResponse;
import com.aisale.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketMessageController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public ChatMessageResponse sendMessage(@Payload ChatMessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        
        if (principal == null) {
            log.warn("Unauthorized message attempt");
            throw new RuntimeException("未授权的消息发送");
        }
        
        Long senderId = Long.parseLong(principal.getName());
        log.info("WebSocket message received: sender={}, receiver={}", senderId, request.getReceiverId());
        
        return chatService.sendMessage(senderId, request.getReceiverId(), request.getContent());
    }
}