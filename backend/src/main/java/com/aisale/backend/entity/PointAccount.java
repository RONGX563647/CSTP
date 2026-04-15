package com.aisale.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "point_accounts", indexes = {
    @Index(name = "idx_point_account_user", columnList = "user_id", unique = true)
})
public class PointAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** 总累计积分 */
    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    /** 可用积分（总积分 - 已消费积分） */
    @Column(name = "available_points", nullable = false)
    private Integer availablePoints = 0;

    /** 已消费积分 */
    @Column(name = "used_points", nullable = false)
    private Integer usedPoints = 0;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
