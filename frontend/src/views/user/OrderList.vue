<template>
  <MobileLayout title="我的订单" :show-tab-bar="true">
    <div class="order-list-page">
      <!-- 切换标签 -->
      <div class="tab-container">
        <el-radio-group v-model="currentTab" @change="handleTabChange" size="small">
          <el-radio-button label="buyer">我买的</el-radio-button>
          <el-radio-button label="seller">我卖的</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 筛选条件 -->
      <div class="filter-container">
        <el-select v-model="statusFilter" placeholder="订单状态" clearable @change="fetchOrders" size="small">
          <el-option label="全部状态" value="" />
          <el-option label="待付款" :value="OrderStatus.PENDING_PAYMENT" />
          <el-option label="待提货" :value="OrderStatus.PENDING_PICKUP" />
          <el-option label="待确认" :value="OrderStatus.PENDING_CONFIRM" />
          <el-option label="待评价" :value="OrderStatus.PENDING_REVIEW" />
          <el-option label="已完成" :value="OrderStatus.COMPLETED" />
          <el-option label="已取消" :value="OrderStatus.CANCELLED" />
        </el-select>
      </div>

      <!-- 订单列表 -->
      <div class="order-list">
        <!-- 加载中 -->
        <div v-if="loading" class="loading-container">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载中...</span>
        </div>

        <!-- 空列表 -->
        <el-empty v-else-if="orders.length === 0" description="暂无订单" />

        <!-- 订单卡片 -->
        <div v-else>
          <div
            v-for="order in orders"
            :key="order.id"
            class="order-card"
          >
            <!-- 订单头部 -->
            <div class="order-header">
              <span class="order-no">订单号：{{ order.orderNo }}</span>
              <el-tag :type="getOrderStatusColor(order.status)" size="small">
                {{ getOrderStatusText(order.status) }}
              </el-tag>
            </div>

            <!-- 商品信息 -->
            <div class="product-info" @click="goToDetail(order.id)">
              <div class="product-image">
                <el-image
                  :src="order.productImage || '/placeholder.png'"
                  fit="cover"
                  class="thumb"
                />
              </div>
              <div class="product-detail">
                <div class="product-name">{{ order.productName }}</div>
                <div class="product-meta">
                  <span>单价：¥{{ order.price }}</span>
                  <span>数量：x{{ order.quantity }}</span>
                </div>
              </div>
            </div>

            <!-- 订单金额 -->
            <div class="order-total">
              <span class="label">合计：</span>
              <span class="amount">¥{{ order.totalAmount }}</span>
            </div>

            <!-- 操作按钮 -->
            <div class="order-actions">
              <el-button
                v-if="showCancelButton(order.status)"
                type="danger"
                size="small"
                plain
                @click="handleCancel(order)"
              >
                取消订单
              </el-button>
              <el-button
                v-if="showPayButton(order.status)"
                type="primary"
                size="small"
                @click="handlePay(order)"
              >
                确认付款
              </el-button>
              <el-button
                v-if="showPickupButton(order.status)"
                type="success"
                size="small"
                @click="handlePickup(order)"
              >
                确认提货
              </el-button>
              <el-button
                v-if="showConfirmButton(order.status)"
                type="success"
                size="small"
                @click="handleConfirm(order)"
              >
                确认收款
              </el-button>
              <el-button
                v-if="showReviewButton(order)"
                type="primary"
                size="small"
                @click="handleReview(order)"
              >
                评价
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="totalPages > 1" class="pagination-container">
        <el-pagination
          layout="prev, pager, next"
          :total="totalElements"
          :page-size="size"
          :current-page="page + 1"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import MobileLayout from '@/layouts/MobileLayout.vue'
import {
  getMyBuyerOrders,
  getMySellerOrders,
  payOrder,
  confirmPickup,
  confirmPayment,
  cancelOrder,
  Order,
  OrderStatus,
  getOrderStatusText,
  getOrderStatusColor
} from '@/api/order'

const router = useRouter()

const currentTab = ref<'buyer' | 'seller'>('buyer')
const isBuyerTab = computed(() => currentTab.value === 'buyer')

