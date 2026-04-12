package com.aisale.backend.controller;

import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.dto.ProductRequest;
import com.aisale.backend.dto.ProductResponse;
import com.aisale.backend.entity.Product;
import com.aisale.backend.service.ProductService;
import com.aisale.backend.util.JwtRequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "用户端商品", description = "用户管理自己的二手商品")
@RestController
@RequestMapping("/api/user/products")
@RequiredArgsConstructor
public class UserProductController {

    private final ProductService productService;
    private final JwtRequestUtils jwtRequestUtils;

    // ==================== 公共浏览接口（无需登录）====================

    @Operation(summary = "获取公开在售商品列表")
    @GetMapping("/public/on-sale")
    public ApiResponse<Page<ProductResponse>> getPublicOnSaleProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponse> products = productService.getPublicOnSaleProducts(pageable);
        return ApiResponse.success(products);
    }

    @Operation(summary = "获取公开商品详情")
    @GetMapping("/public/{id}")
    public ApiResponse<ProductResponse> getPublicProductById(@PathVariable Long id) {
        ProductResponse product = productService.getPublicProductById(id);
        return ApiResponse.success(product);
    }

    @Operation(summary = "获取推荐商品")
    @GetMapping("/public/featured")
    public ApiResponse<List<ProductResponse>> getPublicFeaturedProducts() {
        List<ProductResponse> products = productService.getFeaturedProducts();
        return ApiResponse.success(products);
    }

    @Operation(summary = "按分类获取商品")
    @GetMapping("/public/category/{category}")
    public ApiResponse<Page<ProductResponse>> getProductsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.getProductsByCategoryPublic(category, pageable);
        return ApiResponse.success(products);
    }

    @Operation(summary = "搜索商品（公开）")
    @GetMapping("/public/search")
    public ApiResponse<Page<ProductResponse>> searchPublicProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.searchPublicProducts(
                name, category, minPrice, maxPrice, pageable);
        return ApiResponse.success(products);
    }

    // ==================== 已登录用户接口 ====================

    @Operation(summary = "发布商品")
    @PostMapping
    public ApiResponse<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest productRequest,
            HttpServletRequest request) {
        Long sellerId = jwtRequestUtils.getCurrentUserId(request);
        ProductResponse product = productService.createProduct(productRequest, sellerId);
        return ApiResponse.success("商品发布成功", product);
    }

    @Operation(summary = "更新商品")
    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest productRequest,
            HttpServletRequest request) {
        Long sellerId = jwtRequestUtils.getCurrentUserId(request);
        ProductResponse product = productService.updateProduct(id, productRequest, sellerId);
        return ApiResponse.success("商品更新成功", product);
    }

    @Operation(summary = "获取我的商品详情")
    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long sellerId = jwtRequestUtils.getCurrentUserId(request);
        ProductResponse product = productService.getProductById(id, sellerId);
        return ApiResponse.success(product);
    }

    @Operation(summary = "获取我的商品列表（分页）")
    @GetMapping("/my")
    public ApiResponse<Page<ProductResponse>> getMyProducts(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Long sellerId = jwtRequestUtils.getCurrentUserId(request);
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponse> products = productService.getMyProducts(sellerId, pageable);
        return ApiResponse.success(products);
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long sellerId = jwtRequestUtils.getCurrentUserId(request);
        productService.deleteProduct(id, sellerId);
        return ApiResponse.success("商品删除成功", null);
    }

    @Operation(summary = "更新商品库存")
    @PutMapping("/{id}/stock")
    public ApiResponse<ProductResponse> updateStock(
            @PathVariable Long id,
            @RequestParam Integer stock,
            HttpServletRequest request) {
        Long sellerId = jwtRequestUtils.getCurrentUserId(request);
        ProductResponse product = productService.updateStock(id, stock, sellerId);
        return ApiResponse.success("库存更新成功", product);
    }

    @Operation(summary = "更新商品状态")
    @PutMapping("/{id}/status")
    public ApiResponse<ProductResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam Product.ProductStatus status,
            HttpServletRequest request) {
        Long sellerId = jwtRequestUtils.getCurrentUserId(request);
        ProductResponse product = productService.updateStatus(id, status, sellerId);
        return ApiResponse.success("状态更新成功", product);
    }

    @Operation(summary = "获取我的商品统计")
    @GetMapping("/stats")
    public ApiResponse<ProductStats> getProductStats(
            HttpServletRequest request) {
        Long sellerId = jwtRequestUtils.getCurrentUserId(request);
        long total = productService.countMyProducts(sellerId);
        ProductStats stats = new ProductStats(total, 0);
        return ApiResponse.success(stats);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ProductStats {
        private long totalProducts;
        private long onSaleProducts;
    }
}
