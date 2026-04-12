package com.aisale.backend.service;

import com.aisale.backend.dto.OrderRequest;
import com.aisale.backend.dto.OrderResponse;
import com.aisale.backend.entity.Order;
import com.aisale.backend.entity.OrderLog;
import com.aisale.backend.entity.OrderReview;
import com.aisale.backend.entity.Product;
import com.aisale.backend.exception.business.ConflictException;
import com.aisale.backend.exception.business.ForbiddenException;
import com.aisale.backend.exception.business.NotFoundException;
import com.aisale.backend.repository.OrderLogRepository;
import com.aisale.backend.repository.OrderRepository;
import com.aisale.backend.repository.OrderReviewRepository;
import com.aisale.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderReviewRepository orderReviewRepository;
    private final OrderLogRepository orderLogRepository;
    private final ProductRepository productRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 生成订单号：yyyyMMddHHmmss + 6 位随机数
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return timestamp + sb;
    }

    /**
     * 创建订单
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request, Long buyerId) {
        log.info("创建订单 - 买家 ID: {}, 商品 ID: {}, 数量：{}", buyerId, request.getProductId(), request.getQuantity());

        // 1. 检查商品是否存在
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("商品不存在"));

        // 2. 检查商品是否在售
        if (!product.getIsOnSale() || product.getStatus() != Product.ProductStatus.ON_SALE) {
            log.warn("商品不可售 - 商品 ID: {}, 状态：{}", product.getId(), product.getStatus());
            throw new ConflictException("商品已下架或不可售");
        }

        // 3. 检查库存是否充足
        if (product.getStock() < request.getQuantity()) {
            log.warn("库存不足 - 商品 ID: {}, 库存：{}, 需求：{}", product.getId(), product.getStock(), request.getQuantity());
            throw new ConflictException("库存不足");
        }

        // 4. 检查是否已有未完成的订单
        if (orderRepository.hasActiveOrder(request.getProductId())) {
            log.warn("商品已有进行中的订单 - 商品 ID: {}", product.getId());
            throw new ConflictException("该商品已有进行中的订单");
        }

        // 5. 不能购买自己的商品
        if (product.getSellerId().equals(buyerId)) {
            log.warn("用户尝试购买自己的商品 - 用户 ID: {}, 商品 ID: {}", buyerId, product.getId());
            throw new ForbiddenException("不能购买自己的商品");
        }

        // 6. 生成订单号
        String orderNo = generateOrderNo();

        // 7. 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setBuyerId(buyerId);
        order.setSellerId(product.getSellerId());
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setProductImage(product.getMainImage());
        order.setPrice(product.getPrice());
        order.setQuantity(request.getQuantity());
        order.setTotalAmount(product.getPrice().multiply(new java.math.BigDecimal(request.getQuantity())));
        order.setMeetLocation(request.getMeetLocation());
        order.setMeetTime(request.getMeetTime());
        order.setBuyerRemark(request.getBuyerRemark());

        order = orderRepository.save(order);
        log.info("订单创建成功 - 订单 ID: {}, 订单号：{}", order.getId(), orderNo);

        // 8. 扣减库存
        product.setStock(product.getStock() - request.getQuantity());
        if (product.getStock() == 0) {
            product.setStatus(Product.ProductStatus.OUT_OF_STOCK);
        }
        productRepository.save(product);
        log.debug("库存已扣减 - 商品 ID: {}, 新库存：{}", product.getId(), product.getStock());

        // 9. 记录订单日志
        logOrderAction(order.getId(), buyerId, OrderLog.OperatorRole.BUYER,
                       "CREATE_ORDER", null, Order.OrderStatus.PENDING_PAYMENT,
                       "订单创建成功");

        return OrderResponse.fromEntity(order);
    }

    /**
     * 买家确认付款
     */
    @Transactional
    public OrderResponse payOrder(Long orderId, Long buyerId) {
        log.info("买家确认付款 - 订单 ID: {}, 买家 ID: {}", orderId, buyerId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));

        // 验证订单属于买家
        if (!order.getBuyerId().equals(buyerId)) {
            log.warn("无权操作订单 - 订单 ID: {}, 用户 ID: {}", orderId, buyerId);
            throw new ForbiddenException("无权操作此订单");
        }

        // 验证订单状态
        if (order.getStatus() != Order.OrderStatus.PENDING_PAYMENT) {
            log.warn("订单状态不允许付款 - 订单 ID: {}, 当前状态：{}", orderId, order.getStatus());
            throw new ConflictException("订单状态不允许付款");
        }

        Order.OrderStatus oldStatus = order.getStatus();
        order.setStatus(Order.OrderStatus.PENDING_PICKUP);
        order.setPaymentTime(LocalDateTime.now());
        order = orderRepository.save(order);

        log.info("付款成功 - 订单 ID: {}, 状态：{} -> {}", orderId, oldStatus, order.getStatus());
        logOrderAction(orderId, buyerId, OrderLog.OperatorRole.BUYER,
                       "PAY_ORDER", oldStatus, order.getStatus(), "买家已确认付款");

        return OrderResponse.fromEntity(order);
    }

    /**
     * 买家确认提货
     */
    @Transactional
    public OrderResponse confirmPickup(Long orderId, Long buyerId) {
        log.info("买家确认提货 - 订单 ID: {}, 买家 ID: {}", orderId, buyerId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));

        // 验证订单属于买家
        if (!order.getBuyerId().equals(buyerId)) {
            log.warn("无权操作订单 - 订单 ID: {}, 用户 ID: {}", orderId, buyerId);
            throw new ForbiddenException("无权操作此订单");
        }

        // 验证订单状态
        if (order.getStatus() != Order.OrderStatus.PENDING_PICKUP) {
            log.warn("订单状态不允许提货 - 订单 ID: {}, 当前状态：{}", orderId, order.getStatus());
            throw new ConflictException("订单状态不允许提货");
        }

        Order.OrderStatus oldStatus = order.getStatus();
        order.setStatus(Order.OrderStatus.PENDING_CONFIRM);
        order.setPickupTime(LocalDateTime.now());
        order = orderRepository.save(order);

        log.info("提货成功 - 订单 ID: {}, 状态：{} -> {}", orderId, oldStatus, order.getStatus());
        logOrderAction(orderId, buyerId, OrderLog.OperatorRole.BUYER,
                       "CONFIRM_PICKUP", oldStatus, order.getStatus(), "买家已确认提货");

        return OrderResponse.fromEntity(order);
    }

    /**
     * 卖家确认收款
     */
    @Transactional
    public OrderResponse confirmPayment(Long orderId, Long sellerId) {
        log.info("卖家确认收款 - 订单 ID: {}, 卖家 ID: {}", orderId, sellerId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));

        // 验证订单属于卖家
        if (!order.getSellerId().equals(sellerId)) {
            log.warn("无权操作订单 - 订单 ID: {}, 用户 ID: {}", orderId, sellerId);
            throw new ForbiddenException("无权操作此订单");
        }

        // 验证订单状态
        if (order.getStatus() != Order.OrderStatus.PENDING_CONFIRM) {
            log.warn("订单状态不允许确认 - 订单 ID: {}, 当前状态：{}", orderId, order.getStatus());
            throw new ConflictException("订单状态不允许确认");
        }

        Order.OrderStatus oldStatus = order.getStatus();
        order.setStatus(Order.OrderStatus.PENDING_REVIEW);
        order.setConfirmTime(LocalDateTime.now());
        order = orderRepository.save(order);

        log.info("确认收款成功 - 订单 ID: {}, 状态：{} -> {}", orderId, oldStatus, order.getStatus());
        logOrderAction(orderId, sellerId, OrderLog.OperatorRole.SELLER,
                       "CONFIRM_PAYMENT", oldStatus, order.getStatus(), "卖家已确认收款");

        return OrderResponse.fromEntity(order);
    }

    /**
     * 取消订单
     */
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId, String reason, boolean isBuyer) {
        log.info("取消订单 - 订单 ID: {}, 用户 ID: {}, 取消方：{}, 原因：{}", orderId, userId, isBuyer ? "买家" : "卖家", reason);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));

        // 验证取消权限
        if (isBuyer && !order.getBuyerId().equals(userId)) {
            log.warn("无权取消订单 - 订单 ID: {}, 用户 ID: {}", orderId, userId);
            throw new ForbiddenException("无权取消此订单");
        }
        if (!isBuyer && !order.getSellerId().equals(userId)) {
            log.warn("无权取消订单 - 订单 ID: {}, 用户 ID: {}", orderId, userId);
            throw new ForbiddenException("无权取消此订单");
        }

        // 验证订单状态
        if (order.getStatus() == Order.OrderStatus.COMPLETED ||
            order.getStatus() == Order.OrderStatus.CANCELLED ||
            order.getStatus() == Order.OrderStatus.REFUNDED) {
            log.warn("订单状态无法取消 - 订单 ID: {}, 当前状态：{}", orderId, order.getStatus());
            throw new ConflictException("当前订单状态无法取消");
        }

        Order.OrderStatus oldStatus = order.getStatus();
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(reason);
        order.setCancelRole(isBuyer ? Order.CancelRole.BUYER : Order.CancelRole.SELLER);
        order = orderRepository.save(order);

        // 恢复库存
        Product product = productRepository.findById(order.getProductId())
                .orElseThrow(() -> new NotFoundException("商品不存在"));
        product.setStock(product.getStock() + order.getQuantity());
        if (product.getIsOnSale() && product.getStock() > 0) {
            product.setStatus(Product.ProductStatus.ON_SALE);
        }
        productRepository.save(product);

        log.info("订单取消成功 - 订单 ID: {}, 状态：{} -> {}, 库存恢复：{}", orderId, oldStatus, Order.OrderStatus.CANCELLED, product.getStock());
        logOrderAction(orderId, userId, isBuyer ? OrderLog.OperatorRole.BUYER : OrderLog.OperatorRole.SELLER,
                       "CANCEL_ORDER", oldStatus, Order.OrderStatus.CANCELLED,
                       reason != null ? reason : (isBuyer ? "买家取消订单" : "卖家取消订单"));

        return OrderResponse.fromEntity(order);
    }

    /**
     * 获取订单详情
     */
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));

        // 验证权限：只有买家或卖家可以查看
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new ForbiddenException("无权查看此订单");
        }

        OrderResponse response = OrderResponse.fromEntity(order);

        // 设置评价状态
        List<OrderReview> reviews = orderReviewRepository.findByOrderId(orderId);
        response.setHasBuyerReview(reviews.stream()
                .anyMatch(r -> r.getReviewType() == OrderReview.ReviewType.BUYER_REVIEW));
        response.setHasSellerReview(reviews.stream()
                .anyMatch(r -> r.getReviewType() == OrderReview.ReviewType.SELLER_REVIEW));

        return response;
    }

    /**
     * 获取买家订单列表
     */
    public Page<OrderResponse> getBuyerOrders(Long buyerId, Pageable pageable) {
        return orderRepository.findByBuyerId(buyerId, pageable)
                .map(OrderResponse::fromEntity);
    }

    /**
     * 获取卖家订单列表
     */
    public Page<OrderResponse> getSellerOrders(Long sellerId, Pageable pageable) {
        return orderRepository.findBySellerId(sellerId, pageable)
                .map(OrderResponse::fromEntity);
    }

    /**
     * 获取买家指定状态的订单
     */
    public Page<OrderResponse> getBuyerOrdersByStatus(Long buyerId, Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findByBuyerIdAndStatus(buyerId, status, pageable)
                .map(OrderResponse::fromEntity);
    }

    /**
     * 获取卖家指定状态的订单
     */
    public Page<OrderResponse> getSellerOrdersByStatus(Long sellerId, Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findBySellerIdAndStatus(sellerId, status, pageable)
                .map(OrderResponse::fromEntity);
    }

    /**
     * 记录订单日志
     */
    private void logOrderAction(Long orderId, Long operatorId, OrderLog.OperatorRole role,
                                String action, Order.OrderStatus fromStatus,
                                Order.OrderStatus toStatus, String remark) {
        OrderLog log = new OrderLog();
        log.setOrderId(orderId);
        log.setOperatorId(operatorId);
        log.setOperatorRole(role);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(remark);
        orderLogRepository.save(log);
    }

    // ==================== 管理端方法 ====================

    /**
     * 获取所有订单
     */
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(OrderResponse::fromEntity);
    }

    /**
     * 管理员查看订单详情（无需权限检查）
     */
    public OrderResponse getOrderByIdForAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));
        return OrderResponse.fromEntity(order);
    }

    /**
     * 多条件查询订单
     */
    public Page<OrderResponse> searchOrders(String orderNo, Long buyerId, Long sellerId,
                                             Order.OrderStatus status,
                                             LocalDateTime startTime, LocalDateTime endTime,
                                             Pageable pageable) {
        return orderRepository.searchOrders(orderNo, buyerId, sellerId, status, startTime, endTime, pageable)
                .map(OrderResponse::fromEntity);
    }

    /**
     * 管理员强制修改订单状态
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, Order.OrderStatus status, Long adminId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));

        Order.OrderStatus oldStatus = order.getStatus();
        order.setStatus(status);
        order = orderRepository.save(order);

        logOrderAction(orderId, adminId, OrderLog.OperatorRole.ADMIN,
                       "ADMIN_UPDATE_STATUS", oldStatus, status, "管理员强制修改订单状态");

        return OrderResponse.fromEntity(order);
    }

    /**
     * 添加管理员备注
     */
    @Transactional
    public OrderResponse updateAdminRemark(Long orderId, String remark, Long adminId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("订单不存在"));

        order.setAdminRemark(remark);
        order = orderRepository.save(order);

        logOrderAction(orderId, adminId, OrderLog.OperatorRole.ADMIN,
                       "UPDATE_ADMIN_REMARK", null, null, "管理员添加备注：" + remark);

        return OrderResponse.fromEntity(order);
    }

    /**
     * 获取订单统计
     */
    public OrderStats getOrderStats() {
        long total = orderRepository.count();
        long pendingPayment = orderRepository.countByStatus(Order.OrderStatus.PENDING_PAYMENT);
        long pendingPickup = orderRepository.countByStatus(Order.OrderStatus.PENDING_PICKUP);
        long pendingConfirm = orderRepository.countByStatus(Order.OrderStatus.PENDING_CONFIRM);
        long pendingReview = orderRepository.countByStatus(Order.OrderStatus.PENDING_REVIEW);
        long completed = orderRepository.countByStatus(Order.OrderStatus.COMPLETED);
        long cancelled = orderRepository.countByStatus(Order.OrderStatus.CANCELLED);

        return new OrderStats(total, pendingPayment, pendingPickup, pendingConfirm,
                              pendingReview, completed, cancelled);
    }

    /**
     * 获取订单日志
     */
    public List<OrderLog> getOrderLogs(Long orderId) {
        return orderLogRepository.findByOrderIdOrderByCreateTimeAsc(orderId);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class OrderStats {
        private long totalOrders;
        private long pendingPayment;
        private long pendingPickup;
        private long pendingConfirm;
        private long pendingReview;
        private long completed;
        private long cancelled;
    }
}
