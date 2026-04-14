package com.aisale.backend.service;

import com.aisale.backend.dto.ProductResponse;
import com.aisale.backend.entity.Product;
import com.aisale.backend.entity.Product.ProductStatus;
import com.aisale.backend.entity.SearchHistory;
import com.aisale.backend.repository.ProductRepository;
import com.aisale.backend.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品推荐服务
 * 
 * 推荐算法策略：
 * 1. 首页推荐：精选推荐 > 热门商品 > 新品 > 折扣商品，综合排序
 * 2. 分类推荐：同分类下按销量+浏览量+折扣综合排序
 * 3. 猜你喜欢（个性化）：基于用户搜索历史和浏览行为的推荐
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductRecommendService {

    private final ProductRepository productRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final ProductSearchService productSearchService;

    // ==================== 推荐权重配置 ====================
    
    /** 推荐标记权重 */
    private static final double WEIGHT_FEATURED = 30.0;
    /** 销量权重 */
    private static final double WEIGHT_SALES = 2.0;
    /** 浏览量权重 */
    private static final double WEIGHT_VIEWS = 0.1;
    /** 折扣权重（折扣越低权重越高） */
    private static final double WEIGHT_DISCOUNT = 25.0;
    /** 新品权重（7天内发布的商品） */
    private static final double WEIGHT_NEW = 20.0;
    /** 新品时间衰减系数（每天衰减比例） */
    private static final double NEW_DECAY_RATE = 0.05;
    /** 新品有效期（天） */
    private static final int NEW_PRODUCT_DAYS = 14;

    /**
     * 获取首页推荐商品
     * 算法：综合评分 = 推荐标记权重 + 销量权重 + 浏览量权重 + 折扣权重 + 新品权重
     * 
     * @param limit 返回数量
     * @return 推荐商品列表
     */
    @Cacheable(value = "homeRecommend", key = "#limit")
    @Transactional(readOnly = true)
    public List<ProductResponse> getHomeRecommend(int limit) {
        log.debug("获取首页推荐商品, limit={}", limit);
        
        // 1. 获取所有在售商品（限制查询范围避免全表扫描）
        Pageable largePage = PageRequest.of(0, Math.min(limit * 5, 200));
        List<Product> onSaleProducts = productRepository
            .findByIsOnSaleTrueAndStatus(ProductStatus.ON_SALE, largePage)
            .getContent();

        if (onSaleProducts.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 计算推荐评分并排序
        LocalDateTime now = LocalDateTime.now();
        List<ScoredProduct> scoredProducts = onSaleProducts.stream()
            .map(p -> new ScoredProduct(p, calculateRecommendScore(p, now)))
            .sorted(Comparator.comparingDouble(ScoredProduct::getScore).reversed())
            .collect(Collectors.toList());

        // 3. 保证推荐商品优先，然后按评分排序
        List<ScoredProduct> featured = scoredProducts.stream()
            .filter(sp -> sp.product.getIsFeatured() != null && sp.product.getIsFeatured())
            .collect(Collectors.toList());
        List<ScoredProduct> nonFeatured = scoredProducts.stream()
            .filter(sp -> sp.product.getIsFeatured() == null || !sp.product.getIsFeatured())
            .collect(Collectors.toList());

        List<ScoredProduct> result = new ArrayList<>();
        result.addAll(featured);
        result.addAll(nonFeatured);

        // 4. 截取并转换
        return result.stream()
            .limit(limit)
            .map(sp -> ProductResponse.fromEntity(sp.product))
            .collect(Collectors.toList());
    }

    /**
     * 获取分类推荐商品
     * 算法：同分类下按综合评分排序
     * 
     * @param category 分类名称
     * @param limit 返回数量
     * @return 推荐商品列表
     */
    @Cacheable(value = "categoryRecommend", key = "#category + '_' + #limit")
    @Transactional(readOnly = true)
    public List<ProductResponse> getCategoryRecommend(String category, int limit) {
        log.debug("获取分类推荐商品, category={}, limit={}", category, limit);
        
        List<Product> products = productRepository.findByCategory(category, PageRequest.of(0, Math.min(limit * 3, 100)))
            .getContent();

        if (products.isEmpty()) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now();
        return products.stream()
            .filter(p -> p.getStatus() == ProductStatus.ON_SALE)
            .map(p -> new ScoredProduct(p, calculateRecommendScore(p, now)))
            .sorted(Comparator.comparingDouble(ScoredProduct::getScore).reversed())
            .limit(limit)
            .map(sp -> ProductResponse.fromEntity(sp.product))
            .collect(Collectors.toList());
    }

    /**
     * 猜你喜欢 - 基于用户行为的个性化推荐
     * 
     * 算法：
     * 1. 获取用户搜索历史，提取兴趣关键词
     * 2. 获取用户偏好分类
     * 3. 基于兴趣关键词搜索相关商品
     * 4. 基于偏好分类推荐热门商品
     * 5. 补充全局热门商品
     * 6. 合并去重，综合排序
     *
     * @param userId 用户ID（可为null，null时返回全局热门）
     * @param limit 返回数量
     * @return 推荐商品列表
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getPersonalizedRecommend(Long userId, int limit) {
        log.debug("获取个性化推荐, userId={}, limit={}", userId, limit);

        // 未登录用户：返回全局热门+精选
        if (userId == null) {
            return getHomeRecommend(limit);
        }

        Set<Long> addedIds = new HashSet<>();
        List<ScoredProduct> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. 基于搜索历史的推荐
        List<SearchHistory> searchHistories = searchHistoryRepository
            .findByUserIdOrderByLastSearchTimeDesc(userId);
        
        // 提取用户兴趣关键词（按搜索频次加权）
        Map<String, Double> interestKeywords = new LinkedHashMap<>();
        for (SearchHistory sh : searchHistories) {
            double weight = sh.getSearchCount() * 1.0;
            // 越近搜索的关键词权重越高
            long daysSince = ChronoUnit.DAYS.between(sh.getLastSearchTime(), now);
            if (daysSince < 7) {
                weight *= (1.0 + (7 - daysSince) * 0.1);
            }
            interestKeywords.merge(sh.getKeyword(), weight, (oldVal, newVal) -> oldVal + newVal);
        }

        // 按权重排序取前5个关键词
        List<String> topKeywords = interestKeywords.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // 用关键词搜索相关商品
        for (String keyword : topKeywords) {
            if (result.size() >= limit) break;
            try {
                Pageable smallPage = PageRequest.of(0, 10);
                List<Product> searched = productRepository.searchMultiField(
                    keyword, null, null, null, ProductStatus.ON_SALE, smallPage
                ).getContent();
                
                for (Product p : searched) {
                    if (!addedIds.contains(p.getId())) {
                        double score = calculateRecommendScore(p, now) + 50.0; // 搜索历史匹配加分
                        result.add(new ScoredProduct(p, score));
                        addedIds.add(p.getId());
                    }
                }
            } catch (Exception e) {
                log.warn("基于关键词搜索推荐失败: keyword={}", keyword, e);
            }
        }

        // 2. 基于偏好分类的推荐
        Set<String> preferredCategories = extractPreferredCategories(topKeywords);
        for (String category : preferredCategories) {
            if (result.size() >= limit) break;
            List<ProductResponse> categoryHot = productSearchService.getHotProductsByCategory(category, 5);
                for (ProductResponse pr : categoryHot) {
                    if (!addedIds.contains(pr.getId())) {
                        try {
                            Optional<Product> opt = productRepository.findById(pr.getId());
                            if (opt.isPresent()) {
                                Product p = opt.get();
                                double score = calculateRecommendScore(p, now) + 30.0; // 分类偏好加分
                                result.add(new ScoredProduct(p, score));
                                addedIds.add(p.getId());
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                }
        }

        // 3. 补充精选推荐商品
        List<ProductResponse> featured = productSearchService.getHotProducts(10);
        for (ProductResponse pr : featured) {
            if (result.size() >= limit) break;
            if (!addedIds.contains(pr.getId())) {
                try {
                    Optional<Product> opt = productRepository.findById(pr.getId());
                    if (opt.isPresent()) {
                        Product p = opt.get();
                        double score = calculateRecommendScore(p, now) + WEIGHT_FEATURED;
                        result.add(new ScoredProduct(p, score));
                        addedIds.add(p.getId());
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        // 4. 按综合评分排序
        result.sort(Comparator.comparingDouble(ScoredProduct::getScore).reversed());

        return result.stream()
            .limit(limit)
            .map(sp -> ProductResponse.fromEntity(sp.product))
            .collect(Collectors.toList());
    }

    /**
     * 获取新品推荐
     * 算法：按创建时间倒序，优先在售商品
     */
    @Cacheable(value = "newProducts", key = "#limit")
    @Transactional(readOnly = true)
    public List<ProductResponse> getNewProducts(int limit) {
        List<Product> allProducts = productRepository
            .findByIsOnSaleTrueAndStatus(ProductStatus.ON_SALE, PageRequest.of(0, limit * 2))
            .getContent();

        LocalDateTime now = LocalDateTime.now();
        return allProducts.stream()
            .filter(p -> {
                if (p.getCreatedAt() == null) return false;
                long days = ChronoUnit.DAYS.between(p.getCreatedAt(), now);
                return days <= NEW_PRODUCT_DAYS;
            })
            .sorted(Comparator.comparing(Product::getCreatedAt).reversed())
            .limit(limit)
            .map(ProductResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * 获取折扣推荐
     * 算法：按折扣力度排序（折扣越低越优先）
     */
    @Cacheable(value = "discountProducts", key = "#limit")
    @Transactional(readOnly = true)
    public List<ProductResponse> getDiscountProducts(int limit) {
        List<Product> allProducts = productRepository
            .findByIsOnSaleTrueAndStatus(ProductStatus.ON_SALE, PageRequest.of(0, limit * 3))
            .getContent();

        return allProducts.stream()
            .filter(p -> p.getDiscount() != null && p.getDiscount().compareTo(BigDecimal.valueOf(0.9)) < 0)
            .sorted(Comparator.comparing(Product::getDiscount))
            .limit(limit)
            .map(ProductResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * 清除推荐缓存
     */
    @CacheEvict(value = {"homeRecommend", "categoryRecommend", "newProducts", "discountProducts"}, allEntries = true)
    public void clearRecommendCache() {
        log.info("商品推荐缓存已清除");
    }

    // ==================== 私有方法 ====================

    /**
     * 计算商品推荐评分
     * 
     * 评分公式：
     * score = featured_weight + sales_weight * salesCount + views_weight * viewCount 
     *       + discount_weight * (1 - discount) + new_weight * newFactor
     * 
     * 其中 newFactor 随时间衰减：newFactor = max(0, 1 - daysSinceCreated * decay_rate)
     */
    private double calculateRecommendScore(Product product, LocalDateTime now) {
        double score = 0.0;

        // 1. 推荐标记
        if (product.getIsFeatured() != null && product.getIsFeatured()) {
            score += WEIGHT_FEATURED;
        }

        // 2. 销量加权
        score += WEIGHT_SALES * Math.log1p(product.getSalesCount());

        // 3. 浏览量加权
        score += WEIGHT_VIEWS * Math.log1p(product.getViewCount());

        // 4. 折扣加权（折扣越低，分数越高）
        if (product.getDiscount() != null && product.getDiscount().compareTo(BigDecimal.ONE) < 0) {
            double discountValue = 1.0 - product.getDiscount().doubleValue();
            score += WEIGHT_DISCOUNT * discountValue;
        }

        // 5. 新品加权（随时间衰减）
        if (product.getCreatedAt() != null) {
            long daysSinceCreated = ChronoUnit.DAYS.between(product.getCreatedAt(), now);
            if (daysSinceCreated >= 0 && daysSinceCreated <= NEW_PRODUCT_DAYS) {
                double newFactor = Math.max(0, 1.0 - daysSinceCreated * NEW_DECAY_RATE);
                score += WEIGHT_NEW * newFactor;
            }
        }

        return score;
    }

    /**
     * 从关键词中提取偏好分类
     */
    private Set<String> extractPreferredCategories(List<String> keywords) {
        Set<String> categories = new LinkedHashSet<>();
        
        // 关键词-分类映射（与搜索服务保持一致）
        Map<String, String> keywordCategoryMap = new HashMap<>();
        keywordCategoryMap.put("手机", "手机数码");
        keywordCategoryMap.put("iphone", "手机数码");
        keywordCategoryMap.put("华为", "手机数码");
        keywordCategoryMap.put("小米", "手机数码");
        keywordCategoryMap.put("三星", "手机数码");
        keywordCategoryMap.put("电脑", "电脑办公");
        keywordCategoryMap.put("笔记本", "电脑办公");
        keywordCategoryMap.put("键盘", "电脑办公");
        keywordCategoryMap.put("鼠标", "电脑办公");
        keywordCategoryMap.put("冰箱", "家用电器");
        keywordCategoryMap.put("洗衣机", "家用电器");
        keywordCategoryMap.put("空调", "家用电器");
        keywordCategoryMap.put("电视", "家用电器");
        keywordCategoryMap.put("游戏", "娱乐玩具");
        keywordCategoryMap.put("玩具", "娱乐玩具");
        keywordCategoryMap.put("手办", "娱乐玩具");

        for (String keyword : keywords) {
            String lowerKw = keyword.toLowerCase();
            for (Map.Entry<String, String> entry : keywordCategoryMap.entrySet()) {
                if (lowerKw.contains(entry.getKey()) || entry.getKey().contains(lowerKw)) {
                    categories.add(entry.getValue());
                }
            }
        }

        return categories;
    }

    /**
     * 评分商品内部类
     */
    private static class ScoredProduct {
        final Product product;
        final double score;

        ScoredProduct(Product product, double score) {
            this.product = product;
            this.score = score;
        }

        double getScore() {
            return score;
        }
    }
}
