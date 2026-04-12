<template>
  <div class="order-detail-page">
    <!-- 加载中 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <template v-else-if="order">
      <!-- 订单状态卡片 -->
      <div class="status-card">
        <div class="status-icon">
          <el-icon :size="40" :color="getStatusColor(order.status)">
            <component :is="getStatusIcon(order.status)" />
          </el-icon>
        </div>
        <div class="status-text">
          <div class="status-title">{{ getOrderStatusText(order.status) }}</div>
          <div class="status-desc">{{ getStatusDescription(order.status) }}</div>
        </div>
      </div>

      <!-- 订单信息 -->
      <div class="order-info-card">
        <div class="card-header">订单信息</div>
        <div class="info-row">
          <span class="label">订单号</span>
          <span class="value">{{ order.orderNo }}</span>
        </div>
        <div class="info-row">
          <span class="label">创建时间</span>
          <span class="value">{{ formatDate(order.createdAt) }}</span>
        </div>
        <div class="info-row" v-if="order.meetLocation">
          <span class="label">面交地点</span>
          <span class="value">{{ order.meetLocation }}</span>
        </div>
        <div class="info-row" v-if="order.meetTime">
          <span class="label">约定时间</span>
          <span class="value">{{ formatDate(order.meetTime) }}</span>
        </div>
        <div class="info-row" v-if="order.buyerRemark">
          <span class="label">买家留言</span>
          <span class="value">{{ order.buyerRemark }}</span>
        </div>
        <div class="info-row" v-if="order.cancelReason">
          <span class="label">取消原因</span>
          <span class="value">{{ order.cancelReason }}</span>
        </div>
      </div>

      <!-- 商品信息 -->
      <div class="product-card">
        <div class="card-header">商品信息</div>
        <div class="product-content">
          <el-image
            :src="order.productImage || '/placeholder.png'"
            fit="cover"
            class="product-image"
          />
          <div class="product-info">
            <div class="product-name">{{ order.productName }}</div>
            <div class="product-meta">
              <span>单价：¥{{ order.price }}</span>
              <span>数量：x{{ order.quantity }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 交易信息 -->
      <div class="transaction-card">
        <div class="card-header">交易信息</div>
        <div class="info-grid">
          <div class="info-item">
            <span class="label">订单金额</span>
            <span class="value amount">¥{{ order.totalAmount }}</span>
          </div>
          <div class="info-item" v-if="order.paymentTime">
            <span class="label">付款时间</span>
            <span class="value">{{ formatDate(order.paymentTime) }}</span>
          </div>
          <div class="info-item" v-if="order.pickupTime">
            <span class="label">提货时间</span>
            <span class="value">{{ formatDate(order.pickupTime) }}</span>
          </div>
          <div class="info-item" v-if="order.confirmTime">
            <span class="label">确认时间</span>
            <span class="value">{{ formatDate(order.confirmTime) }}</span>
          </div>
          <div class="info-item" v-if="order.completeTime">
            <span class="label">完成时间</span>
            <span class="value">{{ formatDate(order.completeTime) }}</span>
          </div>
        </div>
      </div>

      <!-- 对方信息 -->
      <div class="counterparty-card">
        <div class="card-header">
          {{ isBuyer ? '卖家信息' : '买家信息' }}
        </div>
        <div class="info-row">
          <span class="label">姓名</span>
          <span class="value">{{ counterpartyName }}</span>
        </div>
        <div class="info-row" v-if="counterpartyPhone">
          <span class="label">联系电话</span>
          <span class="value">{{ counterpartyPhone }}</span>
        </div>
      </div>

      <!-- 订单评价 -->
      <div v-if="order.status === 'COMPLETED' || order.hasBuyerReview || order.hasSellerReview" class="review-card">
        <div class="card-header">订单评价</div>
        <div v-if="order.hasBuyerReview" class="review-item">
          <div class="review-header">
            <span class="review-type">买家评价</span>
            <div class="rating">
              <el-rate v-model="buyerReview.rating" disabled />
            </div>
          </div>
          <div class="review-content">{{ buyerReview.content }}</div>
        </div>
        <div v-if="order.hasSellerReview" class="review-item">
          <div class="review-header">
            <span class="review-type">卖家评价</span>
            <div class="rating">
              <el-rate v-model="sellerReview.rating" disabled />
            </div>
          </div>
          <div class="review-content">{{ sellerReview.content }}</div>
        </div>
      </div>

      <!-- 底部操作栏 -->
      <div class="action-bar">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-button
          v-if="showPayButton"
          type="primary"
          @click="handlePay"
        >
          确认付款
        </el-button>
        <el-button
          v-if="showPickupButton"
          type="success"
          @click="handlePickup"
        >
          确认提货
        </el-button>
        <el-button
          v-if="showConfirmButton"
          type="success"
          @click="handleConfirm"
        >
          确认收款
        </el-button>
        <el-button
          v-if="showReviewButton"
          type="primary"
          @click="handleReview"
        >
          评价
        </el-button>
        <el-button
          v-if="showCancelButton"
          type="danger"
          @click="handleCancel"
        >
          取消订单
        </el-button>
      </div>
    </template>

    <!-- 订单不存在 -->
    <el-empty v-else description="订单不存在" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, ArrowLeft, CircleCheck, Clock, CloseCircle, Timer } from '@element-plus/icons-vue'
