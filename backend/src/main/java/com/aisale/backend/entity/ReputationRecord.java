package com.aisale.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 信誉流水记录实体
 * 记录用户信誉分变化的详细历史
 */
@Data
@Entity
@Table(name = "reputation_records", indexes = {
    @Index(name = "idx_reputation_record_user", columnList = "user_id"),
    @Index(name = "idx_reputation_record_time", columnList = "created_at")
})
public class ReputationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 关联评价ID */
    @Column(name = "review_id")
    private Long reviewId;

    /** 记录类型 */
    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false)
    private RecordType type;

    /** 信誉变化（正/负） */
    @Column(name = "score_change", nullable = false)
    private Integer scoreChange;

    /** 变化后信誉分 */
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    /** 本次评分（如果来自评价） */
    private Integer rating;

    /** 评价内容摘要 */
    @Column(length = 200)
    private String content;

    /** 描述说明 */
    @Column(length = 500)
    private String description;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    /**
     * 记录类型枚举
     */
    public enum RecordType {
        /** 收到新评价 */
        REVIEW_ADD,
        /** 评价被回复 */
        REVIEW_REPLY,
        /** 管理员调整 */
        ADMIN_ADJUST
    }
}