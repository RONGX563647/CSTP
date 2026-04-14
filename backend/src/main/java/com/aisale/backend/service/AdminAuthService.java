package com.aisale.backend.service;

import com.aisale.backend.dto.AuthResponse;
import com.aisale.backend.dto.LoginRequest;
import com.aisale.backend.entity.Admin;
import com.aisale.backend.repository.AdminRepository;
import com.aisale.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("管理员账号不存在"));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        if (admin.getStatus() == Admin.AdminStatus.INACTIVE) {
            throw new RuntimeException("账号已停用");
        }

        String role = admin.getRole() == Admin.AdminRole.SUPER_ADMIN ? "SUPER_ADMIN" : "ADMIN";
        String token = jwtUtil.generateToken(admin.getUsername(), role, admin.getId());

        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .id(admin.getId())
                .username(admin.getUsername())
                .nickname(admin.getNickname())
                .avatar(admin.getAvatar())
                .role(role)
                .build();
    }

    public Admin getCurrentAdmin(String username) {
        return adminRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));
    }

    @Transactional
    public Admin createAdmin(String username, String password, String email, String nickname, Admin.AdminRole role) {
        if (adminRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        if (email != null && adminRepository.existsByEmail(email)) {
            throw new RuntimeException("邮箱已存在");
        }

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setEmail(email);
        admin.setNickname(nickname != null ? nickname : username);
        admin.setRole(role);

        return adminRepository.save(admin);
    }
}