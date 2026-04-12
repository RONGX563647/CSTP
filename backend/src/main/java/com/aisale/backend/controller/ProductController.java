package com.aisale.backend.controller;

import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.dto.ProductRequest;
import com.aisale.backend.dto.ProductResponse;
import com.aisale.backend.entity.Product;
import com.aisale.backend.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "商品管理", description = "商品 CRUD 接口（管理端）")
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "创建商品")
    @PostMapping
    public ApiResponse<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProductForAdmin(request);
        return ApiResponse.success("商品创建成功", product);
    }

    @Operation(summary = "更新商品")
    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.updateProductForAdmin(id, request);
        return ApiResponse.success("商品更新成功", product);
    }

    @Operation(summary = "获取商品详情")
    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id, null);
        return ApiResponse.success(product);
    }

    @Operation(summary = "获取商品列表（分页）")
    @GetMapping
    public ApiResponse<Page<ProductResponse>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponse> products = productService.getAllProducts(pageable);
        return ApiResponse.success(products);
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProductForAdmin(id);
        return ApiResponse.success("商品删除成功", null);
    }

    @Operation(summary = "更新商品库存")
    @PutMapping("/{id}/stock")
    public ApiResponse<ProductResponse> updateStock(
            @PathVariable Long id,
            @RequestParam Integer stock) {
        ProductResponse product = productService.updateStockForAdmin(id, stock);
        return ApiResponse.success("库存更新成功", product);
    }

    @Operation(summary = "更新商品状态")
    @PutMapping("/{id}/status")
    public ApiResponse<ProductResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam Product.ProductStatus status) {
        ProductResponse product = productService.updateStatusForAdmin(id, status);
        return ApiResponse.success("状态更新成功", product);
    }

    @Operation(summary = "获取商品统计")
    @GetMapping("/stats")
    public ApiResponse<ProductStats> getProductStats() {
        long total = productService.countProducts();
        long onSale = productService.countOnSaleProducts();
        ProductStats stats = new ProductStats(total, onSale);
        return ApiResponse.success(stats);
    }

    // ==================== 用户商品管理 ====================

    @Operation(summary = "查看指定用户的商品")
    @GetMapping("/user/{userId}")
    public ApiResponse<Page<ProductResponse>> getProductsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.getProductsBySellerId(userId, pageable);
        return ApiResponse.success(products);
    }

    @Operation(summary = "获取用户商品统计")
    @GetMapping("/user/{userId}/stats")
    public ApiResponse<UserProductStats> getUserProductStats(@PathVariable Long userId) {
        long total = productService.countProductsBySellerId(userId);
        return ApiResponse.success(new UserProductStats(total));
    }

    // ==================== 商品上下架管理 ====================

    @Operation(summary = "设置商品上下架状态")
    @PutMapping("/{id}/sale-status")
    public ApiResponse<ProductResponse> setSaleStatus(
            @PathVariable Long id,
            @RequestParam Boolean isOnSale) {
        ProductResponse product = productService.setSaleStatus(id, isOnSale);
        return ApiResponse.success(isOnSale ? "商品已上架" : "商品已下架", product);
    }

    @Operation(summary = "设置商品推荐状态")
    @PutMapping("/{id}/featured")
    public ApiResponse<ProductResponse> setFeaturedStatus(
            @PathVariable Long id,
            @RequestParam Boolean isFeatured) {
        ProductResponse product = productService.setFeaturedStatus(id, isFeatured);
        return ApiResponse.success(isFeatured ? "已设为推荐" : "已取消推荐", product);
    }

    // ==================== 高级查询 ====================

    @Operation(summary = "多条件查询商品")
    @GetMapping("/query")
    public ApiResponse<Page<ProductResponse>> queryProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Product.ProductStatus status,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.searchProductsWithSeller(
                name, category, minPrice, maxPrice, status, sellerId, pageable);
        return ApiResponse.success(products);
    }

    @Operation(summary = "查询售罄商品")
    @GetMapping("/out-of-stock")
    public ApiResponse<Page<ProductResponse>> getOutOfStockProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.getOutOfStockProducts(pageable);
        return ApiResponse.success(products);
    }

    @Operation(summary = "查询下架商品")
    @GetMapping("/off-sale")
    public ApiResponse<Page<ProductResponse>> getOffSaleProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.getOffSaleProducts(pageable);
        return ApiResponse.success(products);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class UserProductStats {
        private long totalProducts;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ProductStats {
        private long totalProducts;
        private long onSaleProducts;
    }
}
