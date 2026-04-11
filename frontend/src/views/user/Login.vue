<template>
  <AuthLayout>
    <el-form
      ref="formRef"
      :model="loginForm"
      :rules="formRules"
      class="login-form"
      size="large"
    >
      <h2 class="form-title">欢迎回来</h2>
      <p class="form-subtitle">请输入您的账号信息登录</p>

      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          placeholder="用户名/邮箱/手机号"
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

      <div class="form-footer">
        <span>还没有账号？</span>
        <router-link to="/register" class="link">立即注册</router-link>
      </div>

      <div class="divider">
        <span>其他登录方式</span>
      </div>

      <div class="admin-link">
        <router-link to="/admin/login" class="link">
          <el-icon><Monitor /></el-icon>
          管理员入口
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
    { required: true, message: '请输入用户名', trigger: 'blur' }
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
      await authStore.login({
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

.divider {
  display: flex;
  align-items: center;
  margin: 20px 0;
  color: #9CA3AF;
  font-size: 13px;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background-color: #E5E7EB;
}

.divider span {
  padding: 0 12px;
}

.admin-link {
  text-align: center;
  margin-top: 16px;
}

.admin-link .link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #6B7280;
  font-size: 14px;
  padding: 8px 16px;
  border: 1px solid #E5E7EB;
  border-radius: 8px;
  transition: all 0.2s;
}

.admin-link .link:hover {
  color: #F59E0B;
  border-color: #F59E0B;
  background-color: #FFFBEB;
}
</style>
