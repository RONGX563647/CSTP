package com.aisale.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchResult {
    private ProductResponse product;
    private double relevanceScore;
    private String matchType;
}