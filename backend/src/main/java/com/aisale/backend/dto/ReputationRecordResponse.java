package com.aisale.backend.dto;

import com.aisale.backend.entity.ReputationRecord;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 信誉记录响应 DTO
 */
@Data
public class ReputationRecordResponse {

    private Long id;

    private Long userId;

    private Long reviewId;

    private String type;

    private Integer scoreChange;

    private Integer balanceAfter;

    private Integer rating;

    private String content;

    private String description;

    private LocalDateTime createdAt;

    /**
     * 从实体转换
     */
    public static ReputationRecordResponse fromEntity(ReputationRecord record) {
        ReputationRecordResponse response = new ReputationRecordResponse();
        response.setId(record.getId());
        response.setUserId(record.getUserId());
        response.setReviewId(record.getReviewId());
        response.setType(record.getType().name());
        response.setScoreChange(record.getScoreChange());
        response.setBalanceAfter(record.getBalanceAfter());
        response.setRating(record.getRating());
        response.setContent(record.getContent());
        response.setDescription(record.getDescription());
        response.setCreatedAt(record.getCreatedAt());
        return response;
    }
}