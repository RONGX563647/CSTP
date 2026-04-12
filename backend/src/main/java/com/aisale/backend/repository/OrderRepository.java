package com.aisale.backend.repository;

import com.aisale.backend.entity.Order;
import com.aisale.backend.entity.Order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 根据订单号查询
     */
    Order findByOrderNo(String orderNo);

    /**
     * 根据买家 ID 查询订单
     */
    Page<Order> findByBuyerId(Long buyerId, Pageable pageable);

    /**
     * 根据卖家 ID 查询订单
     */
    Page<Order> findBySellerId(Long sellerId, Pageable pageable);

    /**
     * 根据商品 ID 查询订单
     */
    Page<Order> findByProductId(Long productId, Pageable pageable);

    /**
     * 根据订单状态查询
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * 根据买家 ID 和状态查询
     */
    Page<Order> findByBuyerIdAndStatus(Long buyerId, OrderStatus status, Pageable pageable);

    /**
     * 根据卖家 ID 和状态查询
     */
    Page<Order> findBySellerIdAndStatus(Long sellerId, OrderStatus status, Pageable pageable);

    /**
     * 检查商品是否有未完成的订单
     */
    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.productId = :productId " +
           "AND o.status NOT IN ('CANCELLED', 'REFUNDED', 'COMPLETED')")
    boolean hasActiveOrder(@Param("productId") Long productId);

    /**
     * 统计指定状态的订单数量
     */
    long countByStatus(OrderStatus status);

    /**
     * 统计买家的订单数量
     */
    long countByBuyerId(Long buyerId);

    /**
     * 统计卖家的订单数量
     */
    long countBySellerId(Long sellerId);

    /**
     * 多条件查询订单
     */
    @Query("SELECT o FROM Order o WHERE " +
           "(:orderNo IS NULL OR o.orderNo LIKE %:orderNo%) AND " +
           "(:buyerId IS NULL OR o.buyerId = :buyerId) AND " +
           "(:sellerId IS NULL OR o.sellerId = :sellerId) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:startTime IS NULL OR o.createdAt >= :startTime) AND " +
           "(:endTime IS NULL OR o.createdAt <= :endTime)")
    Page<Order> searchOrders(
            @Param("orderNo") String orderNo,
            @Param("buyerId") Long buyerId,
            @Param("sellerId") Long sellerId,
            @Param("status") OrderStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * 获取买家在指定时间段内的订单
     */
    @Query("SELECT o FROM Order o WHERE o.buyerId = :buyerId " +
           "AND o.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByBuyerIdAndTimeRange(
            @Param("buyerId") Long buyerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
