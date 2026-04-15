package com.aisale.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "check_ins", indexes = {
    @Index(name = "idx_checkin_user_date", columnList = "user_id, check_in_date", unique = true)
})
public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "continuous_days", nullable = false)
    private Integer continuousDays = 1;

    @Column(name = "reward_points")
    private Integer rewardPoints = 0;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
