package com.aisale.backend.service;

import com.aisale.backend.dto.OrderRequest;
import com.aisale.backend.dto.OrderResponse;
import com.aisale.backend.entity.Order;
import com.aisale.backend.entity.Product;
import com.aisale.backend.entity.OrderLog;
import com.aisale.backend.repository.OrderRepository;
import com.aisale.backend.repository.OrderReviewRepository;
import com.aisale.backend.repository.OrderLogRepository;
import com.aisale.backend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * OrderService 单元测试
 * 测试订单的付款、提货、取消等核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 单元测试")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderReviewRepository orderReviewRepository;

    @Mock
    private OrderLogRepository orderLogRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private Product testProduct;
    private Order testOrder;
    private final Long BUYER_ID = 1L;
    private final Long SELLER_ID = 2L;
    private final Long PRODUCT_ID = 1L;
    private final Long ORDER_ID = 1L;

    @BeforeEach
    void setUp() {
        // 初始化测试商品
        testProduct = new Product();
        testProduct.setId(PRODUCT_ID);
        testProduct.setName("测试商品");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(10);
        testProduct.setIsOnSale(true);
        testProduct.setStatus(Product.ProductStatus.ON_SALE);
        testProduct.setSellerId(SELLER_ID);
        testProduct.setMainImage("http://example.com/image.jpg");

        // 初始化测试订单
        testOrder = new Order();
        testOrder.setId(ORDER_ID);
        testOrder.setOrderNo("20260412120000123456");
        testOrder.setBuyerId(BUYER_ID);
        testOrder.setSellerId(SELLER_ID);
        testOrder.setProductId(PRODUCT_ID);
        testOrder.setProductName("测试商品");
        testOrder.setPrice(new BigDecimal("99.99"));
        testOrder.setQuantity(1);
        testOrder.setTotalAmount(new BigDecimal("99.99"));
        testOrder.setStatus(Order.OrderStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("创建订单 - 成功")
    void createOrder_Success() {
        // Given
        OrderRequest request = new OrderRequest();
        request.setProductId(PRODUCT_ID);
        request.setQuantity(1);
        request.setMeetLocation("测试地点");
        request.setMeetTime(LocalDateTime.now().plusDays(1));

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(testProduct));
        when(orderRepository.hasActiveOrder(PRODUCT_ID)).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OrderResponse response = orderService.createOrder(request, BUYER_ID);

        // Then
        assertNotNull(response);
        assertEquals("测试商品", response.getProductName());
        assertEquals(Order.OrderStatus.PENDING_PAYMENT, response.getStatus());

        verify(productRepository).save(argThat(p -> p.getStock() == 9));
        verify(orderLogRepository).save(any(OrderLog.class));
    }

    @Test
    @DisplayName("创建订单 - 商品不存在")
    void createOrder_ProductNotFound() {
        // Given
        OrderRequest request = new OrderRequest();
        request.setProductId(PRODUCT_ID);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(request, BUYER_ID);
        });
        assertEquals("商品不存在", exception.getMessage());
    }

    @Test
    @DisplayName("创建订单 - 库存不足")
    void createOrder_InsufficientStock() {
        // Given
        testProduct.setStock(0);
        OrderRequest request = new OrderRequest();
        request.setProductId(PRODUCT_ID);
        request.setQuantity(1);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(testProduct));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(request, BUYER_ID);
        });
        assertEquals("库存不足", exception.getMessage());
    }

    @Test
    @DisplayName("创建订单 - 购买自己的商品")
    void createOrder_BuyOwnProduct() {
        // Given
        testProduct.setSellerId(BUYER_ID);
        OrderRequest request = new OrderRequest();
        request.setProductId(PRODUCT_ID);

        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(testProduct));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.createOrder(request, BUYER_ID);
        });
        assertEquals("不能购买自己的商品", exception.getMessage());
    }

    @Test
    @DisplayName("买家确认付款 - 成功")
    void payOrder_Success() {
        // Given
        testOrder.setStatus(Order.OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OrderResponse response = orderService.payOrder(ORDER_ID, BUYER_ID);

        // Then
        assertNotNull(response);
        assertEquals(Order.OrderStatus.PENDING_PICKUP, response.getStatus());
        assertNotNull(response.getPaymentTime());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(Order.OrderStatus.PENDING_PICKUP, savedOrder.getStatus());

        verify(orderLogRepository).save(any(OrderLog.class));
    }

    @Test
    @DisplayName("买家确认付款 - 订单不存在")
    void payOrder_OrderNotFound() {
        // Given
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.payOrder(ORDER_ID, BUYER_ID);
        });
        assertEquals("订单不存在", exception.getMessage());
    }

    @Test
    @DisplayName("买家确认付款 - 无权操作")
    void payOrder_Unauthorized() {
        // Given
        testOrder.setStatus(Order.OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));

        // When & Then
        Long wrongBuyerId = 999L;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.payOrder(ORDER_ID, wrongBuyerId);
        });
        assertEquals("无权操作此订单", exception.getMessage());
    }

    @Test
    @DisplayName("买家确认付款 - 状态不正确")
    void payOrder_WrongStatus() {
        // Given
        testOrder.setStatus(Order.OrderStatus.PENDING_PICKUP);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.payOrder(ORDER_ID, BUYER_ID);
        });
        assertEquals("订单状态不允许付款", exception.getMessage());
    }

    @Test
    @DisplayName("买家确认提货 - 成功")
    void confirmPickup_Success() {
        // Given
        testOrder.setStatus(Order.OrderStatus.PENDING_PICKUP);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OrderResponse response = orderService.confirmPickup(ORDER_ID, BUYER_ID);

        // Then
        assertNotNull(response);
        assertEquals(Order.OrderStatus.PENDING_CONFIRM, response.getStatus());
        assertNotNull(response.getPickupTime());

        verify(orderLogRepository).save(any(OrderLog.class));
    }

    @Test
    @DisplayName("卖家确认收款 - 成功")
    void confirmPayment_Success() {
        // Given
        testOrder.setStatus(Order.OrderStatus.PENDING_CONFIRM);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OrderResponse response = orderService.confirmPayment(ORDER_ID, SELLER_ID);

        // Then
        assertNotNull(response);
        assertEquals(Order.OrderStatus.PENDING_REVIEW, response.getStatus());
        assertNotNull(response.getConfirmTime());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(Order.OrderStatus.PENDING_REVIEW, savedOrder.getStatus());

        verify(orderLogRepository).save(any(OrderLog.class));
    }

    @Test
    @DisplayName("取消订单 - 买家取消成功")
    void cancelOrder_ByBuyer_Success() {
        // Given
        testOrder.setStatus(Order.OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OrderResponse response = orderService.cancelOrder(ORDER_ID, BUYER_ID, "不想要了", true);

        // Then
        assertNotNull(response);
        assertEquals(Order.OrderStatus.CANCELLED, response.getStatus());
        assertEquals("不想要了", response.getCancelReason());
        assertEquals(Order.CancelRole.BUYER, response.getCancelRole());

        verify(productRepository).save(argThat(p -> p.getStock() == 11));
        verify(orderLogRepository).save(any(OrderLog.class));
    }

    @Test
    @DisplayName("取消订单 - 状态不可取消")
    void cancelOrder_InvalidStatus() {
        // Given
        testOrder.setStatus(Order.OrderStatus.COMPLETED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.cancelOrder(ORDER_ID, BUYER_ID, "不想要了", true);
        });
        assertEquals("当前订单状态无法取消", exception.getMessage());
    }

    @Test
    @DisplayName("获取订单详情 - 成功")
    void getOrderById_Success() {
        // Given
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));
        when(orderReviewRepository.findByOrderId(ORDER_ID)).thenReturn(List.of());

        // When
        OrderResponse response = orderService.getOrderById(ORDER_ID, BUYER_ID);

        // Then
        assertNotNull(response);
        assertEquals(ORDER_ID, response.getId());
        assertFalse(response.isHasBuyerReview());
        assertFalse(response.isHasSellerReview());
    }

    @Test
    @DisplayName("获取订单详情 - 无权查看")
    void getOrderById_Unauthorized() {
        // Given
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(testOrder));

        // When & Then
        Long strangerId = 999L;
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            orderService.getOrderById(ORDER_ID, strangerId);
        });
        assertEquals("无权查看此订单", exception.getMessage());
    }
}
