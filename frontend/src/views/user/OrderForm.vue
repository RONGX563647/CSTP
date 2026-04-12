<template>
  <div class="order-form-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">确认订单</h1>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <template v-else-if="product">
      <!-- 商品信息 -->
      <div class="product-card">
        <div class="card-header">商品信息</div>
        <div class="product-content">
          <el-image
            :src="product.mainImage || '/placeholder.png'"
            fit="cover"
            class="product-image"
          />
          <div class="product-info">
            <div class="product-name">{{ product.name }}</div>
            <div class="product-meta">
              <span>单价：¥{{ product.price }}</span>
              <span>库存：{{ product.stock }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 订单表单 -->
      <div class="form-card">
        <div class="card-header">订单信息</div>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="order-form"
        >
          <el-form-item label="购买数量" prop="quantity">
            <el-input-number
              v-model="form.quantity"
              :min="1"
              :max="product.stock"
              :disabled="product.stock <= 0"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="面交地点" prop="meetLocation">
            <el-input
              v-model="form.meetLocation"
              placeholder="请输入约定的面交地点，如：图书馆门口"
              clearable
            />
          </el-form-item>

          <el-form-item label="约定时间" prop="meetTime">
            <el-date-picker
              v-model="form.meetTime"
              type="datetime"
              placeholder="请选择约定时间"
              :disabled-date="disabledDate"
              style="width: 100%"
            />
          </el-form-item>

          <el-form-item label="买家留言" prop="buyerRemark">
            <el-input
              v-model="form.buyerRemark"
              type="textarea"
              :rows="3"
              placeholder="给卖家留言（选填）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- 费用明细 -->
      <div class="summary-card">
        <div class="card-header">费用明细</div>
        <div class="summary-row">
          <span class="label">商品单价</span>
          <span class="value">¥{{ product.price }}</span>
        </div>
        <div class="summary-row">
          <span class="label">购买数量</span>
          <span class="value">x{{ form.quantity }}</span>
        </div>
        <div class="summary-row total">
          <span class="label">合计</span>
          <span class="value amount">¥{{ totalAmount }}</span>
        </div>
      </div>

      <!-- 提示信息 -->
      <div class="tips-card">
        <div class="tips-title">
          <el-icon><InfoFilled /></el-icon>
          交易提示
        </div>
        <ul class="tips-list">
          <li>校园二手交易仅限线下自提或面交</li>
          <li>请与卖家约定好面交地点和时间</li>
          <li>当面验货确认无误后再确认提货</li>
          <li>完成交易后请如实评价</li>
        </ul>
      </div>
    </template>

    <!-- 底部操作栏 -->
    <div class="action-bar">
      <el-button @click="goBack">取消</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        @click="handleSubmit"
      >
        提交订单
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Loading, InfoFilled } from '@element-plus/icons-vue'
import { getPublicProductById } from '@/api/product'
import { createOrder } from '@/api/order'
import type { Product } from '@/api/types'

const router = useRouter()
const route = useRoute()

const formRef = ref<FormInstance>()
const product = ref<Product | null>(null)
const loading = ref(true)
const submitting = ref(false)

const form = ref({
  quantity: 1,
  meetLocation: '',
  meetTime: '',
  buyerRemark: ''
})

const rules: FormRules = {
  quantity: [
    { required: true, message: '请输入购买数量', trigger: 'blur' },
    { type: 'number', min: 1, message: '数量至少为 1', trigger: 'blur' }
  ],
  meetLocation: [
    { required: true, message: '请输入面交地点', trigger: 'blur' }
  ],
  meetTime: [
    { required: true, message: '请选择约定时间', trigger: 'change' }
  ]
}

// 总金额
const totalAmount = computed(() => {
  if (!product.value) return '0'
  return (product.value.price * form.value.quantity).toFixed(2)
})

// 禁用过去的日期
const disabledDate = (date: Date) => {
  return date.getTime() < Date.now() - 24 * 60 * 60 * 1000
}

// 获取商品信息
const fetchProduct = async () => {
  loading.value = true
  try {
    const res = await getPublicProductById(Number(route.params.productId))
    product.value = res.data.data
    // 检查库存
    if (product.value.stock <= 0) {
      ElMessage.warning('商品已售罄')
    }
  } catch (error) {
    console.error('获取商品信息失败:', error)
    ElMessage.error('商品信息加载失败')
  } finally {
    loading.value = false
  }
}

// 提交订单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    if (!product.value) {
      ElMessage.error('商品信息加载失败')
      return
    }

    if (product.value.stock < form.value.quantity) {
      ElMessage.error('库存不足')
      return
    }

    submitting.value = true
    try {
      await createOrder({
        productId: product.value.id,
        quantity: form.value.quantity,
        meetLocation: form.value.meetLocation,
        meetTime: form.value.meetTime ? new Date(form.value.meetTime).toISOString() : '',
        buyerRemark: form.value.buyerRemark
      })
      ElMessage.success('订单创建成功')
      router.push('/user/orders/my/buyer')
    } catch (error) {
      console.error('创建订单失败:', error)
      ElMessage.error('创建订单失败')
    } finally {
      submitting.value = false
    }
  })
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchProduct()
})
</script>

<style scoped>
.order-form-page {
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
.product-card,
.form-card,
.summary-card,
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

/* 表单 */
.order-form {
  padding: 16px;
}

/* 费用明细 */
.summary-card {
  padding-bottom: 16px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 16px;
}

.summary-row.total {
  border-top: 1px solid #E5E5E5;
  margin: 0 16px;
  padding-top: 12px;
}

.summary-row .label {
  font-size: 14px;
  color: #6B7280;
}

.summary-row .value {
  font-size: 14px;
  color: #1F2937;
}

.summary-row.total .value.amount {
  font-size: 18px;
  color: #F59E0B;
  font-weight: 600;
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
