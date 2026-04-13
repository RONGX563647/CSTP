package com.aisale.backend.service;

import cn.hutool.core.bean.BeanUtil;
import com.aisale.backend.dto.ChangePasswordRequest;
import com.aisale.backend.dto.UpdateProfileRequest;
import com.aisale.backend.dto.UserProfileResponse;
import com.aisale.backend.entity.Order;
import com.aisale.backend.entity.User;
import com.aisale.backend.exception.business.ConflictException;
import com.aisale.backend.exception.business.NotFoundException;
import com.aisale.backend.exception.business.UnauthorizedException;
import com.aisale.backend.repository.OrderRepository;
import com.aisale.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 根据 ID 获取用户详细信息
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        return UserProfileResponse.fromUser(user);
    }

    /**
     * 根据用户名获取用户详细信息
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        return UserProfileResponse.fromUser(user);
    }

    /**
     * 根据 ID 更新用户信息
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        return updateUserFields(user, request);
    }

    /**
     * 根据用户名更新用户信息
     */
    @Transactional
    public UserProfileResponse updateProfileByUsername(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        return updateUserFields(user, request);
    }

    /**
     * 更新用户字段的公共方法
     */
    private UserProfileResponse updateUserFields(User user, UpdateProfileRequest request) {
        // 检查邮箱是否已被其他用户使用
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), user.getId())) {
                throw new ConflictException("邮箱已被其他账号使用");
            }
            user.setEmail(request.getEmail());
            // 邮箱变更，重置验证状态
            user.setEmailVerified(false);
        }

        // 检查手机号是否已被其他用户使用
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhoneAndIdNot(request.getPhone(), user.getId())) {
                throw new ConflictException("手机号已被其他账号使用");
            }
            user.setPhone(request.getPhone());
        }

        // 更新其他字段
        user.setNickname(request.getNickname());
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        User updatedUser = userRepository.save(user);

        log.info("用户信息更新成功 - 用户 ID: {}, 昵称：{}", updatedUser.getId(), updatedUser.getNickname());

        return UserProfileResponse.fromUser(updatedUser);
    }

    /**
     * 根据 ID 修改密码
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        changePasswordInternal(user, request);
    }

    /**
     * 根据用户名修改密码
     */
    @Transactional
    public void changePasswordByUsername(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        changePasswordInternal(user, request);
    }

    /**
     * 修改密码的内部方法
     */
    private void changePasswordInternal(User user, ChangePasswordRequest request) {
        // 验证原密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new UnauthorizedException("原密码错误");
        }

        // 检查新密码是否与原密码相同
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ConflictException("新密码不能与原密码相同");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("用户密码修改成功 - 用户 ID: {}", user.getId());
    }

    /**
     * 根据 ID 删除账号（软删除）
     * 检查是否有未完成的订单
     */
    @Transactional
    public void deleteAccount(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        deleteAccountInternal(user, password);
    }

    /**
     * 根据用户名删除账号（软删除）
     * 检查是否有未完成的订单
     */
    @Transactional
    public void deleteAccountByUsername(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        deleteAccountInternal(user, password);
    }

    /**
     * 删除账号的内部方法
     */
    private void deleteAccountInternal(User user, String password) {
        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new UnauthorizedException("密码错误");
        }

        // 检查是否有未完成的订单
        List<Order.OrderStatus> activeStatuses = List.of(
                Order.OrderStatus.PENDING_PAYMENT,
                Order.OrderStatus.PENDING_PICKUP,
                Order.OrderStatus.PENDING_CONFIRM,
                Order.OrderStatus.PENDING_REVIEW
        );

        for (Order.OrderStatus status : activeStatuses) {
            Page<Order> orders = orderRepository.findByBuyerIdAndStatus(user.getId(), status,
                    org.springframework.data.domain.PageRequest.of(0, 1));
            if (!orders.isEmpty()) {
                throw new ConflictException("存在未完成的订单，无法注销账号");
            }
        }

        // 软删除：设置状态为 INACTIVE
        user.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(user);

        log.info("用户账号注销成功 - 用户 ID: {}", user.getId());
    }
}
