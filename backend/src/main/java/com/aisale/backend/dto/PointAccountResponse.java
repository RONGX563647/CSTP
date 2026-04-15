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
    /** 签到累计获得积分 */
    private Integer checkInPoints;
    /** 订单奖励累计获得积分 */
    private Integer orderPoints;
}
