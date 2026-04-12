package com.aisale.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    private String name;

    private String description;

    private BigDecimal price;

    private BigDecimal originalPrice;

    private Integer stock;

    private String mainImage;

    private List<String> images;

    private String category;

    private List<String> tags;

    private Boolean isOnSale;

    private Boolean isFeatured;
}
