package com.aisale.backend.controller;

import com.aisale.backend.dto.OrderResponse;
import com.aisale.backend.entity.Order;
import com.aisale.backend.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "管理端订单", description = "管理员管理订单")
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "获取所有订单列表")
    @GetMapping
    public ApiResponse<Page<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderResponse> orders = orderService.getAllOrders(pageable);
        return ApiResponse.success(orders);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable Long id) {
        OrderResponse order = orderService.getOrderByIdForAdmin(id);
        return ApiResponse.success(order);
    }

    @Operation(summary = "多条件查询订单")
    @GetMapping("/query")
    public ApiResponse<Page<OrderResponse>> searchOrders(
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long buyerId,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(required = false) Order.OrderStatus status,
            @RequestParam(required = false) LocalDateTime startTime,
            @RequestParam(required = false) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<OrderResponse> orders = orderService.searchOrders(
                orderNo, buyerId, sellerId, status, startTime, endTime, pageable);
        return ApiResponse.success(orders);
    }

    @Operation(summary = "管理员强制修改订单状态")
    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @RequestParam Order.OrderStatus status) {
        // 管理员 ID 暂时传 null，实际应该从登录信息获取
        OrderResponse order = orderService.updateOrderStatus(id, status, null);
        return ApiResponse.success("订单状态已更新", order);
    }

    @Operation(summary = "添加管理员备注")
    @PutMapping("/{id}/remark")
    public ApiResponse<OrderResponse> updateAdminRemark(
            @PathVariable Long id,
            @RequestParam String remark) {
        // 管理员 ID 暂时传 null，实际应该从登录信息获取
        OrderResponse order = orderService.updateAdminRemark(id, remark, null);
        return ApiResponse.success("备注已添加", order);
    }

    @Operation(summary = "获取订单统计")
    @GetMapping("/stats")
    public ApiResponse<OrderStats> getOrderStats() {
        OrderService.OrderStats stats = orderService.getOrderStats();
        OrderStats orderStats = new OrderStats(
                stats.getTotalOrders(),
                stats.getPendingPayment(),
                stats.getPendingPickup(),
                stats.getPendingConfirm(),
                stats.getPendingReview(),
                stats.getCompleted(),
                stats.getCancelled()
        );
        return ApiResponse.success(orderStats);
    }

    @Operation(summary = "获取订单日志")
    @GetMapping("/{id}/logs")
    public ApiResponse<java.util.List<com.aisale.backend.entity.OrderLog>> getOrderLogs(
            @PathVariable Long id) {
        return ApiResponse.success(orderService.getOrderLogs(id));
    }

    // 内部类用于响应
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ApiResponse<T> {
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(String message, T data) {
            return new ApiResponse<>(message, data);
        }

        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(null, data);
        }
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
