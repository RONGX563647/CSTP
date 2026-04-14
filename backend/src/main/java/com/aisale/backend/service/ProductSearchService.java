package com.aisale.backend.service;

import com.aisale.backend.dto.ProductResponse;
import com.aisale.backend.dto.ProductSearchResult;
import com.aisale.backend.entity.Product;
import com.aisale.backend.entity.Product.ProductStatus;
import com.aisale.backend.entity.SearchHistory;
import com.aisale.backend.repository.ProductRepository;
import com.aisale.backend.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final SearchHistoryRepository searchHistoryRepository;

    /**
     * 同义词映射表 - 用于扩展搜索关键词
     * key: 标准词, value: 同义词列表
     */
    private static final Map<String, List<String>> SYNONYM_MAP = new HashMap<>();

    static {
        SYNONYM_MAP.put("手机", Arrays.asList("电话", "移动", "iphone", "华为", "小米", "三星", "oppo", "vivo"));
        SYNONYM_MAP.put("电脑", Arrays.asList("笔记本", "计算机", "pc", "macbook", "thinkpad", "台式机"));
        SYNONYM_MAP.put("平板", Arrays.asList("ipad", "tablet", "pad"));
        SYNONYM_MAP.put("耳机", Arrays.asList("earphone", "headphone", "airpods", "蓝牙耳机", "头戴式"));
        SYNONYM_MAP.put("键盘", Arrays.asList("keyboard", "机械键盘", "薄膜键盘"));
        SYNONYM_MAP.put("鼠标", Arrays.asList("mouse", "光电鼠"));
        SYNONYM_MAP.put("显示器", Arrays.asList("屏幕", "monitor", "显示屏"));
        SYNONYM_MAP.put("相机", Arrays.asList("摄影", "单反", "微单", "camera", "拍立得"));
        SYNONYM_MAP.put("游戏机", Arrays.asList("ps5", "switch", "xbox", "掌机"));
        SYNONYM_MAP.put("音箱", Arrays.asList("音响", "speaker", "蓝牙音箱"));
    }

    /**
     * 分类关键词映射 - 输入关键词自动关联分类
     */
    private static final Map<String, String> KEYWORD_CATEGORY_MAP = new HashMap<>();

    static {
        KEYWORD_CATEGORY_MAP.put("手机", "手机数码");
        KEYWORD_CATEGORY_MAP.put("iphone", "手机数码");
        KEYWORD_CATEGORY_MAP.put("华为", "手机数码");
        KEYWORD_CATEGORY_MAP.put("小米", "手机数码");
        KEYWORD_CATEGORY_MAP.put("电脑", "电脑办公");
        KEYWORD_CATEGORY_MAP.put("笔记本", "电脑办公");
        KEYWORD_CATEGORY_MAP.put("键盘", "电脑办公");
        KEYWORD_CATEGORY_MAP.put("鼠标", "电脑办公");
        KEYWORD_CATEGORY_MAP.put("冰箱", "家用电器");
        KEYWORD_CATEGORY_MAP.put("洗衣机", "家用电器");
        KEYWORD_CATEGORY_MAP.put("空调", "家用电器");
        KEYWORD_CATEGORY_MAP.put("电视", "家用电器");
        KEYWORD_CATEGORY_MAP.put("游戏", "娱乐玩具");
        KEYWORD_CATEGORY_MAP.put("玩具", "娱乐玩具");
        KEYWORD_CATEGORY_MAP.put("手办", "娱乐玩具");
    }

    @Transactional
    public Page<ProductSearchResult> searchWithRelevance(String keyword, String category,
                                                          BigDecimal minPrice, BigDecimal maxPrice,
                                                          Long userId, int page, int size) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            recordSearchHistory(userId, keyword.trim());
        }

        Pageable pageable = PageRequest.of(page, size);

        // 1. 用原始关键词搜索
        Page<Product> products = productRepository.searchMultiField(
            keyword, category, minPrice, maxPrice, ProductStatus.ON_SALE, pageable
        );

        List<Product> allProducts = new ArrayList<>(products.getContent());

        // 2. 如果原始关键词搜索结果不足，使用同义词扩展搜索
        if (keyword != null && !keyword.trim().isEmpty() && allProducts.size() < size) {
            Set<Long> existingIds = allProducts.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

            List<String> synonyms = expandKeywords(keyword.trim());
            for (String synonym : synonyms) {
                if (allProducts.size() >= size * 2) break; // 足够多了就停止
                Page<Product> synonymResults = productRepository.searchMultiField(
                    synonym, category, minPrice, maxPrice, ProductStatus.ON_SALE, pageable
                );
                for (Product p : synonymResults.getContent()) {
                    if (!existingIds.contains(p.getId())) {
                        allProducts.add(p);
                        existingIds.add(p.getId());
                    }
                }
            }
        }

        // 3. 计算相关性得分并排序
        List<ProductSearchResult> results = allProducts.stream()
            .map(product -> {
                double score = calculateEnhancedRelevance(product, keyword);
                String matchType = determineMatchType(product, keyword);
                return ProductSearchResult.builder()
                    .product(ProductResponse.fromEntity(product))
                    .relevanceScore(score)
                    .matchType(matchType)
                    .build();
            })
            .sorted(Comparator.comparingDouble(ProductSearchResult::getRelevanceScore).reversed())
            .collect(Collectors.toList());

        // 4. 分页处理
        int start = page * size;
        int end = Math.min(start + size, results.size());
        if (start > results.size()) {
            start = results.size();
            end = results.size();
        }
        List<ProductSearchResult> pagedResults = results.subList(start, end);

        return new PageImpl<>(pagedResults, pageable, results.size());
    }

    /**
     * 扩展关键词 - 通过同义词表获取相关词
     */
    private List<String> expandKeywords(String keyword) {
        List<String> expanded = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        // 检查关键词是否是某个同义词组的一部分
        for (Map.Entry<String, List<String>> entry : SYNONYM_MAP.entrySet()) {
            String standardWord = entry.getKey();
            List<String> synonyms = entry.getValue();

            // 如果关键词匹配标准词或其同义词
            if (standardWord.toLowerCase().contains(lowerKeyword) ||
                lowerKeyword.contains(standardWord.toLowerCase())) {
                expanded.add(standardWord);
                expanded.addAll(synonyms);
            }
            for (String synonym : synonyms) {
                if (synonym.toLowerCase().contains(lowerKeyword) ||
                    lowerKeyword.contains(synonym.toLowerCase())) {
                    expanded.add(standardWord);
                    expanded.addAll(synonyms);
                    break;
                }
            }
        }

        // 去重并移除与原始关键词相同的项
        return expanded.stream()
            .distinct()
            .filter(w -> !w.equalsIgnoreCase(keyword))
            .collect(Collectors.toList());
    }

    @Cacheable(value = "hotProducts", key = "#limit")
    @Transactional(readOnly = true)
    public List<ProductResponse> getHotProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Product> products = productRepository.findTopBySalesCount(ProductStatus.ON_SALE, pageable);
        return products.stream()
            .map(ProductResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "hotProductsByCategory", key = "#category + '_' + #limit")
    @Transactional(readOnly = true)
    public List<ProductResponse> getHotProductsByCategory(String category, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Product> products = productRepository.findTopByCategoryAndSalesCount(
            ProductStatus.ON_SALE, category, pageable
        );
        return products.stream()
            .map(ProductResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getHotKeywords(int limit) {
        List<String> keywords = searchHistoryRepository.findHotKeywords();
        return keywords.stream().limit(limit).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getUserSearchHistory(Long userId, int limit) {
        List<SearchHistory> history = searchHistoryRepository.findByUserIdOrderByLastSearchTimeDesc(userId);
        return history.stream()
            .map(SearchHistory::getKeyword)
            .limit(limit)
            .collect(Collectors.toList());
    }

    @Transactional
    public void clearUserSearchHistory(Long userId) {
        searchHistoryRepository.deleteByUserId(userId);
        log.info("Cleared search history for user: {}", userId);
    }

    @CacheEvict(value = {"productSearch", "hotProducts", "hotProductsByCategory"}, allEntries = true)
    public void clearSearchCache() {
        log.info("Product search cache cleared");
    }

    private void recordSearchHistory(Long userId, String keyword) {
        if (userId == null) return;

        Optional<SearchHistory> existing = searchHistoryRepository.findByUserIdAndKeyword(userId, keyword);

        if (existing.isPresent()) {
            SearchHistory history = existing.get();
            history.setSearchCount(history.getSearchCount() + 1);
            history.setLastSearchTime(LocalDateTime.now());
            searchHistoryRepository.save(history);
        } else {
            SearchHistory history = new SearchHistory();
            history.setUserId(userId);
            history.setKeyword(keyword);
            history.setSearchCount(1);
            history.setLastSearchTime(LocalDateTime.now());
            searchHistoryRepository.save(history);
        }
    }

    /**
     * 增强版相关性评分算法
     * 综合考虑：名称匹配、描述匹配、标签匹配、分类匹配、同义词匹配、热度加权
     */
    private double calculateEnhancedRelevance(Product product, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return calculateBaseScore(product);
        }

        double score = 0.0;
        String lowerKeyword = keyword.toLowerCase().trim();
        String[] keywords = lowerKeyword.split("\\s+");

        for (String kw : keywords) {
            if (kw.length() < 1) continue; // 允许单字搜索

            // ===== 名称匹配（权重最高）=====
            if (product.getName() != null) {
                String lowerName = product.getName().toLowerCase();
                if (lowerName.equals(kw)) {
                    score += 200.0; // 完全匹配，最高权重
                } else if (lowerName.startsWith(kw)) {
                    score += 150.0; // 前缀匹配
                } else if (lowerName.contains(kw)) {
                    int position = lowerName.indexOf(kw);
                    score += 120.0 - (position * 0.5); // 包含匹配，越靠前越高
                }
                // 名称中任意词匹配（分词后匹配）
                String[] nameWords = lowerName.split("[\\s,，、|]+");
                for (String nameWord : nameWords) {
                    if (nameWord.startsWith(kw) && !lowerName.contains(kw)) {
                        score += 60.0; // 分词后前缀匹配
                    } else if (nameWord.contains(kw) && !lowerName.contains(kw)) {
                        score += 40.0; // 分词后包含匹配
                    }
                }
            }

            // ===== 分类匹配 =====
            if (product.getCategory() != null) {
                String lowerCategory = product.getCategory().toLowerCase();
                if (lowerCategory.equals(kw)) {
                    score += 80.0;
                } else if (lowerCategory.contains(kw)) {
                    score += 60.0;
                }
            }

            // ===== 标签匹配 =====
            if (product.getTags() != null) {
                for (String tag : product.getTags()) {
                    String lowerTag = tag.toLowerCase();
                    if (lowerTag.equals(kw)) {
                        score += 60.0;
                    } else if (lowerTag.contains(kw)) {
                        score += 45.0;
                    }
                }
            }

            // ===== 描述匹配 =====
            if (product.getDescription() != null) {
                String lowerDesc = product.getDescription().toLowerCase();
                if (lowerDesc.contains(kw)) {
                    int count = countOccurrences(lowerDesc, kw);
                    score += 25.0 * Math.min(count, 5); // 最多5次计数，避免长描述刷分
                }
            }

            // ===== 同义词匹配加分 =====
            score += calculateSynonymBonus(product, kw);
        }

        // ===== 热度加权 =====
        score += product.getSalesCount() * 0.5;
        score += product.getViewCount() * 0.05;

        // ===== 推荐商品加分 =====
        if (product.getIsFeatured() != null && product.getIsFeatured()) {
            score += 30.0;
        }

        // ===== 折扣商品加分 =====
        if (product.getDiscount() != null && product.getDiscount().compareTo(BigDecimal.valueOf(0.8)) < 0) {
            score += 20.0;
        }

        return score;
    }

    /**
     * 同义词匹配加分
     * 如果商品信息中包含了关键词的同义词，给予一定加分
     */
    private double calculateSynonymBonus(Product product, String keyword) {
        double bonus = 0.0;
        String lowerKeyword = keyword.toLowerCase();

        for (Map.Entry<String, List<String>> entry : SYNONYM_MAP.entrySet()) {
            String standardWord = entry.getKey();
            List<String> synonyms = entry.getValue();

            boolean keywordMatch = standardWord.equalsIgnoreCase(lowerKeyword) ||
                synonyms.stream().anyMatch(s -> s.equalsIgnoreCase(lowerKeyword));

            if (!keywordMatch) continue;

            // 关键词命中同义词组，检查商品是否包含同义词
            Set<String> allSynonymWords = new HashSet<>(synonyms);
            allSynonymWords.add(standardWord);

            // 检查商品名称
            if (product.getName() != null) {
                String lowerName = product.getName().toLowerCase();
                for (String synonym : allSynonymWords) {
                    if (lowerName.contains(synonym.toLowerCase()) && !lowerName.contains(lowerKeyword)) {
                        bonus += 35.0; // 名称同义词匹配
                        break;
                    }
                }
            }

            // 检查商品描述
            if (product.getDescription() != null) {
                String lowerDesc = product.getDescription().toLowerCase();
                for (String synonym : allSynonymWords) {
                    if (lowerDesc.contains(synonym.toLowerCase()) && !lowerDesc.contains(lowerKeyword)) {
                        bonus += 15.0; // 描述同义词匹配
                        break;
                    }
                }
            }

            // 检查商品标签
            if (product.getTags() != null) {
                for (String tag : product.getTags()) {
                    for (String synonym : allSynonymWords) {
                        if (tag.toLowerCase().contains(synonym.toLowerCase()) && !tag.toLowerCase().contains(lowerKeyword)) {
                            bonus += 25.0; // 标签同义词匹配
                            break;
                        }
                    }
                }
            }

            break; // 只匹配第一个同义词组
        }

        return bonus;
    }

    private double calculateBaseScore(Product product) {
        double score = 100.0;
        score += product.getSalesCount() * 0.5;
        score += product.getViewCount() * 0.05;
        if (product.getIsFeatured() != null && product.getIsFeatured()) {
            score += 30.0;
        }
        return score;
    }

    private int countOccurrences(String text, String keyword) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }

    /**
     * 确定匹配类型 - 用于前端展示匹配来源
     */
    private String determineMatchType(Product product, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return "none";
        }

        String lowerKeyword = keyword.toLowerCase();

        if (product.getName() != null && product.getName().toLowerCase().contains(lowerKeyword)) {
            return "name";
        }

        if (product.getCategory() != null && product.getCategory().toLowerCase().contains(lowerKeyword)) {
            return "category";
        }

        if (product.getTags() != null) {
            for (String tag : product.getTags()) {
                if (tag.toLowerCase().contains(lowerKeyword)) {
                    return "tag";
                }
            }
        }

        if (product.getDescription() != null && product.getDescription().toLowerCase().contains(lowerKeyword)) {
            return "description";
        }

        // 检查同义词匹配
        if (isSynonymMatch(product, lowerKeyword)) {
            return "synonym";
        }

        return "none";
    }

    /**
     * 检查是否是同义词匹配
     */
    private boolean isSynonymMatch(Product product, String lowerKeyword) {
        for (Map.Entry<String, List<String>> entry : SYNONYM_MAP.entrySet()) {
            String standardWord = entry.getKey();
            List<String> synonyms = entry.getValue();

            boolean keywordMatch = standardWord.equalsIgnoreCase(lowerKeyword) ||
                synonyms.stream().anyMatch(s -> s.equalsIgnoreCase(lowerKeyword));

            if (!keywordMatch) continue;

            Set<String> allSynonymWords = new HashSet<>(synonyms);
            allSynonymWords.add(standardWord);

            if (product.getName() != null) {
                String lowerName = product.getName().toLowerCase();
                for (String synonym : allSynonymWords) {
                    if (lowerName.contains(synonym.toLowerCase())) return true;
                }
            }
            if (product.getDescription() != null) {
                String lowerDesc = product.getDescription().toLowerCase();
                for (String synonym : allSynonymWords) {
                    if (lowerDesc.contains(synonym.toLowerCase())) return true;
                }
            }
            if (product.getTags() != null) {
                for (String tag : product.getTags()) {
                    for (String synonym : allSynonymWords) {
                        if (tag.toLowerCase().contains(synonym.toLowerCase())) return true;
                    }
                }
            }
            break;
        }
        return false;
    }
}
