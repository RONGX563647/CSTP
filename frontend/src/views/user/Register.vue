<template>
  <AuthLayout>
    <el-form
      ref="formRef"
      :model="registerForm"
      :rules="formRules"
      class="register-form"
      size="large"
    >
      <h2 class="form-title">创建账号</h2>
      <p class="form-subtitle">注册即可享受 AI 智能销售服务</p>

      <el-form-item prop="username">
        <el-input
          v-model="registerForm.username"
          placeholder="用户名（3-20 个字符）"
          prefix-icon="User"
          clearable
          maxlength="20"
        />
      </el-form-item>

      <el-form-item prop="password">
        <el-input
          v-model="registerForm.password"
          type="password"
          placeholder="密码（6-40 个字符）"
          prefix-icon="Lock"
          show-password
          maxlength="40"
        />
      </el-form-item>

      <el-form-item prop="confirmPassword">
        <el-input
          v-model="registerForm.confirmPassword"
          type="password"
          placeholder="确认密码"
          prefix-icon="Lock"
          show-password
        />
      </el-form-item>

      <el-form-item prop="nickname">
        <el-input
          v-model="registerForm.nickname"
          placeholder="昵称（可选）"
          prefix-icon="Edit"
          clearable
        />
      </el-form-item>

      <el-form-item prop="email">
        <el-input
          v-model="registerForm.email"
          placeholder="邮箱（可选）"
          prefix-icon="Message"
          clearable
        />
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          class="register-btn"
          :loading="loading"
          @click="handleRegister"
        >
          {{ loading ? '注册中...' : '立即注册' }}
        </el-button>
      </el-form-item>

      <div class="form-footer">
        <span>已有账号？</span>
        <router-link to="/login" class="link">立即登录</router-link>
      </div>

      <div class="agreement">
        <el-checkbox v-model="agreeAgreement">
          我已阅读并同意
          <el-link type="primary" :underline="false">用户协议</el-link>
          和
          <el-link type="primary" :underline="false">隐私政策</el-link>
        </el-checkbox>
      </div>
    </el-form>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import AuthLayout from '@/layouts/AuthLayout.vue'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const agreeAgreement = ref(false)

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: ''
})

// 自定义验证器：确认密码
const validateConfirmPassword = (_rule: any, value: string, callback: any) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const formRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度必须在 3-20 个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 40, message: '密码长度必须在 6-40 个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    {
      type: 'email',
      message: '请输入有效的邮箱地址',
      trigger: 'blur'
    }
  ]
}

const handleRegister = async () => {
  if (!formRef.value) return

  // 检查是否同意协议
  if (!agreeAgreement.value) {
    ElMessage.warning('请先同意用户协议和隐私政策')
    return
  }

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await authStore.register({
        username: registerForm.username,
        password: registerForm.password,
        nickname: registerForm.nickname || undefined,
        email: registerForm.email || undefined
      })

      ElMessage.success('注册成功')
      router.push('/home')
    } catch (error) {
      console.error('注册失败:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.register-form {
  width: 100%;
}

.form-title {
  font-size: 22px;
  font-weight: 600;
  color: #1F2937;
  text-align: center;
  margin-bottom: 8px;
}

.form-subtitle {
  font-size: 14px;
  color: #6B7280;
  text-align: center;
  margin-bottom: 24px;
}

.register-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
  border: none;
  border-radius: 12px;
}

.register-btn:hover {
  opacity: 0.9;
}

.form-footer {
  text-align: center;
  color: #6B7280;
  font-size: 14px;
  margin-top: 16px;
}

.form-footer .link {
  color: #F59E0B;
  font-weight: 500;
  margin-left: 4px;
}

.form-footer .link:hover {
  color: #D97706;
}

.agreement {
  margin-top: 16px;
  font-size: 13px;
}

.agreement :deep(.el-checkbox) {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  color: #6B7280;
  line-height: 1.5;
}
</style>
