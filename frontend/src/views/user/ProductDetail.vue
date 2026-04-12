<template>
  <div class="product-detail-page">
    <!-- 加载中 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <template v-else-if="product">
      <!-- 商品图片轮播 -->
      <div class="image-gallery">
        <el-carousel :interval="4000" height="300px" v-if="product.images && product.images.length > 0">
          <el-carousel-item v-for="(img, index) in product.images" :key="index">
            <el-image :src="img" fit="contain" class="carousel-image" />
          </el-carousel-item>
        </el-carousel>
        <el-image v-else :src="product.mainImage" fit="cover" class="main-image" />
      </div>

      <!-- 商品信息 -->
      <div class="product-info">
        <h1 class="product-title">{{ product.name }}</h1>
        <div class="product-status">
          <el-tag :type="getStatusColor(product.status)">
            {{ getStatusText(product.status) }}
          </el-tag>
          <el-tag type="warning" v-if="product.isFeatured" effect="dark">推荐</el-tag>
        </div>

        <!-- 价格 -->
        <div class="price-section">
          <div class="current-price">
            <span class="currency">¥</span>
            <span class="amount">{{ product.price }}</span>
          </div>
          <div class="original-price" v-if="product.originalPrice > product.price">
            ¥{{ product.originalPrice }}
          </div>
          <div class="discount-tag" v-if="product.discount">
            {{ (product.discount * 10).toFixed(1) }}折
          </div>
        </div>

        <!-- 商品描述 -->
        <div class="description-section">
          <h3 class="section-title">商品描述</h3>
          <p class="description">{{ product.description || '暂无描述' }}</p>
        </div>

        <!-- 商品信息网格 -->
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">分类</span>
            <span class="info-value">{{ product.category }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">库存</span>
            <span class="info-value" :class="{ 'out-of-stock': product.stock <= 0 }">
              {{ product.stock > 0 ? product.stock : '缺货' }}
            </span>
          </div>
          <div class="info-item">
            <span class="info-label">销量</span>
            <span class="info-value">{{ product.salesCount }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">浏览</span>
            <span class="info-value">{{ product.viewCount }}</span>
          </div>
        </div>

        <!-- 标签 -->
        <div class="tags-section" v-if="product.tags && product.tags.length > 0">
          <span class="section-label">标签：</span>
          <el-tag
            v-for="(tag, index) in product.tags"
            :key="index"
            size="small"
            class="product-tag"
          >
            {{ tag }}
          </el-tag>
        </div>

        <!-- 卖家信息 -->
        <div class="seller-section">
          <span class="section-label">卖家：</span>
          <span class="seller-name">{{ product.sellerName || `用户${product.sellerId}` }}</span>
        </div>

        <!-- 更新时间 -->
        <div class="update-time">
          更新于：{{ formatDate(product.updatedAt) }}
        </div>
      </div>

      <!-- 底部操作栏 -->
      <div class="action-bar">
        <el-button @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-button
          v-if="isOwnProduct"
          type="primary"
          @click="goToEdit"
        >
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-button
          v-if="product.stock > 0 && product.status === 'ON_SALE'"
          type="success"
          size="large"
          @click="handleBuy"
        >
          立即购买
        </el-button>
        <el-button
          v-else
          type="info"
          size="large"
          disabled
        >
          暂时缺货
        </el-button>
      </div>
    </template>

    <!-- 商品不存在 -->
    <el-empty v-else description="商品不存在或已下架" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading, ArrowLeft, Edit } from '@element-plus/icons-vue'
import { getPublicProductById } from '@/api/product'
import { Product, ProductStatus, ProductStatusText, ProductStatusColor } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const product = ref<Product | null>(null)
const loading = ref(true)

// 是否是自己的商品
const isOwnProduct = computed(() => {
  if (!product.value) return false
  // 简单判断：如果用户已登录且用户 ID 匹配
  return authStore.userInfo?.id === product.value.sellerId
})

// 获取商品详情
const fetchProduct = async () => {
  loading.value = true
  try {
    const res = await getPublicProductById(Number(route.params.id))
    product.value = res.data.data
  } catch (error) {
    console.error('获取商品详情失败:', error)
    ElMessage.error('商品不存在或已下架')
  } finally {
    loading.value = false
  }
}

// 获取状态文本
const getStatusText = (status: ProductStatus) => {
  return ProductStatusText[status] || ''
}

// 获取状态颜色
const getStatusColor = (status: ProductStatus) => {
  return ProductStatusColor[status] || 'info'
}

// 格式化日期
const formatDate = (dateStr: string) => {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

// 返回
const goBack = () => {
  router.back()
}

// 去编辑
const goToEdit = () => {
  router.push(`/user/products/${product.value!.id}/edit`)
}

// 购买
const handleBuy = () => {
  ElMessage.info('购买功能开发中...')
}

onMounted(() => {
  fetchProduct()
})
</script>

<style scoped>
.product-detail-page {
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

/* 图片轮播 */
.image-gallery {
  background: #FFFFFF;
  margin-bottom: 12px;
}

.carousel-image,
.main-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

/* 商品信息 */
.product-info {
  background: #FFFFFF;
  padding: 16px;
  margin-bottom: 12px;
}

.product-title {
  font-size: 20px;
  font-weight: 600;
  color: #1F2937;
  margin: 0 0 12px 0;
}

.product-status {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

/* 价格 */
.price-section {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #E5E5E5;
  margin-bottom: 16px;
}

.current-price {
  display: flex;
  align-items: baseline;
  color: #F59E0B;
}

.current-price .currency {
  font-size: 16px;
  font-weight: 600;
}

.current-price .amount {
  font-size: 28px;
  font-weight: 700;
}

.original-price {
  font-size: 14px;
  color: #9CA3AF;
  text-decoration: line-through;
}

.discount-tag {
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
  color: #FFFFFF;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

/* 描述 */
.description-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1F2937;
  margin: 0 0 8px 0;
}

.description {
  font-size: 14px;
  color: #4B5563;
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 12px;
  background: #F9FAFB;
  border-radius: 8px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #9CA3AF;
}

.info-value {
  font-size: 14px;
  color: #1F2937;
  font-weight: 500;
}

.info-value.out-of-stock {
  color: #EF4444;
}

/* 标签 */
.tags-section {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.section-label {
  font-size: 14px;
  color: #6B7280;
  white-space: nowrap;
}

.product-tag {
  background: #F3F4F6;
  color: #4B5563;
}

/* 卖家 */
.seller-section {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #E5E5E5;
}

.seller-name {
  font-size: 14px;
  color: #4B5563;
}

.update-time {
  font-size: 12px;
  color: #9CA3AF;
  text-align: right;
  margin-top: 16px;
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
