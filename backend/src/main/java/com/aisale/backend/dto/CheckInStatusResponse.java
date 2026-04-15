package com.aisale.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInStatusResponse {

    private Boolean todayCheckedIn;
    private Integer continuousDays;
    private Integer totalCheckInDays;
    private LocalDate lastCheckInDate;
    private List<LocalDate> recentCheckInDates;
}
