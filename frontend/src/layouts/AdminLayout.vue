<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-header">
        <h1>AiSale 管理端</h1>
      </div>
      <el-menu
        :default-active="activeMenu"
        background-color="#1F2937"
        text-color="#9CA3AF"
        active-text-color="#FDE68A"
        :collapse="sidebarCollapsed"
        router
      >
        <el-menu-item index="/admin/products">
          <el-icon><ShoppingCart /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/products/query" @click="handleQueryClick">
          <el-icon><Search /></el-icon>
          <span>商品查询</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 主内容区 -->
    <div class="main-container">
      <!-- 顶部栏 -->
      <header class="top-bar">
        <div class="top-bar-left">
          <el-button text @click="sidebarCollapsed = !sidebarCollapsed">
            <el-icon><Fold /></el-icon>
          </el-button>
        </div>
        <div class="top-bar-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="authStore.userInfo?.avatar || undefined">
                {{ userInitial }}
              </el-avatar>
              <span class="username">{{ authStore.userInfo?.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 内容区 -->
      <main class="content">
        <slot></slot>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ShoppingCart, Search, Fold
} from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const sidebarCollapsed = ref(false)

const activeMenu = computed(() => {
  return route.path
})

const userInitial = computed(() => {
  const name = authStore.userInfo?.username || ''
  return name.charAt(0).toUpperCase()
})

const handleCommand = (command: string) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      authStore.logout()
      ElMessage.success('已退出登录')
      router.push('/admin/login')
    }).catch(() => {})
  }
}

const handleQueryClick = () => {
  ElMessage.info('商品查询功能开发中，请使用商品管理页面的筛选功能')
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background: #F3F4F6;
}

/* 侧边栏 */
.sidebar {
  width: 240px;
  background: #1F2937;
  transition: width 0.3s;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid #374151;
  flex-shrink: 0;
}

.sidebar-header h1 {
  font-size: 18px;
  font-weight: 600;
  color: #FDE68A;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
}

.sidebar.collapsed .sidebar-header h1 {
  display: none;
}

.sidebar :deep(.el-menu) {
  border-right: none;
  overflow: hidden;
}

.sidebar :deep(.el-menu-item) {
  height: 50px;
}

/* 主内容区 */
.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 顶部栏 */
.top-bar {
  height: 60px;
  background: #FFFFFF;
  border-bottom: 1px solid #E5E5E5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.top-bar-left {
  display: flex;
  align-items: center;
}

.top-bar-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #1F2937;
}

/* 内容区 */
.content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
</style>
