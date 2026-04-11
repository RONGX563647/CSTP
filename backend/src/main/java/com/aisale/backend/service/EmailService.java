package com.aisale.backend.service;

import org.springframework.stereotype.Service;

/**
 * 邮箱验证服务 - 预留接口，当前实现返回true
 */
@Service
public class EmailService {

    /**
     * 发送验证码到邮箱
     * @param email 目标邮箱
     * @return 是否发送成功（当前直接返回true）
     */
    public boolean sendVerificationCode(String email) {
        // TODO: 实现邮件发送逻辑
        return true;
    }

    /**
     * 验证邮箱验证码
     * @param email 邮箱地址
     * @param code 验证码
     * @return 是否验证成功（当前直接返回true）
     */
    public boolean verifyCode(String email, String code) {
        // TODO: 实现验证码校验逻辑
        return true;
    }

    /**
     * 发送重置密码邮件
     * @param email 目标邮箱
     * @return 是否发送成功（当前直接返回true）
     */
    public boolean sendResetPasswordEmail(String email) {
        // TODO: 实现重置密码邮件发送
        return true;
    }
}