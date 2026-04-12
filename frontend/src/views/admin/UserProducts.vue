<template>
  <AdminLayout>
    <div class="admin-user-products-page">
    <div class="page-header">
      <div class="header-left">
        <el-button text @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
          返回商品管理
        </el-button>
        <h1>用户商品列表</h1>
      </div>
    </div>

    <!-- 用户信息卡片 -->
    <el-card class="user-info-card">
      <div class="user-info">
        <el-avatar :size="64" :src="userInfo?.avatar || undefined">
          {{ userInitial }}
        </el-avatar>
        <div class="user-details">
          <h2>{{ userInfo?.nickname || userInfo?.username }}</h2>
          <p>用户名：{{ userInfo?.username }}</p>
          <p v-if="userInfo?.phone">手机号：{{ userInfo?.phone }}</p>
          <el-tag :type="userInfo?.role === 'ADMIN' || userInfo?.role === 'SUPER_ADMIN' ? 'danger' : 'primary'">
            {{ userInfo?.role === 'ADMIN' || userInfo?.role === 'SUPER_ADMIN' ? '管理员' : '普通用户' }}
          </el-tag>
        </div>
        <div class="user-stats">
          <div class="stat-item">
            <div class="stat-value">{{ stats.total }}</div>
            <div class="stat-label">商品总数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value on-sale">{{ stats.onSale }}</div>
            <div class="stat-label">出售中</div>
          </div>
          <div class="stat-item">
            <div class="stat-value off-sale">{{ stats.offSale }}</div>
            <div class="stat-label">已下架</div>
          </div>
          <div class="stat-item">
            <div class="stat-value sold-out">{{ stats.soldOut }}</div>
            <div class="stat-label">已售罄</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 商品表格 -->
    <el-card class="table-card">
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="商品图片" width="100">
          <template #default="{ row }">
            <el-image
              :src="row.mainImage"
              fit="cover"
              style="width: 60px; height: 60px; border-radius: 4px;"
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="price" label="价格" width="100">
          <template #default="{ row }">
            <span class="price">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusColor(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上架" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.isOnSale"
              @change="handleToggleSale(row)"
              active-color="#13ce66"
              inactive-color="#ff4949"
            />
          </template>
        </el-table-column>
        <el-table-column label="推荐" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.isFeatured"
              @change="handleToggleFeatured(row)"
              active-color="#F59E0B"
              inactive-color="#D1D5DB"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="goToEdit(row.id)">
              编辑
            </el-button>
            <el-button size="small" text type="danger" @click="handleDelete(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchProducts"
          @current-change="fetchProducts"
        />
      </div>
    </el-card>
  </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getProductsByUser, getUserProductStats, deleteProductForAdmin, setSaleStatus, setFeaturedStatus } from '@/api/product'
import { Product, ProductStatus, ProductStatusText, ProductStatusColor } from '@/api/types'
import AdminLayout from '@/layouts/AdminLayout.vue'

const router = useRouter()
const route = useRoute()

const userId = computed(() => Number(route.params.userId))

const tableData = ref<Product[]>([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const stats = reactive({
  total: 0,
  onSale: 0,
  offSale: 0,
  soldOut: 0
})

// 模拟用户信息（实际应从后端获取）
const userInfo = ref({
  id: userId.value,
  username: '用户' + userId.value,
  nickname: '用户' + userId.value,
  avatar: '',
  role: 'USER',
  phone: ''
})

const userInitial = computed(() => {
  const name = userInfo.value?.nickname || userInfo.value?.username || ''
  return name.charAt(0).toUpperCase()
})

// 获取商品列表
const fetchProducts = async () => {
  loading.value = true
  try {
    const res = await getProductsByUser(userId.value, {
      page: pagination.page - 1,
      size: pagination.size
    })
    const { content, totalElements } = res.data.data
    tableData.value = content
    pagination.total = totalElements

    // 更新统计
    stats.total = totalElements
    stats.onSale = content.filter((p: Product) => p.status === ProductStatus.ON_SALE).length
    stats.offSale = content.filter((p: Product) => p.status === ProductStatus.OFF_SALE).length
    stats.soldOut = content.filter((p: Product) => p.status === ProductStatus.OUT_OF_STOCK).length
  } catch (error) {
    console.error('获取商品列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取用户统计
const fetchStats = async () => {
  try {
    const res = await getUserProductStats(userId.value)
    stats.total = res.data.data.totalProducts
  } catch (error) {
    console.error('获取统计失败:', error)
  }
}

// 上下架商品
const handleToggleSale = async (product: Product) => {
  try {
    await setSaleStatus(product.id, product.isOnSale)
    ElMessage.success(product.isOnSale ? '商品已上架' : '商品已下架')
  } catch (error) {
    product.isOnSale = !product.isOnSale
  }
}

// 推荐商品
const handleToggleFeatured = async (product: Product) => {
  try {
    await setFeaturedStatus(product.id, product.isFeatured)
    ElMessage.success(product.isFeatured ? '已设为推荐' : '已取消推荐')
  } catch (error) {
    product.isFeatured = !product.isFeatured
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
      await deleteProductForAdmin(id)
      ElMessage.success('商品已删除')
      fetchProducts()
    } catch (error) {
      console.error('删除失败:', error)
    }
  }).catch(() => {})
}

// 去编辑
const goToEdit = (id: number) => {
  router.push(`/admin/products/${id}/edit`)
}

// 返回
const goBack = () => {
  router.push('/admin/products')
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
  fetchProducts()
  fetchStats()
})
</script>

<style scoped>
.admin-user-products-page {
  padding: 20px;
  background: #F5F5F6;
  min-height: calc(100vh - 100px);
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h1 {
  font-size: 20px;
  font-weight: 600;
  color: #1F2937;
  margin: 0;
}

/* 用户信息卡片 */
.user-info-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 24px;
}

.user-details {
  flex: 1;
}

.user-details h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1F2937;
  margin: 0 0 8px 0;
}

.user-details p {
  font-size: 14px;
  color: #6B7280;
  margin: 4px 0;
}

.user-stats {
  display: flex;
  gap: 32px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1F2937;
}

.stat-value.on-sale {
  color: #10B981;
}

.stat-value.off-sale {
  color: #6B7280;
}

.stat-value.sold-out {
  color: #EF4444;
}

.stat-label {
  font-size: 13px;
  color: #9CA3AF;
  margin-top: 4px;
}

/* 表格卡片 */
.table-card {
  border-radius: 8px;
}

.table-card :deep(.el-card__body) {
  padding: 20px;
}

.price {
  color: #F59E0B;
  font-weight: 600;
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
