<template>
  <AdminLayout>
    <div class="admin-user-detail-page">
      <!-- 返回按钮 -->
      <div class="page-header">
        <div class="breadcrumb">
          <el-link @click="goBack">用户管理</el-link>
          <span class="separator">/</span>
          <span>用户详情</span>
        </div>
      </div>

      <el-row :gutter="20" v-loading="loading">
        <!-- 用户信息卡片 -->
        <el-col :span="8">
          <el-card class="user-info-card">
            <template #header>
              <span>基本信息</span>
            </template>
            <div class="user-avatar-wrapper">
              <el-avatar :size="100" :src="user?.avatar || undefined">
                {{ user?.nickname?.charAt(0).toUpperCase() || user?.username.charAt(0).toUpperCase() }}
              </el-avatar>
            </div>
            <div class="info-item">
              <span class="label">用户名：</span>
              <span class="value">{{ user?.username }}</span>
            </div>
            <div class="info-item">
              <span class="label">昵称：</span>
              <span class="value">{{ user?.nickname || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">邮箱：</span>
              <span class="value">{{ user?.email || '-' }}</span>
              <el-tag v-if="user?.emailVerified" size="small" type="success" style="margin-left: 8px;">已验证</el-tag>
            </div>
            <div class="info-item">
              <span class="label">手机号：</span>
              <span class="value">{{ user?.phone || '-' }}</span>
            </div>
            <div class="info-item">
              <span class="label">状态：</span>
              <el-tag :type="getStatusTagType(user?.status || UserStatus.ACTIVE)">
                {{ getStatusText(user?.status || UserStatus.ACTIVE) }}
              </el-tag>
            </div>
            <div class="info-item">
              <span class="label">注册时间：</span>
              <span class="value">{{ formatDate(user?.createdAt || '') }}</span>
            </div>
            <div class="info-item">
              <span class="label">更新时间：</span>
              <span class="value">{{ formatDate(user?.updatedAt || '') }}</span>
            </div>
          </el-card>
        </el-col>

        <!-- 操作面板 -->
        <el-col :span="16">
          <el-card class="action-card">
            <template #header>
              <span>管理操作</span>
            </template>

            <!-- 修改状态 -->
            <div class="action-section">
              <h4>修改用户状态</h4>
              <el-radio-group v-model="newStatus" size="default">
                <el-radio-button value="ACTIVE">正常</el-radio-button>
                <el-radio-button value="INACTIVE">未激活</el-radio-button>
                <el-radio-button value="BANNED">已禁用</el-radio-button>
              </el-radio-group>
              <el-button type="primary" @click="handleChangeStatus" :loading="actionLoading">
                确认修改
              </el-button>
            </div>

            <!-- 重置密码 -->
            <div class="action-section">
              <h4>重置密码</h4>
              <el-input
                v-model="newPassword"
                type="password"
                placeholder="请输入新密码"
                show-password
                style="width: 300px; margin-right: 12px;"
              />
              <el-button type="warning" @click="handleResetPassword" :loading="actionLoading">
                重置密码
              </el-button>
            </div>

            <!-- 删除用户 -->
            <div class="action-section danger-section">
              <h4>危险操作</h4>
              <el-button type="danger" @click="handleDelete" :loading="actionLoading">
                删除用户（软删除）
              </el-button>
              <p class="danger-tip">删除后用户状态将变为未激活，数据不会被物理删除</p>
            </div>
          </el-card>

          <!-- 用户订单统计 -->
          <el-card class="stats-card" style="margin-top: 20px;">
            <template #header>
              <span>订单统计</span>
            </template>
            <el-descriptions :column="3" border>
              <el-descriptions-item label="作为买家">
                {{ userStats.buyerOrders }}
              </el-descriptions-item>
              <el-descriptions-item label="作为卖家">
                {{ userStats.sellerOrders }}
              </el-descriptions-item>
              <el-descriptions-item label="完成订单">
                {{ userStats.completedOrders }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AdminLayout from '@/layouts/AdminLayout.vue'
import { getUserById, updateUserStatus, resetUserPassword, deleteUser, type User, UserStatus } from '@/api/user'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const actionLoading = ref(false)
const user = ref<User | null>(null)
const newStatus = ref<UserStatus>(UserStatus.ACTIVE)
const newPassword = ref('')

const userStats = ref({
  buyerOrders: 0,
  sellerOrders: 0,
  completedOrders: 0
})

// 获取用户详情
const fetchUserDetail = async () => {
  const userId = route.params.id as string
  if (!userId) return

  loading.value = true
  try {
    const response = await getUserById(Number(userId))
    user.value = response.data.data
    newStatus.value = user.value.status
  } catch (error: any) {
    console.error('获取用户详情失败:', error)
  } finally {
    loading.value = false
  }
}

// 修改状态
const handleChangeStatus = async () => {
  if (!user.value) return

  actionLoading.value = true
  try {
    await updateUserStatus(user.value.id, newStatus.value)
    ElMessage.success('状态已更新')
    fetchUserDetail()
  } catch (error: any) {
    console.error('更新状态失败:', error)
  } finally {
    actionLoading.value = false
  }
}

// 重置密码
const handleResetPassword = async () => {
  if (!user.value) return

  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('密码长度至少 6 位')
    return
  }

  actionLoading.value = true
  try {
    await resetUserPassword(user.value.id, newPassword.value)
    ElMessage.success(`密码已重置为：${newPassword.value}`)
    newPassword.value = ''
  } catch (error: any) {
    console.error('重置密码失败:', error)
  } finally {
    actionLoading.value = false
  }
}

// 删除用户
const handleDelete = async () => {
  if (!user.value) return

  try {
    await ElMessageBox.confirm(
      `确定要删除用户"${user.value.username}"吗？`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    actionLoading.value = true
    await deleteUser(user.value.id)
    ElMessage.success('用户已删除')
    router.push('/admin/users')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除用户失败:', error)
    }
  } finally {
    actionLoading.value = false
  }
}

const goBack = () => {
  router.back()
}

const getStatusTagType = (status: UserStatus) => {
  switch (status) {
    case UserStatus.ACTIVE:
      return 'success'
    case UserStatus.INACTIVE:
      return 'info'
    case UserStatus.BANNED:
      return 'danger'
    default:
      return 'info'
  }
}

const getStatusText = (status: UserStatus) => {
  switch (status) {
    case UserStatus.ACTIVE:
      return '正常'
    case UserStatus.INACTIVE:
      return '未激活'
    case UserStatus.BANNED:
      return '已禁用'
    default:
      return '未知'
  }
}

const formatDate = (dateString: string) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  fetchUserDetail()
})
</script>

<style scoped>
.admin-user-detail-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.breadcrumb .el-link {
  cursor: pointer;
}

.separator {
  margin: 0 8px;
  color: #9CA3AF;
}

.user-info-card,
.action-card,
.stats-card {
  background: #FFFFFF;
}

.user-avatar-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #F3F4F6;
}

.info-item:last-child {
  border-bottom: none;
}

.info-item .label {
  width: 80px;
  color: #6B7280;
  font-size: 14px;
}

.info-item .value {
  color: #1F2937;
  font-size: 14px;
}

.action-section {
  margin-bottom: 24px;
}

.action-section h4 {
  font-size: 16px;
  font-weight: 500;
  color: #1F2937;
  margin-bottom: 12px;
}

.danger-section {
  border: 1px solid #FEE2E2;
  background: #FEF2F2;
  padding: 16px;
  border-radius: 8px;
}

.danger-tip {
  font-size: 13px;
  color: #EF4444;
  margin-top: 8px;
  margin-bottom: 0;
}
</style>
