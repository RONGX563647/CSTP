package com.aisale.backend.service;

import com.aisale.backend.dto.ProductRequest;
import com.aisale.backend.dto.ProductResponse;
import com.aisale.backend.entity.Product;
import com.aisale.backend.exception.business.ForbiddenException;
import com.aisale.backend.exception.business.NotFoundException;
import com.aisale.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request, Long sellerId) {
        Product product = new Product();
        product.setSellerId(sellerId);
        updateProductFromRequest(product, request);
        product = productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, Long sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("商品不存在"));
        // 检查商品是否属于当前用户
        if (sellerId != null && !product.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("无权操作此商品");
        }
        updateProductFromRequest(product, request);
        product = productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public void deleteProduct(Long id, Long sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("商品不存在"));
        // 检查商品是否属于当前用户
        if (sellerId != null && !product.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("无权操作此商品");
        }
        productRepository.deleteById(id);
    }

    @Transactional
    public ProductResponse updateStock(Long id, Integer stock, Long sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("商品不存在"));
        // 检查商品是否属于当前用户
        if (sellerId != null && !product.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("无权操作此商品");
        }
        product.setStock(stock);

        // 自动更新状态
        if (stock <= 0) {
            product.setStatus(Product.ProductStatus.OUT_OF_STOCK);
        } else if (!product.getIsOnSale()) {
            product.setStatus(Product.ProductStatus.OFF_SALE);
        } else {
            product.setStatus(Product.ProductStatus.ON_SALE);
        }

        product = productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse updateStatus(Long id, Product.ProductStatus status, Long sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("商品不存在"));
        // 检查商品是否属于当前用户
        if (sellerId != null && !product.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("无权操作此商品");
        }
        product.setStatus(status);

        if (status == Product.ProductStatus.OFF_SALE) {
            product.setIsOnSale(false);
        } else if (status == Product.ProductStatus.ON_SALE && product.getStock() > 0) {
            product.setIsOnSale(true);
        }

        product = productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    // ==================== 管理端方法（不检查权限）====================

    @Transactional
    public ProductResponse createProductForAdmin(ProductRequest request) {
        Product product = new Product();
        updateProductFromRequest(product, request);
        product = productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse updateProductForAdmin(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("商品不存在"));
        updateProductFromRequest(product, request);
        product = productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public void deleteProductForAdmin(Long id) {
        productRepository.deleteById(id);
    }

    @Transactional
    public ProductResponse updateStockForAdmin(Long id, Integer stock) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("商品不存在"));
        product.setStock(stock);

        // 自动更新状态
        if (stock <= 0) {
            product.setStatus(Product.ProductStatus.OUT_OF_STOCK);
        } else if (!product.getIsOnSale()) {
            product.setStatus(Product.ProductStatus.OFF_SALE);
        } else {
            product.setStatus(Product.ProductStatus.ON_SALE);
        }

        product = productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse updateStatusForAdmin(Long id, Product.ProductStatus status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("商品不存在"));
        product.setStatus(status);

        if (status == Product.ProductStatus.OFF_SALE) {
            product.setIsOnSale(false);
        } else if (status == Product.ProductStatus.ON_SALE && product.getStock() > 0) {
            product.setIsOnSale(true);
        }

        product = productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(ProductResponse::fromEntity);
    }

    public Page<ProductResponse> getProductsByStatus(Product.ProductStatus status, Pageable pageable) {
        return productRepository.findByStatus(status, pageable)
                .map(ProductResponse::fromEntity);
    }

    public Page<ProductResponse> getProductsByCategory(String category, Pageable pageable) {
        return productRepository.findByCategory(category, pageable)
                .map(ProductResponse::fromEntity);
    }

    public Page<ProductResponse> getOnSaleProducts(Pageable pageable) {
        return productRepository.findByIsOnSaleTrueAndStatus(Product.ProductStatus.ON_SALE, pageable)
                .map(ProductResponse::fromEntity);
    }

    public List<ProductResponse> getFeaturedProducts() {
        return productRepository.findByIsFeaturedTrueAndStatus(Product.ProductStatus.ON_SALE)
                .stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public Page<ProductResponse> searchProductsForAdmin(String name, String category,
                                                        BigDecimal minPrice, BigDecimal maxPrice,
                                                        Product.ProductStatus status, Pageable pageable) {
        return productRepository.searchProducts(name, category, minPrice, maxPrice, status, null, pageable)
                .map(ProductResponse::fromEntity);
    }

    // ==================== 用户端方法（带权限检查）====================

    public Page<ProductResponse> getMyProducts(Long sellerId, Pageable pageable) {
        return productRepository.findBySellerId(sellerId, pageable)
                .map(ProductResponse::fromEntity);
    }

    public long countMyProducts(Long sellerId) {
        return productRepository.countBySellerId(sellerId);
    }

    public ProductResponse getProductById(Long id, Long sellerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("商品不存在"));
        // 检查商品是否属于当前用户
        if (sellerId != null && !product.getSellerId().equals(sellerId)) {
            throw new ForbiddenException("无权查看此商品");
        }
        // 增加浏览量
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    private void updateProductFromRequest(Product product, ProductRequest request) {
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getOriginalPrice() != null) {
            product.setOriginalPrice(request.getOriginalPrice());
        }
        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }
        if (request.getMainImage() != null) {
            product.setMainImage(request.getMainImage());
        }
        if (request.getImages() != null) {
            product.setImages(request.getImages());
        }
        if (request.getCategory() != null) {
            product.setCategory(request.getCategory());
        }
        if (request.getTags() != null) {
            product.setTags(request.getTags());
        }
        if (request.getIsOnSale() != null) {
            product.setIsOnSale(request.getIsOnSale());
        }
        if (request.getIsFeatured() != null) {
            product.setIsFeatured(request.getIsFeatured());
        }

        // 根据库存状态自动更新商品状态
        if (product.getStock() <= 0) {
            product.setStatus(Product.ProductStatus.OUT_OF_STOCK);
        } else if (!product.getIsOnSale()) {
            product.setStatus(Product.ProductStatus.OFF_SALE);
        } else {
            product.setStatus(Product.ProductStatus.ON_SALE);
        }
    }

    public long countProducts() {
        return productRepository.count();
    }

    public long countOnSaleProducts() {
        return productRepository.countByStatus(Product.ProductStatus.ON_SALE);
    }

    // ==================== 公共浏览方法（无需登录）====================

    /**
     * 获取公开在售商品列表
     */
    public Page<ProductResponse> getPublicOnSaleProducts(Pageable pageable) {
        return productRepository.findByIsOnSaleTrueAndStatus(Product.ProductStatus.ON_SALE, pageable)
                .map(ProductResponse::fromEntity);
    }

    /**
     * 获取公开商品详情（不增加浏览量，不检查权限）
     */
    public ProductResponse getPublicProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("商品不存在"));
        return ProductResponse.fromEntity(product);
    }

    /**
     * 按分类获取公开商品
     */
    public Page<ProductResponse> getProductsByCategoryPublic(String category, Pageable pageable) {
        return productRepository.findByCategory(category, pageable)
                .map(ProductResponse::fromEntity);
    }

    /**
     * 公开搜索商品
     */
    public Page<ProductResponse> searchPublicProducts(String name, String category,
                                                       BigDecimal minPrice, BigDecimal maxPrice,
                                                       Long sellerId,
                                                       Pageable pageable) {
        return productRepository.searchProducts(name, category, minPrice, maxPrice,
                        Product.ProductStatus.ON_SALE, sellerId, pageable)
                .map(ProductResponse::fromEntity);
    }

    // ==================== 管理员 - 用户商品管理 ====================

    /**
     * 获取指定用户的商品列表
     */
    public Page<ProductResponse> getProductsBySellerId(Long sellerId, Pageable pageable) {
        return productRepository.findBySellerId(sellerId, pageable)
                .map(ProductResponse::fromEntity);
    }

    /**
     * 统计指定用户的商品数量
     */
    public long countProductsBySellerId(Long sellerId) {
        return productRepository.countBySellerId(sellerId);
    }

    // ==================== 管理员 - 商品上下架管理 ====================

    /**
     * 设置商品上下架状态（管理员权限）
     */
    @Transactional
    public ProductResponse setSaleStatus(Long id, Boolean isOnSale) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("商品不存在"));
        product.setIsOnSale(isOnSale);

        // 更新商品状态
        if (!isOnSale) {
            product.setStatus(Product.ProductStatus.OFF_SALE);
        } else if (product.getStock() > 0) {
            product.setStatus(Product.ProductStatus.ON_SALE);
        } else {
            product.setStatus(Product.ProductStatus.OUT_OF_STOCK);
        }

        product = productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    /**
     * 设置商品推荐状态（管理员权限）
     */
    @Transactional
    public ProductResponse setFeaturedStatus(Long id, Boolean isFeatured) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("商品不存在"));
        product.setIsFeatured(isFeatured);
        product = productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    // ==================== 管理员 - 高级查询 ====================

    /**
     * 查询售罄商品
     */
    public Page<ProductResponse> getOutOfStockProducts(Pageable pageable) {
        return productRepository.findByStatus(Product.ProductStatus.OUT_OF_STOCK, pageable)
                .map(ProductResponse::fromEntity);
    }

    /**
     * 查询下架商品
     */
    public Page<ProductResponse> getOffSaleProducts(Pageable pageable) {
        return productRepository.findByStatus(Product.ProductStatus.OFF_SALE, pageable)
                .map(ProductResponse::fromEntity);
    }

    /**
     * 管理员多条件查询（支持 sellerId）
     */
    public Page<ProductResponse> searchProductsWithSeller(String name, String category,
                                                           BigDecimal minPrice, BigDecimal maxPrice,
                                                           Product.ProductStatus status,
                                                           Long sellerId, Pageable pageable) {
        return productRepository.searchProducts(name, category, minPrice, maxPrice, status, sellerId, pageable)
                .map(ProductResponse::fromEntity);
    }
}
