package com.aisale.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPointAdjustRequest {

    /** 调整积分（正数加，负数减） */
    private Integer points;

    /** 调整原因 */
    private String reason;
}
