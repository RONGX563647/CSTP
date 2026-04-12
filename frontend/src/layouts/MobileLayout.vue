<template>
  <div class="mobile-layout">
    <!-- 顶部导航栏 -->
    <header class="mobile-header">
      <div class="header-content">
        <div class="header-left">
          <slot name="left">
            <el-button text @click="handleBack">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
          </slot>
        </div>
        <div class="header-title">
          <slot name="title">{{ title }}</slot>
        </div>
        <div class="header-right">
          <slot name="right"></slot>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="main-content">
      <slot></slot>
    </main>

    <!-- 底部标签栏 -->
    <nav class="tab-bar" v-if="showTabBar">
      <div
        class="tab-item"
        :class="{ active: activeTab === 'home' }"
        @click="navigateTo('/user/home')"
      >
        <el-icon :size="24"><HomeFilled /></el-icon>
        <span>首页</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: activeTab === 'products' }"
        @click="navigateTo('/user/products')"
      >
        <el-icon :size="24"><ShoppingCart /></el-icon>
        <span>商品</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: activeTab === 'orders' }"
        @click="navigateTo('/user/orders/buyer')"
      >
        <el-icon :size="24"><List /></el-icon>
        <span>订单</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: activeTab === 'address' }"
        @click="navigateTo('/user/addresses')"
      >
        <el-icon :size="24"><Location /></el-icon>
        <span>地址</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: activeTab === 'profile' }"
        @click="navigateTo('/user/home')"
      >
        <el-icon :size="24"><User /></el-icon>
        <span>我的</span>
      </div>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, HomeFilled, Location, User, ShoppingCart, List } from '@element-plus/icons-vue'

interface Props {
  title?: string
  showTabBar?: boolean
}

withDefaults(defineProps<Props>(), {
  title: '',
  showTabBar: false
})

const router = useRouter()
const route = useRoute()

const activeTab = computed(() => {
  const path = route.path
  if (path === '/user/home' || path === '/') return 'home'
  if (path.includes('products')) return 'products'
  if (path.includes('orders')) return 'orders'
  if (path.includes('address')) return 'address'
  if (path.includes('profile') || path === '/user/home') return 'profile'
  return ''
})

const handleBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/user/home')
  }
}

const navigateTo = (path: string) => {
  router.push(path)
}
</script>

<style scoped>
.mobile-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #F5F5F5;
}

/* 顶部导航栏 */
.mobile-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.2);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 50px;
  padding: 0 16px;
}

.header-left,
.header-right {
  width: 48px;
  display: flex;
  align-items: center;
}

.header-title {
  flex: 1;
  text-align: center;
  font-size: 17px;
  font-weight: 600;
  color: #1F2937;
}

.header-left .el-button {
  color: #1F2937;
}

/* 主内容区 */
.main-content {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* 底部标签栏 */
.tab-bar {
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 60px;
  background: #FFFFFF;
  border-top: 1px solid #E5E5E5;
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 16px;
  color: #9CA3AF;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.2s;
}

.tab-item.active {
  color: #F59E0B;
}

.tab-item .el-icon {
  margin-bottom: 2px;
}
</style>
