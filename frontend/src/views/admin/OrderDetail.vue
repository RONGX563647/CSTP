<template>
  <AdminLayout>
    <div class="admin-order-detail-page">
      <div class="page-header">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
        <h1 class="page-title">订单详情</h1>
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

            <!-- 订单信息 -->
            <el-card class="info-card">
              <template #header>
                <span class="card-title">订单信息</span>
              </template>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
                <el-descriptions-item label="创建时间">{{ formatDate(order.createdAt) }}</el-descriptions-item>
                <el-descriptions-item label="商品名称">{{ order.productName }}</el-descriptions-item>
                <el-descriptions-item label="商品图片">
                  <el-image
                    :src="order.productImage || '/placeholder.png'"
                    fit="cover"
                    style="width: 60px; height: 60px; border-radius: 4px;"
                  />
                </el-descriptions-item>
                <el-descriptions-item label="单价">¥{{ order.price }}</el-descriptions-item>
                <el-descriptions-item label="数量">{{ order.quantity }}</el-descriptions-item>
                <el-descriptions-item label="订单金额">¥{{ order.totalAmount }}</el-descriptions-item>
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
                <el-timeline-item timestamp="创建订单" placement="top">
                  <el-card>
                    {{ formatDateTime(order.createdAt) }}
                  </el-card>
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.paymentTime"
                  timestamp="付款时间"
                  placement="top"
                >
                  <el-card>
                    {{ formatDateTime(order.paymentTime) }}
                  </el-card>
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.pickupTime"
                  timestamp="提货时间"
                  placement="top"
                >
                  <el-card>
                    {{ formatDateTime(order.pickupTime) }}
                  </el-card>
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.confirmTime"
                  timestamp="确认时间"
                  placement="top"
                >
                  <el-card>
                    {{ formatDateTime(order.confirmTime) }}
                  </el-card>
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.completeTime"
                  timestamp="完成时间"
                  placement="top"
                >
                  <el-card>
                    {{ formatDateTime(order.completeTime) }}
                  </el-card>
                </el-timeline-item>
                <el-timeline-item
                  v-if="order.cancelTime"
                  timestamp="取消时间"
                  placement="top"
                  type="danger"
                >
                  <el-card>
                    {{ formatDateTime(order.cancelTime) }}
                    <div v-if="order.cancelReason" class="cancel-reason">
                      取消原因：{{ order.cancelReason }}
                    </div>
                  </el-card>
                </el-timeline-item>
              </el-timeline>
            </el-card>
          </el-col>

          <!-- 右侧：用户信息 -->
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
                placeholder="添加备注信息"
              />
              <el-button
                type="primary"
                class="mt-2"
                @click="handleSaveRemark"
                style="width: 100%; margin-top: 12px;"
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
                  <span class="review-type">买家评价</span>
                </div>
                <div class="review-content">评价内容待加载...</div>
              </div>
              <div v-if="order.hasSellerReview" class="review-item">
                <div class="review-header">
                  <span class="review-type">卖家评价</span>
                </div>
                <div class="review-content">评价内容待加载...</div>
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
import { Loading, ArrowLeft } from '@element-plus/icons-vue'
import { getAdminOrderById, updateAdminRemark, Order, OrderStatus, getOrderStatusText, getOrderStatusColor } from '@/api/order'
import AdminLayout from '@/layouts/AdminLayout.vue'

const router = useRouter()
const route = useRoute()

const order = ref<Order | null>(null)
const loading = ref(true)
const adminRemark = ref('')

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

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return '未设定'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

// 格式化日期时间
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
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('订单不存在')
  } finally {
    loading.value = false
  }
}

// 保存备注
const handleSaveRemark = async () => {
  try {
    await updateAdminRemark(order.value!.id, adminRemark.value)
    ElMessage.success('备注已保存')
  } catch (error) {
    console.error('保存备注失败:', error)
    ElMessage.error('保存备注失败')
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchOrder()
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
.review-card {
  margin-bottom: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1F2937;
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
}

.review-type {
  font-size: 13px;
  font-weight: 500;
  color: #6B7280;
}

.review-content {
  font-size: 14px;
  color: #4B5563;
}

/* 取消原因 */
.cancel-reason {
  margin-top: 8px;
  font-size: 13px;
  color: #EF4444;
}

.mt-2 {
  margin-top: 8px;
}
</style>
