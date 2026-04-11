package com.aisale.backend.controller;

import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.dto.AuthResponse;
import com.aisale.backend.dto.LoginRequest;
import com.aisale.backend.entity.Admin;
import com.aisale.backend.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员认证", description = "管理员登录相关接口")
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = adminAuthService.login(request);
            return ApiResponse.success("登录成功", response);
        } catch (RuntimeException e) {
            return ApiResponse.error(401, e.getMessage());
        }
    }

    @Operation(summary = "获取当前管理员信息")
    @GetMapping("/me")
    public ApiResponse<Admin> getCurrentAdmin(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Admin admin = adminAuthService.getCurrentAdmin(userDetails.getUsername());
            return ApiResponse.success(admin);
        } catch (RuntimeException e) {
            return ApiResponse.error(404, e.getMessage());
        }
    }

    @Operation(summary = "创建管理员账号（超级管理员权限）")
    @PostMapping("/create")
    public ApiResponse<Admin> createAdmin(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String nickname,
            @RequestParam(defaultValue = "ADMIN") String role,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Admin.AdminRole adminRole = Admin.AdminRole.valueOf(role.toUpperCase());
            Admin admin = adminAuthService.createAdmin(username, password, email, nickname, adminRole);
            return ApiResponse.success("创建成功", admin);
        } catch (RuntimeException e) {
            return ApiResponse.error(400, e.getMessage());
        }
    }
}