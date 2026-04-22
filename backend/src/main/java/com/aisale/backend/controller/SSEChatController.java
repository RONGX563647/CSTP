package com.aisale.backend.controller;

import com.aisale.backend.service.SSESessionManager;
import com.aisale.backend.util.JwtRequestUtils;
import com.aisale.backend.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "SSE 聊天", description = "Server-Sent Events 聊天消息推送")
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Slf4j
public class SSEChatController {

    private final SSESessionManager sseSessionManager;
    private final JwtRequestUtils jwtRequestUtils;
    private final JwtUtil jwtUtil;

    @Operation(summary = "建立 SSE 连接")
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @RequestParam(required = false) String token,
            HttpServletRequest request) {
        Long userId;
        if (token != null && !token.isEmpty()) {
            userId = jwtUtil.extractUserId(token);
            log.info("SSE connection request from userId={} (via URL param)", userId);
        } else {
            userId = jwtRequestUtils.getCurrentUserId(request);
            log.info("SSE connection request from userId={} (via Header)", userId);
        }
        return sseSessionManager.register(userId);
    }

    @Operation(summary = "断开 SSE 连接")
    @DeleteMapping("/chat")
    public void disconnect(HttpServletRequest request) {
        Long userId = jwtRequestUtils.getCurrentUserId(request);
        log.info("SSE disconnect request from userId={}", userId);
        sseSessionManager.remove(userId);
    }
}