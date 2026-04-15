package com.aisale.backend.controller;

import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.dto.PointAccountResponse;
import com.aisale.backend.dto.PointRecordResponse;
import com.aisale.backend.entity.PointRecord;
import com.aisale.backend.service.PointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户积分", description = "用户积分相关接口")
@RestController
@RequestMapping("/api/user/points")
@RequiredArgsConstructor
public class UserPointsController {

    private final PointService pointService;

    @Operation(summary = "获取积分账户概览")
    @GetMapping("/account")
    public ApiResponse<PointAccountResponse> getAccountOverview(
            @AuthenticationPrincipal UserDetails userDetails) {
        PointAccountResponse response = pointService.getAccountOverview(userDetails.getUsername());
        return ApiResponse.success(response);
    }

    @Operation(summary = "获取积分流水记录")
    @GetMapping("/records")
    public ApiResponse<Page<PointRecordResponse>> getPointRecords(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) PointRecord.PointType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PointRecordResponse> records = pointService.getPointRecords(
                userDetails.getUsername(), type, page, size);
        return ApiResponse.success(records);
    }
}
