package com.aisale.backend.controller;

import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.dto.CheckInResponse;
import com.aisale.backend.dto.CheckInStatusResponse;
import com.aisale.backend.service.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "用户签到", description = "用户签到相关接口")
@RestController
@RequestMapping("/api/user/checkin")
@RequiredArgsConstructor
public class CheckInController {

    private final CheckInService checkInService;

    @Operation(summary = "用户签到")
    @PostMapping
    public ApiResponse<CheckInResponse> checkIn(@AuthenticationPrincipal UserDetails userDetails) {
        CheckInResponse response = checkInService.checkIn(userDetails.getUsername());
        return ApiResponse.success("签到成功", response);
    }

    @Operation(summary = "获取签到状态")
    @GetMapping("/status")
    public ApiResponse<CheckInStatusResponse> getCheckInStatus(@AuthenticationPrincipal UserDetails userDetails) {
        CheckInStatusResponse response = checkInService.getCheckInStatus(userDetails.getUsername());
        return ApiResponse.success(response);
    }

    @Operation(summary = "获取月度签到日历")
    @GetMapping("/calendar")
    public ApiResponse<List<LocalDate>> getCheckInCalendar(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {
        LocalDate now = LocalDate.now();
        if (year == 0) year = now.getYear();
        if (month == 0) month = now.getMonthValue();
        List<LocalDate> dates = checkInService.getCheckInCalendar(userDetails.getUsername(), year, month);
        return ApiResponse.success(dates);
    }
}
