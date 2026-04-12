package com.aisale.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal originalPrice;

    @Column(nullable = false)
    private Integer stock = 0;

    private String mainImage;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url")
    private List<String> images = new ArrayList<>();

    @Column(length = 50)
    private String category;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Column(precision = 3, scale = 2)
    private BigDecimal discount;

    private Boolean isOnSale = true;

    private Boolean isFeatured = false;

    private Integer salesCount = 0;

    private Integer viewCount = 0;

    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.ON_SALE;

    @Column(name = "seller_id")
    private Long sellerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", insertable = false, updatable = false)
    private User seller;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (originalPrice.compareTo(BigDecimal.ZERO) == 0) {
            originalPrice = price;
        }
        if (originalPrice.compareTo(BigDecimal.ZERO) > 0) {
            discount = price.divide(originalPrice, 2, java.math.RoundingMode.HALF_UP);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (originalPrice.compareTo(BigDecimal.ZERO) > 0) {
            discount = price.divide(originalPrice, 2, java.math.RoundingMode.HALF_UP);
        }
    }

    public enum ProductStatus {
        ON_SALE,      // 在售
        OFF_SALE,     // 下架
        OUT_OF_STOCK  // 售罄
    }
}
