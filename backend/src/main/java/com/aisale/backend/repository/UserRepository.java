package com.aisale.backend.repository;

import com.aisale.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    /**
     * 检查邮箱是否已被其他用户使用
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * 检查手机号是否已被其他用户使用
     */
    boolean existsByPhoneAndIdNot(String phone, Long id);

    /**
     * 根据状态统计用户数量
     */
    long countByStatus(User.UserStatus status);

    /**
     * 根据条件查询用户（分页）
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT u FROM User u WHERE " +
        "(:username IS NULL OR u.username LIKE %:username%) AND " +
        "(:nickname IS NULL OR u.nickname LIKE %:nickname%) AND " +
        "(:email IS NULL OR u.email LIKE %:email%) AND " +
        "(:phone IS NULL OR u.phone LIKE %:phone%) AND " +
        "(:status IS NULL OR u.status = :status)"
    )
    org.springframework.data.domain.Page<User> findByConditions(
        @org.springframework.data.repository.query.Param("username") String username,
        @org.springframework.data.repository.query.Param("nickname") String nickname,
        @org.springframework.data.repository.query.Param("email") String email,
        @org.springframework.data.repository.query.Param("phone") String phone,
        @org.springframework.data.repository.query.Param("status") User.UserStatus status,
        org.springframework.data.domain.Pageable pageable
    );
}