package com.aisale.backend.dto;

import com.aisale.backend.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;

    private Long sellerId;

    private String sellerName;

    private String name;

    private String description;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    private String mainImage;

    private List<String> images;

    private String category;

    private List<String> tags;

    private BigDecimal discount;

    private Boolean isOnSale;

    private Boolean isFeatured;

    private Integer salesCount;

    private Integer viewCount;

    private Product.ProductStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static ProductResponse fromEntity(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sellerId(product.getSellerId())
                .sellerName(product.getSeller() != null ? product.getSeller().getUsername() : null)
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .stock(product.getStock())
                .mainImage(product.getMainImage())
                .images(product.getImages())
                .category(product.getCategory())
                .tags(product.getTags())
                .discount(product.getDiscount())
                .isOnSale(product.getIsOnSale())
                .isFeatured(product.getIsFeatured())
                .salesCount(product.getSalesCount())
                .viewCount(product.getViewCount())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
