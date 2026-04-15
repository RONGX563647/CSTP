<template>
  <MobileLayout :title="pageTitle" :show-tab-bar="true">
    <div class="recommend-page">
      <!-- 标签切换 -->
      <div class="tab-nav">
        <div
          v-for="tab in tabs"
          :key="tab.value"
          class="tab-item"
          :class="{ active: currentTab === tab.value }"
          @click="switchTab(tab.value)"
        >
          <span>{{ tab.label }}</span>
          <div class="tab-indicator" v-if="currentTab === tab.value"></div>
        </div>
      </div>

      <!-- 推荐商品网格 -->
      <div class="product-grid" v-if="productList.length > 0">
        <div
          v-for="product in productList"
          :key="product.id"
          class="product-card"
          @click="goToDetail(product.id)"
        >
          <div class="product-image">
            <el-image
              :src="product.mainImage"
              fit="cover"
              :lazy="true"
            >
              <template #placeholder>
                <div class="image-placeholder">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
              <template #error>
                <div class="image-placeholder">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <!-- 推荐标签 -->
            <div class="product-badges">
              <span class="badge badge-featured" v-if="product.isFeatured">精选</span>
              <span class="badge badge-discount" v-if="product.discount && product.discount < 0.8">
                {{ Math.round((1 - product.discount) * 100) }}%OFF
              </span>
              <span class="badge badge-new" v-if="isNewProduct(product.createdAt)">新品</span>
            </div>
          </div>
          <div class="product-info">
            <h3 class="product-name">{{ product.name }}</h3>
            <div class="product-tags" v-if="product.tags && product.tags.length > 0">
              <span class="product-tag" v-for="tag in product.tags.slice(0, 2)" :key="tag">{{ tag }}</span>
            </div>
            <div class="product-bottom">
              <div class="price-row">
                <span class="price">¥{{ product.price }}</span>
                <span class="original-price" v-if="product.originalPrice > product.price">
                  ¥{{ product.originalPrice }}
                </span>
              </div>
              <span class="sales">{{ product.salesCount }}人想要</span>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-if="productList.length === 0 && !loading" description="暂无推荐商品" />

      <div v-if="loading" class="loading-more">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Picture, Loading } from '@element-plus/icons-vue'
import {
  getHomeRecommend,
  getPersonalizedRecommend,
  getNewProducts,
  getDiscountProducts
} from '@/api/product'
import { Product } from '@/api/types'
import MobileLayout from '@/layouts/MobileLayout.vue'

const router = useRouter()
const route = useRoute()

const tabs = [
  { label: '为你推荐', value: 'home' },
  { label: '猜你喜欢', value: 'personalized' },
  { label: '新品上架', value: 'new' },
  { label: '特价折扣', value: 'discount' },
]

const currentTab = ref('home')
const productList = ref<Product[]>([])
const loading = ref(false)

const pageTitle = computed(() => {
  const tab = tabs.find(t => t.value === currentTab.value)
  return tab ? tab.label : '推荐'
})

const switchTab = (tab: string) => {
  if (currentTab.value === tab) return
  currentTab.value = tab
  productList.value = []
  fetchRecommend()
}

const fetchRecommend = async () => {
  loading.value = true
  try {
    let res: any
    switch (currentTab.value) {
      case 'home':
        res = await getHomeRecommend(40)
        break
      case 'personalized':
        res = await getPersonalizedRecommend(40)
        break
      case 'new':
        res = await getNewProducts(40)
        break
      case 'discount':
        res = await getDiscountProducts(40)
        break
      default:
        res = await getHomeRecommend(40)
    }
    productList.value = res.data.data || []
  } catch (e) {
    console.error('获取推荐商品失败:', e)
  } finally {
    loading.value = false
  }
}

const goToDetail = (id: number) => {
  router.push(`/user/products/${id}`)
}

const isNewProduct = (createdAt: string) => {
  if (!createdAt) return false
  const created = new Date(createdAt)
  const now = new Date()
  const diffDays = (now.getTime() - created.getTime()) / (1000 * 60 * 60 * 24)
  return diffDays <= 7
}

onMounted(() => {
  // 支持从路由参数指定初始 tab
  const tab = route.query.tab as string
  if (tab && tabs.some(t => t.value === tab)) {
    currentTab.value = tab
  }
  fetchRecommend()
})
</script>

<style scoped>
.recommend-page {
  min-height: calc(100vh - var(--header-height));
  background: var(--bg-color);
  padding-bottom: 20px;
}

/* 标签导航 */
.tab-nav {
  display: flex;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-light);
  position: sticky;
  top: 0;
  z-index: 100;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 0 8px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 14px;
  transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
}

.tab-item.active {
  color: var(--primary-color);
  font-weight: 600;
}

.tab-indicator {
  width: 24px;
  height: 3px;
  background: var(--primary-color);
  border-radius: 2px;
  margin-top: 6px;
}

/* 商品网格 */
.product-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  padding: 8px;
}

.product-card {
  background: var(--bg-card);
  border-radius: var(--radius);
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s;
  -webkit-tap-highlight-color: transparent;
}

.product-card:active {
  transform: scale(0.98);
}

.product-image {
  position: relative;
  aspect-ratio: 1;
  background: var(--bg-color);
}

.product-image .el-image {
  width: 100%;
  height: 100%;
}

.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-light);
  font-size: 28px;
}

/* 商品徽标 */
.product-badges {
  position: absolute;
  top: 6px;
  left: 6px;
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.badge {
  display: inline-block;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
  line-height: 1.4;
}

.badge-featured {
  background: linear-gradient(135deg, #F59E0B, #D97706);
  color: #FFFFFF;
}

.badge-discount {
  background: linear-gradient(135deg, #EF4444, #DC2626);
  color: #FFFFFF;
}

.badge-new {
  background: linear-gradient(135deg, #3B82F6, #2563EB);
  color: #FFFFFF;
}

.product-info {
  padding: 8px 10px 10px;
}

.product-name {
  font-size: 13px;
  font-weight: 400;
  color: var(--text-primary);
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
}

.product-tags {
  display: flex;
  gap: 4px;
  margin-bottom: 6px;
}

.product-tag {
  display: inline-block;
  padding: 1px 5px;
  background: #FEF3C7;
  color: #92400E;
  border-radius: 3px;
  font-size: 10px;
  line-height: 1.4;
}

.product-bottom {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.price {
  font-size: 15px;
  font-weight: 600;
  color: var(--primary-color);
}

.original-price {
  font-size: 11px;
  color: var(--text-placeholder);
  text-decoration: line-through;
}

.sales {
  font-size: 11px;
  color: var(--text-placeholder);
}

/* 加载状态 */
.loading-more {
  text-align: center;
  padding: 16px;
  color: var(--text-placeholder);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
</style>
