<template>
  <AuthLayout>
    <el-form
      ref="formRef"
      :model="registerForm"
      :rules="formRules"
      class="register-form"
      size="large"
    >
      <div class="admin-badge">
        <el-icon :size="20"><Monitor /></el-icon>
        <span>管理员注册</span>
      </div>

      <h2 class="form-title">创建管理员账号</h2>
      <p class="form-subtitle">请填写管理员信息创建账号</p>

      <el-form-item prop="username">
        <el-input
          v-model="registerForm.username"
          placeholder="管理员账号（3-20 个字符）"
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

      <el-form-item prop="role">
        <el-select
          v-model="registerForm.role"
          placeholder="选择角色"
          style="width: 100%"
        >
          <el-option label="普通管理员" value="ADMIN" />
          <el-option label="超级管理员" value="SUPER_ADMIN" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button
          type="primary"
          class="register-btn"
          :loading="loading"
          @click="handleRegister"
        >
          {{ loading ? '创建中...' : '创建账号' }}
        </el-button>
      </el-form-item>

      <div class="back-link">
        <router-link to="/admin/login" class="link">
          <el-icon><ArrowLeft /></el-icon>
          返回管理员登录
        </router-link>
      </div>
    </el-form>
  </AuthLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import AuthLayout from '@/layouts/AuthLayout.vue'

const router = useRouter()

const formRef = ref<FormInstance>()
const loading = ref(false)

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',
  role: 'ADMIN'
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
    { required: true, message: '请输入管理员账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度必须在 3-20 个字符之间', trigger: 'blur' }
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
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

const handleRegister = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      // 注意：这里需要调用后端的管理员创建 API
      // 目前后端只有 /api/admin/auth/create 需要认证权限
      const response = await fetch('/api/admin/auth/create', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          username: registerForm.username,
          password: registerForm.password,
          nickname: registerForm.nickname || undefined,
          email: registerForm.email || undefined,
          role: registerForm.role
        })
      })
      const result = await response.json()

      if (result.code === 200) {
        ElMessage.success('管理员账号创建成功')
        router.push('/admin/login')
      } else {
        ElMessage.error(result.message || '创建失败')
      }
    } catch (error) {
      console.error('创建失败:', error)
      ElMessage.error('创建失败，请检查网络连接')
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
