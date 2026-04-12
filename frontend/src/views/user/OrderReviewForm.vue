<template>
  <div class="order-review-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">订单评价</h1>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <template v-else-if="order">
      <!-- 订单信息 -->
      <div class="order-card">
        <div class="card-header">订单信息</div>
        <div class="order-content">
          <div class="order-row">
            <span class="label">订单号</span>
            <span class="value">{{ order.orderNo }}</span>
          </div>
          <div class="order-row">
            <span class="label">商品</span>
            <span class="value">{{ order.productName }}</span>
          </div>
          <div class="order-row">
            <span class="label">金额</span>
            <span class="value amount">¥{{ order.totalAmount }}</span>
          </div>
        </div>
      </div>

      <!-- 评价表单 -->
      <div class="review-card">
        <div class="card-header">
          <span>评价商品</span>
          <el-tag size="small">买家评价</el-tag>
        </div>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="review-form"
        >
          <el-form-item label="评分" prop="rating">
            <el-rate v-model="form.rating" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" />
          </el-form-item>

          <el-form-item label="评价内容" prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="4"
              placeholder="请如实评价商品，分享您的购物体验"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- 评价提示 -->
      <div class="tips-card">
        <div class="tips-title">
          <el-icon><InfoFilled /></el-icon>
          评价提示
        </div>
        <ul class="tips-list">
          <li>请如实评价商品质量和卖家服务</li>
          <li>评价内容将帮助其他买家做出决策</li>
          <li>评价完成后订单将标记为已完成</li>
        </ul>
      </div>
    </template>

    <!-- 底部操作栏 -->
    <div class="action-bar">
      <el-button @click="goBack">返回</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        @click="handleSubmit"
      >
        提交评价
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Loading, InfoFilled } from '@element-plus/icons-vue'
import { getOrderById, createReview, Order, ReviewType } from '@/api/order'

const router = useRouter()
const route = useRoute()

const formRef = ref<FormInstance>()
const order = ref<Order | null>(null)
const loading = ref(true)
const submitting = ref(false)

const form = ref({
  rating: 5,
  content: ''
})

const rules: FormRules = {
  rating: [
    { required: true, message: '请选择评分', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入评价内容', trigger: 'blur' },
    { min: 10, message: '评价内容至少 10 个字', trigger: 'blur' }
  ]
}

// 获取订单信息
const fetchOrder = async () => {
  loading.value = true
  try {
    const res = await getOrderById(Number(route.params.id))
    order.value = res.data.data
  } catch (error) {
    console.error('获取订单信息失败:', error)
    ElMessage.error('订单信息加载失败')
  } finally {
    loading.value = false
  }
}

// 提交评价
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      await createReview(
        Number(route.params.id),
        {
          rating: form.value.rating,
          content: form.value.content
        },
        ReviewType.BUYER_REVIEW
      )
      ElMessage.success('评价成功')
      router.push('/user/orders/my/buyer')
    } catch (error) {
      console.error('提交评价失败:', error)
      ElMessage.error('提交评价失败')
    } finally {
      submitting.value = false
    }
  })
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchOrder()
})
</script>

<style scoped>
.order-review-page {
  min-height: 100vh;
  background: #F5F5F5;
  padding-bottom: 70px;
}

/* 页面标题 */
.page-header {
  background: #FFFFFF;
  padding: 16px;
  margin-bottom: 12px;
}

.page-title {
  font-size: 20px;
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

/* 卡片通用样式 */
.order-card,
.review-card,
.tips-card {
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
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 订单信息 */
.order-content {
  padding: 16px;
}

.order-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #F3F4F6;
}

.order-row:last-child {
  border-bottom: none;
}

.order-row .label {
  font-size: 13px;
  color: #6B7280;
}

.order-row .value {
  font-size: 14px;
  color: #1F2937;
}

.order-row .value.amount {
  color: #F59E0B;
  font-weight: 600;
}

/* 评价表单 */
.review-form {
  padding: 16px;
}

/* 提示信息 */
.tips-card {
  padding: 16px;
}

.tips-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1F2937;
  margin-bottom: 12px;
}

.tips-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tips-list li {
  font-size: 13px;
  color: #6B7280;
  line-height: 1.8;
  padding-left: 20px;
  position: relative;
}

.tips-list li::before {
  content: '•';
  position: absolute;
  left: 8px;
  color: #F59E0B;
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
