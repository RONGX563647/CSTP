<template>
  <AuthLayout>
    <el-form
      ref="formRef"
      :model="loginForm"
      :rules="formRules"
      class="login-form"
      size="large"
    >
      <div class="admin-badge">
        <el-icon :size="20"><Monitor /></el-icon>
        <span>管理员入口</span>
      </div>

      <h2 class="form-title">管理员登录</h2>
      <p class="form-subtitle">请使用管理员账号登录系统</p>

      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          placeholder="管理员账号"
          prefix-icon="User"
          clearable
        />
      </el-form-item>

      <el-form-item prop="password">
        <el-input
          v-model="loginForm.password"
          type="password"
          placeholder="密码"
          prefix-icon="Lock"
          show-password
          @keyup.enter="handleLogin"
        />
      </el-form-item>

      <el-form-item>
        <div class="form-options">
          <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
          <el-link type="primary" :underline="false">忘记密码？</el-link>
        </div>
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          class="login-btn"
          :loading="loading"
          @click="handleLogin"
        >
          {{ loading ? '登录中...' : '登录' }}
        </el-button>
      </el-form-item>

      <div class="back-link">
        <router-link to="/login" class="link">
          <el-icon><ArrowLeft /></el-icon>
          返回用户登录
        </router-link>
      </div>
    </el-form>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import AuthLayout from '@/layouts/AuthLayout.vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const formRules: FormRules = {
  username: [
    { required: true, message: '请输入管理员账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await authStore.adminLogin({
        username: loginForm.username,
        password: loginForm.password
      })

      ElMessage.success('登录成功')

      // 跳转到重定向页面或首页
      const redirect = route.query.redirect as string
      router.push(redirect || '/home')
    } catch (error) {
      console.error('登录失败:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-form {
  width: 100%;
}

.admin-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: linear-gradient(135deg, #FEF3C7 0%, #FDE68A 100%);
  padding: 8px 16px;
  border-radius: 20px;
  margin-bottom: 16px;
  width: fit-content;
  margin-left: auto;
  margin-right: auto;
}

.admin-badge span {
  font-size: 14px;
  font-weight: 500;
  color: #92400E;
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

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  font-weight: 500;
  background: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
  border: none;
  border-radius: 12px;
}

.login-btn:hover {
  opacity: 0.9;
}

.back-link {
  text-align: center;
  margin-top: 16px;
}

.back-link .link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #6B7280;
  font-size: 14px;
  padding: 8px 16px;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  transition: all 0.2s;
}

.back-link .link:hover {
  color: #F59E0B;
  border-color: #F59E0B;
  background-color: #FFFBEB;
}
</style>
