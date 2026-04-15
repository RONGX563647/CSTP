<template>
  <AdminLayout>
    <div class="admin-order-detail-page">
      <div class="page-header">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
        <h1 class="page-title">订单详情</h1>
        <el-button type="primary" @click="handlePrint">
          <el-icon><Printer /></el-icon>
          打印订单
        </el-button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <template v-else-if="order">
        <el-row :gutter="16">
          <!-- 左侧：订单信息 -->
          <el-col :span="16">
            <!-- 订单状态卡片 -->
            <el-card class="status-card">
              <div class="status-content">
                <el-tag :type="getOrderStatusColor(order.status)" size="large">
                  {{ getOrderStatusText(order.status) }}
                </el-tag>
                <div class="status-text">{{ getStatusDescription(order.status) }}</div>
              </div>
            </el-card>

            <!-- 订单基本信息 -->
            <el-card class="info-card">
              <template #header>
                <div class="card-header">
                  <span class="card-title">订单基本信息</span>
                  <el-tag type="info" size="small">订单号：{{ order.orderNo }}</el-tag>
                </div>
              </template>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="商品名称">{{ order.productName }}</el-descriptions-item>
                <el-descriptions-item label="商品图片">
                  <el-image
                    :src="order.productImage || '/placeholder.png'"
                    fit="cover"
                    class="product-thumb"
                    :preview-src-list="[order.productImage || '/placeholder.png']"
                  />
                </el-descriptions-item>
                <el-descriptions-item label="单价">¥{{ order.price }}</el-descriptions-item>
                <el-descriptions-item label="数量">{{ order.quantity }}</el-descriptions-item>
                <el-descriptions-item label="订单金额">
                  <span class="price">¥{{ order.totalAmount }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="面交地点">{{ order.meetLocation || '未设定' }}</el-descriptions-item>
                <el-descriptions-item label="约定时间">{{ formatDateTime(order.meetTime) }}</el-descriptions-item>
                <el-descriptions-item label="买家留言" :span="2">{{ order.buyerRemark || '无' }}</el-descriptions-item>
              </el-descriptions>
            </el-card>

            <!-- 交易时间线 -->
            <el-card class="timeline-card">
              <template #header>
                <span class="card-title">交易时间线</span>
              </template>
              <el-timeline>
                <el-timeline-item
                  timestamp="订单创建"
                  placement="top"
                  :color="getTimelineColor('created')"
                >
                  <el-card class="timeline-card-item">
                    <div class="timeline-content">
                      {{ formatDateTime(order.createdAt) }}
                    </div>
                  </el-card>
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.paymentTime"
                  timestamp="付款时间"
                  placement="top"
                  :color="getTimelineColor('paid')"
                >
                  <el-card class="timeline-card-item">
                    <div class="timeline-content">
                      {{ formatDateTime(order.paymentTime) }}
                    </div>
                  </el-card>
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.pickupTime"
                  timestamp="提货时间"
                  placement="top"
                  :color="getTimelineColor('picked')"
                >
                  <el-card class="timeline-card-item">
                    <div class="timeline-content">
                      {{ formatDateTime(order.pickupTime) }}
                    </div>
                  </el-card>
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.confirmTime"
                  timestamp="确认时间"
                  placement="top"
                  :color="getTimelineColor('confirmed')"
                >
                  <el-card class="timeline-card-item">
                    <div class="timeline-content">
                      {{ formatDateTime(order.confirmTime) }}
                    </div>
                  </el-card>
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.completeTime"
                  timestamp="完成时间"
                  placement="top"
                  :color="getTimelineColor('completed')"
                >
                  <el-card class="timeline-card-item">
                    <div class="timeline-content success">
                      <el-icon><CircleCheck /></el-icon>
                      交易完成
                    </div>
                  </el-card>
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.cancelTime"
                  timestamp="取消时间"
                  placement="top"
                  type="danger"
                >
                  <el-card class="timeline-card-item">
                    <div class="timeline-content cancel">
                      <el-icon><Close /></el-icon>
                      <div class="cancel-info">
                        <div>交易已取消</div>
                        <div v-if="order.cancelReason" class="cancel-reason">原因：{{ order.cancelReason }}</div>
                        <div v-if="order.cancelRole" class="cancel-role">
                          取消方：{{ getCancelRoleText(order.cancelRole) }}
                        </div>
                      </div>
                    </div>
                  </el-card>
                </el-timeline-item>
              </el-timeline>
            </el-card>

            <!-- 订单日志 -->
            <el-card class="logs-card">
              <template #header>
                <div class="card-header">
                  <span class="card-title">订单操作日志</span>
                  <el-button size="small" @click="refreshLogs">
                    <el-icon><Refresh /></el-icon>
                    刷新
                  </el-button>
                </div>
              </template>
              <el-timeline v-if="orderLogs.length > 0" class="logs-timeline">
                <el-timeline-item
                  v-for="log in orderLogs"
                  :key="log.id"
                  :timestamp="formatDateTime(log.createTime)"
                  placement="top"
                >
                  <div class="log-item">
                    <el-tag :type="getActionTagType(log.action)" size="small">
                      {{ log.action }}
                    </el-tag>
                    <span class="log-role">{{ getOperatorRoleText(log.operatorRole) }}</span>
                    <div v-if="log.fromStatus && log.toStatus" class="log-status-change">
                      {{ getOrderStatusText(log.fromStatus) }}
                      <el-icon><ArrowRight /></el-icon>
                      {{ getOrderStatusText(log.toStatus) }}
                    </div>
                    <div v-if="log.remark" class="log-remark">{{ log.remark }}</div>
                  </div>
                </el-timeline-item>
              </el-timeline>
              <el-empty v-else description="暂无日志记录" />
            </el-card>
          </el-col>

          <!-- 右侧：用户信息和操作 -->
          <el-col :span="8">
            <!-- 买家信息 -->
            <el-card class="user-card">
              <template #header>
                <span class="card-title">买家信息</span>
              </template>
              <div class="user-info">
                <div class="user-row">
                  <span class="label">用户名</span>
                  <span class="value">{{ order.buyerName || `用户${order.buyerId}` }}</span>
                </div>
                <div class="user-row">
                  <span class="label">联系电话</span>
                  <span class="value">{{ order.buyerPhone || '未绑定' }}</span>
                </div>
                <div class="user-row">
                  <span class="label">用户 ID</span>
                  <span class="value">{{ order.buyerId }}</span>
                </div>
                <el-button size="small" class="mt-2" @click="viewUser(order.buyerId)">
                  查看用户详情
                </el-button>
              </div>
            </el-card>

            <!-- 卖家信息 -->
            <el-card class="user-card">
              <template #header>
                <span class="card-title">卖家信息</span>
              </template>
              <div class="user-info">
                <div class="user-row">
                  <span class="label">用户名</span>
                  <span class="value">{{ order.sellerName || `用户${order.sellerId}` }}</span>
                </div>
                <div class="user-row">
                  <span class="label">联系电话</span>
                  <span class="value">{{ order.sellerPhone || '未绑定' }}</span>
                </div>
                <div class="user-row">
                  <span class="label">用户 ID</span>
                  <span class="value">{{ order.sellerId }}</span>
                </div>
                <el-button size="small" class="mt-2" @click="viewUser(order.sellerId)">
                  查看用户详情
                </el-button>
              </div>
            </el-card>

            <!-- 管理员备注 -->
            <el-card class="remark-card">
              <template #header>
                <span class="card-title">管理员备注</span>
              </template>
              <el-input
                v-model="adminRemark"
                type="textarea"
                :rows="4"
                placeholder="添加备注信息，仅管理员可见"
              />
              <el-button
                type="primary"
                class="mt-2"
                @click="handleSaveRemark"
                style="width: 100%;"
              >
                保存备注
              </el-button>
            </el-card>

            <!-- 订单评价 -->
            <el-card v-if="order.hasBuyerReview || order.hasSellerReview" class="review-card">
              <template #header>
                <span class="card-title">订单评价</span>
              </template>
              <div v-if="order.hasBuyerReview" class="review-item">
                <div class="review-header">
                  <span class="review-type">
                    <el-icon><User /></el-icon>
                    买家评价
                  </span>
                </div>
                <div class="review-content">{{ buyerReview.content }}</div>
              </div>
              <div v-if="order.hasSellerReview" class="review-item">
                <div class="review-header">
                  <span class="review-type">
                    <el-icon><User /></el-icon>
                    卖家评价
                  </span>
                </div>
                <div class="review-content">{{ sellerReview.content }}</div>
              </div>
            </el-card>

            <!-- 快捷操作 -->
            <el-card class="action-card">
              <template #header>
                <span class="card-title">快捷操作</span>
              </template>
              <div class="action-list">
                <el-button
                  v-for="status in availableStatuses"
                  :key="status.value"
                  size="small"
                  class="action-btn"
                  @click="handleUpdateStatus(status.value)"
                >
                  设为{{ status.label }}
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <!-- 订单不存在 -->
      <el-empty v-else description="订单不存在" />
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Loading, ArrowLeft, ArrowRight, Printer, Refresh,
  User, CircleCheck, Close
} from '@element-plus/icons-vue'
import {
  getAdminOrderById, updateAdminRemark, updateOrderStatus,
  getOrderLogs, Order, OrderLog, OrderStatus,
  getOrderStatusText, getOrderStatusColor
} from '@/api/order'
import AdminLayout from '@/layouts/AdminLayout.vue'

