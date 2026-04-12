package com.aisale.backend.service;

import com.aisale.backend.dto.OrderReviewRequest;
import com.aisale.backend.dto.OrderReviewResponse;
import com.aisale.backend.entity.Order;
import com.aisale.backend.entity.OrderReview;
import com.aisale.backend.repository.OrderRepository;
import com.aisale.backend.repository.OrderReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * OrderReviewService 单元测试
 * 测试订单评价的核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderReviewService 单元测试")
class OrderReviewServiceTest {

    @Mock
    private OrderReviewRepository orderReviewRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderReviewService orderReviewService;

    private Order testOrder;
    private final Long ORDER_ID = 1L;
    private final Long BUYER_ID = 1L;
    private final Long SELLER_ID = 2L;
    private final Long PRODUCT_ID = 1L;

    @BeforeEach
    void setUp() {
        // 初始化测试订单
        testOrder = new Order();
        testOrder.setId(ORDER_ID);
        testOrder.setBuyerId(BUYER_ID);
        testOrder.setSellerId(SELLER_ID);
        testOrder.setProductId(PRODUCT_ID);
        testOrder.setProductName("测试商品");
        testOrder.setPrice(new BigDecimal("99.99"));
        testOrder.setQuantity(1);
        testOrder.setStatus(Order.OrderStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("创建评价 - 买家评价成功")
    void createReview_ByBuyer_Success() {
        // Given
        OrderReviewRequest request = new OrderReviewRequest();
        request.setRating(5);
        request.setContent("商品很好，非常满意！");

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderReviewRepository.existsByOrderIdAndReviewType(ORDER_ID, OrderReview.ReviewType.BUYER_REVIEW)).thenReturn(false);
        when(orderReviewRepository.save(any(OrderReview.class))).thenAnswer(invocation -> {
            OrderReview review = invocation.getArgument(0);
            review.setId(1L);
            return review;
        });

        // When
        OrderReviewResponse response = orderReviewService.createReview(ORDER_ID, request, BUYER_ID, OrderReview.ReviewType.BUYER_REVIEW);

        // Then
        assertNotNull(response);
        assertEquals(5, response.getRating());
        assertEquals("商品很好，非常满意！", response.getContent());
        assertEquals(OrderReview.ReviewType.BUYER_REVIEW, response.getReviewType());

        ArgumentCaptor<OrderReview> reviewCaptor = ArgumentCaptor.forClass(OrderReview.class);
        verify(orderReviewRepository).save(reviewCaptor.capture());
        OrderReview savedReview = reviewCaptor.getValue();
        assertEquals(BUYER_ID, savedReview.getReviewerId());
        assertEquals(SELLER_ID, savedReview.getRevieweeId());
        assertEquals(PRODUCT_ID, savedReview.getProductId());
    }

    @Test
    @DisplayName("创建评价 - 卖家评价成功")
    void createReview_BySeller_Success() {
        // Given
        OrderReviewRequest request = new OrderReviewRequest();
        request.setRating(5);
        request.setContent("买家很好，交易愉快！");

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderReviewRepository.existsByOrderIdAndReviewType(ORDER_ID, OrderReview.ReviewType.SELLER_REVIEW)).thenReturn(false);
        when(orderReviewRepository.save(any(OrderReview.class))).thenAnswer(invocation -> {
            OrderReview review = invocation.getArgument(0);
            review.setId(2L);
            return review;
        });

        // When
        OrderReviewResponse response = orderReviewService.createReview(ORDER_ID, request, SELLER_ID, OrderReview.ReviewType.SELLER_REVIEW);

        // Then
        assertNotNull(response);
        assertEquals(SELLER_ID, response.getReviewerId());
        assertEquals(BUYER_ID, response.getRevieweeId());
        assertEquals(OrderReview.ReviewType.SELLER_REVIEW, response.getReviewType());
    }

