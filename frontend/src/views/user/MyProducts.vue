<template>
  <div class="my-products-page">
    <!-- 统计卡片 -->
    <div class="stats-card">
      <div class="stats-item">
        <div class="stats-value">{{ stats.total }}</div>
        <div class="stats-label">商品总数</div>
      </div>
      <div class="stats-item">
        <div class="stats-value">{{ stats.onSale }}</div>
        <div class="stats-label">出售中</div>
      </div>
      <div class="stats-item">
        <div class="stats-value">{{ stats.offSale }}</div>
        <div class="stats-label">已下架</div>
      </div>
      <div class="stats-item">
        <div class="stats-value">{{ stats.soldOut }}</div>
        <div class="stats-label">已售罄</div>
      </div>
    </div>

    <!-- 操作栏 -->
    <div class="action-bar">
      <el-button type="primary" @click="goToCreate">
        <el-icon><Plus /></el-icon>
        发布商品
      </el-button>
      <el-select v-model="filterStatus" placeholder="筛选状态" size="default" @change="fetchProducts">
        <el-option label="全部" value="" />
        <el-option label="出售中" value="ON_SALE" />
        <el-option label="已下架" value="OFF_SALE" />
        <el-option label="已售罄" value="OUT_OF_STOCK" />
      </el-select>
    </div>

    <!-- 商品列表 -->
    <div class="product-list">
      <div
        v-for="product in productList"
        :key="product.id"
        class="product-item"
      >
        <div class="product-image">
          <el-image :src="product.mainImage" fit="cover" />
          <div class="product-status">
            <el-tag :type="getStatusColor(product.status)" size="small">
              {{ getStatusText(product.status) }}
            </el-tag>
          </div>
        </div>
        <div class="product-info">
          <h3 class="product-name">{{ product.name }}</h3>
          <div class="product-price">
            <span class="current-price">¥{{ product.price }}</span>
            <span class="original-price" v-if="product.originalPrice > product.price">
              ¥{{ product.originalPrice }}
            </span>
          </div>
          <div class="product-meta">
            <span>库存：{{ product.stock }}</span>
            <span>浏览：{{ product.viewCount }}</span>
          </div>
        </div>
        <div class="product-actions">
          <el-button size="small" text @click="goToDetail(product.id)">
            查看
          </el-button>
          <el-button size="small" text type="primary" @click="goToEdit(product.id)">
            编辑
          </el-button>
          <el-button
            size="small"
            text
            :type="product.isOnSale ? 'warning' : 'success'"
            @click="toggleSaleStatus(product)"
          >
            {{ product.isOnSale ? '下架' : '上架' }}
          </el-button>
          <el-button size="small" text type="danger" @click="handleDelete(product.id)">
            删除
          </el-button>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="productList.length === 0 && !loading" description="暂无商品">
      <el-button type="primary" @click="goToCreate">发布商品</el-button>
    </el-empty>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-more">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Loading } from '@element-plus/icons-vue'
import { getMyProducts, deleteProduct, updateProductStatus } from '@/api/product'
import { Product, ProductStatus, ProductStatusText, ProductStatusColor } from '@/api/types'

const router = useRouter()

const productList = ref<Product[]>([])
const loading = ref(false)
const filterStatus = ref('')
const page = ref(0)
const size = ref(10)

// 统计数据
const stats = reactive({
  total: 0,
  onSale: 0,
  offSale: 0,
  soldOut: 0
})

// 获取商品列表
const fetchProducts = async () => {
  loading.value = true
  try {
    const params: any = {
      page: page.value,
      size: size.value,
      sortBy: 'createdAt',
      sortDir: 'desc'
    }
    if (filterStatus.value) {
      params.status = filterStatus.value
    }

    const res = await getMyProducts(params)
    const { content, totalElements } = res.data.data
    productList.value = content
    stats.total = totalElements

    // 计算各状态数量
    stats.onSale = content.filter((p: Product) => p.status === ProductStatus.ON_SALE).length
    stats.offSale = content.filter((p: Product) => p.status === ProductStatus.OFF_SALE).length
    stats.soldOut = content.filter((p: Product) => p.status === ProductStatus.OUT_OF_STOCK).length
  } catch (error) {
    console.error('获取商品列表失败:', error)
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

// 去发布商品
const goToCreate = () => {
  router.push('/user/products/new')
}

// 去详情页
const goToDetail = (id: number) => {
  router.push(`/user/products/${id}`)
}

// 去编辑页
const goToEdit = (id: number) => {
  router.push(`/user/products/${id}/edit`)
}

// 上下架商品
const toggleSaleStatus = async (product: Product) => {
  const newStatus = product.isOnSale ? ProductStatus.OFF_SALE : ProductStatus.ON_SALE
  try {
    await updateProductStatus(product.id, newStatus)
    ElMessage.success(product.isOnSale ? '商品已下架' : '商品已上架')
    fetchProducts()
  } catch (error) {
    console.error('操作失败:', error)
  }
}

// 删除商品
const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要删除该商品吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteProduct(id)
      ElMessage.success('商品已删除')
      fetchProducts()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
.my-products-page {
  min-height: 100vh;
  background: #F5F5F5;
  padding: 16px;
}

/* 统计卡片 */
.stats-card {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 16px;
}

.stats-item {
  text-align: center;
  color: #FFFFFF;
}

.stats-value {
  font-size: 24px;
  font-weight: 700;
}

.stats-label {
  font-size: 12px;
  opacity: 0.9;
  margin-top: 4px;
}

/* 操作栏 */
.action-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.action-bar .el-button {
  flex: 1;
}

/* 商品列表 */
.product-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.product-item {
  background: #FFFFFF;
  border-radius: 12px;
  overflow: hidden;
}

.product-image {
  position: relative;
  height: 200px;
  background: #F9FAFB;
}

.product-image .el-image {
  width: 100%;
  height: 100%;
}

.product-status {
  position: absolute;
  top: 8px;
  left: 8px;
}

.product-info {
  padding: 12px;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: #1F2937;
  margin: 0 0 8px 0;
}

.product-price {
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

.product-actions {
  display: flex;
  border-top: 1px solid #E5E5E5;
}

.product-actions .el-button {
  flex: 1;
  border-radius: 0;
}

/* 加载状态 */
.loading-more {
  text-align: center;
  padding: 16px;
  color: #9CA3AF;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
</style>
