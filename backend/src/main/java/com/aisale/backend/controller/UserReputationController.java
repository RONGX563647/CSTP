package com.aisale.backend.controller;

import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.dto.ReputationAccountResponse;
import com.aisale.backend.dto.ReputationRecordResponse;
import com.aisale.backend.service.ReputationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户信誉", description = "用户信誉查询、信誉记录查看")
@RestController
@RequestMapping("/api/user/reputation")
@RequiredArgsConstructor
@Slf4j
public class UserReputationController {

    private final ReputationService reputationService;

    @Operation(summary = "获取我的信誉概览")
    @GetMapping
    public ApiResponse<ReputationAccountResponse> getMyReputation(
            @AuthenticationPrincipal UserDetails userDetails) {

        String username = userDetails.getUsername();
        log.debug("获取用户信誉概览 - 用户名：{}", username);

        ReputationAccountResponse response = reputationService.getAccountOverview(username);
        return ApiResponse.success(response);
    }

    @Operation(summary = "获取信誉流水记录")
    @GetMapping("/records")
    public ApiResponse<Page<ReputationRecordResponse>> getRecords(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String username = userDetails.getUsername();
        log.debug("获取信誉记录 - 用户名：{}, 页码：{}, 大小：{}", username, page, size);

        Page<ReputationRecordResponse> records = reputationService.getRecords(username, page, size);
        return ApiResponse.success(records);
    }

    @Operation(summary = "获取指定用户信誉信息（公开）")
    @GetMapping("/public/{userId}")
    public ApiResponse<ReputationAccountResponse> getUserReputation(@PathVariable Long userId) {
        log.debug("获取用户公开信誉信息 - 用户ID：{}", userId);

        ReputationAccountResponse response = reputationService.getAccountByUserId(userId);
        return ApiResponse.success(response);
    }
}