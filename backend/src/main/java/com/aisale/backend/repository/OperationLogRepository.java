package com.aisale.backend.repository;

import com.aisale.backend.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志持久层
 */
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long>, JpaSpecificationExecutor<OperationLog> {

    /**
     * 根据操作人ID查询日志
     */
    Page<OperationLog> findByOperatorId(Long operatorId, Pageable pageable);

    /**
     * 根据操作人角色查询日志
     */
    Page<OperationLog> findByOperatorRole(OperationLog.OperatorRole operatorRole, Pageable pageable);

    /**
     * 根据模块查询日志
     */
    Page<OperationLog> findByModule(String module, Pageable pageable);

    /**
     * 根据操作类型查询日志
     */
    Page<OperationLog> findByAction(String action, Pageable pageable);

    /**
     * 根据状态查询日志
     */
    Page<OperationLog> findByStatus(String status, Pageable pageable);

    /**
     * 根据时间范围查询日志
     */
    Page<OperationLog> findByCreateTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * 根据模块和操作类型查询日志
     */
    Page<OperationLog> findByModuleAndAction(String module, String action, Pageable pageable);

    /**
     * 根据操作人ID和角色查询日志
     */
    Page<OperationLog> findByOperatorIdAndOperatorRole(Long operatorId, OperationLog.OperatorRole operatorRole, Pageable pageable);

    /**
     * 根据业务ID和业务类型查询日志
     */
    Page<OperationLog> findByBizIdAndBizType(Long bizId, String bizType, Pageable pageable);

    /**
     * 统计指定时间范围内的日志数量
     */
    @Query("SELECT COUNT(l) FROM OperationLog l WHERE l.createTime BETWEEN :start AND :end")
    long countByTimeRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计指定操作人的日志数量
     */
    long countByOperatorId(Long operatorId);

    /**
     * 统计指定模块的日志数量
     */
    long countByModule(String module);

    /**
     * 查询最近的日志（用于仪表盘）
     */
    List<OperationLog> findTop10ByOrderByCreateTimeDesc();

    /**
     * 统计今日日志数量
     */
    @Query("SELECT COUNT(l) FROM OperationLog l WHERE l.createTime >= :today")
    long countTodayLogs(@Param("today") LocalDateTime today);

    /**
     * 统计每小时的日志数量（用于图表）
     */
    @Query(value = "SELECT HOUR(create_time) as hour, COUNT(*) as count FROM operation_logs " +
            "WHERE DATE(create_time) = DATE(:date) GROUP BY HOUR(create_time) ORDER BY hour", 
            nativeQuery = true)
    List<Object[]> countLogsByHour(@Param("date") LocalDateTime date);

    /**
     * 统计各模块的日志数量
     */
    @Query(value = "SELECT module, COUNT(*) as count FROM operation_logs " +
            "WHERE create_time BETWEEN :start AND :end GROUP BY module ORDER BY count DESC", 
            nativeQuery = true)
    List<Object[]> countLogsByModule(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计各状态的日志数量
     */
    @Query(value = "SELECT status, COUNT(*) as count FROM operation_logs " +
            "WHERE create_time BETWEEN :start AND :end GROUP BY status", 
            nativeQuery = true)
    List<Object[]> countLogsByStatus(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
