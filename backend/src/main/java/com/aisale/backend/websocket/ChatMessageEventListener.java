package com.aisale.backend.websocket;

import com.aisale.backend.service.ChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageEventListener {

    private final ChatWebSocketHandler chatWebSocketHandler;

    @EventListener
    public void handleChatMessageEvent(ChatMessageEvent event) {
        try {
            log.info("Event received: pushing message id={} from senderId={} to userId={}", 
                event.getMessage().getId(), event.getMessage().getSenderId(), event.getTargetUserId());
            chatWebSocketHandler.sendMessageToUser(event.getTargetUserId(), event.getMessage());
            log.debug("Pushed message to userId {} via WebSocket event", event.getTargetUserId());
        } catch (Exception e) {
            log.error("Failed to push message to userId {} via WebSocket event: {}",
                event.getTargetUserId(), e.getMessage());
        }
    }
}
