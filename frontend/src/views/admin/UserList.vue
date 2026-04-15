<template>
  <AdminLayout>
    <div class="admin-user-list-page">
      <!-- 页面头部 -->
      <div class="page-header">
        <h1 class="page-title">用户管理</h1>
      </div>

      <!-- 筛选栏 -->
      <div class="filter-section">
        <el-form :inline="true" :model="filterForm">
          <el-form-item label="用户名">
            <el-input v-model="filterForm.username" placeholder="请输入用户名" clearable />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="filterForm.nickname" placeholder="请输入昵称" clearable />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="filterForm.email" placeholder="请输入邮箱" clearable />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="filterForm.phone" placeholder="请输入手机号" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filterForm.status" placeholder="全部状态" clearable>
              <el-option label="正常" value="ACTIVE" />
              <el-option label="未激活" value="INACTIVE" />
              <el-option label="已禁用" value="BANNED" />
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
              <div class="stat-label">用户总数</div>
              <div class="stat-value">{{ stats.totalUsers }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">正常用户</div>
              <div class="stat-value active">{{ stats.activeUsers }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">未激活</div>
              <div class="stat-value inactive">{{ stats.inactiveUsers }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">已禁用</div>
              <div class="stat-value banned">{{ stats.bannedUsers }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 用户表格 -->
      <el-card class="table-card">
        <el-table
          :data="tableData"
          v-loading="loading"
          border
          stripe
          style="width: 100%"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="头像" width="80">
            <template #default="{ row }">
              <el-avatar :size="40" :src="row.avatar || undefined">
                {{ row.nickname?.charAt(0).toUpperCase() || row.username.charAt(0).toUpperCase() }}
              </el-avatar>
            </template>
          </el-table-column>
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="nickname" label="昵称" width="100" />
          <el-table-column prop="email" label="邮箱" width="150" />
          <el-table-column prop="phone" label="手机号" width="120" />
          <el-table-column prop="emailVerified" label="邮箱验证" width="80">
            <template #default="{ row }">
              <el-tag :type="row.emailVerified ? 'success' : 'danger'">
                {{ row.emailVerified ? '已验证' : '未验证' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="注册时间" width="160">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="viewDetail(row.id)">
                详情
              </el-button>
              <el-dropdown size="small" trigger="click">
                <el-button size="small" text>
                  更多 <el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="showResetPasswordDialog(row)">
                      重置密码
                    </el-dropdown-item>
                    <el-dropdown-item @click="showChangeStatusDialog(row)">
                      修改状态
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="handleDelete(row)" danger>
                      删除用户
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="fetchUserList"
            @current-change="fetchUserList"
          />
        </div>
      </el-card>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import {
  getUserList,
  getUserStats,
  updateUserStatus,
  resetUserPassword,
  deleteUser,
  type User,
  type UserStats,
  type UserQueryRequest,
  UserStatus
} from '@/api/user'

const router = useRouter()

const loading = ref(false)
const tableData = ref<User[]>([])
const stats = ref<UserStats>({
  totalUsers: 0,
  activeUsers: 0,
  inactiveUsers: 0,
  bannedUsers: 0
})

const filterForm = reactive<UserQueryRequest>({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  status: undefined
})

const pagination = reactive({
  page: 0,
  size: 10,
  total: 0
})

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true
  try {
    const params: UserQueryRequest = {
      page: pagination.page,
      size: pagination.size,
      username: filterForm.username || undefined,
      nickname: filterForm.nickname || undefined,
      email: filterForm.email || undefined,
      phone: filterForm.phone || undefined,
      status: filterForm.status || undefined
    }

    const response = await getUserList(params)
    const data = response.data.data

    tableData.value = data.content
    pagination.total = data.totalElements
  } catch (error: any) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取用户统计
const fetchUserStats = async () => {
  try {
    const response = await getUserStats()
    stats.value = response.data.data
  } catch (error: any) {
    console.error('获取用户统计失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 0
  fetchUserList()
}

// 重置
const handleReset = () => {
  filterForm.username = ''
  filterForm.nickname = ''
  filterForm.email = ''
  filterForm.phone = ''
  filterForm.status = undefined
  pagination.page = 0
  fetchUserList()
}

// 查看详情
const viewDetail = (id: number) => {
  router.push(`/admin/users/${id}`)
}

// 显示修改状态对话框
const showChangeStatusDialog = async (row: User) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的状态（ACTIVE/INACTIVE/BANNED）', '修改用户状态', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: row.status,
      inputPattern: /^(ACTIVE|INACTIVE|BANNED)$/,
      inputErrorMessage: '请输入有效的状态：ACTIVE、INACTIVE 或 BANNED'
    })
    await updateUserStatus(row.id, value as UserStatus)
    ElMessage.success('状态已更新')
    fetchUserList()
    fetchUserStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('更新状态失败:', error)
    }
  }
}

// 显示重置密码对话框
const showResetPasswordDialog = (row: User) => {
  ElMessageBox.prompt('请输入新的密码：', '重置密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /^.{6,20}$/,
    inputErrorMessage: '密码长度必须在 6-20 位之间'
  }).then(async ({ value }) => {
    try {
      await resetUserPassword(row.id, value)
      ElMessage.success('密码已重置为：' + value)
    } catch (error: any) {
      console.error('重置密码失败:', error)
    }
  }).catch(() => {})
}

// 删除用户
const handleDelete = (row: User) => {
  ElMessageBox.confirm(
    `确定要删除用户"${row.username}"吗？此操作将把用户状态设置为未激活。`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteUser(row.id)
      ElMessage.success('用户已删除')
      fetchUserList()
      fetchUserStats()
    } catch (error: any) {
      console.error('删除用户失败:', error)
    }
  }).catch(() => {})
}

// 获取状态标签类型
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

// 获取状态文本
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

// 格式化日期
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
  fetchUserList()
  fetchUserStats()
})
</script>

<style scoped>
.admin-user-list-page {
  padding: 20px;
}

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

.filter-section {
  background: #FFFFFF;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  text-align: center;
  padding: 8px 0;
}

.stat-label {
  font-size: 14px;
  color: #6B7280;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #1F2937;
}

.stat-value.active {
  color: #10B981;
}

.stat-value.inactive {
  color: #6B7280;
}

.stat-value.banned {
  color: #EF4444;
}

.table-card {
  background: #FFFFFF;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
