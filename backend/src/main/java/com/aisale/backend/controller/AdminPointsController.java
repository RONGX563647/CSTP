package com.aisale.backend.controller;

import com.aisale.backend.dto.AdminPointAdjustRequest;
import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.dto.PointRecordResponse;
import com.aisale.backend.entity.User;
import com.aisale.backend.exception.business.NotFoundException;
import com.aisale.backend.repository.UserRepository;
import com.aisale.backend.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理员积分管理", description = "管理员积分调整接口")
@RestController
@RequestMapping("/api/admin/points")
@RequiredArgsConstructor
public class AdminPointsController {

    private final PointService pointService;
    private final UserRepository userRepository;

    @Operation(summary = "管理员调整用户积分")
    @PostMapping("/adjust/{userId}")
    public ApiResponse<PointRecordResponse> adjustPoints(
            @AuthenticationPrincipal UserDetails adminDetails,
            @PathVariable Long userId,
            @RequestBody AdminPointAdjustRequest request) {
        // 验证目标用户存在
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("目标用户不存在"));

        PointRecordResponse response = pointService.adminAdjustPoints(targetUser.getId(), request);
        return ApiResponse.success("积分调整成功", response);
    }
}
