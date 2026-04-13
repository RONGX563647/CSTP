package com.aisale.backend.websocket;

import java.security.Principal;

public class WebSocketPrincipal implements Principal {

    private final Long userId;
    private final String username;

    public WebSocketPrincipal(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}