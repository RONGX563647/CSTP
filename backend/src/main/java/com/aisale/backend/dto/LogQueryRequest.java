package com.aisale.backend.dto;

import com.aisale.backend.entity.OperationLog;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志查询请求参数
 */
@Data
public class LogQueryRequest {
    
    /**
     * 页码（从0开始）
     */
    private int page = 0;
    
    /**
     * 每页大小
     */
    private int size = 10;
    
    /**
     * 操作人ID
     */
    private Long operatorId;
    
    /**
     * 操作人角色
     */
    private OperationLog.OperatorRole operatorRole;
    
    /**
     * 模块
     */
    private String module;
    
    /**
     * 操作类型
     */
    private String action;
    
    /**
     * 状态：SUCCESS, FAIL
     */
    private String status;
    
    /**
     * 操作人用户名（模糊查询）
     */
    private String operatorName;
    
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 关键词（模糊匹配描述和URL）
     */
    private String keyword;
}
