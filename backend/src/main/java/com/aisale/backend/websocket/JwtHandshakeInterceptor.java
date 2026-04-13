package com.aisale.backend.websocket;

import com.aisale.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        
        if (token == null) {
            log.warn("WebSocket handshake failed: no token provided");
            return false;
        }
        
        try {
            String username = jwtUtil.extractUsername(token);
            
            if (jwtUtil.isTokenExpired(token)) {
                log.warn("WebSocket handshake failed: token expired");
                return false;
            }
            
            Long userId = jwtUtil.extractUserId(token);
            String role = jwtUtil.extractRole(token);
            
            attributes.put("username", username);
            attributes.put("userId", userId);
            attributes.put("role", role);
            
            log.info("WebSocket handshake success: user={}, userId={}", username, userId);
            return true;
        } catch (Exception e) {
            log.error("WebSocket handshake error: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake exception: {}", exception.getMessage());
        }
    }

    private String extractToken(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    String token = param.substring(6);
                    if (token.startsWith("Bearer ")) {
                        token = token.substring(7);
                    }
                    return token;
                }
            }
        }
        return null;
    }
}