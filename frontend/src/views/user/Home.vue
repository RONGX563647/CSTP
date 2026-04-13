<template>
  <MobileLayout title="首页" :show-tab-bar="true">
    <div class="user-home-page">
      <!-- 用户信息卡片 -->
      <div class="user-card">
        <div class="user-avatar">
          <el-avatar :size="64" :src="authStore.userInfo?.avatar || undefined">
            {{ userInitial }}
          </el-avatar>
        </div>
        <div class="user-info">
          <h2 class="user-name">{{ authStore.userInfo?.nickname || authStore.userInfo?.username }}</h2>
          <p class="user-phone">{{ authStore.userInfo?.phone || '未绑定手机号' }}</p>
        </div>
      </div>

      <!-- 商品管理入口 -->
      <div class="menu-section">
        <h3 class="menu-title">商品管理</h3>
        <div class="menu-list">
          <div class="menu-item" @click="navigateTo('/user/products')">
            <div class="menu-icon">
              <el-icon :size="24"><ShoppingCart /></el-icon>
            </div>
            <span class="menu-label">商品市场</span>
            <el-icon class="menu-arrow"><ArrowRight /></el-icon>
          </div>
          <div class="menu-item" @click="navigateTo('/user/products/my')">
            <div class="menu-icon">
              <el-icon :size="24"><List /></el-icon>
            </div>
            <span class="menu-label">我的商品</span>
            <el-icon class="menu-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <!-- 订单管理入口 -->
      <div class="menu-section">
        <h3 class="menu-title">订单管理</h3>
        <div class="menu-list">
          <div class="menu-item" @click="navigateTo('/user/orders/buyer')">
            <div class="menu-icon">
              <el-icon :size="24"><Document /></el-icon>
            </div>
            <span class="menu-label">我买的订单</span>
            <el-icon class="menu-arrow"><ArrowRight /></el-icon>
          </div>
          <div class="menu-item" @click="navigateTo('/user/orders/seller')">
            <div class="menu-icon">
              <el-icon :size="24"><FolderOpened /></el-icon>
            </div>
            <span class="menu-label">我卖的订单</span>
            <el-icon class="menu-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <!-- 收货管理 -->
      <div class="menu-section">
        <h3 class="menu-title">收货管理</h3>
        <div class="menu-list">
          <div class="menu-item" @click="navigateTo('/user/addresses')">
            <div class="menu-icon">
              <el-icon :size="24"><Location /></el-icon>
            </div>
            <span class="menu-label">收货地址</span>
            <el-icon class="menu-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <div class="menu-section">
        <h3 class="menu-title">账户设置</h3>
        <div class="menu-list">
          <div class="menu-item" @click="navigateTo('/user/profile')">
            <div class="menu-icon">
              <el-icon :size="24"><User /></el-icon>
            </div>
            <span class="menu-label">个人信息</span>
            <el-icon class="menu-arrow"><ArrowRight /></el-icon>
          </div>
          <div class="menu-item" @click="navigateTo('/user/password')">
            <div class="menu-icon">
              <el-icon :size="24"><Lock /></el-icon>
            </div>
            <span class="menu-label">修改密码</span>
            <el-icon class="menu-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </div>

      <!-- 退出登录 -->
      <div class="logout-section">
        <el-button type="danger" size="large" block @click="handleLogout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-button>
      </div>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Location, ArrowRight, User, Lock, SwitchButton,
  ShoppingCart, List, Document, FolderOpened
} from '@element-plus/icons-vue'
import MobileLayout from '@/layouts/MobileLayout.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const userInitial = computed(() => {
  const name = authStore.userInfo?.nickname || authStore.userInfo?.username || ''
  return name.charAt(0).toUpperCase()
})

const navigateTo = (path: string) => {
  router.push(path)
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped>
.user-home-page {
  min-height: calc(100vh - 110px);
  background: #F5F5F5;
}

/* 用户信息卡片 */
.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 16px;
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
  margin-bottom: 16px;
}

.user-avatar {
  flex-shrink: 0;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 20px;
  font-weight: 600;
  color: #1F2937;
  margin-bottom: 4px;
}

.user-phone {
  font-size: 14px;
  color: #6B7280;
}

/* 菜单区域 */
.menu-section {
  margin-bottom: 16px;
}

.menu-title {
  font-size: 14px;
  font-weight: 500;
  color: #6B7280;
  padding: 12px 16px;
}

.menu-list {
  background: #FFFFFF;
  border-radius: 12px;
  margin: 0 16px;
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #F3F4F6;
  cursor: pointer;
  transition: background 0.2s;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item:hover {
  background: #F9FAFB;
}

.menu-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
  border-radius: 10px;
  color: #FFFFFF;
}

.menu-label {
  flex: 1;
  font-size: 15px;
  color: #1F2937;
}

.menu-arrow {
  color: #9CA3AF;
}

/* 退出登录 */
.logout-section {
  padding: 24px 16px;
}
</style>