const router = useRouter()
const route = useRoute()

const order = ref<Order | null>(null)
const loading = ref(true)
const adminRemark = ref('')
const orderLogs = ref<OrderLog[]>([])
const buyerReview = ref({ content: '' })
const sellerReview = ref({ content: '' })

const availableStatuses = [
  { label: '待付款', value: OrderStatus.PENDING_PAYMENT },
  { label: '待提货', value: OrderStatus.PENDING_PICKUP },
  { label: '待确认', value: OrderStatus.PENDING_CONFIRM },
  { label: '待评价', value: OrderStatus.PENDING_REVIEW },
  { label: '已完成', value: OrderStatus.COMPLETED },
  { label: '已取消', value: OrderStatus.CANCELLED }
]

// 获取状态描述
const getStatusDescription = (status: OrderStatus) => {
  const descMap: Record<OrderStatus, string> = {
    [OrderStatus.PENDING_PAYMENT]: '等待买家付款',
    [OrderStatus.PENDING_PICKUP]: '等待买家确认提货',
    [OrderStatus.PENDING_CONFIRM]: '等待卖家确认收款',
    [OrderStatus.PENDING_REVIEW]: '等待双方评价',
    [OrderStatus.COMPLETED]: '交易已完成',
    [OrderStatus.CANCELLED]: '订单已取消',
    [OrderStatus.REFUNDED]: '订单已退款'
  }
  return descMap[status] || ''
}

