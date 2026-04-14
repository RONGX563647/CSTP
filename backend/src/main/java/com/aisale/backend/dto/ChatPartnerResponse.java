package com.aisale.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatPartnerResponse {
    private Long userId;
    private String username;
    private String avatar;
    private String lastMessage;
    private String lastMessageTime;
    private Integer unreadCount;
}
