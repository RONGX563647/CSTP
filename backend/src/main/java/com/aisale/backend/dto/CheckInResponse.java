package com.aisale.backend.dto;

import com.aisale.backend.entity.CheckIn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResponse {

    private Long id;
    private Long userId;
    private LocalDate checkInDate;
    private Integer continuousDays;
    private Integer rewardPoints;
    private Boolean todayCheckedIn;
    private Integer totalCheckInDays;

    public static CheckInResponse fromEntity(CheckIn checkIn, boolean todayCheckedIn, int totalCheckInDays) {
        return CheckInResponse.builder()
                .id(checkIn.getId())
                .userId(checkIn.getUserId())
                .checkInDate(checkIn.getCheckInDate())
                .continuousDays(checkIn.getContinuousDays())
                .rewardPoints(checkIn.getRewardPoints())
                .todayCheckedIn(todayCheckedIn)
                .totalCheckInDays(totalCheckInDays)
                .build();
    }
}
