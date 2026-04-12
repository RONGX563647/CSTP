package com.aisale.backend.repository;

import com.aisale.backend.entity.OrderReview;
import com.aisale.backend.entity.OrderReview.ReviewType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderReviewRepository extends JpaRepository<OrderReview, Long> {

    /**
     * 根据订单 ID 查询评价
     */
    List<OrderReview> findByOrderId(Long orderId);

    /**
     * 根据订单 ID 和评价类型查询
     */
    Optional<OrderReview> findByOrderIdAndReviewType(Long orderId, ReviewType reviewType);

    /**
     * 根据评价人 ID 查询评价
     */
    List<OrderReview> findByReviewerId(Long reviewerId);

    /**
     * 根据被评价人 ID 查询评价
     */
    List<OrderReview> findByRevieweeId(Long revieweeId);

    /**
     * 根据商品 ID 查询评价
     */
    List<OrderReview> findByProductId(Long productId);

    /**
     * 统计订单的评价数量
     */
    long countByOrderId(Long orderId);

    /**
     * 统计商品的评分总数
     */
    long countByProductId(Long productId);

    /**
     * 检查指定订单是否已有指定类型的评价
     */
    boolean existsByOrderIdAndReviewType(Long orderId, ReviewType reviewType);
}
