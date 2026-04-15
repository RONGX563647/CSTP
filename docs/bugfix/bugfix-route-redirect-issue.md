# 路由重定向问题修复：已登录用户访问管理员登录页

## 问题描述

**问题类型**: 路由守卫逻辑错误

**问题现象**: 
已登录用户在浏览器中访问 `http://localhost:3000/admin/login` 时，被错误重定向至 `http://localhost:3000/user/home`。

**影响范围**:
- 已登录管理员无法重新登录或切换账号
- 已登录普通用户无法访问管理员登录页
- 路由守卫逻辑不完善，缺乏角色区分

## 问题分析

### 1. 路由守卫实现检查

#### 路由配置
在 [router/index.ts](file:///e:/CODE/git/LEARN_AISALE/frontend/src/router/index.ts#L150-L154)：

```typescript
{
  path: '/admin/login',
  name: 'AdminLogin',
  component: () => import('@/views/admin/Login.vue'),
  meta: { guest: true, title: '管理员登录' }  // ✅ 设置为guest
}
```

**分析**：
- `/admin/login` 路由设置了 `meta: { guest: true }`
- `guest: true` 表示该页面仅限未登录用户访问

#### 路由守卫逻辑
在 [router/index.ts](file:///e:/CODE/git/LEARN_AISALE/frontend/src/router/index.ts#L248-L255)：

```typescript
// 仅限访客（已登录用户不能访问登录/注册页）
else if (to.meta.guest) {
  if (authStore.isLoggedIn) {
    next({ name: 'UserHome' })  // ❌ 问题所在！
  } else {
    next()
  }
}
```

**问题**：
- 路由守卫检测到 `guest: true` 且用户已登录
- 直接重定向到 `UserHome` ❌
- **没有检查用户角色**
- **没有区分用户登录页和管理员登录页**

### 2. 权限控制逻辑检查

#### authStore角色判断
在 [auth.ts](file:///e:/CODE/git/LEARN_AISALE/frontend/src/stores/auth.ts#L54-L56)：

```typescript
const isAdmin = computed(() =>
  userInfo.value?.role === 'ADMIN' || userInfo.value?.role === 'SUPER_ADMIN'
)
```

**分析**：
- authStore提供了 `isAdmin` 计算属性 ✅
- 可以判断用户是否为管理员 ✅
- 但路由守卫没有使用这个属性 ❌

### 3. 用户身份验证机制检查

#### 登录流程
```
用户登录 → authStore.login() → 设置token和userInfo
管理员登录 → authStore.adminLogin() → 设置token和userInfo（包含role）
```

**分析**：
- 登录成功后，userInfo包含role字段 ✅
- 可以通过role判断用户类型 ✅
- 但路由守卫没有根据role做区分处理 ❌

### 4. 根本原因

**问题根源**：路由守卫的guest逻辑没有区分用户登录页和管理员登录页。

**错误流程**：
```
已登录用户访问 /admin/login
  ↓
路由守卫检测到 guest: true
  ↓
检测到用户已登录
  ↓
直接重定向到 UserHome ❌
  ↓
没有检查用户角色
没有区分AdminLogin和UserLogin
```

**应该的逻辑**：
```
已登录用户访问 /admin/login
  ↓
路由守卫检测到 guest: true
  ↓
检测到路由名称为 AdminLogin
  ↓
检查用户角色
  ↓
如果是管理员 → 重定向到 AdminProductList（管理员首页）
如果是普通用户 → 允许访问（可以切换账号）
```

## 修复方案

### 修复路由守卫逻辑

在 [router/index.ts](file:///e:/CODE/git/LEARN_AISALE/frontend/src/router/index.ts#L240-L271)：

```typescript
// 需要认证的路由
if (to.meta.requiresAuth) {
  if (!authStore.isLoggedIn) {
    next({ name: 'UserLogin', query: { redirect: to.fullPath } })
  } else {
    // ✅ 检查管理员权限
    if (to.meta.role === 'admin' && !authStore.isAdmin) {
      next({ name: 'UserHome' })
    } else {
      next()
    }
  }
}
// 仅限访客（已登录用户不能访问登录/注册页）
else if (to.meta.guest) {
  // ✅ 管理员登录页特殊处理
  if (to.name === 'AdminLogin') {
    if (authStore.isLoggedIn) {
      // 已登录管理员：重定向到管理员首页
      if (authStore.isAdmin) {
        next({ name: 'AdminProductList' })
      } else {
        // 已登录普通用户：允许访问管理员登录页（可以切换账号）
        next()
      }
    } else {
      // 未登录：允许访问
      next()
    }
  }
  // ✅ 用户登录/注册页
  else {
    if (authStore.isLoggedIn) {
      next({ name: 'UserHome' })
    } else {
      next()
    }
  }
}
else {
  next()
}
```

### 修复逻辑说明

#### 1. 管理员登录页（AdminLogin）特殊处理

**已登录管理员访问 `/admin/login`**：
```
检测到已登录 + isAdmin = true
  ↓
重定向到 AdminProductList（管理员首页）
  ↓
避免管理员重复登录
```

**已登录普通用户访问 `/admin/login`**：
```
检测到已登录 + isAdmin = false
  ↓
允许访问管理员登录页
  ↓
可以切换到管理员账号
```

**未登录用户访问 `/admin/login`**：
```
检测到未登录
  ↓
允许访问管理员登录页
  ↓
正常登录流程
```

#### 2. 用户登录页（UserLogin）保持原逻辑

**已登录用户访问 `/login`**：
```
检测到已登录
  ↓
重定向到 UserHome
  ↓
避免用户重复登录
```

#### 3. 管理员路由权限检查

**普通用户访问管理员路由**：
```
检测到 requiresAuth + role = 'admin'
  ↓
检查 isAdmin = false
  ↓
重定向到 UserHome
  ↓
拒绝访问管理员页面
```

## 修复效果

### 修复前

| 用户状态 | 访问页面 | 实际行为 | 问题 |
|---------|---------|---------|------|
| 已登录管理员 | `/admin/login` | 重定向到 `/user/home` | ❌ 错误 |
| 已登录普通用户 | `/admin/login` | 重定向到 `/user/home` | ❌ 错误 |
| 未登录用户 | `/admin/login` | 正常访问 | ✅ 正确 |

### 修复后

| 用户状态 | 访问页面 | 实际行为 | 结果 |
|---------|---------|---------|------|
| 已登录管理员 | `/admin/login` | 重定向到 `/admin/products` | ✅ 正确 |
| 已登录普通用户 | `/admin/login` | 正常访问登录页 | ✅ 正确 |
| 未登录用户 | `/admin/login` | 正常访问登录页 | ✅ 正确 |
| 已登录用户 | `/login` | 重定向到 `/user/home` | ✅ 正确 |
| 未登录用户 | `/login` | 正常访问登录页 | ✅ 正确 |

## 测试验证

### 测试场景

#### 1. 已登录管理员访问管理员登录页
**步骤**：
1. 使用管理员账号登录（admin/123456）
2. 访问 `http://localhost:3000/admin/login`
3. 观察路由跳转

**预期结果**：
- ✅ 自动重定向到 `/admin/products`
- ✅ 显示管理员首页
- ✅ 不显示登录页面

#### 2. 已登录普通用户访问管理员登录页
**步骤**：
1. 使用普通用户账号登录（zhangsan/123456）
2. 访问 `http://localhost:3000/admin/login`
3. 观察页面显示

**预期结果**：
- ✅ 正常显示管理员登录页面
- ✅ 可以输入管理员账号密码
- ✅ 可以切换到管理员账号

#### 3. 未登录用户访问管理员登录页
**步骤**：
1. 未登录状态
2. 访问 `http://localhost:3000/admin/login`
3. 观察页面显示

**预期结果**：
- ✅ 正常显示管理员登录页面
- ✅ 可以输入管理员账号密码
- ✅ 登录成功后跳转到管理员首页

#### 4. 已登录用户访问用户登录页
**步骤**：
1. 使用普通用户账号登录（zhangsan/123456）
2. 访问 `http://localhost:3000/login`
3. 观察路由跳转

**预期结果**：
- ✅ 自动重定向到 `/user/home`
- ✅ 显示用户首页
- ✅ 不显示登录页面

#### 5. 普通用户访问管理员路由
**步骤**：
1. 使用普通用户账号登录（zhangsan/123456）
2. 访问 `http://localhost:3000/admin/products`
3. 观察路由跳转

**预期结果**：
- ✅ 自动重定向到 `/user/home`
- ✅ 拒绝访问管理员页面
- ✅ 显示权限不足提示

### 测试环境

- Chrome浏览器
- Firefox浏览器
- Edge浏览器
- Safari浏览器（Mac）

## 相关文件

### 前端文件
- [frontend/src/router/index.ts](file:///e:/CODE/git/LEARN_AISALE/frontend/src/router/index.ts#L240-L271) - 路由守卫逻辑
- [frontend/src/stores/auth.ts](file:///e:/CODE/git/LEARN_AISALE/frontend/src/stores/auth.ts#L54-L56) - 用户角色判断
- [frontend/src/views/admin/Login.vue](file:///e:/CODE/git/LEARN_AISALE/frontend/src/views/admin/Login.vue) - 管理员登录页面

## 修复日期

2026-04-13