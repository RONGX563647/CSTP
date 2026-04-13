<template>
  <MobileLayout title="编辑资料">
    <div class="profile-edit-page">
      <div class="form-container">
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
          <!-- 昵称 -->
          <el-form-item label="昵称" prop="nickname">
            <el-input
              v-model="form.nickname"
              placeholder="请输入昵称"
              maxlength="20"
              show-word-limit
            />
          </el-form-item>

          <!-- 头像 URL -->
          <el-form-item label="头像 URL" prop="avatar">
            <el-input
              v-model="form.avatar"
              placeholder="请输入头像图片链接（可选）"
            />
            <div v-if="form.avatar" class="avatar-preview">
              <el-avatar :size="64" :src="form.avatar" />
            </div>
          </el-form-item>

          <!-- 邮箱 -->
          <el-form-item label="邮箱" prop="email">
            <el-input
              v-model="form.email"
              type="email"
              placeholder="请输入邮箱地址（可选）"
            />
            <el-alert
              v-if="originalEmail && form.email !== originalEmail"
              type="warning"
              description="修改邮箱后需要重新验证"
              show-icon
              :closable="false"
              style="margin-top: 8px;"
            />
          </el-form-item>

          <!-- 手机号 -->
          <el-form-item label="手机号" prop="phone">
            <el-input
              v-model="form.phone"
              placeholder="请输入手机号（可选）"
              maxlength="11"
            />
          </el-form-item>
        </el-form>
      </div>

      <!-- 保存按钮 -->
      <div class="action-buttons">
        <el-button type="primary" size="large" block @click="handleSubmit" :loading="loading">
          保存修改
        </el-button>
      </div>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import MobileLayout from '@/layouts/MobileLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { updateProfile } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  nickname: '',
  avatar: '',
  email: '',
  phone: ''
})

const originalEmail = ref('')

// 用户信息
const userInfo = computed(() => authStore.userInfo)

// 表单验证规则
const rules = computed<FormRules>(() => ({
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 20, message: '昵称不能超过 20 个字符', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}))

// 填充表单
onMounted(() => {
  if (userInfo.value) {
    form.nickname = userInfo.value.nickname || ''
    form.avatar = userInfo.value.avatar || ''
    form.email = userInfo.value.email || ''
    form.phone = userInfo.value.phone || ''
    originalEmail.value = userInfo.value.email || ''
  }
})

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await updateProfile({
        nickname: form.nickname,
        avatar: form.avatar || undefined,
        email: form.email || undefined,
        phone: form.phone || undefined
      })

      // 更新 store 中的用户信息
      await authStore.getUserInfo()

      ElMessage.success('保存成功')
      router.back()
    } catch (error: any) {
      // 错误已由拦截器处理
      console.error('保存失败:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.profile-edit-page {
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

.avatar-preview {
  margin-top: 16px;
  display: flex;
  justify-content: center;
}

.action-buttons {
  padding: 0 16px;
  margin-top: 16px;
}
</style>
