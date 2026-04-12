package com.aisale.backend.controller;

import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.dto.OrderRequest;
import com.aisale.backend.dto.OrderResponse;
import com.aisale.backend.dto.OrderReviewRequest;
import com.aisale.backend.dto.OrderReviewResponse;
import com.aisale.backend.entity.Order;
import com.aisale.backend.entity.OrderReview;
import com.aisale.backend.service.OrderReviewService;
import com.aisale.backend.service.OrderService;
import com.aisale.backend.util.JwtRequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "用户端订单", description = "用户管理自己的订单")
@RestController
@RequestMapping("/api/user/orders")
@RequiredArgsConstructor
public class UserOrderController {

    private final OrderService orderService;
    private final OrderReviewService orderReviewService;
    private final JwtRequestUtils jwtRequestUtils;

    @Operation(summary = "创建订单")
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request,
            HttpServletRequest httpRequest) {
        Long buyerId = jwtRequestUtils.getCurrentUserId(httpRequest);
        OrderResponse order = orderService.createOrder(request, buyerId);
        return ApiResponse.success("订单创建成功", order);
    }

    @Operation(summary = "获取我的订单列表（我买的）")
    @GetMapping("/my/buyer")
    public ApiResponse<Page<OrderResponse>> getMyBuyerOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest httpRequest) {
        Long buyerId = jwtRequestUtils.getCurrentUserId(httpRequest);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderResponse> orders = orderService.getBuyerOrders(buyerId, pageable);
        return ApiResponse.success(orders);
    }

    @Operation(summary = "获取我的订单列表（我卖的）")
    @GetMapping("/my/seller")
    public ApiResponse<Page<OrderResponse>> getMySellerOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest httpRequest) {
        Long sellerId = jwtRequestUtils.getCurrentUserId(httpRequest);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderResponse> orders = orderService.getSellerOrders(sellerId, pageable);
        return ApiResponse.success(orders);
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = jwtRequestUtils.getCurrentUserId(httpRequest);
        OrderResponse order = orderService.getOrderById(id, userId);
        return ApiResponse.success(order);
    }

    @Operation(summary = "买家确认付款")
    @PutMapping("/{id}/pay")
    public ApiResponse<OrderResponse> payOrder(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long buyerId = jwtRequestUtils.getCurrentUserId(httpRequest);
        OrderResponse order = orderService.payOrder(id, buyerId);
        return ApiResponse.success("付款成功", order);
    }

    @Operation(summary = "买家确认提货")
    @PutMapping("/{id}/pickup")
    public ApiResponse<OrderResponse> confirmPickup(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long buyerId = jwtRequestUtils.getCurrentUserId(httpRequest);
        OrderResponse order = orderService.confirmPickup(id, buyerId);
        return ApiResponse.success("提货确认成功", order);
    }

    @Operation(summary = "卖家确认收款")
    @PutMapping("/{id}/confirm")
    public ApiResponse<OrderResponse> confirmPayment(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long sellerId = jwtRequestUtils.getCurrentUserId(httpRequest);
        OrderResponse order = orderService.confirmPayment(id, sellerId);
        return ApiResponse.success("收款确认成功", order);
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            HttpServletRequest httpRequest) {
        Long userId = jwtRequestUtils.getCurrentUserId(httpRequest);
        // 判断是买家还是卖家取消
        OrderResponse order = orderService.getOrderById(id, userId);
        boolean isBuyer = order.getBuyerId().equals(userId);
        OrderResponse cancelledOrder = orderService.cancelOrder(id, userId, reason, isBuyer);
        return ApiResponse.success(isBuyer ? "订单取消成功" : "订单已拒绝", cancelledOrder);
    }

    @Operation(summary = "提交评价")
    @PostMapping("/{id}/review")
    public ApiResponse<OrderReviewResponse> createReview(
            @PathVariable Long id,
            @Valid @RequestBody OrderReviewRequest request,
            @RequestParam OrderReview.ReviewType type,
            HttpServletRequest httpRequest) {
        Long userId = jwtRequestUtils.getCurrentUserId(httpRequest);
        OrderReviewResponse review = orderReviewService.createReview(id, request, userId, type);
        return ApiResponse.success("评价成功", review);
    }

    @Operation(summary = "获取订单评价列表")
    @GetMapping("/{id}/reviews")
    public ApiResponse<List<OrderReviewResponse>> getOrderReviews(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        // 先验证权限
        Long userId = jwtRequestUtils.getCurrentUserId(httpRequest);
        orderService.getOrderById(id, userId);
        List<OrderReviewResponse> reviews = orderReviewService.getReviewsByOrderId(id);
        return ApiResponse.success(reviews);
    }

    @Operation(summary = "回复评价")
    @PutMapping("/review/{reviewId}/reply")
    public ApiResponse<OrderReviewResponse> replyReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody OrderReviewRequest request,
            HttpServletRequest httpRequest) {
        Long userId = jwtRequestUtils.getCurrentUserId(httpRequest);
        OrderReviewRequest reviewRequest = new OrderReviewRequest();
        reviewRequest.setContent(request.getContent());
        OrderReviewResponse response = orderReviewService.replyReview(reviewId, request.getContent(), userId);
        return ApiResponse.success("回复成功", response);
    }

    @Operation(summary = "获取我的评价列表")
    @GetMapping("/reviews/my")
    public ApiResponse<List<OrderReviewResponse>> getMyReviews(
            HttpServletRequest httpRequest) {
        Long userId = jwtRequestUtils.getCurrentUserId(httpRequest);
        List<OrderReviewResponse> reviews = orderReviewService.getReviewsByReviewerId(userId);
        return ApiResponse.success(reviews);
    }

    @Operation(summary = "获取我收到的评价")
    @GetMapping("/reviews/received")
    public ApiResponse<List<OrderReviewResponse>> getReceivedReviews(
            HttpServletRequest httpRequest) {
        Long userId = jwtRequestUtils.getCurrentUserId(httpRequest);
        List<OrderReviewResponse> reviews = orderReviewService.getReviewsByRevieweeId(userId);
        return ApiResponse.success(reviews);
    }

    @Operation(summary = "获取订单日志")
    @GetMapping("/{id}/logs")
    public ApiResponse<List<com.aisale.backend.entity.OrderLog>> getOrderLogs(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = jwtRequestUtils.getCurrentUserId(httpRequest);
        // 验证订单权限
        orderService.getOrderById(id, userId);
        return ApiResponse.success(orderService.getOrderLogs(id));
    }
}
