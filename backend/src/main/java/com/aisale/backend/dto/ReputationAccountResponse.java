package com.aisale.backend.dto;

import com.aisale.backend.entity.ReputationAccount;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 信誉账户响应 DTO
 */
@Data
@Builder
public class ReputationAccountResponse {

    private Long id;

    private Long userId;

    /** 总信誉分 */
    private Integer totalScore;

    /** 评价总数 */
    private Integer totalReviews;

    /** 好评数 */
    private Integer goodReviews;

    /** 中评数 */
    private Integer neutralReviews;

    /** 差评数 */
    private Integer badReviews;

    /** 好评率 */
    private Double goodRate;

    /** 平均评分 */
    private Double avgRating;

    /** 信誉等级 */
    private Integer level;

    /** 等级名称 */
    private String levelName;

    /** 等级图标 */
    private String levelIcon;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 从实体转换
     */
    public static ReputationAccountResponse fromEntity(ReputationAccount account) {
        ReputationAccount.ReputationLevel reputationLevel = ReputationAccount.ReputationLevel.fromLevel(account.getLevel());

        double goodRate = 0.0;
        if (account.getTotalReviews() > 0) {
            goodRate = (account.getGoodReviews() * 100.0) / account.getTotalReviews();
        }

        return ReputationAccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .totalScore(account.getTotalScore())
                .totalReviews(account.getTotalReviews())
                .goodReviews(account.getGoodReviews())
                .neutralReviews(account.getNeutralReviews())
                .badReviews(account.getBadReviews())
                .goodRate(goodRate)
                .avgRating(account.getAvgRating())
                .level(account.getLevel())
                .levelName(reputationLevel.getName())
                .levelIcon(reputationLevel.getIcon())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}