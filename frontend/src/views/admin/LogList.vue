<template>
  <AdminLayout>
    <div class="admin-log-page">
      <!-- 页面头部 -->
      <div class="page-header">
        <h1 class="page-title">日志管理</h1>
      </div>

      <!-- 统计卡片 -->
      <el-row :gutter="16" class="stats-row">
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">今日日志</div>
              <div class="stat-value">{{ stats.todayCount }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">本周日志</div>
              <div class="stat-value">{{ stats.weekCount }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">总日志数</div>
              <div class="stat-value">{{ stats.totalCount }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover">
            <div class="stat-card">
              <div class="stat-label">失败操作</div>
              <div class="stat-value error">{{ stats.statusStats?.FAIL || 0 }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 筛选栏 -->
      <div class="filter-section">
        <el-form :inline="true" :model="filterForm">
          <el-form-item label="模块">
            <el-select v-model="filterForm.module" placeholder="全部模块" clearable style="width: 120px">
              <el-option v-for="mod in modules" :key="mod" :label="getModuleText(mod)" :value="mod" />
            </el-select>
          </el-form-item>
          <el-form-item label="操作类型">
            <el-select v-model="filterForm.action" placeholder="全部类型" clearable style="width: 120px">
              <el-option v-for="act in actions" :key="act" :label="getActionText(act)" :value="act" />
            </el-select>
          </el-form-item>
          <el-form-item label="操作人">
            <el-input v-model="filterForm.operatorName" placeholder="操作人用户名" clearable style="width: 150px" />
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="filterForm.operatorRole" placeholder="全部角色" clearable style="width: 100px">
              <el-option label="买家" value="BUYER" />
              <el-option label="卖家" value="SELLER" />
              <el-option label="管理员" value="ADMIN" />
              <el-option label="系统" value="SYSTEM" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="filterForm.status" placeholder="全部状态" clearable style="width: 100px">
              <el-option label="成功" value="SUCCESS" />
              <el-option label="失败" value="FAIL" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 240px"
            />
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="filterForm.keyword" placeholder="描述/URL关键词" clearable style="width: 150px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 操作栏 -->
      <div class="action-bar">
        <el-button type="danger" plain @click="handleCleanup" :loading="cleaning">
          <el-icon><Delete /></el-icon>
          清理旧日志
        </el-button>
        <el-button type="primary" plain @click="handleExport">
          <el-icon><Download /></el-icon>
          导出日志
        </el-button>
      </div>

      <!-- 日志表格 -->
      <el-card class="table-card">
        <el-table
          :data="tableData"
          v-loading="loading"
          border
          stripe
          style="width: 100%"
          :default-sort="{ prop: 'createTime', order: 'descending' }"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="module" label="模块" width="100">
            <template #default="{ row }">
              <el-tag>{{ getModuleText(row.module) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="action" label="操作" width="100">
            <template #default="{ row }">
              <span>{{ getActionText(row.action) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="operatorName" label="操作人" width="120">
            <template #default="{ row }">
              <span v-if="row.operatorName">{{ row.operatorName }}</span>
              <el-tag v-else size="small" type="info">系统</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="operatorRole" label="角色" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="getRoleTagType(row.operatorRole)">
                {{ getRoleText(row.operatorRole) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="getStatusTagType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ip" label="IP地址" width="130" show-overflow-tooltip />
          <el-table-column prop="duration" label="耗时" width="80">
            <template #default="{ row }">
              <span v-if="row.duration">{{ row.duration }}ms</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="时间" width="160" sortable>
            <template #default="{ row }">
              {{ formatDate(row.createTime) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" text type="primary" @click="viewDetail(row)">
                详情
              </el-button>
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
            @size-change="fetchLogList"
            @current-change="fetchLogList"
          />
        </div>
      </el-card>

      <!-- 详情对话框 -->
      <el-dialog v-model="detailDialogVisible" title="日志详情" width="700px">
        <el-descriptions :column="2" border v-if="currentLog">
          <el-descriptions-item label="ID">{{ currentLog.id }}</el-descriptions-item>
          <el-descriptions-item label="模块">{{ getModuleText(currentLog.module) }}</el-descriptions-item>
          <el-descriptions-item label="操作">{{ getActionText(currentLog.action) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(currentLog.status)" size="small">
              {{ getStatusText(currentLog.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作人">{{ currentLog.operatorName || '系统' }}</el-descriptions-item>
          <el-descriptions-item label="角色">{{ getRoleText(currentLog.operatorRole) }}</el-descriptions-item>
          <el-descriptions-item label="IP地址" :span="2">{{ currentLog.ip || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求方法">{{ currentLog.requestMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ currentLog.duration ? currentLog.duration + 'ms' : '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求URL" :span="2">
            <span class="url-text">{{ currentLog.requestUrl || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentLog.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="错误信息" :span="2" v-if="currentLog.message">
            <span class="error-text">{{ currentLog.message }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="请求参数" :span="2" v-if="currentLog.requestParams">
            <pre class="params-text">{{ currentLog.requestParams }}</pre>
          </el-descriptions-item>
          <el-descriptions-item label="User-Agent" :span="2">
            <span class="user-agent-text">{{ currentLog.userAgent || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ formatDate(currentLog.createTime) }}</el-descriptions-item>
        </el-descriptions>
      </el-dialog>

      <!-- 清理确认对话框 -->
      <el-dialog v-model="cleanupDialogVisible" title="清理旧日志" width="500px">
        <el-form :model="cleanupForm" label-width="120px">
          <el-form-item label="清理此日期之前的日志">
            <el-date-picker
              v-model="cleanupForm.before"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item>
            <el-alert type="warning" :closable="false">
              清理操作不可逆，请谨慎操作！
            </el-alert>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="cleanupDialogVisible = false">取消</el-button>
          <el-button type="danger" @click="confirmCleanup" :loading="cleaning">确认清理</el-button>
        </template>
      </el-dialog>
    </div>
  </AdminLayout>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download } from '@element-plus/icons-vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import {
  getLogList,
  getLogStats,
  cleanupOldLogs,
  getModules,
  getActions,
  type OperationLog,
  type LogQueryRequest,
  type LogStats,
  Module,
  ActionType,
  OperatorRole,
  getModuleText,
  getActionText,
  getRoleText,
  getStatusText,
  getStatusTagType
} from '@/api/log'

const loading = ref(false)
const cleaning = ref(false)
const tableData = ref<OperationLog[]>([])
const stats = ref<LogStats>({
  todayCount: 0,
  weekCount: 0,
  totalCount: 0,
  moduleStats: {},
  statusStats: {}
})
const modules = ref<string[]>(Object.values(Module))
const actions = ref<string[]>(Object.values(ActionType))

const filterForm = reactive<LogQueryRequest>({
  module: undefined,
  action: undefined,
  operatorName: '',
  operatorRole: undefined,
  status: undefined,
  keyword: ''
})

const dateRange = ref<string[]>([])

const pagination = reactive({
  page: 0,
  size: 20,
  total: 0
})

const detailDialogVisible = ref(false)
const cleanupDialogVisible = ref(false)
const currentLog = ref<OperationLog | null>(null)
const cleanupForm = reactive({
  before: ''
})

// 获取日志列表
const fetchLogList = async () => {
  loading.value = true
  try {
    const params: LogQueryRequest = {
      page: pagination.page,
      size: pagination.size,
      module: filterForm.module || undefined,
      action: filterForm.action || undefined,
      operatorName: filterForm.operatorName || undefined,
      operatorRole: filterForm.operatorRole || undefined,
      status: filterForm.status || undefined,
      keyword: filterForm.keyword || undefined,
      startTime: dateRange.value[0] || undefined,
      endTime: dateRange.value[1] || undefined
    }

    const response = await getLogList(params)
    const data = response.data.data

    tableData.value = data.content
    pagination.total = data.totalElements
  } catch (error: any) {
    console.error('获取日志列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取统计数据
const fetchStats = async () => {
  try {
    const response = await getLogStats()
    stats.value = response.data.data
  } catch (error: any) {
    console.error('获取日志统计失败:', error)
  }
}

// 获取模块列表
const fetchModules = async () => {
  try {
    const response = await getModules()
    modules.value = response.data.data
  } catch (error: any) {
    console.error('获取模块列表失败:', error)
  }
}

// 获取操作类型列表
const fetchActions = async () => {
  try {
    const response = await getActions()
    actions.value = response.data.data
  } catch (error: any) {
    console.error('获取操作类型列表失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 0
  fetchLogList()
}

// 重置
const handleReset = () => {
  filterForm.module = undefined
  filterForm.action = undefined
  filterForm.operatorName = ''
  filterForm.operatorRole = undefined
  filterForm.status = undefined
  filterForm.keyword = ''
  dateRange.value = []
  pagination.page = 0
  fetchLogList()
}

// 查看详情
const viewDetail = (row: OperationLog) => {
  currentLog.value = row
  detailDialogVisible.value = true
}

// 清理旧日志
const handleCleanup = () => {
  cleanupForm.before = ''
  cleanupDialogVisible.value = true
}

// 确认清理
const confirmCleanup = async () => {
  if (!cleanupForm.before) {
    ElMessage.warning('请选择清理日期')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确定要清理此日期之前的所有日志吗？此操作不可逆！',
      '警告',
      {
        confirmButtonText: '确认清理',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    cleaning.value = true
    await cleanupOldLogs(cleanupForm.before)
    ElMessage.success('日志清理完成')
    cleanupDialogVisible.value = false
    fetchLogList()
    fetchStats()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('清理日志失败:', error)
    }
  } finally {
    cleaning.value = false
  }
}

// 导出日志
const handleExport = () => {
  ElMessage.info('导出功能开发中')
}

// 获取角色标签类型
const getRoleTagType = (role: OperatorRole | undefined): string => {
  switch (role) {
    case OperatorRole.ADMIN:
      return 'warning'
    case OperatorRole.SELLER:
      return 'success'
    case OperatorRole.BUYER:
      return 'primary'
    case OperatorRole.SYSTEM:
      return 'info'
    default:
      return 'info'
  }
}

// 格式化日期
const formatDate = (dateString: string | undefined) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  fetchLogList()
  fetchStats()
  fetchModules()
  fetchActions()
})
</script>

<style scoped>
.admin-log-page {
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

.stat-value.error {
  color: #EF4444;
}

.filter-section {
  background: #FFFFFF;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.action-bar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}

.table-card {
  background: #FFFFFF;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.url-text {
  word-break: break-all;
  font-size: 12px;
  color: #409EFF;
}

.error-text {
  color: #F56C6C;
  font-size: 12px;
}

.params-text {
  max-height: 150px;
  overflow-y: auto;
  margin: 0;
  padding: 8px;
  background: #F5F7FA;
  border-radius: 4px;
  font-size: 12px;
  word-break: break-all;
  white-space: pre-wrap;
}

.user-agent-text {
  word-break: break-all;
  font-size: 12px;
  color: #909399;
}
</style>
