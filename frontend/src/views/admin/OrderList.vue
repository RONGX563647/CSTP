<template>
  <AdminLayout>
    <div class="admin-order-list-page">
      <div class="page-header">
        <h1 class="page-title">订单管理</h1>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-section">
        <el-form :inline="true" :model="filterForm">
          <el-form-item label="订单号">
            <el-input v-model="filterForm.orderNo" placeholder="请输入订单号" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item label="买家 ID">
            <el-input-number v-model="filterForm.buyerId" placeholder="买家 ID" :min="0" style="width: 150px" />
          </el-form-item>
          <el-form-item label="卖家 ID">
            <el-input-number v-model="filterForm.sellerId" placeholder="卖家 ID" :min="0" style="width: 150px" />
          </el-form-item>
          <el-form-item label="订单状态">
            <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 150px">
              <el-option label="待付款" :value="OrderStatus.PENDING_PAYMENT" />
              <el-option label="待提货" :value="OrderStatus.PENDING_PICKUP" />
              <el-option label="待确认" :value="OrderStatus.PENDING_CONFIRM" />
              <el-option label="待评价" :value="OrderStatus.PENDING_REVIEW" />
              <el-option label="已完成" :value="OrderStatus.COMPLETED" />
              <el-option label="已取消" :value="OrderStatus.CANCELLED" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stats-row">
        <el-col :span="4">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">
                <el-icon color="#6B7280"><Document /></el-icon>
                总订单数
              </div>
              <div class="stat-value">{{ stats.totalOrders }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">
                <el-icon color="#F59E0B"><Clock /></el-icon>
                待付款
              </div>
              <div class="stat-value pending-payment">{{ stats.pendingPayment }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">
                <el-icon color="#3B82F6"><ShoppingCart /></el-icon>
                待提货
              </div>
              <div class="stat-value pending-pickup">{{ stats.pendingPickup }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">
                <el-icon color="#3B82F6"><Check /></el-icon>
                待确认
              </div>
              <div class="stat-value pending-confirm">{{ stats.pendingConfirm }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">
                <el-icon color="#10B981"><ChatDotRound /></el-icon>
                待评价
              </div>
              <div class="stat-value pending-review">{{ stats.pendingReview }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">
                <el-icon color="#10B981"><CircleCheck /></el-icon>
                已完成
              </div>
              <div class="stat-value completed">{{ stats.completed }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 订单表格 -->
      <el-card class="table-card">
        <el-table
          :data="tableData"
          v-loading="loading"
          border
          stripe
          style="width: 100%"
        >
          <el-table-column prop="orderNo" label="订单号" width="180" />
          <el-table-column label="商品信息" min-width="200">
            <template #default="{ row }">
              <div class="product-cell">
                <el-image
                  :src="row.productImage || '/placeholder.png'"
                  fit="cover"
                  class="product-thumb"
                />
                <div class="product-info">
                  <div class="product-name">{{ row.productName }}</div>
                  <div class="product-spec">¥{{ row.price }} × {{ row.quantity }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="买家" width="120">
            <template #default="{ row }">
              <div class="user-cell">
                <div class="user-name">{{ row.buyerName || `ID:${row.buyerId}` }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="卖家" width="120">
            <template #default="{ row }">
              <div class="user-cell">
                <div class="user-name">{{ row.sellerName || `ID:${row.sellerId}` }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="totalAmount" label="订单金额" width="100">
            <template #default="{ row }">
              <span class="price">¥{{ row.totalAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getOrderStatusColor(row.status)">
                {{ getOrderStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="下单时间" width="160">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="goToDetail(row.id)">
                详情
              </el-button>
              <el-button size="small" text type="warning" @click="showLogModal(row)">
                日志
              </el-button>
              <el-dropdown size="small" @command="(cmd: OrderStatus) => handleStatusChange(cmd, row)">
                <el-button size="small" text type="info">
                  修改状态<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="status in availableStatuses"
                      :key="status.value"
                      :command="status.value"
                    >
                      {{ status.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchOrders"
            @current-change="fetchOrders"
          />
        </div>
      </el-card>

      <!-- 订单日志对话框 -->
      <el-dialog v-model="logDialogVisible" title="订单日志" width="700px">
        <el-timeline v-if="currentOrderLogs.length > 0">
          <el-timeline-item
            v-for="log in currentOrderLogs"
            :key="log.id"
            :timestamp="formatDateTime(log.createTime)"
            placement="top"
          >
            <el-card class="log-card">
              <div class="log-header">
                <el-tag :type="getActionTagType(log.action)" size="small">{{ log.action }}</el-tag>
                <span class="log-role">{{ getOperatorRoleText(log.operatorRole) }}</span>
              </div>
              <div v-if="log.fromStatus && log.toStatus" class="log-status-change">
                <el-tag type="info" size="small">{{ getOrderStatusText(log.fromStatus) }}</el-tag>
                <el-icon><Right /></el-icon>
                <el-tag :type="getOrderStatusColor(log.toStatus)" size="small">
                  {{ getOrderStatusText(log.toStatus) }}
                </el-tag>
              </div>
              <div v-if="log.remark" class="log-remark">{{ log.remark }}</div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无日志记录" />
      </el-dialog>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown, Search, Refresh, Document, Clock, ShoppingCart,
  Check, ChatDotRound, CircleCheck, Right
} from '@element-plus/icons-vue'
import { searchOrders, updateOrderStatus, getOrderLogs, Order, OrderStatus, OrderLog, getOrderStatusText, getOrderStatusColor } from '@/api/order'
import AdminLayout from '@/layouts/AdminLayout.vue'

const router = useRouter()

const filterForm = reactive({
  orderNo: '',
  buyerId: undefined as number | undefined,
  sellerId: undefined as number | undefined,
  status: '' as OrderStatus | ''
})

const dateRange = ref<[Date, Date] | null>(null)

const tableData = ref<Order[]>([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const stats = ref({
  totalOrders: 0,
  pendingPayment: 0,
  pendingPickup: 0,
  pendingConfirm: 0,
  pendingReview: 0,
  completed: 0,
  cancelled: 0
})

const logDialogVisible = ref(false)
const currentOrderLogs = ref<OrderLog[]>([])

const availableStatuses = [
  { label: '待付款', value: OrderStatus.PENDING_PAYMENT },
  { label: '待提货', value: OrderStatus.PENDING_PICKUP },
  { label: '待确认', value: OrderStatus.PENDING_CONFIRM },
  { label: '待评价', value: OrderStatus.PENDING_REVIEW },
  { label: '已完成', value: OrderStatus.COMPLETED },
  { label: '已取消', value: OrderStatus.CANCELLED }
]

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page - 1,
      size: pagination.size
    }
    if (filterForm.orderNo) params.orderNo = filterForm.orderNo
    if (filterForm.buyerId) params.buyerId = filterForm.buyerId
    if (filterForm.sellerId) params.sellerId = filterForm.sellerId
    if (filterForm.status) params.status = filterForm.status
    if (dateRange.value) {
      params.startTime = dateRange.value[0].toISOString()
      params.endTime = dateRange.value[1].toISOString()
    }

    const res = await searchOrders(params)
    const { content, totalElements } = res.data.data
    tableData.value = content
    pagination.total = totalElements

    // 更新统计
    stats.value.totalOrders = totalElements
    stats.value.pendingPayment = content.filter((o: Order) => o.status === OrderStatus.PENDING_PAYMENT).length
    stats.value.pendingPickup = content.filter((o: Order) => o.status === OrderStatus.PENDING_PICKUP).length
    stats.value.pendingConfirm = content.filter((o: Order) => o.status === OrderStatus.PENDING_CONFIRM).length
    stats.value.pendingReview = content.filter((o: Order) => o.status === OrderStatus.PENDING_REVIEW).length
    stats.value.completed = content.filter((o: Order) => o.status === OrderStatus.COMPLETED).length
  } catch (error) {
    console.error('获取订单列表失败:', error)
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchOrders()
}

// 重置
const handleReset = () => {
  filterForm.orderNo = ''
  filterForm.buyerId = undefined
  filterForm.sellerId = undefined
  filterForm.status = ''
  dateRange.value = null
  pagination.page = 1
  fetchOrders()
}

// 格式化日期
const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 格式化日期时间
const formatDateTime = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 获取操作人角色文本
const getOperatorRoleText = (role: string) => {
  const roleMap: Record<string, string> = {
    BUYER: '买家',
    SELLER: '卖家',
    SYSTEM: '系统',
    ADMIN: '管理员'
  }
  return roleMap[role] || role
}

// 获取操作标签类型
const getActionTagType = (action: string) => {
  const typeMap: Record<string, any> = {
    CREATE_ORDER: 'success',
    PAY_ORDER: 'warning',
    CONFIRM_PICKUP: 'primary',
    CONFIRM_PAYMENT: 'success',
    CANCEL_ORDER: 'danger',
    ADMIN_UPDATE_STATUS: 'info',
    UPDATE_ADMIN_REMARK: 'info'
  }
  return typeMap[action] || 'info'
}

// 显示日志对话框
const showLogModal = async (order: Order) => {
  try {
    const res = await getOrderLogs(order.id)
    currentOrderLogs.value = res.data.data
    logDialogVisible.value = true
  } catch (error) {
    console.error('获取日志失败:', error)
  }
}

// 修改订单状态
const handleStatusChange = async (status: OrderStatus, order: Order) => {
  try {
    await ElMessage.confirm(`确定将订单 "${order.orderNo}" 的状态修改为 "${getOrderStatusText(status)}" 吗？`, '确认修改', {
      type: 'warning'
    })
    await updateOrderStatus(order.id, status)
    ElMessage.success('订单状态已更新')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('更新状态失败:', error)
      ElMessage.error('更新订单状态失败')
    }
  }
}

// 跳转详情
const goToDetail = (orderId: number) => {
  router.push(`/admin/orders/${orderId}`)
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.admin-order-list-page {
  padding: 20px;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1F2937;
  margin: 0;
}

/* 筛选栏 */
.filter-section {
  background: #FFFFFF;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.filter-section :deep(.el-form-item) {
  margin-bottom: 12px;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #6B7280;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1F2937;
}

.stat-value.pending-payment {
  color: #F59E0B;
}

.stat-value.pending-pickup {
  color: #3B82F6;
}

.stat-value.pending-confirm {
  color: #3B82F6;
}

.stat-value.pending-review {
  color: #10B981;
}

.stat-value.completed {
  color: #10B981;
}

/* 表格卡片 */
.table-card {
  border-radius: 8px;
}

.table-card :deep(.el-card__body) {
  padding: 20px;
}

/* 商品单元格 */
.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-thumb {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  object-fit: cover;
}

.product-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name {
  font-size: 14px;
  color: #1F2937;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 150px;
}

.product-spec {
  font-size: 12px;
  color: #9CA3AF;
}

/* 用户单元格 */
.user-cell {
  text-align: center;
}

.user-name {
  font-size: 14px;
  color: #1F2937;
}

.price {
  color: #F59E0B;
  font-weight: 600;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

/* 日志卡片 */
.log-card {
  margin-bottom: 12px;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.log-role {
  font-size: 12px;
  color: #6B7280;
}

.log-status-change {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.log-remark {
  font-size: 13px;
  color: #4B5563;
  background: #F9FAFB;
  padding: 8px;
  border-radius: 4px;
}
</style>
