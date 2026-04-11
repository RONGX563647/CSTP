package com.aisale.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String role;

    public AuthResponse(String token, Long id, String username, String nickname, String avatar, String role) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.avatar = avatar;
        this.role = role;
    }
}