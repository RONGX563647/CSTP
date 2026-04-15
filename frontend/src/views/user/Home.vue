<template>
  <MobileLayout :show-header="false" :show-tab-bar="true">
    <div class="home-page">
      <!-- 顶部搜索区 -->
      <div class="search-header">
        <div class="search-box" @click="goToSearch">
          <div class="search-box-inner">
            <el-icon class="search-icon"><Search /></el-icon>
            <span class="search-placeholder">搜索你想要的宝贝</span>
          </div>
        </div>
      </div>

      <!-- 精选推荐横滑 -->
      <div class="featured-section" v-if="featuredList.length > 0">
        <div class="section-title">
          <span class="title-text">精选推荐</span>
          <span class="title-more" @click="goToRecommend">更多 ›</span>
        </div>
        <div class="featured-scroll">
          <div
            v-for="product in featuredList"
            :key="product.id"
            class="featured-card"
            @click="goToDetail(product.id)"
          >
            <div class="featured-image">
              <el-image :src="product.mainImage" fit="cover" :lazy="true">
                <template #error>
                  <div class="image-placeholder-sm">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
            </div>
            <div class="featured-info">
              <span class="featured-name">{{ product.name }}</span>
              <span class="featured-price">¥{{ product.price }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 分类横滑 -->
      <div class="category-nav">
        <div
          v-for="cat in categories"
          :key="cat.value"
          class="category-item"
          :class="{ active: currentCategory === cat.value }"
          @click="selectCategory(cat.value)"
        >
          <span>{{ cat.label }}</span>
        </div>
      </div>

      <!-- 推荐商品列表 -->
      <div class="section-header" v-if="currentCategory === ''">
        <span class="section-label">为你推荐</span>
        <span class="section-action" @click="goToRecommend">查看全部 ›</span>
      </div>

      <div class="product-grid">
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
            <div class="product-badges">
              <span class="badge badge-featured" v-if="product.isFeatured">精选</span>
              <span class="badge badge-discount" v-if="product.discount && product.discount < 0.8">
                {{ Math.round((1 - product.discount) * 100) }}%OFF
              </span>
            </div>
          </div>
          <div class="product-info">
            <h3 class="product-name">{{ product.name }}</h3>
            <div class="product-bottom">
              <span class="price">¥{{ product.price }}</span>
              <span class="seller">{{ product.sellerName || '匿名' }}</span>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-if="productList.length === 0 && !loading" description="暂无商品" />

      <div v-if="loading" class="loading-more">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-if="noMore && productList.length > 0" class="no-more">
        <span>没有更多了</span>
      </div>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Picture, Loading } from '@element-plus/icons-vue'
import { searchPublicProducts, getHomeRecommend, getPublicFeaturedProducts } from '@/api/product'
import { Product } from '@/api/types'
import MobileLayout from '@/layouts/MobileLayout.vue'

const router = useRouter()

const categories = [
  { label: '推荐', value: '' },
  { label: '手机数码', value: '手机数码' },
  { label: '电脑办公', value: '电脑办公' },
  { label: '家用电器', value: '家用电器' },
  { label: '娱乐玩具', value: '娱乐玩具' },
]

