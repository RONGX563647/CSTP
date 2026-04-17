package com.aisale.backend.controller;

import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.dto.ReputationAccountResponse;
import com.aisale.backend.dto.ReputationRecordResponse;
import com.aisale.backend.dto.AdminReputationAdjustRequest;
import com.aisale.backend.dto.UserAdminResponse;
import com.aisale.backend.dto.UserQueryRequest;
import com.aisale.backend.entity.User;
import com.aisale.backend.service.AdminUserService;
import com.aisale.backend.service.ReputationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端用户管理控制器
 */
@Tag(name = "管理端用户", description = "管理员管理用户")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final ReputationService reputationService;

    @Operation(summary = "获取用户列表")
    @GetMapping
    public ApiResponse<Page<UserAdminResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            UserQueryRequest queryRequest) {
        Page<UserAdminResponse> users = adminUserService.getAllUsers(page, size, queryRequest);
        return ApiResponse.success(users);
    }

    @Operation(summary = "获取用户详情")
    @GetMapping("/{id}")
    public ApiResponse<UserAdminResponse> getUserById(@PathVariable Long id) {
        UserAdminResponse user = adminUserService.getUserById(id);
        return ApiResponse.success(user);
    }

    @Operation(summary = "更新用户状态")
    @PutMapping("/{id}/status")
    public ApiResponse<UserAdminResponse> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        // 验证并解析状态参数
        User.UserStatus userStatus;
        try {
            userStatus = User.UserStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的用户状态: " + status + ", 可选值: ACTIVE, INACTIVE, BANNED");
        }

        UserAdminResponse user = adminUserService.updateUserStatus(id, userStatus);
        return ApiResponse.success("用户状态已更新", user);
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/{id}/reset-password")
    public ApiResponse<String> resetPassword(
            @PathVariable Long id,
            @RequestParam String newPassword) {
        adminUserService.resetPassword(id, newPassword);
        return ApiResponse.success("密码已重置");
    }

    @Operation(summary = "删除用户（软删除）")
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ApiResponse.success("用户已删除");
    }

    @Operation(summary = "获取用户统计")
    @GetMapping("/stats")
    public ApiResponse<AdminUserService.UserStats> getUserStats() {
        AdminUserService.UserStats stats = adminUserService.getUserStats();
        return ApiResponse.success(stats);
    }

    @Operation(summary = "获取用户订单统计")
    @GetMapping("/{id}/order-stats")
    public ApiResponse<AdminUserService.UserOrderStats> getUserOrderStats(@PathVariable Long id) {
        AdminUserService.UserOrderStats stats = adminUserService.getUserOrderStats(id);
        return ApiResponse.success(stats);
    }

    @Operation(summary = "获取用户信誉详情")
    @GetMapping("/{id}/reputation")
    public ApiResponse<ReputationAccountResponse> getUserReputation(@PathVariable Long id) {
        ReputationAccountResponse reputation = reputationService.getAccountByUserId(id);
        return ApiResponse.success(reputation);
    }

    @Operation(summary = "调整用户信誉分")
    @PostMapping("/{id}/reputation/adjust")
    public ApiResponse<ReputationRecordResponse> adjustUserReputation(
            @PathVariable Long id,
            @Valid @RequestBody AdminReputationAdjustRequest request) {
        ReputationRecordResponse record = reputationService.adminAdjust(id, request);
        return ApiResponse.success("信誉已调整", record);
    }
}
