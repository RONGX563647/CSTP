<template>
  <MobileLayout title="注销账号">
    <div class="delete-account-page">
      <!-- 警告提示 -->
      <div class="warning-section">
        <el-alert
          title="账号注销警告"
          type="error"
          :closable="false"
          show-icon
        >
          <template #default>
            <p>账号注销后将在 7 天后被永久删除，且无法恢复。</p>
            <p style="margin-top: 8px;">注销前请确保：</p>
            <ul style="margin-top: 4px; padding-left: 20px;">
              <li>所有订单已完成或已取消</li>
              <li>没有进行中的交易</li>
              <li>已备份重要数据</li>
            </ul>
          </template>
        </el-alert>
      </div>

      <!-- 密码确认 -->
      <div class="form-container">
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
          <el-form-item label="当前密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入当前密码确认身份"
              show-password
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- 确认文字 -->
      <div class="confirm-text">
        <el-checkbox v-model="confirmed">
          我已了解账号注销的风险，确认要注销账号
        </el-checkbox>
      </div>

      <!-- 注销按钮 -->
      <div class="action-buttons">
        <el-button
          type="danger"
          size="large"
          block
          @click="handleDelete"
          :loading="loading"
          :disabled="!confirmed"
        >
          确认注销账号
        </el-button>
      </div>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import MobileLayout from '@/layouts/MobileLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { deleteAccount } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const confirmed = ref(false)

const form = reactive({
  password: ''
})

// 表单验证规则
const rules = computed<FormRules>(() => ({
  password: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ]
}))

// 删除账号
const handleDelete = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    // 二次确认
    try {
      await ElMessageBox.confirm(
        '此操作将注销您的账号，账号注销后将在 7 天后被永久删除。确认继续？',
        '危险操作',
        {
          confirmButtonText: '确认注销',
          cancelButtonText: '取消',
          type: 'error'
        }
      )
    } catch {
      return
    }

    loading.value = true
    try {
      await deleteAccount(form.password)

      ElMessage.success('账号已注销')

      // 清除本地数据并跳转首页
      authStore.logout()
      setTimeout(() => {
        router.push('/')
      }, 1000)
    } catch (error: any) {
      // 错误已由拦截器处理
      console.error('注销账号失败:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.delete-account-page {
  min-height: calc(100vh - 60px);
  background: #F5F5F5;
  padding-bottom: 24px;
}

.warning-section {
  padding: 16px;
}

.form-container {
  background: #FFFFFF;
  margin: 16px;
  padding: 16px;
  border-radius: 12px;
}

.confirm-text {
  padding: 0 16px 16px;
}

.confirm-text :deep(.el-checkbox) {
  color: #EF4444;
  font-weight: 500;
}

.action-buttons {
  padding: 0 16px;
}
</style>