import { getOrderById, payOrder, confirmPickup, confirmPayment, cancelOrder, Order, OrderStatus, getOrderStatusText } from '@/api/order'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const order = ref<Order | null>(null)
const loading = ref(true)
const buyerReview = ref({ rating: 0, content: '' })
const sellerReview = ref({ rating: 0, content: '' })

// 是否是买家
const isBuyer = computed(() => {
  return order.value?.buyerId === authStore.userInfo?.id
})

// 对方信息
const counterpartyName = computed(() => {
  if (!order.value) return ''
  return isBuyer.value ? order.value.sellerName : order.value.buyerName
})

const counterpartyPhone = computed(() => {
  if (!order.value) return ''
  return isBuyer.value ? order.value.sellerPhone : order.value.buyerPhone
})

// 按钮显示逻辑
const showPayButton = computed(() => {
  return isBuyer.value && order.value?.status === OrderStatus.PENDING_PAYMENT
})

const showPickupButton = computed(() => {
  return isBuyer.value && order.value?.status === OrderStatus.PENDING_PICKUP
})

const showConfirmButton = computed(() => {
  return !isBuyer.value && order.value?.status === OrderStatus.PENDING_CONFIRM
})

const showReviewButton = computed(() => {
  return isBuyer.value &&
         order.value?.status === OrderStatus.PENDING_REVIEW &&
         !order.value?.hasBuyerReview
})

const showCancelButton = computed(() => {
  const status = order.value?.status
  return status === OrderStatus.PENDING_PAYMENT ||
         status === OrderStatus.PENDING_PICKUP
})

// 获取订单详情
const fetchOrder = async () => {
  loading.value = true
  try {
    const res = await getOrderById(Number(route.params.id))
    order.value = res.data.data
    // 如果有评价，加载评价信息（这里简化处理）
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('订单不存在')
  } finally {
    loading.value = false
  }
}

// 获取状态图标
const getStatusIcon = (status: OrderStatus) => {
  const iconMap: Record<OrderStatus, any> = {
    [OrderStatus.PENDING_PAYMENT]: Clock,
    [OrderStatus.PENDING_PICKUP]: Timer,
    [OrderStatus.PENDING_CONFIRM]: CircleCheck,
    [OrderStatus.PENDING_REVIEW]: Timer,
    [OrderStatus.COMPLETED]: CircleCheck,
    [OrderStatus.CANCELLED]: CloseCircle,
    [OrderStatus.REFUNDED]: CloseCircle
  }
  return iconMap[status] || Clock
}

// 获取状态颜色
const getStatusColor = (status: OrderStatus) => {
  const colorMap: Record<OrderStatus, string> = {
    [OrderStatus.PENDING_PAYMENT]: '#F59E0B',
    [OrderStatus.PENDING_PICKUP]: '#3B82F6',
    [OrderStatus.PENDING_CONFIRM]: '#3B82F6',
    [OrderStatus.PENDING_REVIEW]: '#10B981',
    [OrderStatus.COMPLETED]: '#10B981',
    [OrderStatus.CANCELLED]: '#EF4444',
    [OrderStatus.REFUNDED]: '#6B7280'
  }
  return colorMap[status] || '#9CA3AF'
}

