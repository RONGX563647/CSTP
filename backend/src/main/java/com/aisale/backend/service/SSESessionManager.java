package com.aisale.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SSESessionManager {

    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 注册用户 SSE 连接
     */
    public SseEmitter register(Long userId) {
        // 30分钟超时
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        // 先完成旧连接，防止竞态条件
        SseEmitter oldEmitter = emitters.put(userId, emitter);
        if (oldEmitter != null) {
            oldEmitter.complete();
            log.info("SSE connection replaced for userId={}", userId);
        }

        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.info("SSE connection completed for userId={}", userId);
        });

        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.info("SSE connection timeout for userId={}", userId);
        });

        emitter.onError(e -> {
            emitters.remove(userId);
            log.error("SSE connection error for userId={}: {}", userId, e.getMessage());
        });

        log.info("SSE registered for userId={}", userId);
        
        // 发送连接确认消息
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("{\"userId\":" + userId + ",\"status\":\"connected\"}"));
            log.debug("Connection confirmation sent to userId={}", userId);
        } catch (IOException e) {
            log.error("Failed to send connection confirmation to userId={}: {}", userId, e.getMessage());
        }
        
        return emitter;
    }

    /**
     * 向指定用户发送消息
     */
    public void sendToUser(Long userId, Object message) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("chat")
                    .data(message));
                log.debug("SSE message sent to userId={}", userId);
            } catch (IOException | IllegalStateException e) {
                log.error("SSE send failed for userId={}: {}", userId, e.getMessage());
                emitters.remove(userId);
            }
        } else {
            log.debug("User {} is offline, message will be stored", userId);
        }
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        return emitters.containsKey(userId);
    }

    /**
     * 移除用户连接
     */
    public void remove(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE connection removed for userId={}", userId);
        }
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineCount() {
        return emitters.size();
    }
}