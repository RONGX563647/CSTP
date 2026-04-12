package com.aisale.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", unique = true, nullable = false, length = 32)
    private String orderNo;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", insertable = false, updatable = false)
    private User buyer;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", insertable = false, updatable = false)
    private User seller;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "product_image", length = 500)
    private String productImage;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "meet_location", length = 500)
    private String meetLocation;

    @Column(name = "meet_time")
    private LocalDateTime meetTime;

    @Column(name = "buyer_remark", length = 500)
    private String buyerRemark;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;

    @Column(name = "confirm_time")
    private LocalDateTime confirmTime;

    @Column(name = "complete_time")
    private LocalDateTime completeTime;

    @Column(name = "cancel_time")
    private LocalDateTime cancelTime;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_role")
    private CancelRole cancelRole;

    @Column(name = "admin_remark", length = 500)
    private String adminRemark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 订单状态枚举
     */
    public enum OrderStatus {
        PENDING_PAYMENT,  // 待付款
        PENDING_PICKUP,   // 待提货
        PENDING_CONFIRM,  // 待确认
        PENDING_REVIEW,   // 待评价
        COMPLETED,        // 已完成
        CANCELLED,        // 已取消
        REFUNDED          // 已退款
    }

    /**
     * 取消方枚举
     */
    public enum CancelRole {
        BUYER,   // 买家取消
        SELLER,  // 卖家取消
        SYSTEM   // 系统取消
    }
}
