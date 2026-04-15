<template>
  <MobileLayout :title="pageTitle" :show-tab-bar="false">
    <div class="order-detail-page">
      <!-- 加载中 -->
      <div v-if="loading" class="loading-container">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <template v-else-if="order">
        <!-- 订单状态卡片 -->
        <div class="status-card" :class="getStatusBgColor(order.status)">
          <div class="status-icon">
            <el-icon :size="36" color="#FFFFFF">
              <component :is="getStatusIcon(order.status)" />
            </el-icon>
          </div>
          <div class="status-text">
            <div class="status-title">{{ getOrderStatusText(order.status) }}</div>
            <div class="status-desc">{{ getStatusDescription(order.status) }}</div>
          </div>
        </div>

        <!-- 商品信息 -->
        <div class="section-card">
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

        <!-- 订单信息 -->
        <div class="section-card">
          <div class="card-header">订单信息</div>
          <div class="info-row">
            <span class="label">订单号</span>
            <span class="value">{{ order.orderNo }}</span>
          </div>
          <div class="info-row">
            <span class="label">下单时间</span>
            <span class="value">{{ formatDateTime(order.createdAt) }}</span>
          </div>
          <div class="info-row" v-if="order.meetLocation">
            <span class="label">面交地点</span>
            <span class="value">{{ order.meetLocation }}</span>
          </div>
          <div class="info-row" v-if="order.meetTime">
            <span class="label">约定时间</span>
            <span class="value">{{ formatDateTime(order.meetTime) }}</span>
          </div>
          <div class="info-row" v-if="order.buyerRemark">
            <span class="label">买家留言</span>
            <span class="value">{{ order.buyerRemark }}</span>
          </div>
          <div class="info-row" v-if="order.cancelReason">
            <span class="label">取消原因</span>
            <span class="value cancel">{{ order.cancelReason }}</span>
          </div>
        </div>

        <!-- 交易时间线 -->
        <div class="section-card">
          <div class="card-header">交易进度</div>
          <el-timeline class="timeline">
            <el-timeline-item
              timestamp="创建订单"
              placement="top"
              :color="getTimelineColor('created')"
            >
              <div class="timeline-item">
                {{ formatDateTime(order.createdAt) }}
              </div>
            </el-timeline-item>
            <el-timeline-item
              v-if="order.paymentTime"
              timestamp="付款时间"
              placement="top"
              :color="getTimelineColor('paid')"
            >
              <div class="timeline-item">
                {{ formatDateTime(order.paymentTime) }}
              </div>
            </el-timeline-item>
            <el-timeline-item
              v-if="order.pickupTime"
              timestamp="提货时间"
              placement="top"
              :color="getTimelineColor('picked')"
            >
              <div class="timeline-item">
                {{ formatDateTime(order.pickupTime) }}
              </div>
            </el-timeline-item>
            <el-timeline-item
              v-if="order.confirmTime"
              timestamp="确认时间"
              placement="top"
              :color="getTimelineColor('confirmed')"
            >
              <div class="timeline-item">
                {{ formatDateTime(order.confirmTime) }}
              </div>
            </el-timeline-item>
            <el-timeline-item
              v-if="order.completeTime"
              timestamp="完成时间"
              placement="top"
              :color="getTimelineColor('completed')"
            >
              <div class="timeline-item">
                {{ formatDateTime(order.completeTime) }}
              </div>
            </el-timeline-item>
            <el-timeline-item
              v-if="order.cancelTime"
              timestamp="取消时间"
              placement="top"
              type="danger"
            >
              <div class="timeline-item cancel">
                {{ formatDateTime(order.cancelTime) }}
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <!-- 对方信息 -->
        <div class="section-card">
          <div class="card-header">{{ isBuyer ? '卖家信息' : '买家信息' }}</div>
          <div class="info-row">
            <span class="label">用户名</span>
            <span class="value">{{ counterpartyName }}</span>
          </div>
          <div class="info-row" v-if="counterpartyPhone">
            <span class="label">联系电话</span>
            <span class="value">{{ counterpartyPhone }}</span>
          </div>
          <div class="contact-action">
            <el-button type="warning" size="small" @click="contactCounterparty">
              <el-icon><ChatDotRound /></el-icon>
              联系{{ isBuyer ? '卖家' : '买家' }}
            </el-button>
          </div>
        </div>

        <!-- 订单评价 -->
        <div v-if="order.status === 'COMPLETED' || order.hasBuyerReview || order.hasSellerReview" class="section-card">
          <div class="card-header">订单评价</div>
          <div v-if="order.hasBuyerReview" class="review-item">
            <div class="review-header">
              <span class="review-type">买家评价</span>
              <el-rate v-model="buyerReview.rating" disabled size="small" />
            </div>
            <div class="review-content">{{ buyerReview.content }}</div>
          </div>
          <div v-if="order.hasSellerReview" class="review-item">
            <div class="review-header">
              <span class="review-type">卖家评价</span>
              <el-rate v-model="sellerReview.rating" disabled size="small" />
            </div>
            <div class="review-content">{{ sellerReview.content }}</div>
          </div>
        </div>
      </template>

      <!-- 订单不存在 -->
      <el-empty v-else description="订单不存在" />

      <!-- 底部操作栏 -->
      <div class="action-bar" v-if="order && (showPayButton || showPickupButton || showConfirmButton || showReviewButton || showCancelButton)">
        <el-button
          v-if="showCancelButton"
          type="danger"
          plain
          @click="handleCancel"
        >
          取消订单
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
      </div>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading, Clock, CircleCheck, CircleClose, Timer, ChatDotRound } from '@element-plus/icons-vue'
