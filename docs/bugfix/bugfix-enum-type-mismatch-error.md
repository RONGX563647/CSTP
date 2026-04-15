# Bug修复：修复枚举类型转换错误

## 问题描述

系统中存在多个地方出现 `MethodArgumentTypeMismatchException` 和 `IllegalArgumentException` 异常，导致应用程序在接收到无效的枚举值时崩溃。

**错误示例**：
```
ERROR c.a.b.handler.GlobalExceptionHandler - 未捕获的异常：Failed to convert value of type 'java.lang.String' to required type 'com.aisale.backend.entity.User$UserStatus'
ERROR c.a.b.handler.GlobalExceptionHandler - 未捕获的异常：无效的用户状态: INVALID_STATUS, 可选值: ACTIVE, INACTIVE, BANNED
```

## 问题根源

在多个控制器中，直接使用枚举类型作为请求参数，当客户端传递无效的枚举值时，Spring框架无法自动转换，导致异常抛出。

涉及的控制器端点：
- `AdminUserController` - 用户状态更新端点
- `ProductController` - 商品状态更新和查询端点  
- `UserProductController` - 用户商品状态更新端点
- `AdminOrderController` - 订单状态更新和查询端点

## 解决方案

### 1. 修改控制器参数接收方式

将直接使用枚举类型的参数改为字符串类型，并添加手动验证：

```java
// 修复前
@RequestParam User.UserStatus status

// 修复后  
@RequestParam String status
```

然后在方法内部手动验证和转换：

```java
try {
    User.UserStatus userStatus = User.UserStatus.valueOf(status.toUpperCase());
} catch (IllegalArgumentException e) {
    throw new IllegalArgumentException("无效的用户状态: " + status + ", 可选值: ACTIVE, INACTIVE, BANNED");
}
```

### 2. 增强全局异常处理器

在 `GlobalExceptionHandler.java` 中添加了对 `MethodArgumentTypeMismatchException` 和 `IllegalArgumentException` 的处理，提供友好的错误响应。

## 修复范围

### 用户管理模块
- **端点**: `PUT /api/admin/users/{id}/status`
- **修复**: 将 `@RequestParam User.UserStatus status` 改为 `@RequestParam String status`

### 商品管理模块
- **端点**: `PUT /api/admin/products/{id}/status`
- **端点**: `GET /api/admin/products/query`
- **修复**: 相应的参数类型从枚举改为字符串并添加验证

### 用户商品管理模块
- **端点**: `PUT /api/user/products/{id}/status`  
- **修复**: 参数类型从枚举改为字符串并添加验证

### 订单管理模块
- **端点**: `PUT /api/admin/orders/{id}/status`
- **端点**: `GET /api/admin/orders/query`
- **修复**: 相应的参数类型从枚举改为字符串并添加验证

## 验证

1. 编译成功：`mvn clean compile`
2. 测试通过：`mvn test`
3. 手动验证：传递无效枚举值时返回友好的错误信息而非系统错误

## 变更影响

- **正向影响**: 系统健壮性显著提升，不再因无效枚举值导致崩溃
- **兼容性**: API行为不变，只是错误处理更加友好
- **性能**: 无性能影响

## 相关代码文件

- `backend/src/main/java/com/aisale/backend/controller/AdminUserController.java`
- `backend/src/main/java/com/aisale/backend/controller/ProductController.java`  
- `backend/src/main/java/com/aisale/backend/controller/UserProductController.java`
- `backend/src/main/java/com/aisale/backend/controller/AdminOrderController.java`
- `backend/src/main/java/com/aisale/backend/handler/GlobalExceptionHandler.java`
- `backend/src/main/java/com/aisale/backend/service/AdminUserService.java`

## 修复日期

2026年4月15日

## 作者

刘荣翔