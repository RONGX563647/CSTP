package com.aisale.backend.dto;

import com.aisale.backend.entity.Order;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrderResponse {

    private Long id;

    private String orderNo;

    private Long buyerId;

    private String buyerName;

    private String buyerPhone;

    private Long sellerId;

    private String sellerName;

    private String sellerPhone;

    private Long productId;

    private String productName;

    private String productImage;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal totalAmount;

    private String meetLocation;

    private LocalDateTime meetTime;

    private String buyerRemark;

    private Order.OrderStatus status;

    private LocalDateTime paymentTime;

    private LocalDateTime pickupTime;

    private LocalDateTime confirmTime;

    private LocalDateTime completeTime;

    private LocalDateTime cancelTime;

    private String cancelReason;

    private Order.CancelRole cancelRole;

    private String adminRemark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private boolean hasBuyerReview;

    private boolean hasSellerReview;

    public static OrderResponse fromEntity(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setBuyerId(order.getBuyerId());
        response.setSellerId(order.getSellerId());
        response.setProductId(order.getProductId());
        response.setProductName(order.getProductName());
        response.setProductImage(order.getProductImage());
        response.setPrice(order.getPrice());
        response.setQuantity(order.getQuantity());
        response.setTotalAmount(order.getTotalAmount());
        response.setMeetLocation(order.getMeetLocation());
        response.setMeetTime(order.getMeetTime());
        response.setBuyerRemark(order.getBuyerRemark());
        response.setStatus(order.getStatus());
        response.setPaymentTime(order.getPaymentTime());
        response.setPickupTime(order.getPickupTime());
        response.setConfirmTime(order.getConfirmTime());
        response.setCompleteTime(order.getCompleteTime());
        response.setCancelTime(order.getCancelTime());
        response.setCancelReason(order.getCancelReason());
        response.setCancelRole(order.getCancelRole());
        response.setAdminRemark(order.getAdminRemark());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        // 设置买家信息（如果存在）
        if (order.getBuyer() != null) {
            response.setBuyerName(order.getBuyer().getNickname());
            response.setBuyerPhone(order.getBuyer().getPhone());
        }

        // 设置卖家信息（如果存在）
        if (order.getSeller() != null) {
            response.setSellerName(order.getSeller().getNickname());
            response.setSellerPhone(order.getSeller().getPhone());
        }

        return response;
    }
}
