package com.aisale.backend.service;

import com.aisale.backend.dto.AuthResponse;
import com.aisale.backend.dto.LoginRequest;
import com.aisale.backend.dto.RegisterRequest;
import com.aisale.backend.entity.User;
import com.aisale.backend.repository.UserRepository;
import com.aisale.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }

        // 检查手机号是否已存在
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("手机号已被注册");
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());

        // 邮箱验证（预留，当前直接返回true）
        if (request.getEmail() != null) {
            boolean emailVerified = emailService.verifyCode(request.getEmail(), "any-code");
            user.setEmailVerified(emailVerified);
        }

        user = userRepository.save(user);

        // 生成JWT
        String token = jwtUtil.generateToken(user.getUsername(), "USER", user.getId());

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role("USER")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        if (user.getStatus() == User.UserStatus.BANNED) {
            throw new RuntimeException("账号已被禁用");
        }

        if (user.getStatus() == User.UserStatus.INACTIVE) {
            throw new RuntimeException("账号未激活");
        }

        String token = jwtUtil.generateToken(user.getUsername(), "USER", user.getId());

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .role("USER")
                .build();
    }

    public User getCurrentUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public boolean sendEmailVerification(String email) {
        return emailService.sendVerificationCode(email);
    }
}