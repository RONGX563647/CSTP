<template>
  <div class="home">
    <!-- 顶部导航栏 -->
    <header class="header">
      <div class="header-content">
        <div class="logo">
          <el-icon :size="28" color="#F59E0B"><ShoppingCart /></el-icon>
          <span class="logo-text">AiSale</span>
        </div>
        <div class="user-info">
          <el-avatar :size="36" :src="authStore.userInfo?.avatar || undefined">
            {{ userInitial }}
          </el-avatar>
          <span class="username">{{ authStore.userInfo?.nickname || authStore.userInfo?.username }}</span>
          <el-badge :value="userRole" class="role-badge" />
          <el-button text @click="handleLogout">
            <el-icon><SwitchButton /></el-icon>
            退出登录
          </el-button>
        </div>
      </div>
    </header>

    <!-- 主要内容 -->
    <main class="main-content">
      <div class="welcome-card">
        <div class="welcome-icon">
          <el-icon :size="48" color="#F59E0B"><ShoppingCart /></el-icon>
        </div>
        <h1 class="welcome-title">欢迎来到 AiSale</h1>
        <p class="welcome-subtitle">AI 智能销售系统</p>
        <p class="welcome-message">
          您好，{{ authStore.userInfo?.nickname || authStore.userInfo?.username }}！
          您已经成功登录系统。
        </p>

        <div class="info-cards">
          <div class="info-card">
            <div class="info-icon">
              <el-icon :size="24"><User /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">用户名</span>
              <span class="info-value">{{ authStore.userInfo?.username }}</span>
            </div>
          </div>

          <div class="info-card">
            <div class="info-icon">
              <el-icon :size="24"><Edit /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">昵称</span>
              <span class="info-value">{{ authStore.userInfo?.nickname || '-' }}</span>
            </div>
          </div>

          <div class="info-card">
            <div class="info-icon">
              <el-icon :size="24"><Flag /></el-icon>
            </div>
            <div class="info-content">
              <span class="info-label">角色</span>
              <span class="info-value role-user" v-if="authStore.isUser">普通用户</span>
              <span class="info-value role-admin" v-else-if="authStore.isAdmin">管理员</span>
            </div>
          </div>
        </div>

        <div class="actions">
          <el-button type="primary" size="large">
            <el-icon><DataAnalysis /></el-icon>
            开始使用
          </el-button>
          <el-button size="large" plain>
            <el-icon><Setting /></el-icon>
            账号设置
          </el-button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const userRole = computed(() => {
  if (authStore.isAdmin) return '管理员'
  if (authStore.isUser) return '用户'
  return ''
})

const userInitial = computed(() => {
  const name = authStore.userInfo?.nickname || authStore.userInfo?.username || ''
  return name.charAt(0).toUpperCase()
})

const handleLogout = () => {
  authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.home {
  min-height: 100vh;
  background: linear-gradient(135deg, #FFFDF5 0%, #FEF3C7 100%);
}

/* 顶部导航栏 */
.header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  padding: 12px 24px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #1F2937;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: #1F2937;
}

.role-badge {
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  color: #92400E;
  font-weight: 500;
}

/* 主内容区 */
.main-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 48px 24px;
}

.welcome-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(245, 158, 11, 0.15);
  padding: 48px;
  text-align: center;
}

.welcome-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 96px;
  height: 96px;
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
  border-radius: 24px;
  margin-bottom: 24px;
  box-shadow: 0 8px 24px rgba(245, 158, 11, 0.25);
}

.welcome-title {
  font-size: 28px;
  font-weight: 700;
  color: #1F2937;
  margin-bottom: 8px;
}

.welcome-subtitle {
  font-size: 16px;
  color: #6B7280;
  margin-bottom: 24px;
}

.welcome-message {
  font-size: 15px;
  color: #4B5563;
  line-height: 1.6;
  margin-bottom: 32px;
}

/* 信息卡片 */
.info-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 32px;
}

.info-card {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: #FFFBEB;
  border-radius: 12px;
  border: 1px solid #FDE68A;
}

.info-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
  border-radius: 10px;
  margin-right: 16px;
  color: #FFFFFF;
}

.info-content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  flex: 1;
}

.info-label {
  font-size: 13px;
  color: #6B7280;
  margin-bottom: 4px;
}

.info-value {
  font-size: 16px;
  font-weight: 500;
  color: #1F2937;
}

.info-value.role-user {
  color: #F59E0B;
}

.info-value.role-admin {
  color: #DC2626;
}

/* 操作按钮 */
.actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

/* 响应式适配 */
@media (max-width: 640px) {
  .welcome-card {
    padding: 32px 24px;
  }

  .welcome-title {
    font-size: 24px;
  }

  .actions {
    flex-direction: column;
  }

  .actions .el-button {
    width: 100%;
  }
}
</style>
