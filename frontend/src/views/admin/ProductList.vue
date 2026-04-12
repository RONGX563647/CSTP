<template>
  <div class="admin-product-list-page">
    <div class="page-header">
      <h1 class="page-title">商品管理</h1>
      <el-button type="primary" @click="goToCreate">
        <el-icon><Plus /></el-icon>
        新增商品
      </el-button>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-section">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="商品名称">
          <el-input v-model="filterForm.name" placeholder="请输入商品名称" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filterForm.category" placeholder="全部分类" clearable>
            <el-option label="手机数码" value="手机数码" />
            <el-option label="电脑办公" value="电脑办公" />
            <el-option label="家用电器" value="家用电器" />
            <el-option label="娱乐玩具" value="娱乐玩具" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterForm.status" placeholder="全部状态" clearable>
            <el-option label="在售" value="ON_SALE" />
            <el-option label="下架" value="OFF_SALE" />
            <el-option label="售罄" value="OUT_OF_STOCK" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">商品总数</div>
            <div class="stat-value">{{ stats.total }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">出售中</div>
            <div class="stat-value on-sale">{{ stats.onSale }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">已下架</div>
            <div class="stat-value off-sale">{{ stats.offSale }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-label">已售罄</div>
            <div class="stat-value sold-out">{{ stats.soldOut }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

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
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="goToEdit(row.id)">
              编辑
            </el-button>
            <el-button size="small" text type="danger" @click="handleDelete(row.id)">
              删除
            </el-button>
            <el-button
              size="small"
              text
              @click="viewUserProducts(row.sellerId)"
            >
              查看卖家
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
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getAllProducts, deleteProductForAdmin, setSaleStatus, setFeaturedStatus } from '@/api/product'
import { Product, ProductStatus, ProductStatusText, ProductStatusColor } from '@/api/types'

const router = useRouter()

const filterForm = reactive({
  name: '',
  category: '',
  status: ''
})

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

// 获取商品列表
const fetchProducts = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size,
      name: filterForm.name || undefined,
      category: filterForm.category || undefined,
      status: filterForm.status as ProductStatus | undefined
    }
    const res = await getAllProducts(params)
    const { content, totalElements } = res.data.data
    tableData.value = content
    pagination.total = totalElements
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

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchProducts()
}

// 重置
const handleReset = () => {
  filterForm.name = ''
  filterForm.category = ''
  filterForm.status = ''
  pagination.page = 1
  fetchProducts()
}

// 获取状态文本
const getStatusText = (status: ProductStatus) => {
  return ProductStatusText[status] || ''
}

// 获取状态颜色
const getStatusColor = (status: ProductStatus) => {
  return ProductStatusColor[status] || 'info'
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

// 查看卖家商品
const viewUserProducts = (userId: number) => {
  router.push(`/admin/products/user/${userId}`)
}

// 去新增
const goToCreate = () => {
  router.push('/admin/products/new')
}

// 去编辑
const goToEdit = (id: number) => {
  router.push(`/admin/products/${id}/edit`)
}

onMounted(() => {
  fetchProducts()
})
</script>

<style scoped>
.admin-product-list-page {
  padding: 20px;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #1F2937;
  margin: 0;
}

/* 筛选栏 */
.filter-section {
  background: #FFFFFF;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-label {
  font-size: 14px;
  color: #6B7280;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
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
