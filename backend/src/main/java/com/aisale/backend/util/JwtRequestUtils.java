package com.aisale.backend.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtRequestUtils {

    private final JwtUtil jwtUtil;

    /**
     * 从请求头获取当前登录用户 ID
     * @throws RuntimeException 当 token 无效或未提供时抛出异常
     */
    public Long getCurrentUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未提供认证令牌");
        }
        String token = authHeader.substring(7);
        try {
            Long userId = jwtUtil.extractUserId(token);
            if (userId == null) {
                throw new RuntimeException("无效的令牌：缺少用户 ID");
            }
            return userId;
        } catch (Exception e) {
            throw new RuntimeException("令牌解析失败：" + e.getMessage());
        }
    }

    /**
     * 从请求头获取当前登录用户名
     * @throws RuntimeException 当 token 无效或未提供时抛出异常
     */
    public String getCurrentUsername(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未提供认证令牌");
        }
        String token = authHeader.substring(7);
        try {
            String username = jwtUtil.extractUsername(token);
            if (username == null) {
                throw new RuntimeException("无效的令牌：缺少用户名");
            }
            return username;
        } catch (Exception e) {
            throw new RuntimeException("令牌解析失败：" + e.getMessage());
        }
    }
}
