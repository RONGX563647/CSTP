package com.aisale.backend.controller;

import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.dto.ChangePasswordRequest;
import com.aisale.backend.dto.UpdateProfileRequest;
import com.aisale.backend.dto.UserProfileResponse;
import com.aisale.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@Tag(name = "用户管理", description = "用户信息管理、密码修改、账号注销")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前用户详细信息")
    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        log.debug("获取用户信息 - 用户名：{}", username);

        UserProfileResponse profile = userService.getProfileByUsername(username);
        return ApiResponse.success(profile);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/profile")
    public ApiResponse<UserProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {

        String username = userDetails.getUsername();
        log.info("更新用户信息 - 用户名：{}, 昵称：{}", username, request.getNickname());

        UserProfileResponse profile = userService.updateProfileByUsername(username, request);
        return ApiResponse.success("更新成功", profile);
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        String username = userDetails.getUsername();
        log.info("修改密码 - 用户名：{}", username);

        userService.changePasswordByUsername(username, request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除账号")
    @PostMapping("/delete-account")
    public ApiResponse<Void> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String password) {

        String username = userDetails.getUsername();
        log.info("删除账号 - 用户名：{}", username);

        userService.deleteAccountByUsername(username, password);
        return ApiResponse.success();
    }
}
