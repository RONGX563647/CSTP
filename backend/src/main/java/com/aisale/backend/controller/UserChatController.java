package com.aisale.backend.controller;

import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.dto.ChatMessageRequest;
import com.aisale.backend.dto.ChatMessageResponse;
import com.aisale.backend.service.ChatService;
import com.aisale.backend.util.JwtRequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "聊天消息", description = "即时通讯消息管理")
@RestController
@RequestMapping("/api/user/chat")
@RequiredArgsConstructor
@Slf4j
public class UserChatController {

    private final ChatService chatService;
    private final JwtRequestUtils jwtRequestUtils;

    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public ApiResponse<ChatMessageResponse> sendMessage(
            @RequestBody ChatMessageRequest request,
            HttpServletRequest httpRequest) {
        Long senderId = jwtRequestUtils.getCurrentUserId(httpRequest);
        log.info("Sending message from {} to {}", senderId, request.getReceiverId());
        ChatMessageResponse response = chatService.sendMessage(senderId, request.getReceiverId(), request.getContent());
        return ApiResponse.success("消息发送成功", response);
    }

    @Operation(summary = "获取与指定用户的聊天记录")
    @GetMapping("/conversation/{userId}")
    public ApiResponse<List<ChatMessageResponse>> getConversation(
            @PathVariable Long userId,
            HttpServletRequest request) {
        Long currentUserId = jwtRequestUtils.getCurrentUserId(request);
        List<ChatMessageResponse> messages = chatService.getConversation(currentUserId, userId);
        return ApiResponse.success(messages);
    }

    @Operation(summary = "获取聊天对象列表")
    @GetMapping("/partners")
    public ApiResponse<List<Long>> getChatPartners(HttpServletRequest request) {
        Long currentUserId = jwtRequestUtils.getCurrentUserId(request);
        List<Long> partners = chatService.getChatPartners(currentUserId);
        return ApiResponse.success(partners);
    }

    @Operation(summary = "获取未读消息")
    @GetMapping("/unread")
    public ApiResponse<List<ChatMessageResponse>> getUnreadMessages(HttpServletRequest request) {
        Long currentUserId = jwtRequestUtils.getCurrentUserId(request);
        List<ChatMessageResponse> messages = chatService.getUnreadMessages(currentUserId);
        return ApiResponse.success(messages);
    }

    @Operation(summary = "标记消息已读")
    @PutMapping("/read/{messageId}")
    public ApiResponse<Void> markAsRead(@PathVariable Long messageId) {
        chatService.markAsRead(messageId);
        return ApiResponse.success();
    }
}