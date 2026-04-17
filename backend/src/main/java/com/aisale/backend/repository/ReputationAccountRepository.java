package com.aisale.backend.repository;

import com.aisale.backend.entity.ReputationAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReputationAccountRepository extends JpaRepository<ReputationAccount, Long> {

    /**
     * 根据用户ID查询信誉账户
     */
    Optional<ReputationAccount> findByUserId(Long userId);

    /**
     * 检查用户是否已有信誉账户
     */
    boolean existsByUserId(Long userId);
}