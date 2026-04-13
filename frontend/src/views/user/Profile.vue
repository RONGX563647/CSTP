<template>
  <MobileLayout title="个人信息">
    <div class="profile-page">
      <!-- 用户信息卡片 -->
      <div class="profile-header">
        <div class="avatar-section">
          <el-avatar :size="80" :src="userInfo?.avatar || undefined">
            {{ userInitial }}
          </el-avatar>
        </div>
        <div class="user-name-section">
          <h2>{{ userInfo?.nickname || userInfo?.username }}</h2>
          <p>{{ userInfo?.username }}</p>
        </div>
      </div>

      <!-- 信息列表 -->
      <div class="profile-list">
        <div class="profile-item">
          <span class="item-label">昵称</span>
          <span class="item-value">{{ userInfo?.nickname || '-' }}</span>
        </div>
        <div class="profile-item">
          <span class="item-label">用户名</span>
          <span class="item-value">{{ userInfo?.username }}</span>
        </div>
        <div class="profile-item">
          <span class="item-label">邮箱</span>
          <span class="item-value">
            {{ userInfo?.email || '-' }}
            <el-tag v-if="userInfo?.email && !emailVerified" size="small" type="warning" style="margin-left: 8px;">未验证</el-tag>
            <el-tag v-if="userInfo?.email && emailVerified" size="small" type="success" style="margin-left: 8px;">已验证</el-tag>
          </span>
        </div>
        <div class="profile-item">
          <span class="item-label">手机号</span>
          <span class="item-value">{{ userInfo?.phone || '-' }}</span>
        </div>
        <div class="profile-item">
          <span class="item-label">注册时间</span>
          <span class="item-value">{{ formattedCreatedAt }}</span>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button type="primary" block size="large" @click="goToEdit">
          编辑资料
        </el-button>
        <el-button type="warning" block size="large" @click="goToChangePassword">
          修改密码
        </el-button>
        <el-button type="danger" plain block size="large" @click="goToDeleteAccount">
          注销账号
        </el-button>
      </div>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import MobileLayout from '@/layouts/MobileLayout.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const userInfo = computed(() => authStore.userInfo)
const emailVerified = computed(() => authStore.userInfo?.emailVerified)

const userInitial = computed(() => {
  const name = userInfo.value?.nickname || userInfo.value?.username || ''
  return name.charAt(0).toUpperCase()
})

const formattedCreatedAt = computed(() => {
  if (!userInfo.value?.createdAt) return '-'
  return new Date(userInfo.value.createdAt).toLocaleDateString('zh-CN')
})

const goToEdit = () => {
  router.push('/user/profile/edit')
}

const goToChangePassword = () => {
  router.push('/user/password')
}

const goToDeleteAccount = () => {
  router.push('/user/delete-account')
}

onMounted(() => {
  // 刷新用户信息
  authStore.getUserInfo()
})
</script>

<style scoped>
.profile-page {
  min-height: calc(100vh - 60px);
  background: #F5F5F5;
  padding-bottom: 24px;
}

/* 头部卡片 */
.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px 16px;
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
  margin-bottom: 16px;
}

.avatar-section {
  flex-shrink: 0;
}

.user-name-section h2 {
  font-size: 20px;
  font-weight: 600;
  color: #1F2937;
  margin: 0 0 4px 0;
}

.user-name-section p {
  font-size: 14px;
  color: #6B7280;
  margin: 0;
}

/* 信息列表 */
.profile-list {
  background: #FFFFFF;
  margin: 0 16px 16px;
  border-radius: 12px;
  overflow: hidden;
}

.profile-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #F3F4F6;
}

.profile-item:last-child {
  border-bottom: none;
}

.item-label {
  font-size: 14px;
  color: #6B7280;
}

.item-value {
  font-size: 15px;
  color: #1F2937;
  display: flex;
  align-items: center;
}

/* 操作按钮 */
.action-buttons {
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
