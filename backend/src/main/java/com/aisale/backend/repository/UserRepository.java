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
}