import MobileLayout from '@/layouts/MobileLayout.vue'
import { getOrderById, payOrder, confirmPickup, confirmPayment, cancelOrder, Order, OrderStatus, getOrderStatusText, getOrderReviews, OrderReview } from '@/api/order'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const order = ref<Order | null>(null)
const loading = ref(true)
const buyerReview = ref({ rating: 0, content: '' })
const sellerReview = ref({ rating: 0, content: '' })

const pageTitle = computed(() => `订单详情`)

// 是否是买家
const isBuyer = computed(() => {
  return order.value?.buyerId === authStore.userInfo?.id
})

// 对方信息
const counterpartyId = computed(() => {
  if (!order.value) return 0
  return isBuyer.value ? order.value.sellerId : order.value.buyerId
})

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

    // 加载评价信息
    if (order.value.hasBuyerReview || order.value.hasSellerReview) {
      const reviewRes = await getOrderReviews(order.value.id)
      const reviews = reviewRes.data.data
      if (order.value.hasBuyerReview) {
        const bReview = reviews.find((r: OrderReview) => r.reviewType === 'BUYER_REVIEW')
        if (bReview) {
          buyerReview.value = { rating: bReview.rating, content: bReview.content }
        }
      }
      if (order.value.hasSellerReview) {
        const sReview = reviews.find((r: OrderReview) => r.reviewType === 'SELLER_REVIEW')
        if (sReview) {
          sellerReview.value = { rating: sReview.rating, content: sReview.content }
        }
      }
    }
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
    [OrderStatus.CANCELLED]: CircleClose,
    [OrderStatus.REFUNDED]: CircleClose
  }
  return iconMap[status] || Clock
}

// 获取状态背景色
const getStatusBgColor = (status: OrderStatus) => {
  const colorMap: Record<OrderStatus, string> = {
    [OrderStatus.PENDING_PAYMENT]: 'bg-warning',
    [OrderStatus.PENDING_PICKUP]: 'bg-primary',
    [OrderStatus.PENDING_CONFIRM]: 'bg-info',
    [OrderStatus.PENDING_REVIEW]: 'bg-success',
    [OrderStatus.COMPLETED]: 'bg-success',
    [OrderStatus.CANCELLED]: 'bg-danger',
    [OrderStatus.REFUNDED]: 'bg-info'
  }
  return colorMap[status] || 'bg-info'
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

// 格式化日期时间
const formatDateTime = (dateStr?: string) => {
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

// 联系对方
const contactCounterparty = () => {
  if (!counterpartyId.value) return
  router.push(`/user/chat/${counterpartyId.value}`)
}

onMounted(() => {
  fetchOrder()
})
</script>

<style scoped>
.order-detail-page {
  min-height: 100%;
  background: #F5F5F5;
  padding-bottom: 80px;
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
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 16px;
  color: #FFFFFF;
  margin-bottom: 12px;
}

.status-card.bg-warning {
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
}

.status-card.bg-primary {
  background: linear-gradient(135deg, #60A5FA 0%, #3B82F6 100%);
}

.status-card.bg-info {
  background: linear-gradient(135deg, #67E8F9 0%, #06B6D4 100%);
}

.status-card.bg-success {
  background: linear-gradient(135deg, #6EE7B7 0%, #10B981 100%);
}

.status-card.bg-danger {
  background: linear-gradient(135deg, #FCA5A5 0%, #EF4444 100%);
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
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 4px;
}

.status-desc {
  font-size: 12px;
  opacity: 0.9;
}

/* 卡片通用样式 */
.section-card {
  background: #FFFFFF;
  border-radius: 8px;
  margin-bottom: 12px;
  overflow: hidden;
}

.card-header {
  padding: 12px 16px;
  font-size: 15px;
  font-weight: 600;
  color: #1F2937;
  border-bottom: 1px solid #E5E5E5;
}

/* 商品信息 */
.product-content {
  display: flex;
  gap: 12px;
  padding: 16px;
}

.product-image {
  width: 80px;
  height: 80px;
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
  font-size: 14px;
  color: #1F2937;
  font-weight: 500;
}

.product-meta {
  font-size: 12px;
  color: #9CA3AF;
  display: flex;
  gap: 12px;
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
  text-align: right;
  max-width: 60%;
}

.info-row .value.cancel {
  color: #EF4444;
}

/* 时间线 */
.timeline {
  padding: 16px;
}

.timeline-item {
  font-size: 13px;
  color: #4B5563;
}

.timeline-item.cancel {
  color: #EF4444;
}

/* 联系按钮 */
.contact-action {
  padding: 8px 16px 12px;
  display: flex;
  justify-content: flex-end;
}

/* 评价 */
.review-item {
  padding: 12px 16px;
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