const orders = ref<Order[]>([])
const loading = ref(true)
const page = ref(0)
const size = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)
const statusFilter = ref<OrderStatus | ''>('')

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      size: size.value
    }
    if (statusFilter.value) {
      params.status = statusFilter.value
    }

    const api = isBuyerTab.value ? getMyBuyerOrders : getMySellerOrders
    const res = await api(params)
    const data = res.data.data
    orders.value = data.content
    totalElements.value = data.totalElements
    totalPages.value = data.totalPages
  } catch (error) {
    console.error('获取订单列表失败:', error)
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

// 切换标签
const handleTabChange = () => {
  page.value = 0
  fetchOrders()
}

// 分页
const handlePageChange = (newPage: number) => {
  page.value = newPage - 1
  fetchOrders()
}

// 显示按钮逻辑
const showPayButton = (status: OrderStatus) => {
  return isBuyerTab.value && status === OrderStatus.PENDING_PAYMENT
}

const showPickupButton = (status: OrderStatus) => {
  return isBuyerTab.value && status === OrderStatus.PENDING_PICKUP
}

const showConfirmButton = (status: OrderStatus) => {
  return !isBuyerTab.value && status === OrderStatus.PENDING_CONFIRM
}

const showReviewButton = (order: Order) => {
  return isBuyerTab.value &&
         order.status === OrderStatus.PENDING_REVIEW &&
         !order.hasBuyerReview
}

const showCancelButton = (status: OrderStatus) => {
  return status === OrderStatus.PENDING_PAYMENT ||
         status === OrderStatus.PENDING_PICKUP
}

// 操作处理
const handlePay = async (order: Order) => {
  try {
    await ElMessageBox.confirm('确认已付款吗？', '付款确认', {
      type: 'info'
    })
    await payOrder(order.id)
    ElMessage.success('付款成功')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('付款失败:', error)
      ElMessage.error('付款失败')
    }
  }
}

const handlePickup = async (order: Order) => {
  try {
    await ElMessageBox.confirm('确认已提货吗？', '提货确认', {
      type: 'info'
    })
    await confirmPickup(order.id)
    ElMessage.success('提货确认成功')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提货确认失败:', error)
      ElMessage.error('提货确认失败')
    }
  }
}

const handleConfirm = async (order: Order) => {
  try {
    await ElMessageBox.confirm('确认已收款吗？', '收款确认', {
      type: 'info'
    })
    await confirmPayment(order.id)
    ElMessage.success('收款确认成功')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('收款确认失败:', error)
      ElMessage.error('收款确认失败')
    }
  }
}

const handleReview = (order: Order) => {
  router.push(`/user/orders/${order.id}/review`)
}

const handleCancel = async (order: Order) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', {
      type: 'warning',
      inputPattern: /.+/,
      inputErrorMessage: '请输入取消原因'
    })
    await cancelOrder(order.id, value)
    ElMessage.success('订单取消成功')
    fetchOrders()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
      ElMessage.error('取消订单失败')
    }
  }
}

// 跳转详情
const goToDetail = (orderId: number) => {
  router.push(`/user/orders/${orderId}`)
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.order-list-page {
  min-height: 100%;
  background: var(--bg-color);
  padding-bottom: 20px;
}

/* 切换标签 */
.tab-container {
  background: var(--bg-card);
  padding: 12px 16px;
  margin-bottom: 8px;
}

/* 筛选容器 */
.filter-container {
  background: var(--bg-card);
  padding: 8px 16px;
  margin-bottom: 8px;
}

/* 订单列表 */
.order-list {
  padding: 0 12px;
}

.loading-container {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  gap: 12px;
  color: var(--text-placeholder);
}

/* 订单卡片 */
.order-card {
  background: var(--bg-card);
  border-radius: var(--radius);
  padding: 14px;
  margin-bottom: 8px;
}

/* 订单头部 */
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-light);
}

.order-no {
  font-size: 12px;
  color: var(--text-placeholder);
}

/* 商品信息 */
.product-info {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;
  cursor: pointer;
}

.product-image .thumb {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-sm);
  object-fit: cover;
}

.product-detail {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.product-name {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-meta {
  font-size: 12px;
  color: var(--text-placeholder);
  display: flex;
  gap: 12px;
}

/* 订单金额 */
.order-total {
  display: flex;
  justify-content: flex-end;
  align-items: baseline;
  padding: 8px 0;
  border-top: 1px solid var(--border-light);
}

.order-total .label {
  font-size: 13px;
  color: var(--text-secondary);
}

.order-total .amount {
  font-size: 17px;
  color: var(--primary-color);
  font-weight: 600;
}

/* 操作按钮 */
.order-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 10px;
}

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: center;
  padding: 20px 16px;
  background: var(--bg-card);
  margin-top: 8px;
  border-radius: var(--radius);
}
</style>
