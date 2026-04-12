package com.aisale.backend.dto;

import com.aisale.backend.entity.OrderReview;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrderReviewResponse {

    private Long id;

    private Long orderId;

    private Long reviewerId;

    private String reviewerName;

    private Long revieweeId;

    private String revieweeName;

    private Long productId;

    private Integer rating;

    private String content;

    private String replyContent;

    private LocalDateTime replyTime;

    private OrderReview.ReviewType reviewType;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static OrderReviewResponse fromEntity(OrderReview review) {
        OrderReviewResponse response = new OrderReviewResponse();
        response.setId(review.getId());
        response.setOrderId(review.getOrderId());
        response.setReviewerId(review.getReviewerId());
        response.setRevieweeId(review.getRevieweeId());
        response.setProductId(review.getProductId());
        response.setRating(review.getRating());
        response.setContent(review.getContent());
        response.setReplyContent(review.getReplyContent());
        response.setReplyTime(review.getReplyTime());
        response.setReviewType(review.getReviewType());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());

        // 设置评价人名称（如果有）
        if (review.getOrder() != null && review.getOrder().getBuyer() != null) {
            if (review.getReviewType() == OrderReview.ReviewType.BUYER_REVIEW) {
                // 买家评价，reviewer 是买家
                response.setReviewerName(review.getOrder().getBuyer().getNickname());
                response.setRevieweeName(review.getOrder().getSeller().getNickname());
            } else {
                // 卖家评价，reviewer 是卖家
                response.setReviewerName(review.getOrder().getSeller().getNickname());
                response.setRevieweeName(review.getOrder().getBuyer().getNickname());
            }
        }

        return response;
    }
}
