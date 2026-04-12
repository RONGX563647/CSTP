package com.aisale.backend.repository;

import com.aisale.backend.entity.OrderLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderLogRepository extends JpaRepository<OrderLog, Long> {

    /**
     * 根据订单 ID 查询日志
     */
    Page<OrderLog> findByOrderId(Long orderId, Pageable pageable);

    /**
     * 根据订单 ID 查询所有日志（按时间排序）
     */
    List<OrderLog> findByOrderIdOrderByCreateTimeAsc(Long orderId);

    /**
     * 根据操作人 ID 查询日志
     */
    Page<OrderLog> findByOperatorId(Long operatorId, Pageable pageable);

    /**
     * 统计订单的日志数量
     */
    long countByOrderId(Long orderId);
}
