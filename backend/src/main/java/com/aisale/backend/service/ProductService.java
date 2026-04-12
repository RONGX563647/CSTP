package com.aisale.backend.service;

import com.aisale.backend.dto.ProductRequest;
import com.aisale.backend.dto.ProductResponse;
import com.aisale.backend.entity.Product;
import com.aisale.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        updateProductFromRequest(product, request);
        product = productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        updateProductFromRequest(product, request);
        product = productRepository.save(product);
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

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        // 增加浏览量
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);

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

    public Page<ProductResponse> searchProducts(String name, String category,
                                                 BigDecimal minPrice, BigDecimal maxPrice,
                                                 Product.ProductStatus status, Pageable pageable) {
        return productRepository.searchProducts(name, category, minPrice, maxPrice, status, pageable)
                .map(ProductResponse::fromEntity);
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    @Transactional
    public ProductResponse updateStock(Long id, Integer stock) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
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
    public ProductResponse updateStatus(Long id, Product.ProductStatus status) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        product.setStatus(status);

        if (status == Product.ProductStatus.OFF_SALE) {
            product.setIsOnSale(false);
        } else if (status == Product.ProductStatus.ON_SALE && product.getStock() > 0) {
            product.setIsOnSale(true);
        }

        product = productRepository.save(product);
        return ProductResponse.fromEntity(product);
    }

    public long countProducts() {
        return productRepository.count();
    }

    public long countOnSaleProducts() {
        return productRepository.countByStatus(Product.ProductStatus.ON_SALE);
    }
}