    @Test
    @DisplayName("创建评价 - 订单状态不允许")
    void createReview_WrongOrderStatus() {
        // Given
        testOrder.setStatus(Order.OrderStatus.PENDING_PAYMENT);
        OrderReviewRequest request = new OrderReviewRequest();
        request.setRating(5);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderReviewService.createReview(ORDER_ID, request, BUYER_ID, OrderReview.ReviewType.BUYER_REVIEW);
        });
        assertEquals("订单状态不允许评价", exception.getMessage());
    }

    @Test
    @DisplayName("创建评价 - 无权评价")
    void createReview_Unauthorized() {
        // Given
        OrderReviewRequest request = new OrderReviewRequest();
        request.setRating(5);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));

        // When & Then - 陌生人尝试评价
        Long strangerId = 999L;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderReviewService.createReview(ORDER_ID, request, strangerId, OrderReview.ReviewType.BUYER_REVIEW);
        });
        assertEquals("无权评价此订单", exception.getMessage());
    }

    @Test
    @DisplayName("创建评价 - 重复评价")
    void createReview_DuplicateReview() {
        // Given
        OrderReviewRequest request = new OrderReviewRequest();
        request.setRating(5);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderReviewRepository.existsByOrderIdAndReviewType(ORDER_ID, OrderReview.ReviewType.BUYER_REVIEW)).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderReviewService.createReview(ORDER_ID, request, BUYER_ID, OrderReview.ReviewType.BUYER_REVIEW);
        });
        assertEquals("您已经评价过此订单", exception.getMessage());
    }

    @Test
    @DisplayName("创建评价 - 双方评价后订单完成")
    void createReview_BothReviews_CompleteOrder() {
        // Given
        OrderReviewRequest request = new OrderReviewRequest();
        request.setRating(5);
        request.setContent("卖家评价");

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));
        // 买家已评价，卖家评价保存后也存在（第二次调用时返回 true）
        when(orderReviewRepository.existsByOrderIdAndReviewType(ORDER_ID, OrderReview.ReviewType.BUYER_REVIEW)).thenReturn(true);
        when(orderReviewRepository.existsByOrderIdAndReviewType(ORDER_ID, OrderReview.ReviewType.SELLER_REVIEW))
            .thenReturn(false)  // 第一次调用（检查是否重复评价）返回 false
            .thenReturn(true);  // 第二次调用（检查是否双方都评价）返回 true
        when(orderReviewRepository.save(any(OrderReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        orderReviewService.createReview(ORDER_ID, request, SELLER_ID, OrderReview.ReviewType.SELLER_REVIEW);

        // Then
        verify(orderRepository).save(argThat(order -> order.getStatus() == Order.OrderStatus.COMPLETED));
    }

    @Test
    @DisplayName("回复评价 - 成功")
    void replyReview_Success() {
        // Given
        Long reviewId = 1L;
        OrderReview review = new OrderReview();
        review.setId(reviewId);
        review.setRevieweeId(SELLER_ID);
        review.setReviewType(OrderReview.ReviewType.SELLER_REVIEW);

        when(orderReviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(orderReviewRepository.save(any(OrderReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OrderReviewResponse response = orderReviewService.replyReview(reviewId, "感谢好评，欢迎下次光临！", SELLER_ID);

        // Then
        assertNotNull(response);
        assertEquals("感谢好评，欢迎下次光临！", response.getReplyContent());
        assertNotNull(response.getReplyTime());

        ArgumentCaptor<OrderReview> reviewCaptor = ArgumentCaptor.forClass(OrderReview.class);
        verify(orderReviewRepository).save(reviewCaptor.capture());
        OrderReview savedReview = reviewCaptor.getValue();
        assertEquals("感谢好评，欢迎下次光临！", savedReview.getReplyContent());
    }

    @Test
    @DisplayName("回复评价 - 评价不存在")
    void replyReview_NotFound() {
        // Given
        Long reviewId = 999L;
        when(orderReviewRepository.findById(reviewId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderReviewService.replyReview(reviewId, "回复内容", 1L);
        });
        assertEquals("评价不存在", exception.getMessage());
    }

    @Test
    @DisplayName("回复评价 - 无权回复")
    void replyReview_Unauthorized() {
        // Given
        Long reviewId = 1L;
        OrderReview review = new OrderReview();
        review.setId(reviewId);
        review.setRevieweeId(SELLER_ID); // 被评价人是卖家

        when(orderReviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        // When & Then - 买家尝试回复卖家的评价
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderReviewService.replyReview(reviewId, "回复内容", BUYER_ID);
        });
        assertEquals("无权回复此评价", exception.getMessage());
    }

    @Test
    @DisplayName("获取订单评价列表 - 成功")
    void getReviewsByOrderId_Success() {
        // Given
        OrderReview review1 = new OrderReview();
        review1.setId(1L);
        review1.setReviewType(OrderReview.ReviewType.BUYER_REVIEW);
        review1.setRating(5);
        review1.setContent("买家评价");

        OrderReview review2 = new OrderReview();
        review2.setId(2L);
        review2.setReviewType(OrderReview.ReviewType.SELLER_REVIEW);
        review2.setRating(5);
        review2.setContent("卖家评价");

        when(orderReviewRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(review1, review2));

        // When
        List<OrderReviewResponse> responses = orderReviewService.getReviewsByOrderId(ORDER_ID);

        // Then
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertTrue(responses.stream().anyMatch(r -> r.getReviewType() == OrderReview.ReviewType.BUYER_REVIEW));
        assertTrue(responses.stream().anyMatch(r -> r.getReviewType() == OrderReview.ReviewType.SELLER_REVIEW));
    }

    @Test
    @DisplayName("获取用户的评价列表 - 成功")
    void getReviewsByReviewerId_Success() {
        // Given
        OrderReview review = new OrderReview();
        review.setId(1L);
        review.setReviewerId(BUYER_ID);
        review.setReviewType(OrderReview.ReviewType.BUYER_REVIEW);

        when(orderReviewRepository.findByReviewerId(BUYER_ID)).thenReturn(List.of(review));

        // When
        List<OrderReviewResponse> responses = orderReviewService.getReviewsByReviewerId(BUYER_ID);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(BUYER_ID, responses.get(0).getReviewerId());
    }

    @Test
    @DisplayName("获取用户收到的评价列表 - 成功")
    void getReviewsByRevieweeId_Success() {
        // Given
        OrderReview review = new OrderReview();
        review.setId(1L);
        review.setRevieweeId(SELLER_ID);
        review.setReviewType(OrderReview.ReviewType.BUYER_REVIEW);

        when(orderReviewRepository.findByRevieweeId(SELLER_ID)).thenReturn(List.of(review));

        // When
        List<OrderReviewResponse> responses = orderReviewService.getReviewsByRevieweeId(SELLER_ID);

        // Then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(SELLER_ID, responses.get(0).getRevieweeId());
    }
}
