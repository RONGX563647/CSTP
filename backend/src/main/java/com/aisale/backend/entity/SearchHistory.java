package com.aisale.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "search_history")
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String keyword;

    @Column(name = "search_count")
    private Integer searchCount = 1;

    @Column(name = "last_search_time")
    private LocalDateTime lastSearchTime;

    @PrePersist
    protected void onCreate() {
        if (lastSearchTime == null) {
            lastSearchTime = LocalDateTime.now();
        }
    }
}