// 获取状态描述
const getStatusDescription = (status: OrderStatus) => {
  const descMap: Record<OrderStatus, string> = {
    [OrderStatus.PENDING_PAYMENT]: '请买家确认付款',
    [OrderStatus.PENDING_PICKUP]: '请买家确认提货',
    [OrderStatus.PENDING_CONFIRM]: '请卖家确认收款',
    [OrderStatus.PENDING_REVIEW]: '等待双方评价',
    [OrderStatus.COMPLETED]: '交易已完成',
    [OrderStatus.CANCELLED]: '订单已取消',
    [OrderStatus.REFUNDED]: '订单已退款'
  }
  return descMap[status] || ''
}

// 格式化日期
const formatDate = (dateStr?: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 操作处理
const handlePay = async () => {
  try {
    await ElMessageBox.confirm('确认已付款吗？', '付款确认', { type: 'info' })
    await payOrder(order.value!.id)
    ElMessage.success('付款成功')
    fetchOrder()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('付款失败')
    }
  }
}

const handlePickup = async () => {
  try {
    await ElMessageBox.confirm('确认已提货吗？', '提货确认', { type: 'info' })
    await confirmPickup(order.value!.id)
    ElMessage.success('提货确认成功')
    fetchOrder()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('提货确认失败')
    }
  }
}

const handleConfirm = async () => {
  try {
    await ElMessageBox.confirm('确认已收款吗？', '收款确认', { type: 'info' })
    await confirmPayment(order.value!.id)
    ElMessage.success('收款确认成功')
    fetchOrder()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('收款确认失败')
    }
  }
}

const handleReview = () => {
  router.push(`/user/orders/${order.value!.id}/review`)
}

const handleCancel = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
      type: 'warning',
      inputPattern: /.+/,
      inputErrorMessage: '请输入取消原因'
    })
    await cancelOrder(order.value!.id, value)
    ElMessage.success('订单取消成功')
    fetchOrder()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('取消订单失败')
    }
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
.order-detail-page {
  min-height: 100vh;
  background: #F5F5F5;
  padding-bottom: 70px;
}

/* 加载中 */
.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  gap: 12px;
  color: #9CA3AF;
}

/* 状态卡片 */
.status-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 16px;
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 100%);
  color: #FFFFFF;
  margin-bottom: 12px;
}

.status-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-text {
  flex: 1;
}

.status-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 4px;
}

.status-desc {
  font-size: 13px;
  opacity: 0.9;
}

/* 卡片通用样式 */
.order-info-card,
.product-card,
.transaction-card,
.counterparty-card,
.review-card {
  background: #FFFFFF;
  border-radius: 8px;
  margin-bottom: 12px;
  overflow: hidden;
}

.card-header {
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #1F2937;
  border-bottom: 1px solid #E5E5E5;
}

/* 信息行 */
.info-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #F3F4F6;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row .label {
  font-size: 13px;
  color: #6B7280;
}

.info-row .value {
  font-size: 14px;
  color: #1F2937;
}

/* 商品信息 */
.product-content {
  display: flex;
  gap: 12px;
  padding: 16px;
}

.product-image {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  object-fit: cover;
}

.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.product-name {
  font-size: 15px;
  color: #1F2937;
  font-weight: 500;
}

.product-meta {
  font-size: 13px;
  color: #9CA3AF;
  display: flex;
  gap: 12px;
}

/* 交易信息网格 */
.info-grid {
  padding: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #F3F4F6;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  font-size: 13px;
  color: #6B7280;
}

.info-item .value {
  font-size: 14px;
  color: #1F2937;
}

.info-item .value.amount {
  color: #F59E0B;
  font-weight: 600;
}

/* 评价 */
.review-item {
  padding: 16px;
  border-bottom: 1px solid #F3F4F6;
}

.review-item:last-child {
  border-bottom: none;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.review-type {
  font-size: 13px;
  color: #6B7280;
  font-weight: 500;
}

.review-content {
  font-size: 14px;
  color: #4B5563;
  line-height: 1.6;
}

/* 底部操作栏 */
.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  background: #FFFFFF;
  border-top: 1px solid #E5E5E5;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
}

.action-bar .el-button {
  flex: 1;
}
</style>
