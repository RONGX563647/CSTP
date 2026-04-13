<template>
  <MobileLayout title="编辑资料">
    <div class="profile-edit-page">
      <div class="form-container">
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
          <!-- 头像上传 -->
          <el-form-item label="头像" prop="avatar">
            <ImageUpload
              v-model="form.avatar"
              upload-type="avatar"
              :max-size="10"
              placeholder="点击上传头像"
              @success="handleAvatarSuccess"
            />
          </el-form-item>

          <!-- 昵称 -->
          <el-form-item label="昵称" prop="nickname">
            <el-input
              v-model="form.nickname"
              placeholder="请输入昵称"
              maxlength="20"
              show-word-limit
            />
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
import ImageUpload from '@/components/ImageUpload.vue'
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

const userInfo = computed(() => authStore.userInfo)

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

const handleAvatarSuccess = (data: { url: string; fileName: string; size: number }) => {
  console.log('头像上传成功:', data)
}

onMounted(() => {
  if (userInfo.value) {
    form.nickname = userInfo.value.nickname || ''
    form.avatar = userInfo.value.avatar || ''
    form.email = userInfo.value.email || ''
    form.phone = userInfo.value.phone || ''
    originalEmail.value = userInfo.value.email || ''
  }
})

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

      await authStore.getUserInfo()

      ElMessage.success('保存成功')
      router.back()
    } catch (error: any) {
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

.action-buttons {
  padding: 0 16px;
  margin-top: 16px;
}
</style>