package com.aisale.backend.dto;

import lombok.Data;

/**
 * 用户管理查询请求 DTO
 */
@Data
public class UserQueryRequest {

    /**
     * 用户名（模糊查询）
     */
    private String username;

    /**
     * 昵称（模糊查询）
     */
    private String nickname;

    /**
     * 邮箱（模糊查询）
     */
    private String email;

    /**
     * 手机号（模糊查询）
     */
    private String phone;

    /**
     * 用户状态
     */
    private String status;
}
