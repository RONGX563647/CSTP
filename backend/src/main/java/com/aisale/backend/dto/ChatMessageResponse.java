package com.aisale.backend.dto;

import com.aisale.backend.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private Long id;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private String content;
    private String messageType;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static ChatMessageResponse fromEntity(ChatMessage message, String senderName, String receiverName) {
        return ChatMessageResponse.builder()
            .id(message.getId())
            .senderId(message.getSenderId())
            .senderName(senderName)
            .receiverId(message.getReceiverId())
            .receiverName(receiverName)
            .content(message.getContent())
            .messageType(message.getMessageType().name())
            .isRead(message.getIsRead())
            .createdAt(message.getCreatedAt())
            .build();
    }
}