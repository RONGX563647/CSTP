package com.aisale.backend.dto;

import com.aisale.backend.entity.PointRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointRecordResponse {

    private Long id;
    private String type;
    private Integer points;
    private Integer balanceAfter;
    private Long relatedId;
    private String description;
    private LocalDateTime createdAt;

    public static PointRecordResponse fromEntity(PointRecord record) {
        return PointRecordResponse.builder()
                .id(record.getId())
                .type(record.getType().name())
                .points(record.getPoints())
                .balanceAfter(record.getBalanceAfter())
                .relatedId(record.getRelatedId())
                .description(record.getDescription())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