// 获取取消方文本
const getCancelRoleText = (role: string) => {
  const roleMap: Record<string, string> = {
    BUYER: '买家',
    SELLER: '卖家',
    SYSTEM: '系统'
  }
  return roleMap[role] || role
}

// 获取时间线颜色
const getTimelineColor = (stage: string) => {
  const colorMap: Record<string, string> = {
    created: '#9CA3AF',
    paid: '#F59E0B',
    picked: '#3B82F6',
    confirmed: '#10B981',
    completed: '#10B981'
  }
  return colorMap[stage] || '#9CA3AF'
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

const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return '未记录'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 获取订单详情
const fetchOrder = async () => {
  loading.value = true
  try {
    const res = await getAdminOrderById(Number(route.params.id))
    order.value = res.data.data
    adminRemark.value = order.value.adminRemark || ''

    // 加载评价信息
    // 注意：当前 API 返回的评价数据可能不完整，这里简化处理
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('订单不存在')
  } finally {
    loading.value = false
  }
}

// 刷新日志
const refreshLogs = async () => {
  try {
    const res = await getOrderLogs(Number(route.params.id))
    orderLogs.value = res.data.data
  } catch (error) {
    console.error('获取日志失败:', error)
  }
}

// 保存备注
const handleSaveRemark = async () => {
  try {
    await updateAdminRemark(order.value!.id, adminRemark.value)
    ElMessage.success('备注已保存')
    fetchOrder()
  } catch (error) {
    console.error('保存备注失败:', error)
    ElMessage.error('保存备注失败')
  }
}

// 更新订单状态
const handleUpdateStatus = async (status: OrderStatus) => {
  try {
    await updateOrderStatus(order.value!.id, status)
    ElMessage.success('订单状态已更新')
    fetchOrder()
    refreshLogs()
  } catch (error) {
    console.error('更新状态失败:', error)
    ElMessage.error('更新订单状态失败')
  }
}

// 查看用户详情
const viewUser = (userId: number) => {
  router.push(`/admin/products/user/${userId}`)
}

// 打印订单
const handlePrint = () => {
  window.print()
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchOrder()
  refreshLogs()
})
</script>

