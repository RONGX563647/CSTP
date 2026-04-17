package com.aisale.backend.dto;

import com.aisale.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private User.UserStatus status;
    private Boolean emailVerified;
    private LocalDateTime createdAt;

    // 信誉信息（公开信息）
    private Integer reputationScore;
    private Integer reputationLevel;
    private String reputationLevelName;
    private String reputationLevelIcon;
    private Integer reputationTotalReviews;

    /**
     * 从 User 实体转换为 UserProfileResponse
     */
    public static UserProfileResponse fromUser(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * 从 User 实体和信誉信息转换为 UserProfileResponse
     */
    public static UserProfileResponse fromUserWithReputation(User user, ReputationAccountResponse reputation) {
        UserProfileResponse response = fromUser(user);
        if (reputation != null) {
            response.setReputationScore(reputation.getTotalScore());
            response.setReputationLevel(reputation.getLevel());
            response.setReputationLevelName(reputation.getLevelName());
            response.setReputationLevelIcon(reputation.getLevelIcon());
            response.setReputationTotalReviews(reputation.getTotalReviews());
        }
        return response;
    }
}
