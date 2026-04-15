package com.aisale.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "point_records", indexes = {
    @Index(name = "idx_point_record_user", columnList = "user_id"),
    @Index(name = "idx_point_record_user_type", columnList = "user_id, type"),
    @Index(name = "idx_point_record_created", columnList = "created_at")
})
public class PointRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 积分变动类型 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PointType type;

    /** 变动积分（正数增加，负数扣减） */
    @Column(name = "points", nullable = false)
    private Integer points;

    /** 变动后可用余额 */
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    /** 关联业务ID（如签到记录ID、订单ID等） */
    @Column(name = "related_id")
    private Long relatedId;

    /** 描述 */
    @Column(length = 200)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 积分变动类型
     */
    public enum PointType {
        /** 签到奖励 */
        CHECK_IN,
        /** 订单完成奖励 */
        ORDER_COMPLETE,
        /** 订单取消扣回 */
        ORDER_CANCEL,
        /** 管理员调整（加） */
        ADMIN_ADD,
        /** 管理员调整（减） */
        ADMIN_DEDUCT,
        /** 积分兑换消费 */
        EXCHANGE
    }
}
