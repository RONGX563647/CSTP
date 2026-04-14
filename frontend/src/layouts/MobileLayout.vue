<template>
  <div class="mobile-layout">
    <header class="mobile-header" v-if="showHeader">
      <div class="header-content">
        <div class="header-left">
          <slot name="left">
            <el-button text v-if="showBack" @click="handleBack" class="back-btn">
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

    <main class="main-content">
      <slot></slot>
    </main>

    <nav class="tab-bar" v-if="showTabBar">
      <div
        class="tab-item"
        :class="{ active: activeTab === 'home' }"
        @click="navigateTo('/user/home')"
      >
        <el-icon :size="22"><HomeFilled /></el-icon>
        <span>首页</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: activeTab === 'message' }"
        @click="navigateTo('/user/chat')"
      >
        <el-icon :size="22"><ChatDotRound /></el-icon>
        <span>消息</span>
      </div>
      <div
        class="tab-item publish-btn"
        @click="navigateTo('/user/products/new')"
      >
        <div class="publish-icon">
          <el-icon :size="26"><Plus /></el-icon>
        </div>
      </div>
      <div
        class="tab-item"
        :class="{ active: activeTab === 'orders' }"
        @click="navigateTo('/user/orders/buyer')"
      >
        <el-icon :size="22"><Document /></el-icon>
        <span>订单</span>
      </div>
      <div
        class="tab-item"
        :class="{ active: activeTab === 'profile' }"
        @click="navigateTo('/user/profile')"
      >
        <el-icon :size="22"><User /></el-icon>
        <span>我的</span>
      </div>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, HomeFilled, User, Plus, ChatDotRound, Document } from '@element-plus/icons-vue'

interface Props {
  title?: string
  showTabBar?: boolean
  showHeader?: boolean
  showBack?: boolean
}

withDefaults(defineProps<Props>(), {
  title: '',
  showTabBar: false,
  showHeader: true,
  showBack: true
})

const router = useRouter()
const route = useRoute()

const activeTab = computed(() => {
  const path = route.path
  if (path === '/user/home' || path === '/') return 'home'
  if (path.includes('/user/products/new')) return 'publish'
  if (path.includes('/user/chat')) return 'message'
  if (path.includes('/user/orders')) return 'orders'
  if (path.includes('/user/profile') || path.includes('/user/products/my')) return 'profile'
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
  background: var(--bg-color);
}

.mobile-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--bg-header);
  border-bottom: 1px solid var(--border-light);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--header-height);
  padding: 0 12px;
}

.header-left,
.header-right {
  width: 40px;
  display: flex;
  align-items: center;
}

.header-title {
  flex: 1;
  text-align: center;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.back-btn {
  color: var(--text-primary);
  padding: 8px;
}

.main-content {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.tab-bar {
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: calc(var(--tab-bar-height) + env(safe-area-inset-bottom, 0px));
  padding-bottom: env(safe-area-inset-bottom, 0px);
  background: var(--bg-card);
  border-top: 1px solid var(--border-light);
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
  padding: 4px 0;
  color: var(--text-secondary);
  font-size: 10px;
  cursor: pointer;
  transition: color 0.2s;
  flex: 1;
  -webkit-tap-highlight-color: transparent;
}

.tab-item.active {
  color: var(--primary-color);
}

.tab-item.publish-btn {
  flex: 1;
}

.publish-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
  border-radius: 50%;
  color: #FFFFFF;
  box-shadow: 0 4px 12px rgba(255, 149, 0, 0.35);
  transition: transform 0.2s, box-shadow 0.2s;
}

.publish-icon:active {
  transform: scale(0.95);
}
</style>