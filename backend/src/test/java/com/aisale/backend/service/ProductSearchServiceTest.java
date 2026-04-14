package com.aisale.backend.service;

import com.aisale.backend.dto.ProductResponse;
import com.aisale.backend.dto.ProductSearchResult;
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
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ProductSearchService 单元测试
 * 测试智能搜索、搜索历史、热门关键词等核心功能
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductSearchService 单元测试")
class ProductSearchServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @InjectMocks
    private ProductSearchService productSearchService;

    private Product testProduct1;
    private Product testProduct2;
    private SearchHistory testSearchHistory;
    private final Long USER_ID = 1L;

    /**
     * 辅助方法：根据关键词映射动态设置 searchMultiField 的返回值
     * 解决 Mockito 同一方法多次 stubbing 时 anyString() 与 eq() 冲突的问题
     */
    private void setupSearchMultiField(Map<String, Page<Product>> keywordToResult) {
        when(productRepository.searchMultiField(any(), any(), any(), any(), any(), any()))
            .thenAnswer(invocation -> {
                String keyword = invocation.getArgument(0);
                if (keyword != null && keywordToResult.containsKey(keyword)) {
                    return keywordToResult.get(keyword);
                }
                return new PageImpl<>(List.of());
            });
    }

    @BeforeEach
    void setUp() {
        // 初始化测试商品1 - 手机
        testProduct1 = new Product();
        testProduct1.setId(1L);
        testProduct1.setName("iPhone 15 Pro Max");
        testProduct1.setDescription("苹果最新手机，A17 Pro芯片");
        testProduct1.setPrice(new BigDecimal("8999"));
        testProduct1.setOriginalPrice(new BigDecimal("9999"));
        testProduct1.setStock(10);
        testProduct1.setCategory("手机数码");
        testProduct1.setTags(Arrays.asList("手机", "苹果", "5G"));
        testProduct1.setIsOnSale(true);
        testProduct1.setIsFeatured(true);
        testProduct1.setStatus(ProductStatus.ON_SALE);
        testProduct1.setSellerId(2L);
        testProduct1.setSalesCount(100);
        testProduct1.setViewCount(500);
        testProduct1.setDiscount(new BigDecimal("0.9"));
        testProduct1.setCreatedAt(LocalDateTime.now().minusDays(5));

        // 初始化测试商品2 - 电脑
        testProduct2 = new Product();
        testProduct2.setId(2L);
        testProduct2.setName("MacBook Pro 14寸");
        testProduct2.setDescription("M3 Pro芯片笔记本");
        testProduct2.setPrice(new BigDecimal("14999"));
        testProduct2.setOriginalPrice(new BigDecimal("16999"));
        testProduct2.setStock(5);
        testProduct2.setCategory("电脑办公");
        testProduct2.setTags(Arrays.asList("笔记本", "苹果", "电脑"));
        testProduct2.setIsOnSale(true);
        testProduct2.setIsFeatured(false);
        testProduct2.setStatus(ProductStatus.ON_SALE);
        testProduct2.setSellerId(3L);
        testProduct2.setSalesCount(50);
        testProduct2.setViewCount(300);
        testProduct2.setDiscount(new BigDecimal("0.88"));
        testProduct2.setCreatedAt(LocalDateTime.now().minusDays(10));

        // 初始化搜索历史
        testSearchHistory = new SearchHistory();
        testSearchHistory.setId(1L);
        testSearchHistory.setUserId(USER_ID);
        testSearchHistory.setKeyword("手机");
        testSearchHistory.setSearchCount(3);
        testSearchHistory.setLastSearchTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("智能搜索 - 关键词匹配名称")
    void searchWithRelevance_NameMatch() {
        // Given: "iPhone" 搜到商品，其他关键词返回空（同义词扩展不会补充）
        Map<String, Page<Product>> mapping = new HashMap<>();
        mapping.put("iPhone", new PageImpl<>(List.of(testProduct1)));
        setupSearchMultiField(mapping);

        // When
        Page<ProductSearchResult> results = productSearchService.searchWithRelevance(
            "iPhone", null, null, null, USER_ID, 0, 10);

        // Then
        assertNotNull(results);
        assertFalse(results.getContent().isEmpty());
        ProductSearchResult first = results.getContent().get(0);
        assertEquals("name", first.getMatchType());
        assertTrue(first.getRelevanceScore() > 0);
    }

    @Test
    @DisplayName("智能搜索 - 同义词扩展搜索")
    void searchWithRelevance_SynonymExpansion() {
        // Given: 搜索"电话"，原始搜索无结果，同义词"手机"能搜到商品
        Map<String, Page<Product>> mapping = new HashMap<>();
        mapping.put("电话", new PageImpl<>(List.of()));  // 原始搜索无结果
        mapping.put("手机", new PageImpl<>(List.of(testProduct1)));  // 同义词搜索有结果
        setupSearchMultiField(mapping);

        // When
        Page<ProductSearchResult> results = productSearchService.searchWithRelevance(
            "电话", null, null, null, USER_ID, 0, 10);

        // Then: 通过同义词扩展能搜到手机商品
        assertNotNull(results);
        assertFalse(results.getContent().isEmpty());
    }

    @Test
    @DisplayName("智能搜索 - 记录搜索历史")
    void searchWithRelevance_RecordsHistory() {
        // Given
        Map<String, Page<Product>> mapping = new HashMap<>();
        mapping.put("iPhone", new PageImpl<>(List.of(testProduct1)));
        setupSearchMultiField(mapping);

        when(searchHistoryRepository.findByUserIdAndKeyword(USER_ID, "iPhone"))
            .thenReturn(Optional.empty());
        when(searchHistoryRepository.save(any(SearchHistory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        productSearchService.searchWithRelevance("iPhone", null, null, null, USER_ID, 0, 10);

        // Then
        verify(searchHistoryRepository).save(argThat(history ->
            history.getUserId().equals(USER_ID) &&
            history.getKeyword().equals("iPhone") &&
            history.getSearchCount() == 1
        ));
    }

    @Test
    @DisplayName("智能搜索 - 更新已有搜索历史计数")
    void searchWithRelevance_UpdatesExistingHistory() {
        // Given
        Map<String, Page<Product>> mapping = new HashMap<>();
        mapping.put("手机", new PageImpl<>(List.of(testProduct1)));
        setupSearchMultiField(mapping);

        when(searchHistoryRepository.findByUserIdAndKeyword(USER_ID, "手机"))
            .thenReturn(Optional.of(testSearchHistory));

        // When
        productSearchService.searchWithRelevance("手机", null, null, null, USER_ID, 0, 10);

        // Then
        verify(searchHistoryRepository).save(argThat(history ->
            history.getSearchCount() == 4  // 原来是3，+1后是4
        ));
    }

    @Test
    @DisplayName("智能搜索 - 空关键词不记录历史")
    void searchWithRelevance_EmptyKeyword_NoHistory() {
        // Given: 空关键词，使用 any() 匹配（因为空字符串经过 trim 后可能传入 null 或 ""）
        Map<String, Page<Product>> mapping = new HashMap<>();
        // 空关键词时 service 传入 keyword="" 给 repository
        when(productRepository.searchMultiField(any(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(testProduct1)));

        // When
        productSearchService.searchWithRelevance("", null, null, null, USER_ID, 0, 10);

        // Then: 不应记录搜索历史
        verify(searchHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("智能搜索 - 分类过滤")
    void searchWithRelevance_CategoryFilter() {
        // Given: 带分类过滤的搜索，因为category不为null，同义词扩展也会传入相同category
        Map<String, Page<Product>> mapping = new HashMap<>();
        mapping.put("iPhone", new PageImpl<>(List.of(testProduct1)));
        setupSearchMultiField(mapping);

        // When
        Page<ProductSearchResult> results = productSearchService.searchWithRelevance(
            "iPhone", "手机数码", null, null, USER_ID, 0, 10);

        // Then
        assertNotNull(results);
        verify(productRepository).searchMultiField(eq("iPhone"), eq("手机数码"), isNull(), isNull(), eq(ProductStatus.ON_SALE), any(Pageable.class));
    }

    @Test
    @DisplayName("智能搜索 - 相关性评分排序")
    void searchWithRelevance_RelevanceSorting() {
        // Given: 两个商品，名称匹配的应该排在描述匹配的前面
        Product nameMatchProduct = new Product();
        nameMatchProduct.setId(10L);
        nameMatchProduct.setName("手机壳");
        nameMatchProduct.setDescription("保护套");
        nameMatchProduct.setCategory("手机数码");
        nameMatchProduct.setStatus(ProductStatus.ON_SALE);
        nameMatchProduct.setSalesCount(10);
        nameMatchProduct.setViewCount(50);
        nameMatchProduct.setCreatedAt(LocalDateTime.now());
        nameMatchProduct.setSellerId(2L);

        Product descMatchProduct = new Product();
        descMatchProduct.setId(11L);
        descMatchProduct.setName("保护膜");
        descMatchProduct.setDescription("适用于手机的保护膜");
        descMatchProduct.setCategory("配件");
        descMatchProduct.setStatus(ProductStatus.ON_SALE);
        descMatchProduct.setSalesCount(5);
        descMatchProduct.setViewCount(20);
        descMatchProduct.setCreatedAt(LocalDateTime.now());
        descMatchProduct.setSellerId(3L);

        Map<String, Page<Product>> mapping = new HashMap<>();
        mapping.put("手机", new PageImpl<>(List.of(nameMatchProduct, descMatchProduct)));
        setupSearchMultiField(mapping);

        // When
        Page<ProductSearchResult> results = productSearchService.searchWithRelevance(
            "手机", null, null, null, USER_ID, 0, 10);

        // Then: 名称匹配的商品分数应高于描述匹配
        if (results.getContent().size() >= 2) {
            ProductSearchResult first = results.getContent().get(0);
            ProductSearchResult second = results.getContent().get(1);
            assertTrue(first.getRelevanceScore() >= second.getRelevanceScore(),
                "结果应按相关性得分降序排列");
        }
    }

    @Test
    @DisplayName("获取热门商品 - 正常返回")
    void getHotProducts_Success() {
        // Given
        when(productRepository.findTopBySalesCount(eq(ProductStatus.ON_SALE), any(Pageable.class)))
            .thenReturn(List.of(testProduct1, testProduct2));

        // When
        List<ProductResponse> results = productSearchService.getHotProducts(10);

        // Then
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("获取分类热门商品 - 正常返回")
    void getHotProductsByCategory_Success() {
        // Given
        when(productRepository.findTopByCategoryAndSalesCount(eq(ProductStatus.ON_SALE), eq("手机数码"), any(Pageable.class)))
            .thenReturn(List.of(testProduct1));

        // When
        List<ProductResponse> results = productSearchService.getHotProductsByCategory("手机数码", 10);

        // Then
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("获取热门关键词 - 正常返回")
    void getHotKeywords_Success() {
        // Given
        when(searchHistoryRepository.findHotKeywords())
            .thenReturn(List.of("手机", "电脑", "耳机"));

        // When
        List<String> keywords = productSearchService.getHotKeywords(5);

        // Then
        assertNotNull(keywords);
        assertEquals(3, keywords.size());
        assertEquals("手机", keywords.get(0));
    }

    @Test
    @DisplayName("获取用户搜索历史 - 正常返回")
    void getUserSearchHistory_Success() {
        // Given
        when(searchHistoryRepository.findByUserIdOrderByLastSearchTimeDesc(USER_ID))
            .thenReturn(List.of(testSearchHistory));

        // When
        List<String> history = productSearchService.getUserSearchHistory(USER_ID, 10);

        // Then
        assertNotNull(history);
        assertEquals(1, history.size());
        assertEquals("手机", history.get(0));
    }

    @Test
    @DisplayName("清除用户搜索历史 - 成功")
    void clearUserSearchHistory_Success() {
        // When
        productSearchService.clearUserSearchHistory(USER_ID);

        // Then
        verify(searchHistoryRepository).deleteByUserId(USER_ID);
    }
}
