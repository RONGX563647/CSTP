package com.aisale.backend.repository;

import com.aisale.backend.entity.ReputationRecord;
import com.aisale.backend.entity.ReputationRecord.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReputationRecordRepository extends JpaRepository<ReputationRecord, Long> {

    /**
     * 根据用户ID查询信誉记录（分页，按时间倒序）
     */
    Page<ReputationRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 根据用户ID和类型查询信誉记录
     */
    Page<ReputationRecord> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, RecordType type, Pageable pageable);

    /**
     * 根据用户ID查询所有信誉记录
     */
    List<ReputationRecord> findByUserId(Long userId);

    /**
     * 根据评价ID查询信誉记录
     */
    Optional<ReputationRecord> findByReviewId(Long reviewId);

    /**
     * 统计用户某类型的记录数
     */
    long countByUserIdAndType(Long userId, RecordType type);
}