# 3.3.1 积分签到数据联动修复

## 问题描述

签到获得的积分和积分模块"是同一个"，但积分模块却没有积分和对应的记录。签到功能也出现异常：已签到状态未正确更新、连续签到天数不显示。

## 根本原因

### API 数据解包层级错误（核心问题）

项目 `request.ts` 响应拦截器在 `code === 200` 时返回完整的 `AxiosResponse` 对象，后端所有接口统一包装为 `{ code, message, data }` (ApiResponse) 结构。

**数据层级关系：**
```
AxiosResponse.data  →  { code: 200, message: "ok", data: 实际业务数据 }
                         ↑ ApiResponse 对象
```

`checkin.ts` 和 `points.ts` 的 API 函数使用 `return res.data`，返回的是 `ApiResponse` 包装对象（包含 code/message/data），**而非实际业务数据**。

Store 中直接 `status.value = await getStatusApi()` 赋值后：
- 得到的是 `{ code: 200, message: "ok", data: { todayCheckedIn: true, ... } }`
- 访问 `status.value.todayCheckedIn` → `undefined`（该属性在 ApiResponse 上不存在）
- 访问 `status.value.continuousDays` → `undefined`

**这导致签到状态全部显示为默认值（0/false），积分数据全部为空。**

### 修复方案

将 API 函数从 `return res.data` 改为 `return res.data.data`，与 auth store 中的消费模式一致。

| 文件 | 修改前 | 修改后 |
|------|--------|--------|
| `api/checkin.ts` | `return res.data` | `return res.data.data` |
| `api/points.ts` | `return res.data` | `return res.data.data` |

### 项目中其他 API 文件的现状

| API 文件 | 返回方式 | 消费端适配 | 状态 |
|----------|----------|------------|------|
| address.ts | `res.data` (ApiResponse) | `res.data` 取 `.data` 字段 | 碰巧正确 |
| chat/log/order/product/user/oss | AxiosResponse | `res.data.data` 双重解包 | 碰巧正确 |
| **checkin.ts** | ~~`res.data`~~ → `res.data.data` | Store 直接赋值 | **已修复** |
| **points.ts** | ~~`res.data`~~ → `res.data.data` | Store 直接赋值 | **已修复** |

> **建议**：后续统一 API 层规范，所有 API 函数统一返回 `res.data.data`，消费端直接 `const data = await someApi()` 即可。

## 其他修复

### 1. PointAccountResponse 字段命名语义错误

字段 `checkInPoints`/`orderPoints` 实际存储的是**次数**而非积分数，导致语义混乱。

| 修改前 | 修改后 | 说明 |
|--------|--------|------|
| `checkInPoints` | `checkInCount` | 签到累计次数 |
| `orderPoints` | `orderCount` | 订单奖励累计次数 |

涉及文件：
- `backend/dto/PointAccountResponse.java`
- `backend/service/PointService.java`
- `frontend/api/points.ts` 类型定义
- `frontend/views/user/Points.vue` 模板绑定

### 2. 积分流水"加载更多"数据覆盖问题

`fetchRecords` 每次获取一页数据后直接覆盖 `records.value`，点击"加载更多"时会丢失之前已加载的数据。

**修复**：`fetchRecords` 增加 `append` 参数，`append: true` 时追加而非覆盖。

```typescript
// stores/points.ts
const fetchRecords = async (params?: { ..., append?: boolean }) => {
  const newRecords = page?.content ?? []
  if (params?.append) {
    records.value = [...records.value, ...newRecords]
  } else {
    records.value = newRecords
  }
}

// Points.vue loadMore
const loadMore = () => {
  currentPage.value++
  pointsStore.fetchRecords({
    type: filterType.value || undefined,
    page: currentPage.value,
    size: pageSize,
    append: true
  })
}
```

## 修复文件清单

| 文件 | 修改内容 |
|------|----------|
| `backend/.../dto/PointAccountResponse.java` | checkInPoints→checkInCount, orderPoints→orderCount |
| `backend/.../service/PointService.java` | builder 字段名同步更新 |
| `frontend/src/api/checkin.ts` | 3处 `res.data` → `res.data.data` |
| `frontend/src/api/points.ts` | 3处 `res.data` → `res.data.data` + 类型定义更新 |
| `frontend/src/stores/points.ts` | fetchRecords 支持 append 模式 |
| `frontend/src/views/user/Points.vue` | loadMore 用 append + 字段名对齐 |

## 测试验证

1. **签到流程**：点击签到 → 状态正确更新为"已签到" → 连续签到天数显示正确
2. **积分联动**：签到成功后 → 积分页面可用积分增加 → 积分明细出现签到记录
3. **积分流水**：积分页面能正确显示流水记录 → 筛选类型正常 → 加载更多追加而非覆盖
4. **统计显示**：签到次数/订单奖励次数显示正确（非积分数）
