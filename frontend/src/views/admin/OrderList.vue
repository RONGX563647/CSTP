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
            <el-input v-model="filterForm.orderNo" placeholder="请输入订单号" clearable />
          </el-form-item>
          <el-form-item label="订单状态">
            <el-select v-model="filterForm.status" placeholder="全部状态" clearable>
              <el-option label="待付款" :value="OrderStatus.PENDING_PAYMENT" />
              <el-option label="待提货" :value="OrderStatus.PENDING_PICKUP" />
              <el-option label="待确认" :value="OrderStatus.PENDING_CONFIRM" />
              <el-option label="待评价" :value="OrderStatus.PENDING_REVIEW" />
              <el-option label="已完成" :value="OrderStatus.COMPLETED" />
              <el-option label="已取消" :value="OrderStatus.CANCELLED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stats-row">
        <el-col :span="4">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">总订单数</div>
              <div class="stat-value">{{ stats.totalOrders }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">待付款</div>
              <div class="stat-value pending-payment">{{ stats.pendingPayment }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">待提货</div>
              <div class="stat-value pending-pickup">{{ stats.pendingPickup }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">待确认</div>
              <div class="stat-value pending-confirm">{{ stats.pendingConfirm }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">待评价</div>
              <div class="stat-value pending-review">{{ stats.pendingReview }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">已完成</div>
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
          <el-table-column prop="productName" label="商品名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="buyerName" label="买家" width="100" />
          <el-table-column prop="sellerName" label="卖家" width="100" />
          <el-table-column prop="totalAmount" label="金额" width="100">
            <template #default="{ row }">
              <span class="price">¥{{ row.totalAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="60" />
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
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="goToDetail(row.id)">
                详情
              </el-button>
              <el-button size="small" text type="warning" @click="showLogModal(row)">
                日志
              </el-button>
              <el-dropdown size="small" @command="(cmd) => handleStatusChange(cmd, row)">
                <el-button size="small" text>
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
      <el-dialog v-model="logDialogVisible" title="订单日志" width="600px">
        <el-timeline v-if="currentOrderLogs.length > 0">
          <el-timeline-item
            v-for="log in currentOrderLogs"
            :key="log.id"
            :timestamp="formatDate(log.createTime)"
            placement="top"
          >
            <el-card>
              <div class="log-item">
                <div class="log-action">{{ log.action }}</div>
                <div class="log-role">操作人：{{ getOperatorRoleText(log.operatorRole) }}</div>
                <div v-if="log.fromStatus && log.toStatus" class="log-status">
                  {{ getOrderStatusText(log.fromStatus) }} → {{ getOrderStatusText(log.toStatus) }}
                </div>
                <div v-if="log.remark" class="log-remark">{{ log.remark }}</div>
              </div>
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
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { getAllOrders, searchOrders, updateOrderStatus, getOrderLogs, Order, OrderStatus, OrderLog, getOrderStatusText, getOrderStatusColor } from '@/api/order'
import AdminLayout from '@/layouts/AdminLayout.vue'

const router = useRouter()

const filterForm = reactive({
  orderNo: '',
  status: '' as OrderStatus | ''
})

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
    if (filterForm.status) params.status = filterForm.status

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
  filterForm.status = ''
  pagination.page = 1
  fetchOrders()
}

// 格式化日期
const formatDate = (dateStr: string) => {
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
    await updateOrderStatus(order.id, status)
    ElMessage.success('订单状态已更新')
    fetchOrders()
  } catch (error) {
    console.error('更新状态失败:', error)
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
}

.stat-value {
  font-size: 24px;
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

/* 日志项 */
.log-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.log-action {
  font-weight: 600;
  color: #1F2937;
}

.log-role {
  font-size: 12px;
  color: #6B7280;
}

.log-status {
  font-size: 12px;
  color: #9CA3AF;
}

.log-remark {
  font-size: 13px;
  color: #4B5563;
}
</style>
