<template>
  <MobileLayout :show-header="false" :show-tab-bar="true">
    <div class="home-page">
      <!-- 顶部搜索区 -->
      <div class="search-header">
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索你想要的宝贝"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
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

      <!-- 商品列表 -->
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
import { searchPublicProducts } from '@/api/product'
import { Product } from '@/api/types'
import MobileLayout from '@/layouts/MobileLayout.vue'

const router = useRouter()

const categories = [
  { label: '全部', value: '' },
  { label: '手机数码', value: '手机数码' },
  { label: '电脑办公', value: '电脑办公' },
  { label: '家用电器', value: '家用电器' },
  { label: '娱乐玩具', value: '娱乐玩具' },
]

const searchKeyword = ref('')
const currentCategory = ref('')
const productList = ref<Product[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(10)
const noMore = ref(false)

const fetchProducts = async (reset = false) => {
  if (loading.value || (noMore.value && !reset)) return

  loading.value = true

  try {
    const params: any = {
      name: searchKeyword.value,
      category: currentCategory.value,
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

const handleSearch = () => {
  fetchProducts(true)
}

const selectCategory = (category: string) => {
  currentCategory.value = category
  fetchProducts(true)
}

const goToDetail = (id: number) => {
  router.push(`/user/products/${id}`)
}

onMounted(() => {
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

.search-box :deep(.el-input__wrapper) {
  background: var(--bg-color);
  border-radius: 20px;
  box-shadow: none;
  padding: 0 16px;
  height: 36px;
}

.search-box :deep(.el-input__inner) {
  font-size: 13px;
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
