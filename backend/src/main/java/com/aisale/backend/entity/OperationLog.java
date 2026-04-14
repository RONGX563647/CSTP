package com.aisale.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 * 记录系统中所有用户和管理员的操作行为
 */
@Data
@Entity
@Table(name = "operation_logs", indexes = {
    @Index(name = "idx_operator_id", columnList = "operatorId"),
    @Index(name = "idx_module", columnList = "module"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_create_time", columnList = "createTime"),
    @Index(name = "idx_operator_role", columnList = "operatorRole")
})
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 操作模块：如 USER, ORDER, PRODUCT, ADMIN 等
     */
    @Column(nullable = false, length = 50)
    private String module;

    /**
     * 操作类型：如 LOGIN, LOGOUT, CREATE, UPDATE, DELETE, VIEW 等
     */
    @Column(nullable = false, length = 50)
    private String action;

    /**
     * 操作人ID
     */
    @Column(name = "operator_id")
    private Long operatorId;

    /**
     * 操作人用户名
     */
    @Column(name = "operator_name", length = 100)
    private String operatorName;

    /**
     * 操作人角色
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operator_role", length = 20)
    private OperatorRole operatorRole;

    /**
     * 操作描述
     */
    @Column(length = 500)
    private String description;

    /**
     * 请求方法
     */
    @Column(length = 10)
    private String requestMethod;

    /**
     * 请求URL
     */
    @Column(length = 500)
    private String requestUrl;

    /**
     * 请求参数
     */
    @Column(columnDefinition = "TEXT")
    private String requestParams;

    /**
     * 响应状态：SUCCESS, FAIL
     */
    @Column(length = 20)
    private String status;

    /**
     * 响应消息
     */
    @Column(length = 500)
    private String message;

    /**
     * 操作耗时（毫秒）
     */
    private Long duration;

    /**
     * 操作IP地址
     */
    @Column(length = 50)
    private String ip;

    /**
     * 用户代理
     */
    @Column(length = 500)
    private String userAgent;

    /**
     * 关联业务ID（如订单ID、用户ID等）
     */
    @Column(name = "biz_id")
    private Long bizId;

    /**
     * 关联业务类型
     */
    @Column(name = "biz_type", length = 50)
    private String bizType;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
    }

    /**
     * 操作人角色枚举
     */
    public enum OperatorRole {
        BUYER,    // 买家
        SELLER,   // 卖家
        ADMIN,   // 管理员
        SYSTEM   // 系统
    }

    /**
     * 操作模块枚举
     */
    public enum Module {
        USER("用户模块"),
        PRODUCT("商品模块"),
        ORDER("订单模块"),
        ADDRESS("地址模块"),
        CHAT("聊天模块"),
        AUTH("认证模块"),
        ADMIN("管理模块"),
        SYSTEM("系统模块");

        private final String description;

        Module(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 操作类型枚举
     */
    public enum ActionType {
        LOGIN("登录"),
        LOGOUT("登出"),
        REGISTER("注册"),
        CREATE("创建"),
        UPDATE("更新"),
        DELETE("删除"),
        VIEW("查看"),
        QUERY("查询"),
        EXPORT("导出"),
        IMPORT("导入"),
        ENABLE("启用"),
        DISABLE("禁用"),
        APPROVE("审批通过"),
        REJECT("审批拒绝"),
        CANCEL("取消"),
        REFUND("退款"),
        SHIP("发货"),
        CONFIRM("确认");

        private final String description;

        ActionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
