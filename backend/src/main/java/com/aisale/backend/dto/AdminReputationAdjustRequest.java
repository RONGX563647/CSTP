package com.aisale.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员调整信誉请求 DTO
 */
@Data
public class AdminReputationAdjustRequest {

    /** 调整的信誉分（正数为增加，负数为扣减） */
    @NotNull(message = "调整分数不能为空")
    @Min(value = -100, message = "扣减分数不能超过100")
    @Max(value = 100, message = "增加分数不能超过100")
    private Integer score;

    /** 调整原因 */
    @NotBlank(message = "调整原因不能为空")
    private String reason;
}