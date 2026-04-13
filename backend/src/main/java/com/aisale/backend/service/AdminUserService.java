package com.aisale.backend.service;

import com.aisale.backend.dto.UserAdminResponse;
import com.aisale.backend.dto.UserQueryRequest;
import com.aisale.backend.entity.User;
import com.aisale.backend.exception.business.NotFoundException;
import com.aisale.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端用户服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 获取用户列表（分页）
     */
    @Transactional(readOnly = true)
    public Page<UserAdminResponse> getAllUsers(int page, int size, UserQueryRequest queryRequest) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        User.UserStatus status = null;
        if (queryRequest.getStatus() != null && !queryRequest.getStatus().isEmpty()) {
            status = User.UserStatus.valueOf(queryRequest.getStatus().toUpperCase());
        }

        Page<User> userPage = userRepository.findByConditions(
                queryRequest.getUsername(),
                queryRequest.getNickname(),
                queryRequest.getEmail(),
                queryRequest.getPhone(),
                status,
                pageable
        );

        return userPage.map(UserAdminResponse::fromUser);
    }

    /**
     * 根据 ID 获取用户
     */
    @Transactional(readOnly = true)
    public UserAdminResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
        return UserAdminResponse.fromUser(user);
    }

    /**
     * 更新用户状态
     */
    @Transactional
    public UserAdminResponse updateUserStatus(Long id, User.UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        user.setStatus(status);
        User updatedUser = userRepository.save(user);

        log.info("管理员更新用户状态 - 用户 ID: {}, 新状态：{}", id, status);

        return UserAdminResponse.fromUser(updatedUser);
    }

    /**
     * 重置用户密码
     */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("管理员重置用户密码 - 用户 ID: {}", id);
    }

    /**
     * 删除用户（软删除）
     */
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        user.setStatus(User.UserStatus.INACTIVE);
        userRepository.save(user);

        log.info("管理员删除用户 - 用户 ID: {}", id);
    }

    /**
     * 获取用户统计
     */
    @Transactional(readOnly = true)
    public UserStats getUserStats() {
        Long totalUsers = userRepository.count();
        Long activeUsers = userRepository.countByStatus(User.UserStatus.ACTIVE);
        Long inactiveUsers = userRepository.countByStatus(User.UserStatus.INACTIVE);
        Long bannedUsers = userRepository.countByStatus(User.UserStatus.BANNED);

        return new UserStats(totalUsers, activeUsers, inactiveUsers, bannedUsers);
    }

    /**
     * 用户统计内部类
     */
    public record UserStats(
            Long totalUsers,
            Long activeUsers,
            Long inactiveUsers,
            Long bannedUsers
    ) {}
}
