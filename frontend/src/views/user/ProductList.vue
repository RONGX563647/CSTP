<template>
  <MobileLayout :title="pageTitle" :show-tab-bar="!isAdminMode">
    <div class="product-list-page">
      <div class="back-bar" v-if="isAdminMode">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回商品管理
        </el-button>
      </div>

      <div class="search-header">
        <div class="search-box" @click="goToSearch">
          <div class="search-box-inner">
            <el-icon class="search-icon"><Search /></el-icon>
            <span class="search-placeholder">搜索商品名称、描述、标签...</span>
          </div>
        </div>
      </div>

      <div class="filter-bar">
        <div class="filter-item">
          <el-select v-model="searchForm.category" placeholder="分类" clearable size="small" @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="手机数码" value="手机数码" />
            <el-option label="电脑办公" value="电脑办公" />
            <el-option label="家用电器" value="家用电器" />
            <el-option label="娱乐玩具" value="娱乐玩具" />
          </el-select>
        </div>
        <div class="filter-item">
          <el-select v-model="sortBy" size="small" @change="handleSearch">
            <el-option label="最新" value="createdAt,desc" />
            <el-option label="价格↑" value="price,asc" />
            <el-option label="价格↓" value="price,desc" />
          </el-select>
        </div>
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
            <div class="product-status-tag" v-if="product.status && isAdminMode">
              <el-tag :type="getStatusColor(product.status)" size="small">
                {{ getStatusText(product.status) }}
              </el-tag>
            </div>
          </div>
          <div class="product-info">
            <h3 class="product-name">{{ product.name }}</h3>
            <div class="product-price-row">
              <span class="current-price">¥{{ product.price }}</span>
              <span class="original-price" v-if="product.originalPrice > product.price">
                ¥{{ product.originalPrice }}
              </span>
            </div>
            <div class="product-meta">
              <span class="seller">{{ product.sellerName || '匿名' }}</span>
              <span class="sales">{{ product.salesCount }}人想要</span>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-if="productList.length === 0 && !loading" description="暂无商品" />

      <div v-if="loading" class="loading-more">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-if="noMore" class="no-more">
        <span>没有更多了</span>
      </div>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Search, Loading, ArrowLeft, Picture } from '@element-plus/icons-vue'
import { searchPublicProducts } from '@/api/product'
import { Product, ProductStatus, ProductStatusText, ProductStatusColor } from '@/api/types'
import MobileLayout from '@/layouts/MobileLayout.vue'

const router = useRouter()
const route = useRoute()

const isAdminMode = computed(() => route.path.startsWith('/admin/products/user'))
const sellerIdFromRoute = computed(() => {
  const id = route.params.userId
  return id ? Number(id) : undefined
})

const pageTitle = computed(() => {
  if (isAdminMode) {
    return '用户商品列表'
  }
  return '商品市场'
})

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

const goToSearch = () => {
  router.push('/user/search')
}

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

const handleSearch = () => {
  fetchProducts(true)
}

const goBack = () => {
  router.push('/admin/products')
}

const goToDetail = (id: number) => {
  router.push(`/user/products/${id}`)
}

const getStatusText = (status: ProductStatus) => {
  return ProductStatusText[status] || ''
}

const getStatusColor = (status: ProductStatus) => {
  return ProductStatusColor[status] || 'info'
}

onMounted(() => {
  fetchProducts(true)
})
</script>

<style scoped>
.product-list-page {
  min-height: calc(100vh - var(--header-height));
  background: var(--bg-color);
  padding-bottom: 20px;
}

.back-bar {
  padding: 12px 16px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-light);
}

.search-header {
  background: var(--bg-card);
  padding: 12px 16px;
  transition: all 0.3s ease;
}

.search-box {
  position: relative;
  transition: all 0.3s ease;
  cursor: pointer;
}

.search-box-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--bg-color);
  border-radius: 20px;
  color: var(--text-placeholder);
  font-size: 14px;
}

.search-icon {
  font-size: 16px;
}

.search-placeholder {
  flex: 1;
}

.search-box:hover .search-box-inner {
  background: #E5E7EB;
  transition: background 0.2s;
}

.filter-bar {
  display: flex;
  gap: 12px;
  padding: 8px 16px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-light);
}

.filter-item {
  flex-shrink: 0;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  padding: 8px;
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

.product-card {
  background: var(--bg-card);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s;
}

.product-card:hover {
  transform: translateY(-2px);
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
  font-size: 32px;
}

.product-status-tag {
  position: absolute;
  top: 8px;
  left: 8px;
}

.product-info {
  padding: 10px;
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

.product-price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 4px;
}

.current-price {
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-color);
}

.original-price {
  font-size: 12px;
  color: var(--text-placeholder);
  text-decoration: line-through;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-placeholder);
}

.loading-more,
.no-more {
  text-align: center;
  padding: 16px;
  color: var(--text-placeholder);
  font-size: 13px;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
</style>