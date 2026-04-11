package com.aisale.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "admins")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true)
    private String email;

    private String nickname;

    private String avatar;

    @Enumerated(EnumType.STRING)
    private AdminRole role = AdminRole.ADMIN;

    @Enumerated(EnumType.STRING)
    private AdminStatus status = AdminStatus.ACTIVE;

    private LocalDateTime lastLoginAt;

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

    public enum AdminRole {
        SUPER_ADMIN, ADMIN
    }

    public enum AdminStatus {
        ACTIVE, INACTIVE
    }
}