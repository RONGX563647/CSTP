package com.aisale.backend.dto;

import lombok.Data;

import java.util.Map;

/**
 * 日志统计响应
 */
@Data
public class LogStatsResponse {
    
    /**
     * 今日日志数量
     */
    private long todayCount;
    
    /**
     * 本周日志数量
     */
    private long weekCount;
    
    /**
     * 总日志数量
     */
    private long totalCount;
    
    /**
     * 各模块日志统计
     */
    private Map<String, Long> moduleStats;
    
    /**
     * 各状态日志统计
     */
    private Map<String, Long> statusStats;
}
