<template>
  <MobileLayout title="修改密码">
    <div class="change-password-page">
      <div class="form-container">
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
          <!-- 原密码 -->
          <el-form-item label="原密码" prop="oldPassword">
            <el-input
              v-model="form.oldPassword"
              type="password"
              placeholder="请输入原密码"
              show-password
            />
          </el-form-item>

          <!-- 新密码 -->
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="form.newPassword"
              type="password"
              placeholder="请输入新密码"
              show-password
            />
          </el-form-item>

          <!-- 确认新密码 -->
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              show-password
            />
          </el-form-item>
        </el-form>

        <el-alert
          type="info"
          description="修改密码后需要重新登录"
          show-icon
          :closable="false"
          style="margin-top: 16px;"
        />
      </div>

      <!-- 保存按钮 -->
      <div class="action-buttons">
        <el-button type="primary" size="large" block @click="handleSubmit" :loading="loading">
          确认修改
        </el-button>
      </div>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import MobileLayout from '@/layouts/MobileLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { changePassword } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 自定义验证器：确认密码
const validateConfirmPassword = (_rule: any, value: string, callback: Function) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单验证规则
const rules = computed<FormRules>(() => ({
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度必须在 6-20 位之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}))

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await changePassword({
        oldPassword: form.oldPassword,
        newPassword: form.newPassword
      })

      ElMessage.success('密码修改成功，请重新登录')

      // 退出登录并跳转
      authStore.logout()
      setTimeout(() => {
        router.push('/login')
      }, 1000)
    } catch (error: any) {
      // 错误已由拦截器处理
      console.error('修改密码失败:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.change-password-page {
  min-height: calc(100vh - 60px);
  background: #F5F5F5;
  padding-bottom: 24px;
}

.form-container {
  background: #FFFFFF;
  margin: 16px;
  padding: 16px;
  border-radius: 12px;
}

.action-buttons {
  padding: 0 16px;
  margin-top: 16px;
}
</style>
