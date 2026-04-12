<template>
  <MobileLayout :title="page_title" :show-tab-bar="!isAdminMode">
    <div class="product-list-page">
      <!-- 返回按钮（仅管理端显示） -->
      <div class="back-bar" v-if="isAdminMode">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回商品管理
        </el-button>
      </div>

      <!-- 搜索头部 -->
      <div class="search-header">
      <div class="search-box">
        <el-input
          v-model="searchForm.name"
          placeholder="搜索商品"
          clearable
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <template #append>
            <el-button @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <div class="filter-item">
        <span class="filter-label">分类：</span>
        <el-select v-model="searchForm.category" placeholder="全部分类" clearable size="default" @change="handleSearch">
          <el-option label="手机数码" value="手机数码" />
          <el-option label="电脑办公" value="电脑办公" />
          <el-option label="家用电器" value="家用电器" />
          <el-option label="娱乐玩具" value="娱乐玩具" />
        </el-select>
      </div>
      <div class="filter-item">
        <span class="filter-label">价格：</span>
        <el-input-number v-model="searchForm.minPrice" :min="0" placeholder="最低价" size="default" @change="handleSearch" />
        <span class="price-separator">-</span>
        <el-input-number v-model="searchForm.maxPrice" :min="0" placeholder="最高价" size="default" @change="handleSearch" />
      </div>
      <div class="filter-item">
        <span class="filter-label">排序：</span>
        <el-select v-model="sortBy" size="default" @change="handleSearch">
          <el-option label="最新发布" value="createdAt,desc" />
          <el-option label="价格从低到高" value="price,asc" />
          <el-option label="价格从高到低" value="price,desc" />
        </el-select>
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
              <div class="image-loading">
                <el-icon><Loading /></el-icon>
              </div>
            </template>
          </el-image>
          <div class="product-status-tag" v-if="product.status">
            <el-tag :type="getStatusColor(product.status)" size="small">
              {{ getStatusText(product.status) }}
            </el-tag>
          </div>
        </div>
        <div class="product-info">
          <h3 class="product-name">{{ product.name }}</h3>
          <p class="product-desc">{{ product.description }}</p>
          <div class="product-price-row">
            <span class="current-price">¥{{ product.price }}</span>
            <span class="original-price" v-if="product.originalPrice > product.price">
              ¥{{ product.originalPrice }}
            </span>
          </div>
          <div class="product-meta">
            <span class="product-stock" v-if="product.stock > 0">库存：{{ product.stock }}</span>
            <span class="product-stock out-of-stock" v-else>缺货</span>
            <span class="product-sales">销量：{{ product.salesCount }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="productList.length === 0 && !loading" description="暂无商品" />

    <!-- 加载中 -->
    <div v-if="loading" class="loading-more">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <!-- 无更多数据 -->
    <div v-if="noMore" class="no-more">
      <span>没有更多了</span>
    </div>
  </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, Loading, ArrowLeft } from '@element-plus/icons-vue'
import { searchPublicProducts } from '@/api/product'
import { Product, ProductStatus, ProductStatusText, ProductStatusColor } from '@/api/types'
import MobileLayout from '@/layouts/MobileLayout.vue'

const router = useRouter()
const route = useRoute()

// 判断是否为管理端查看用户商品模式
const isAdminMode = computed(() => route.path.startsWith('/admin/products/user'))
// 从路由参数获取 sellerId
const sellerIdFromRoute = computed(() => {
  const id = route.params.userId
  return id ? Number(id) : undefined
})

// 页面标题
const page_title = computed(() => {
  if (isAdminMode) {
    return '用户商品列表'
  }
  return '商品市场'
})

// 搜索表单
const searchForm = reactive({
  name: '',
  category: '',
  minPrice: undefined as number | undefined,
  maxPrice: undefined as number | undefined
})

const sortBy = ref('createdAt,desc')
const productList = ref<Product[]>([])
const loading = ref(false)
const page = ref(0)
const size = ref(10)
const noMore = ref(false)

// 获取商品列表
const fetchProducts = async (reset = false) => {
  if (loading.value || (noMore.value && !reset)) return

  loading.value = true

  try {
    const [sortField, sortDir] = sortBy.value.split(',')
    const params: any = {
      ...searchForm,
      page: reset ? 0 : page.value,
      size: size.value,
      sortBy: sortField,
      sortDir: sortDir as 'asc' | 'desc'
    }

    // 如果是管理端查看用户商品模式，添加 sellerId 参数
    if (isAdminMode.value && sellerIdFromRoute.value) {
      params.sellerId = sellerIdFromRoute.value
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

// 搜索
const handleSearch = () => {
  fetchProducts(true)
}

// 返回
const goBack = () => {
  router.push('/admin/products')
}

// 跳转详情页
const goToDetail = (id: number) => {
  router.push(`/user/products/${id}`)
}

// 获取状态文本
const getStatusText = (status: ProductStatus) => {
  return ProductStatusText[status] || ''
}

// 获取状态颜色
const getStatusColor = (status: ProductStatus) => {
  return ProductStatusColor[status] || 'info'
}

onMounted(() => {
  fetchProducts(true)
})
</script>

<style scoped>
.product-list-page {
  min-height: calc(100vh - 110px);
  background: #F5F5F5;
  padding-bottom: 20px;
}

/* 返回栏 */
.back-bar {
  padding: 12px 16px;
  background: #FFFFFF;
  border-bottom: 1px solid #E5E5E5;
}

/* 搜索头部 */
.search-header {
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
  padding: 16px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.search-box {
  max-width: 600px;
  margin: 0 auto;
}

.search-box :deep(.el-input__wrapper) {
  border-radius: 20px;
}

/* 筛选栏 */
.filter-bar {
  background: #FFFFFF;
  padding: 12px 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  border-bottom: 1px solid #E5E5E5;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  color: #6B7280;
  white-space: nowrap;
}

.price-separator {
  color: #9CA3AF;
  padding: 0 4px;
}

/* 商品网格 */
.product-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 12px;
}

@media (min-width: 768px) {
  .product-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (min-width: 1024px) {
  .product-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

/* 商品卡片 */
.product-card {
  background: #FFFFFF;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.product-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.product-image {
  position: relative;
  aspect-ratio: 1;
  background: #F9FAFB;
}

.product-image .el-image {
  width: 100%;
  height: 100%;
}

.image-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #9CA3AF;
}

.product-status-tag {
  position: absolute;
  top: 8px;
  left: 8px;
}

.product-info {
  padding: 12px;
}

.product-name {
  font-size: 14px;
  font-weight: 500;
  color: #1F2937;
  margin: 0 0 4px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-desc {
  font-size: 12px;
  color: #9CA3AF;
  margin: 0 0 8px 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.current-price {
  font-size: 18px;
  font-weight: 600;
  color: #F59E0B;
}

.original-price {
  font-size: 12px;
  color: #9CA3AF;
  text-decoration: line-through;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #6B7280;
}

.product-stock.out-of-stock {
  color: #EF4444;
}

/* 加载状态 */
.loading-more,
.no-more {
  text-align: center;
  padding: 16px;
  color: #9CA3AF;
  font-size: 14px;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
</style>
