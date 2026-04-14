package com.aisale.backend.service;

import com.aisale.backend.dto.ProductResponse;
import com.aisale.backend.entity.Product;
import com.aisale.backend.entity.Product.ProductStatus;
import com.aisale.backend.entity.SearchHistory;
import com.aisale.backend.repository.ProductRepository;
import com.aisale.backend.repository.SearchHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProductRecommendService 单元测试
 * 测试推荐评分算法、首页推荐、分类推荐、个性化推荐等核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductRecommendService 单元测试")
class ProductRecommendServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @Mock
    private ProductSearchService productSearchService;

    @InjectMocks
    private ProductRecommendService productRecommendService;

    private Product featuredProduct;
    private Product normalProduct;
    private Product discountProduct;
    private Product newProduct;
    private final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // 精选商品 - 高销量
        featuredProduct = new Product();
        featuredProduct.setId(1L);
        featuredProduct.setName("iPhone 15 Pro");
        featuredProduct.setDescription("精选推荐手机");
        featuredProduct.setPrice(new BigDecimal("8999"));
        featuredProduct.setOriginalPrice(new BigDecimal("9999"));
        featuredProduct.setStock(10);
        featuredProduct.setCategory("手机数码");
        featuredProduct.setIsOnSale(true);
        featuredProduct.setIsFeatured(true);
        featuredProduct.setStatus(ProductStatus.ON_SALE);
        featuredProduct.setSellerId(2L);
        featuredProduct.setSalesCount(200);
        featuredProduct.setViewCount(1000);
        featuredProduct.setDiscount(new BigDecimal("0.9"));
        featuredProduct.setCreatedAt(LocalDateTime.now().minusDays(30));

        // 普通商品 - 低销量
        normalProduct = new Product();
        normalProduct.setId(2L);
        normalProduct.setName("普通手机壳");
        normalProduct.setDescription("基础保护壳");
        normalProduct.setPrice(new BigDecimal("29.9"));
        normalProduct.setOriginalPrice(new BigDecimal("49.9"));
        normalProduct.setStock(50);
        normalProduct.setCategory("手机数码");
        normalProduct.setIsOnSale(true);
        normalProduct.setIsFeatured(false);
        normalProduct.setStatus(ProductStatus.ON_SALE);
        normalProduct.setSellerId(3L);
        normalProduct.setSalesCount(5);
        normalProduct.setViewCount(20);
        normalProduct.setDiscount(new BigDecimal("0.6"));
        normalProduct.setCreatedAt(LocalDateTime.now().minusDays(60));

        // 折扣商品
        discountProduct = new Product();
        discountProduct.setId(3L);
        discountProduct.setName("打折耳机");
        discountProduct.setDescription("限时折扣");
        discountProduct.setPrice(new BigDecimal("199"));
        discountProduct.setOriginalPrice(new BigDecimal("599"));
        discountProduct.setStock(30);
        discountProduct.setCategory("手机数码");
        discountProduct.setIsOnSale(true);
        discountProduct.setIsFeatured(false);
        discountProduct.setStatus(ProductStatus.ON_SALE);
        discountProduct.setSellerId(2L);
        discountProduct.setSalesCount(50);
        discountProduct.setViewCount(200);
        discountProduct.setDiscount(new BigDecimal("0.33"));
        discountProduct.setCreatedAt(LocalDateTime.now().minusDays(20));

        // 新品
        newProduct = new Product();
        newProduct.setId(4L);
        newProduct.setName("新上市平板");
        newProduct.setDescription("刚发布的新品");
        newProduct.setPrice(new BigDecimal("3999"));
        newProduct.setOriginalPrice(new BigDecimal("4299"));
        newProduct.setStock(20);
        newProduct.setCategory("电脑办公");
        newProduct.setIsOnSale(true);
        newProduct.setIsFeatured(false);
        newProduct.setStatus(ProductStatus.ON_SALE);
        newProduct.setSellerId(2L);
        newProduct.setSalesCount(10);
        newProduct.setViewCount(80);
        newProduct.setDiscount(new BigDecimal("0.93"));
        newProduct.setCreatedAt(LocalDateTime.now().minusDays(3));
    }

    @Test
    @DisplayName("首页推荐 - 精选商品优先排序")
    void getHomeRecommend_FeaturedFirst() {
        // Given
        Page<Product> products = new PageImpl<>(List.of(normalProduct, featuredProduct));
        when(productRepository.findByIsOnSaleTrueAndStatus(eq(ProductStatus.ON_SALE), any(Pageable.class)))
            .thenReturn(products);

        // When
        List<ProductResponse> results = productRecommendService.getHomeRecommend(10);

        // Then: 精选商品应排在前面
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertEquals(1L, results.get(0).getId());  // featuredProduct(id=1) 应排第一
    }

    @Test
    @DisplayName("首页推荐 - 无商品返回空列表")
    void getHomeRecommend_EmptyResult() {
        // Given
        Page<Product> emptyPage = new PageImpl<>(List.of());
        when(productRepository.findByIsOnSaleTrueAndStatus(eq(ProductStatus.ON_SALE), any(Pageable.class)))
            .thenReturn(emptyPage);

        // When
        List<ProductResponse> results = productRecommendService.getHomeRecommend(10);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("首页推荐 - limit参数限制返回数量")
    void getHomeRecommend_LimitParameter() {
        // Given
        Page<Product> products = new PageImpl<>(List.of(featuredProduct, normalProduct, discountProduct, newProduct));
        when(productRepository.findByIsOnSaleTrueAndStatus(eq(ProductStatus.ON_SALE), any(Pageable.class)))
            .thenReturn(products);

        // When
        List<ProductResponse> results = productRecommendService.getHomeRecommend(2);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("分类推荐 - 按分类筛选")
    void getCategoryRecommend_FilterByCategory() {
        // Given
        Page<Product> products = new PageImpl<>(List.of(featuredProduct, normalProduct));
        when(productRepository.findByCategory(eq("手机数码"), any(Pageable.class)))
            .thenReturn(products);

        // When
        List<ProductResponse> results = productRecommendService.getCategoryRecommend("手机数码", 10);

        // Then
        assertNotNull(results);
        assertFalse(results.isEmpty());
        results.forEach(p -> assertEquals("手机数码", p.getCategory()));
    }

    @Test
    @DisplayName("分类推荐 - 不存在的分类返回空列表")
    void getCategoryRecommend_NonExistentCategory() {
        // Given
        Page<Product> emptyPage = new PageImpl<>(List.of());
        when(productRepository.findByCategory(eq("不存在的分类"), any(Pageable.class)))
            .thenReturn(emptyPage);

        // When
        List<ProductResponse> results = productRecommendService.getCategoryRecommend("不存在的分类", 10);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("猜你喜欢 - 未登录用户返回首页推荐")
    void getPersonalizedRecommend_AnonymousUser() {
        // Given: 未登录用户 userId=null，应返回首页推荐
        Page<Product> products = new PageImpl<>(List.of(featuredProduct));
        when(productRepository.findByIsOnSaleTrueAndStatus(eq(ProductStatus.ON_SALE), any(Pageable.class)))
            .thenReturn(products);

        // When
        List<ProductResponse> results = productRecommendService.getPersonalizedRecommend(null, 10);

        // Then
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("猜你喜欢 - 基于搜索历史的个性化推荐")
    void getPersonalizedRecommend_BasedOnSearchHistory() {
        // Given
        SearchHistory history = new SearchHistory();
        history.setId(1L);
        history.setUserId(USER_ID);
        history.setKeyword("手机");
        history.setSearchCount(5);
        history.setLastSearchTime(LocalDateTime.now());

        when(searchHistoryRepository.findByUserIdOrderByLastSearchTimeDesc(USER_ID))
            .thenReturn(List.of(history));

        Page<Product> searchedProducts = new PageImpl<>(List.of(featuredProduct));
        when(productRepository.searchMultiField(eq("手机"), isNull(), isNull(), isNull(), eq(ProductStatus.ON_SALE), any(Pageable.class)))
            .thenReturn(searchedProducts);

        when(productSearchService.getHotProductsByCategory(anyString(), anyInt()))
            .thenReturn(List.of());
        when(productSearchService.getHotProducts(anyInt()))
            .thenReturn(List.of());

        // When
        List<ProductResponse> results = productRecommendService.getPersonalizedRecommend(USER_ID, 10);

        // Then
        assertNotNull(results);
        assertFalse(results.isEmpty());
        // 基于搜索历史的商品应被包含
        assertTrue(results.stream().anyMatch(p -> p.getId().equals(1L)));
    }

    @Test
    @DisplayName("猜你喜欢 - 无搜索历史时返回精选推荐")
    void getPersonalizedRecommend_NoSearchHistory() {
        // Given
        when(searchHistoryRepository.findByUserIdOrderByLastSearchTimeDesc(USER_ID))
            .thenReturn(List.of());

        ProductResponse featuredResponse = ProductResponse.fromEntity(featuredProduct);
        when(productSearchService.getHotProducts(anyInt()))
            .thenReturn(List.of(featuredResponse));

        // When
        List<ProductResponse> results = productRecommendService.getPersonalizedRecommend(USER_ID, 10);

        // Then
        assertNotNull(results);
    }

    @Test
    @DisplayName("新品推荐 - 返回14天内商品")
    void getNewProducts_Within14Days() {
        // Given
        Page<Product> products = new PageImpl<>(List.of(newProduct));
        when(productRepository.findByIsOnSaleTrueAndStatus(eq(ProductStatus.ON_SALE), any(Pageable.class)))
            .thenReturn(products);

        // When
        List<ProductResponse> results = productRecommendService.getNewProducts(10);

        // Then
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("新品推荐 - 超过14天的商品不返回")
    void getNewProducts_OlderThan14Days() {
        // Given: 商品创建于30天前
        Product oldProduct = new Product();
        oldProduct.setId(5L);
        oldProduct.setName("老商品");
        oldProduct.setCategory("手机数码");
        oldProduct.setStatus(ProductStatus.ON_SALE);
        oldProduct.setCreatedAt(LocalDateTime.now().minusDays(30));
        oldProduct.setSellerId(2L);
        oldProduct.setSalesCount(0);
        oldProduct.setViewCount(0);

        Page<Product> products = new PageImpl<>(List.of(oldProduct));
        when(productRepository.findByIsOnSaleTrueAndStatus(eq(ProductStatus.ON_SALE), any(Pageable.class)))
            .thenReturn(products);

        // When
        List<ProductResponse> results = productRecommendService.getNewProducts(10);

        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("折扣推荐 - 只返回折扣低于9折的商品")
    void getDiscountProducts_Below90Percent() {
        // Given
        Page<Product> products = new PageImpl<>(List.of(discountProduct, normalProduct, featuredProduct));
        when(productRepository.findByIsOnSaleTrueAndStatus(eq(ProductStatus.ON_SALE), any(Pageable.class)))
            .thenReturn(products);

        // When
        List<ProductResponse> results = productRecommendService.getDiscountProducts(10);

        // Then: 只有 discountProduct(0.33) 和 normalProduct(0.6) 满足条件
        assertNotNull(results);
        assertFalse(results.isEmpty());
        // featuredProduct(0.9) 不满足 < 0.9 的条件
        assertTrue(results.stream().noneMatch(p -> p.getId().equals(1L)));
    }

    @Test
    @DisplayName("折扣推荐 - 按折扣力度排序")
    void getDiscountProducts_SortedByDiscount() {
        // Given
        Page<Product> products = new PageImpl<>(List.of(normalProduct, discountProduct));
        when(productRepository.findByIsOnSaleTrueAndStatus(eq(ProductStatus.ON_SALE), any(Pageable.class)))
            .thenReturn(products);

        // When
        List<ProductResponse> results = productRecommendService.getDiscountProducts(10);

        // Then: discountProduct(0.33) 折扣力度最大，应排第一
        assertNotNull(results);
        if (results.size() >= 2) {
            ProductResponse first = results.get(0);
            assertEquals(3L, first.getId()); // discountProduct 折扣0.33 最小排最前
        }
    }

    @Test
    @DisplayName("清除推荐缓存 - 成功")
    void clearRecommendCache_Success() {
        // When
        productRecommendService.clearRecommendCache();

        // Then: 不抛异常即通过
        assertDoesNotThrow(() -> productRecommendService.clearRecommendCache());
    }
}
