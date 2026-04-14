# Bug修复：商品搜索接口空参数问题

## 问题描述

**接口**: `/api/user/products/public/search`

**现象**: 当搜索参数 `name` 和 `category` 为空字符串时，接口返回空数据：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [],
    "totalElements": 0,
    "totalPages": 0,
    "empty": true
  }
}
```

**预期**: 当搜索参数为空时，应返回所有在售商品

## 问题原因

在 [UserProductController.java](file:///e:/CODE/git/LEARN_AISALE/backend/src/main/java/com/aisale/backend/controller/UserProductController.java#L78-L95) 中，空字符串参数直接传递给Repository查询：

```java
// 问题代码
Page<ProductResponse> products = productService.searchPublicProducts(
    name, category, minPrice, maxPrice, sellerId, pageable);
```

Repository的JPQL查询：
```sql
SELECT p FROM Product p WHERE 
(:name IS NULL OR p.name LIKE %:name%) AND 
(:category IS NULL OR p.category = :category) AND ...
```

当 `name=""` 时，条件变为 `p.name LIKE %%`，这会匹配空字符串的商品名称，导致查询失败。

## 修复方案

在Controller层将空字符串转换为null：

```java
@Operation(summary = "搜索商品（公开）")
@GetMapping("/public/search")
public ApiResponse<Page<ProductResponse>> searchPublicProducts(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) Long sellerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir) {
    Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);
    
    // 修复：将空字符串转换为null
    String trimmedName = (name != null && name.trim().isEmpty()) ? null : name;
    String trimmedCategory = (category != null && category.trim().isEmpty()) ? null : category;
    
    Page<ProductResponse> products = productService.searchPublicProducts(
            trimmedName, trimmedCategory, minPrice, maxPrice, sellerId, pageable);
    return ApiResponse.success(products);
}
```

## 修复效果

修复后，当参数为空字符串时：
- `name=""` → 转换为 `null` → 查询条件忽略name参数
- `category=""` → 换为 `null` → 查询条件忽略category参数
- 返回所有状态为 `ON_SALE` 的商品

## 测试验证

请求示例：
```bash
curl "http://localhost:8090/api/user/products/public/search?name=&category=&page=0&size=10"
```

预期返回：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [
      // 所有在售商品列表
    ],
    "totalElements": 10,  // 商品总数
    "totalPages": 1,
    "empty": false
  }
}
```

## 相关文件

- [backend/src/main/java/com/aisale/backend/controller/UserProductController.java](file:///e:/CODE/git/LEARN_AISALE/backend/src/main/java/com/aisale/backend/controller/UserProductController.java#L78-L95)
- [backend/src/main/java/com/aisale/backend/service/ProductService.java](file:///e:/CODE/git/LEARN_AISALE/backend/src/main/java/com/aisale/backend/service/ProductService.java#L313-L321)
- [backend/src/main/java/com/aisale/backend/repository/ProductRepository.java](file:///e:/CODE/git/LEARN_AISALE/backend/src/main/java/com/aisale/backend/repository/ProductRepository.java#L31-L46)

## 修复日期

2026-04-13