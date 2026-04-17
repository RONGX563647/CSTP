<template>
  <MobileLayout :show-header="false" :show-tab-bar="true">
    <div class="profile-page">
      <!-- 用户卡片 -->
      <div class="user-card">
        <div class="user-avatar">
          <el-avatar :size="56" :src="authStore.userInfo?.avatar || undefined">
            {{ userInitial }}
          </el-avatar>
        </div>
        <div class="user-info">
          <h2 class="user-name">{{ authStore.userInfo?.nickname || authStore.userInfo?.username }}</h2>
          <p class="user-id">ID: {{ authStore.userInfo?.id }}</p>
        </div>
        <div class="edit-btn" @click="navigateTo('/user/profile/edit')">
          <el-icon :size="16"><Edit /></el-icon>
        </div>
      </div>

      <!-- 快捷入口 -->
      <div class="quick-actions">
        <div class="action-item" @click="navigateTo('/user/checkin')">
          <div class="action-icon checkin-icon">
            <el-icon :size="22"><Calendar /></el-icon>
          </div>
          <span>签到</span>
        </div>
        <div class="action-item" @click="navigateTo('/user/points')">
          <div class="action-icon points-icon">
            <el-icon :size="22"><Star /></el-icon>
          </div>
          <span>积分</span>
        </div>
        <div class="action-item" @click="navigateTo('/user/reputation')">
          <div class="action-icon reputation-icon">
            <el-icon :size="22"><Medal /></el-icon>
          </div>
          <span>信誉</span>
        </div>
        <div class="action-item" @click="navigateTo('/user/products/my')">
          <div class="action-icon">
            <el-icon :size="22"><Goods /></el-icon>
          </div>
          <span>我的商品</span>
        </div>
        <div class="action-item" @click="navigateTo('/user/orders/buyer')">
          <div class="action-icon">
            <el-icon :size="22"><ShoppingBag /></el-icon>
          </div>
          <span>我买的</span>
        </div>
        <div class="action-item" @click="navigateTo('/user/orders/seller')">
          <div class="action-icon">
            <el-icon :size="22"><Sell /></el-icon>
          </div>
          <span>我卖的</span>
        </div>
        <div class="action-item" @click="navigateTo('/user/addresses')">
          <div class="action-icon">
            <el-icon :size="22"><Location /></el-icon>
          </div>
          <span>地址</span>
        </div>
      </div>

      <!-- 设置列表 -->
      <div class="menu-section">
        <div class="menu-item" @click="navigateTo('/user/password')">
          <div class="menu-left">
            <el-icon :size="18"><Lock /></el-icon>
            <span class="menu-label">修改密码</span>
          </div>
          <el-icon class="menu-arrow"><ArrowRight /></el-icon>
        </div>
        <div class="menu-item" @click="navigateTo('/user/delete-account')">
          <div class="menu-left">
            <el-icon :size="18" class="danger"><Warning /></el-icon>
            <span class="menu-label danger">注销账号</span>
          </div>
          <el-icon class="menu-arrow"><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- 退出 -->
      <div class="logout-section">
        <el-button type="danger" plain block @click="handleLogout">
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
  Edit, ArrowRight, Location, Lock, Warning,
  Goods, ShoppingBag, Sell, Calendar, Star, Medal
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
.profile-page {
  min-height: 100vh;
  background: var(--bg-color);
}

/* 用户卡片 */
.user-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 16px;
  background: var(--bg-card);
}

.user-avatar {
  flex-shrink: 0;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 2px 0;
}

.user-id {
  font-size: 12px;
  color: var(--text-secondary);
  margin: 0;
}

.edit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--bg-color);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
}

.edit-btn:active {
  background: var(--primary-lighter);
  color: var(--primary-color);
}

/* 快捷入口 */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  padding: 16px 0;
  margin-bottom: 8px;
  background: var(--bg-card);
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.action-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: var(--primary-lighter);
  border-radius: 12px;
  color: var(--primary-color);
  transition: transform 0.2s;
}

.action-item:active .action-icon {
  transform: scale(0.92);
}

.action-item span {
  font-size: 12px;
  color: var(--text-primary);
}

.action-icon.checkin-icon {
  background: linear-gradient(135deg, var(--primary-color), #6c5ce7);
  color: #fff;
}

.action-icon.points-icon {
  background: linear-gradient(135deg, #fdcb6e, #e17055);
  color: #fff;
}

.action-icon.reputation-icon {
  background: linear-gradient(135deg, #00b894, #55a3ff);
  color: #fff;
}

/* 设置菜单 */
.menu-section {
  background: var(--bg-card);
  margin-bottom: 8px;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-light);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-item:active {
  background: var(--bg-color);
}

.menu-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.menu-label {
  font-size: 14px;
  color: var(--text-primary);
}

.menu-label.danger {
  color: var(--error-color);
}

.menu-left .danger {
  color: var(--error-color);
}

.menu-arrow {
  color: var(--text-placeholder);
  font-size: 14px;
}

/* 退出 */
.logout-section {
  padding: 20px 16px;
}
</style>
