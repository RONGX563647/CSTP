package com.aisale.backend.repository;

import com.aisale.backend.entity.PointRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PointRecordRepository extends JpaRepository<PointRecord, Long> {

    Page<PointRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<PointRecord> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, PointRecord.PointType type, Pageable pageable);

    List<PointRecord> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long userId, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndType(Long userId, PointRecord.PointType type);

    long countByUserId(Long userId);
}
