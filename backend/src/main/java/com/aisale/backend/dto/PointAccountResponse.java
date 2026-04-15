package com.aisale.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointAccountResponse {

    private Long id;
    private Long userId;
    private Integer totalPoints;
    private Integer availablePoints;
    private Integer usedPoints;
    /** 签到累计次数 */
    private Integer checkInCount;
    /** 订单奖励累计次数 */
    private Integer orderCount;
}
