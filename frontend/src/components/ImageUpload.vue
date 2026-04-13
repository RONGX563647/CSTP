<template>
  <div class="image-upload">
    <el-upload
      ref="uploadRef"
      :action="uploadUrl"
      :headers="headers"
      :show-file-list="false"
      :before-upload="beforeUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      :accept="acceptTypes"
      :disabled="disabled || uploading"
    >
      <slot>
        <div class="upload-trigger" :class="{ 'is-uploading': uploading }">
          <el-icon v-if="!uploading" class="upload-icon"><Plus /></el-icon>
          <el-icon v-else class="upload-icon loading"><Loading /></el-icon>
          <span class="upload-text">{{ uploading ? '上传中...' : placeholder }}</span>
        </div>
      </slot>
    </el-upload>
    
    <div v-if="imageUrl && showPreview" class="preview-container">
      <el-image :src="imageUrl" fit="cover" class="preview-image" />
      <el-button v-if="!disabled" size="small" text type="danger" @click="handleRemove">
        <el-icon><Delete /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete, Loading } from '@element-plus/icons-vue'
import type { UploadInstance, UploadRawFile } from 'element-plus'

interface Props {
  modelValue?: string
  uploadType?: 'avatar' | 'product-main' | 'product-detail'
  maxSize?: number
  accept?: string
  placeholder?: string
  showPreview?: boolean
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  uploadType: 'avatar',
  maxSize: 10,
  accept: 'image/jpeg,image/png,image/gif,image/webp',
  placeholder: '点击上传图片',
  showPreview: true,
  disabled: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'success', data: { url: string; fileName: string; size: number }): void
  (e: 'error', message: string): void
}>()

const uploadRef = ref<UploadInstance>()
const uploading = ref(false)
const imageUrl = ref(props.modelValue)

const uploadUrl = computed(() => {
  const baseUrl = '/api'
  switch (props.uploadType) {
    case 'avatar':
      return `${baseUrl}/user/avatar`
    case 'product-main':
      return `${baseUrl}/user/products/upload/main-image`
    case 'product-detail':
      return `${baseUrl}/user/products/upload/images`
    default:
      return `${baseUrl}/user/avatar`
  }
})

const headers = computed(() => {
  const token = localStorage.getItem('token')
  return {
    Authorization: token || ''
  }
})

const acceptTypes = computed(() => props.accept)

watch(() => props.modelValue, (val) => {
  imageUrl.value = val
})

const beforeUpload = (file: UploadRawFile) => {
  const isImage = props.accept.split(',').some(type => {
    if (type.includes('*')) {
      return file.type.startsWith('image/')
    }
    return file.type === type
  })
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  
  const isLtMaxSize = file.size / 1024 / 1024 < props.maxSize
  if (!isLtMaxSize) {
    ElMessage.error(`图片大小不能超过 ${props.maxSize}MB`)
    return false
  }
  
  uploading.value = true
  return true
}

const handleSuccess = (response: any) => {
  uploading.value = false
  if (response.code === 200) {
    const data = response.data
    imageUrl.value = data.url
    emit('update:modelValue', data.url)
    emit('success', data)
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
    emit('error', response.message)
  }
}

const handleError = () => {
  uploading.value = false
  ElMessage.error('上传失败，请重试')
  emit('error', '上传失败')
}

const handleRemove = () => {
  imageUrl.value = ''
  emit('update:modelValue', '')
}
</script>

<style scoped>
.image-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upload-trigger {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 120px;
  border: 1px dashed #D9D9D9;
  border-radius: 8px;
  background: #FAFAFA;
  cursor: pointer;
  transition: all 0.3s;
}

.upload-trigger:hover {
  border-color: #F59E0B;
  background: #FEF3C7;
}

.upload-trigger.is-uploading {
  border-color: #F59E0B;
  background: #FEF3C7;
  cursor: not-allowed;
}

.upload-icon {
  font-size: 32px;
  color: #8C939D;
}

.upload-icon.loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.upload-text {
  margin-top: 8px;
  font-size: 14px;
  color: #8C939D;
}

.preview-container {
  position: relative;
  display: flex;
  justify-content: center;
  background: #F9FAFB;
  padding: 16px;
  border-radius: 8px;
}

.preview-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
}

.preview-container .el-button {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(255, 255, 255, 0.9);
}
</style>