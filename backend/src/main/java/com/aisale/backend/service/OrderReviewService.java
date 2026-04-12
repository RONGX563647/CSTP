package com.aisale.backend.service;

import com.aisale.backend.dto.OrderReviewRequest;
import com.aisale.backend.dto.OrderReviewResponse;
import com.aisale.backend.entity.Order;
import com.aisale.backend.entity.OrderReview;
import com.aisale.backend.repository.OrderRepository;
import com.aisale.backend.repository.OrderReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderReviewService {

    private final OrderReviewRepository orderReviewRepository;
    private final OrderRepository orderRepository;

    /**
     * 创建评价
     */
    @Transactional
    public OrderReviewResponse createReview(Long orderId, OrderReviewRequest request,
                                             Long userId, OrderReview.ReviewType reviewType) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 验证订单状态
        if (order.getStatus() != Order.OrderStatus.PENDING_REVIEW) {
            throw new RuntimeException("订单状态不允许评价");
        }

        // 验证评价权限
        if (reviewType == OrderReview.ReviewType.BUYER_REVIEW && !order.getBuyerId().equals(userId)) {
            throw new RuntimeException("无权评价此订单");
        }
        if (reviewType == OrderReview.ReviewType.SELLER_REVIEW && !order.getSellerId().equals(userId)) {
            throw new RuntimeException("无权评价此订单");
        }

        // 检查是否已评价
        if (orderReviewRepository.existsByOrderIdAndReviewType(orderId, reviewType)) {
            throw new RuntimeException("您已经评价过此订单");
        }

        OrderReview review = new OrderReview();
        review.setOrderId(orderId);
        review.setReviewerId(userId);
        review.setRevieweeId(reviewType == OrderReview.ReviewType.BUYER_REVIEW ?
                             order.getSellerId() : order.getBuyerId());
        review.setProductId(order.getProductId());
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setReviewType(reviewType);

        review = orderReviewRepository.save(review);

        // 检查双方是否都已评价
        boolean hasBuyerReview = orderReviewRepository.existsByOrderIdAndReviewType(
                orderId, OrderReview.ReviewType.BUYER_REVIEW);
        boolean hasSellerReview = orderReviewRepository.existsByOrderIdAndReviewType(
                orderId, OrderReview.ReviewType.SELLER_REVIEW);

        if (hasBuyerReview && hasSellerReview) {
            order.setStatus(Order.OrderStatus.COMPLETED);
            order.setCompleteTime(java.time.LocalDateTime.now());
            orderRepository.save(order);
        }

        return OrderReviewResponse.fromEntity(review);
    }

    /**
     * 回复评价
     */
    @Transactional
    public OrderReviewResponse replyReview(Long reviewId, String replyContent, Long userId) {
        OrderReview review = orderReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("评价不存在"));

        // 验证回复权限：只有被评价人可以回复
        if (!review.getRevieweeId().equals(userId)) {
            throw new RuntimeException("无权回复此评价");
        }

        review.setReplyContent(replyContent);
        review.setReplyTime(java.time.LocalDateTime.now());
        review = orderReviewRepository.save(review);

        return OrderReviewResponse.fromEntity(review);
    }

    /**
     * 获取订单的评价列表
     */
    public List<OrderReviewResponse> getReviewsByOrderId(Long orderId) {
        return orderReviewRepository.findByOrderId(orderId)
                .stream()
                .map(OrderReviewResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定订单的指定类型评价
     */
    public OrderReviewResponse getReviewByOrderIdAndType(Long orderId, OrderReview.ReviewType reviewType) {
        return orderReviewRepository.findByOrderIdAndReviewType(orderId, reviewType)
                .map(OrderReviewResponse::fromEntity)
                .orElse(null);
    }

    /**
     * 获取用户的评价列表（作为评价人）
     */
    public List<OrderReviewResponse> getReviewsByReviewerId(Long reviewerId) {
        return orderReviewRepository.findByReviewerId(reviewerId)
                .stream()
                .map(OrderReviewResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户收到的评价列表（作为被评价人）
     */
    public List<OrderReviewResponse> getReviewsByRevieweeId(Long revieweeId) {
        return orderReviewRepository.findByRevieweeId(revieweeId)
                .stream()
                .map(OrderReviewResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 获取商品的评价列表
     */
    public List<OrderReviewResponse> getReviewsByProductId(Long productId) {
        return orderReviewRepository.findByProductId(productId)
                .stream()
                .map(OrderReviewResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 计算商品平均评分
     */
    public double getAverageRatingForProduct(Long productId) {
        List<OrderReview> reviews = orderReviewRepository.findByProductId(productId);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(OrderReview::getRating)
                .average()
                .orElse(0.0);
    }
}
