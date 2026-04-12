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

    /**
     * 商品名称（必填）
     */
    @jakarta.validation.constraints.NotBlank(message = "商品名称不能为空")
    @jakarta.validation.constraints.Size(max = 100, message = "商品名称不能超过 100 个字符")
    private String name;

    /**
     * 商品描述（可选）
     */
    @jakarta.validation.constraints.Size(max = 2000, message = "商品描述不能超过 2000 个字符")
    private String description;

    /**
     * 商品价格（必填，必须大于 0）
     */
    @jakarta.validation.constraints.NotNull(message = "商品价格不能为空")
    @jakarta.validation.constraints.DecimalMin(value = "0", inclusive = false, message = "商品价格必须大于 0")
    @jakarta.validation.constraints.Digits(integer = 8, fraction = 2, message = "商品价格格式不正确")
    private BigDecimal price;

    /**
     * 商品原价（可选，必须大于等于 0）
     */
    @jakarta.validation.constraints.DecimalMin(value = "0", inclusive = true, message = "商品原价必须大于等于 0")
    private BigDecimal originalPrice;

    /**
     * 商品库存（必填，必须大于等于 0）
     */
    @jakarta.validation.constraints.NotNull(message = "商品库存不能为空")
    @jakarta.validation.constraints.Min(value = 0, message = "商品库存必须大于等于 0")
    private Integer stock;

    /**
     * 主图 URL（必填）
     */
    @jakarta.validation.constraints.NotBlank(message = "商品主图不能为空")
    private String mainImage;

    /**
     * 商品图片列表（可选）
     */
    private List<String> images;

    /**
     * 商品分类（必填）
     */
    @jakarta.validation.constraints.NotBlank(message = "商品分类不能为空")
    @jakarta.validation.constraints.Size(max = 50, message = "商品分类不能超过 50 个字符")
    private String category;

    /**
     * 商品标签（可选）
     */
    private List<String> tags;

    /**
     * 是否在售（可选，默认 true）
     */
    private Boolean isOnSale;

    /**
     * 是否推荐（可选，默认 false）
     */
    private Boolean isFeatured;
}
