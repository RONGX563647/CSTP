package com.aisale.backend.controller;

import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.dto.LogQueryRequest;
import com.aisale.backend.dto.LogStatsResponse;
import com.aisale.backend.entity.OperationLog;
import com.aisale.backend.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端日志控制器
 */
@Tag(name = "管理端日志", description = "系统操作日志管理")
@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final OperationLogService operationLogService;

    @Operation(summary = "获取日志列表（分页）")
    @GetMapping
    public ApiResponse<Page<OperationLog>> getLogList(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "操作人ID") @RequestParam(required = false) Long operatorId,
            @Parameter(description = "操作人角色") @RequestParam(required = false) OperationLog.OperatorRole operatorRole,
            @Parameter(description = "模块") @RequestParam(required = false) String module,
            @Parameter(description = "操作类型") @RequestParam(required = false) String action,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "操作人用户名") @RequestParam(required = false) String operatorName,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        
        LogQueryRequest request = new LogQueryRequest();
        request.setPage(page);
        request.setSize(size);
        request.setOperatorId(operatorId);
        request.setOperatorRole(operatorRole);
        request.setModule(module);
        request.setAction(action);
        request.setStatus(status);
        request.setOperatorName(operatorName);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setKeyword(keyword);
        
        Page<OperationLog> logs = operationLogService.queryLogs(request);
        return ApiResponse.success(logs);
    }

    @Operation(summary = "获取日志详情")
    @GetMapping("/{id}")
    public ApiResponse<OperationLog> getLogById(@PathVariable Long id) {
        OperationLog log = operationLogService.getLogById(id);
        if (log == null) {
            return ApiResponse.error(404, "日志不存在");
        }
        return ApiResponse.success(log);
    }

    @Operation(summary = "获取日志统计")
    @GetMapping("/stats")
    public ApiResponse<LogStatsResponse> getLogStats() {
        OperationLogService.LogStats stats = operationLogService.getLogStats();
        
        LogStatsResponse response = new LogStatsResponse();
        response.setTodayCount(stats.getTodayCount());
        response.setWeekCount(stats.getWeekCount());
        response.setTotalCount(stats.getTotalCount());
        response.setModuleStats(stats.getModuleStats());
        response.setStatusStats(stats.getStatusStats());
        
        return ApiResponse.success(response);
    }

    @Operation(summary = "获取最近日志")
    @GetMapping("/recent")
    public ApiResponse<List<OperationLog>> getRecentLogs(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") int limit) {
        List<OperationLog> logs = operationLogService.getRecentLogs(limit);
        return ApiResponse.success(logs);
    }

    @Operation(summary = "删除指定时间之前的日志")
    @DeleteMapping("/cleanup")
    public ApiResponse<String> cleanupOldLogs(
            @Parameter(description = "删除此时间之前的日志") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before) {
        long count = operationLogService.deleteOldLogs(before);
        return ApiResponse.success("已删除 " + count + " 条日志");
    }

    @Operation(summary = "获取模块列表")
    @GetMapping("/modules")
    public ApiResponse<String[]> getModules() {
        OperationLog.Module[] modules = OperationLog.Module.values();
        String[] moduleNames = new String[modules.length];
        for (int i = 0; i < modules.length; i++) {
            moduleNames[i] = modules[i].name();
        }
        return ApiResponse.success(moduleNames);
    }

    @Operation(summary = "获取操作类型列表")
    @GetMapping("/actions")
    public ApiResponse<String[]> getActions(
            @Parameter(description = "模块名称") @RequestParam(required = false) String module) {
        OperationLog.ActionType[] actions = OperationLog.ActionType.values();
        String[] actionNames = new String[actions.length];
        for (int i = 0; i < actions.length; i++) {
            actionNames[i] = actions[i].name();
        }
        return ApiResponse.success(actionNames);
    }
}
