package com.aisale.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信誉账户实体
 * 存储用户的信誉分、评价统计和等级信息
 */
@Data
@Entity
@Table(name = "reputation_accounts", indexes = {
    @Index(name = "idx_reputation_account_user", columnList = "user_id", unique = true)
})
public class ReputationAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** 总信誉分（初始100分） */
    @Column(name = "total_score", nullable = false)
    private Integer totalScore = 100;

    /** 收到的评价总数 */
    @Column(name = "total_reviews", nullable = false)
    private Integer totalReviews = 0;

    /** 好评数（4-5分） */
    @Column(name = "good_reviews", nullable = false)
    private Integer goodReviews = 0;

    /** 中评数（3分） */
    @Column(name = "neutral_reviews", nullable = false)
    private Integer neutralReviews = 0;

    /** 差评数（1-2分） */
    @Column(name = "bad_reviews", nullable = false)
    private Integer badReviews = 0;

    /** 平均评分（1-5） */
    @Column(name = "avg_rating")
    private Double avgRating = 0.0;

    /** 信誉等级（1-5级） */
    @Column(name = "level", nullable = false)
    private Integer level = 3;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 信誉等级枚举
     * 5级（优秀）：avgRating >= 4.8, totalReviews >= 50
     * 4级（良好）：avgRating >= 4.5, totalReviews >= 20
     * 3级（一般）：avgRating >= 4.0 或 无评价
     * 2级（较差）：avgRating < 4.0
     * 1级（极差）：avgRating < 3.0
     */
    public enum ReputationLevel {
        EXCELLENT(5, "优秀", "🌟🌟🌟🌟🌟"),
        GOOD(4, "良好", "🌟🌟🌟🌟"),
        NORMAL(3, "一般", "🌟🌟🌟"),
        POOR(2, "较差", "🌟🌟"),
        BAD(1, "极差", "🌟");

        private final int level;
        private final String name;
        private final String icon;

        ReputationLevel(int level, String name, String icon) {
            this.level = level;
            this.name = name;
            this.icon = icon;
        }

        public int getLevel() {
            return level;
        }

        public String getName() {
            return name;
        }

        public String getIcon() {
            return icon;
        }

        public static ReputationLevel fromLevel(int level) {
            for (ReputationLevel rl : values()) {
                if (rl.level == level) {
                    return rl;
                }
            }
            return NORMAL;
        }

        /**
         * 根据评分和评价数计算等级
         */
        public static ReputationLevel calculate(double avgRating, int totalReviews) {
            if (totalReviews == 0) {
                return NORMAL; // 无评价默认一般
            }
            if (avgRating >= 4.8 && totalReviews >= 50) {
                return EXCELLENT;
            }
            if (avgRating >= 4.5 && totalReviews >= 20) {
                return GOOD;
            }
            if (avgRating < 3.0) {
                return BAD;
            }
            if (avgRating < 4.0) {
                return POOR;
            }
            return NORMAL;
        }
    }
}