const currentCategory = ref('')
const productList = ref<Product[]>([])
const featuredList = ref<Product[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(10)
const noMore = ref(false)

const goToSearch = () => {
  router.push('/user/search')
}

const goToRecommend = () => {
  router.push('/user/recommend')
}

const goToDetail = (id: number) => {
  router.push(`/user/products/${id}`)
}

/**
 * 加载精选推荐（横滑区）
 */
const loadFeatured = async () => {
  try {
    const res = await getPublicFeaturedProducts()
    featuredList.value = res.data.data || []
    // 精选不够时用首页推荐补充
    if (featuredList.value.length < 5) {
      const recommendRes = await getHomeRecommend(10)
      const recommendProducts = (recommendRes.data.data || []).filter(
        p => !featuredList.value.some(f => f.id === p.id)
      )
      featuredList.value = [...featuredList.value, ...recommendProducts].slice(0, 10)
    }
  } catch (e) {
    console.error('加载精选推荐失败:', e)
  }
}

/**
 * 加载商品列表
 * - 推荐分类：使用推荐API
 * - 其他分类：使用搜索API
 */
const fetchProducts = async (reset = false) => {
  if (loading.value || (noMore.value && !reset)) return

  loading.value = true

  try {
    if (currentCategory.value === '' && reset) {
      // 推荐分类：使用首页推荐
      const res = await getHomeRecommend(20)
      productList.value = res.data.data || []
      noMore.value = true // 推荐列表不分页
      loading.value = false
      return
    }

    // 其他分类：使用搜索API
    const params: any = {
      category: currentCategory.value || undefined,
      page: reset ? 0 : page.value,
      size: size.value,
      sortBy: 'createdAt',
      sortDir: 'desc'
    }

    const res = await searchPublicProducts(params)
    const { content, number, totalPages } = res.data.data

    if (reset) {
      productList.value = content
      page.value = 1
    } else {
      productList.value = [...productList.value, ...content]
      page.value = number + 1
    }

    noMore.value = page.value >= totalPages
  } catch (error) {
    console.error('获取商品列表失败:', error)
  } finally {
    loading.value = false
  }
}

const selectCategory = (category: string) => {
  if (currentCategory.value === category) return
  currentCategory.value = category
  page.value = 0
  noMore.value = false
  fetchProducts(true)
}

onMounted(async () => {
  await loadFeatured()
  fetchProducts(true)
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: var(--bg-color);
}

/* 搜索栏 */
.search-header {
  background: var(--bg-card);
  padding: 10px 16px 8px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.search-box {
  cursor: pointer;
}

.search-box-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--bg-color);
  border-radius: 20px;
  height: 36px;
  color: var(--text-placeholder);
  font-size: 13px;
  transition: background 0.2s;
}

.search-box:hover .search-box-inner {
  background: #E5E7EB;
}

.search-icon {
  font-size: 14px;
}

.search-placeholder {
  flex: 1;
}

/* 精选推荐横滑 */
.featured-section {
  background: var(--bg-card);
  padding: 12px 0 12px 16px;
  margin-bottom: 8px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-right: 16px;
  margin-bottom: 10px;
}

.title-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.title-more {
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
}

.featured-scroll {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-right: 16px;
  -webkit-overflow-scrolling: touch;
}

.featured-scroll::-webkit-scrollbar {
  display: none;
}

.featured-card {
  flex-shrink: 0;
  width: 120px;
  cursor: pointer;
  transition: transform 0.2s;
  -webkit-tap-highlight-color: transparent;
}

.featured-card:active {
  transform: scale(0.96);
}

.featured-image {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--bg-color);
}

.featured-image .el-image {
  width: 100%;
  height: 100%;
}

.image-placeholder-sm {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-light);
  font-size: 24px;
}

.featured-info {
  padding: 6px 2px 0;
}

.featured-name {
  display: block;
  font-size: 12px;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.featured-price {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--primary-color);
  margin-top: 2px;
}

/* 分类导航 */
.category-nav {
  display: flex;
  gap: 8px;
  padding: 8px 16px 10px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-light);
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.category-nav::-webkit-scrollbar {
  display: none;
}

.category-item {
  flex-shrink: 0;
  padding: 5px 14px;
  border-radius: 14px;
  background: var(--bg-color);
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
}

.category-item.active {
  background: var(--primary-color);
  color: #FFFFFF;
  font-weight: 500;
}

/* 区域标题 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px 4px;
}

.section-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.section-action {
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
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

.product-info {
  padding: 8px 10px 10px;
}

.product-name {
  font-size: 13px;
  font-weight: 400;
  color: var(--text-primary);
  margin: 0 0 6px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
}

.product-bottom {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.price {
  font-size: 15px;
  font-weight: 600;
  color: var(--primary-color);
}

.seller {
  font-size: 11px;
  color: var(--text-placeholder);
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 加载状态 */
.loading-more,
.no-more {
  text-align: center;
  padding: 16px;
  color: var(--text-placeholder);
  font-size: 12px;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
</style>
