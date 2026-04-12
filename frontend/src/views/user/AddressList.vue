<template>
  <MobileLayout title="收货地址" :show-tab-bar="true">
    <div class="address-page">
      <!-- 地址列表 -->
      <div class="address-list">
        <div
          v-for="address in addresses"
          :key="address.id"
          class="address-card"
          :class="{ default: address.isDefault }"
          @click="handleEdit(address)"
        >
          <div class="address-header">
            <div class="receiver-info">
              <span class="receiver-name">{{ address.receiverName }}</span>
              <span class="receiver-phone">{{ address.receiverPhone }}</span>
            </div>
            <el-tag v-if="address.isDefault" size="small" type="warning">默认</el-tag>
          </div>
          <div class="address-detail">
            <el-icon class="location-icon"><Location /></el-icon>
            <span class="address-text">
              {{ address.province }}{{ address.city }}{{ address.district }}{{ address.detailAddress }}
            </span>
          </div>
          <div class="address-actions">
            <el-button
              v-if="!address.isDefault"
              text
              type="warning"
              @click.stop="handleSetDefault(address)"
            >
              设为默认
            </el-button>
            <el-button text type="danger" @click.stop="handleDelete(address)">
              删除
            </el-button>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <el-empty v-if="!loading && addresses.length === 0" description="暂无收货地址" />

      <!-- 添加地址按钮 -->
      <div class="add-button-wrapper">
        <el-button type="primary" size="large" block @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新建收货地址
        </el-button>
      </div>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, Plus } from '@element-plus/icons-vue'
import MobileLayout from '@/layouts/MobileLayout.vue'
import { getUserAddresses, setDefaultAddress, deleteAddress, type Address } from '@/api/address'

const router = useRouter()
const addresses = ref<Address[]>([])
const loading = ref(false)

// 加载地址列表
const loadAddresses = async () => {
  loading.value = true
  try {
    const res = await getUserAddresses()
    addresses.value = res.data || []
  } catch (error) {
    console.error('加载地址列表失败:', error)
    ElMessage.error('加载地址列表失败')
  } finally {
    loading.value = false
  }
}

// 添加地址
const handleAdd = () => {
  router.push('/user/addresses/new')
}

// 编辑地址
const handleEdit = (address: Address) => {
  router.push(`/user/addresses/${address.id}`)
}

// 设为默认
const handleSetDefault = async (address: Address) => {
  try {
    await setDefaultAddress(address.id!)
    ElMessage.success('已设为默认地址')
    loadAddresses()
  } catch (error: any) {
    ElMessage.error(error.message || '设置失败')
  }
}

// 删除地址
const handleDelete = (address: Address) => {
  ElMessageBox.confirm('确定要删除该收货地址吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteAddress(address.id!)
      ElMessage.success('删除成功')
      loadAddresses()
    } catch (error: any) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  loadAddresses()
})
</script>

<style scoped>
.address-page {
  min-height: calc(100vh - 110px);
  padding: 16px;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 100px;
}

.address-card {
  background: #FFFFFF;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  position: relative;
  cursor: pointer;
}

.address-card.default {
  border: 1px solid #F59E0B;
}

.address-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.receiver-info {
  display: flex;
  gap: 12px;
  align-items: center;
}

.receiver-name {
  font-size: 16px;
  font-weight: 600;
  color: #1F2937;
}

.receiver-phone {
  font-size: 14px;
  color: #6B7280;
}

.address-detail {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 12px;
}

.location-icon {
  color: #F59E0B;
  margin-top: 2px;
}

.address-text {
  flex: 1;
  font-size: 14px;
  color: #4B5563;
  line-height: 1.5;
}

.address-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #F3F4F6;
}

.add-button-wrapper {
  position: fixed;
  bottom: 60px;
  left: 0;
  right: 0;
  padding: 16px;
  background: transparent;
  pointer-events: none;
}

.add-button-wrapper .el-button {
  pointer-events: auto;
  max-width: 400px;
  margin: 0 auto;
  display: block;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
}

/* 空状态调整 */
:deep(.el-empty__description) {
  color: #9CA3AF;
}
</style>