<style scoped>
.admin-order-detail-page {
  padding: 20px;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1F2937;
  margin: 0;
  flex: 1;
}

/* 加载中 */
.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  gap: 12px;
  color: #9CA3AF;
}

/* 状态卡片 */
.status-card {
  margin-bottom: 16px;
}

.status-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-text {
  font-size: 16px;
  color: #4B5563;
}

/* 卡片通用样式 */
.info-card,
.timeline-card,
.user-card,
.remark-card,
.review-card,
.action-card,
.logs-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1F2937;
}

/* 商品图片 */
.product-thumb {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  cursor: pointer;
}

.price {
  color: #F59E0B;
  font-weight: 600;
  font-size: 16px;
}

/* 时间线 */
.timeline-card-item {
  margin-bottom: 8px;
}

.timeline-content {
  font-size: 14px;
  color: #4B5563;
}

.timeline-content.success {
  color: #10B981;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.timeline-content.cancel {
  color: #EF4444;
}

.cancel-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.cancel-reason,
.cancel-role {
  font-size: 12px;
  color: #6B7280;
}

/* 用户信息 */
.user-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #F3F4F6;
}

.user-row:last-child {
  border-bottom: none;
}

.user-row .label {
  font-size: 13px;
  color: #6B7280;
}

.user-row .value {
  font-size: 14px;
  color: #1F2937;
}

.mt-2 {
  margin-top: 8px;
}

/* 评价 */
.review-item {
  padding: 12px 0;
  border-bottom: 1px solid #F3F4F6;
}

.review-item:last-child {
  border-bottom: none;
}

.review-header {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.review-type {
  font-size: 13px;
  font-weight: 500;
  color: #6B7280;
  display: flex;
  align-items: center;
  gap: 4px;
}

.review-content {
  font-size: 14px;
  color: #4B5563;
  line-height: 1.6;
}

/* 操作列表 */
.action-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-btn {
  width: 100%;
}

/* 日志项 */
.log-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px;
  background: #F9FAFB;
  border-radius: 4px;
}

.log-role {
  font-size: 12px;
  color: #6B7280;
}

.log-status-change {
  font-size: 12px;
  color: #9CA3AF;
  display: flex;
  align-items: center;
  gap: 4px;
}

.log-remark {
  font-size: 13px;
  color: #4B5563;
}

/* 打印样式 */
@media print {
  .el-button,
  .page-header {
    display: none !important;
  }
}
</style>
