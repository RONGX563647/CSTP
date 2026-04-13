<template>
  <div class="multi-image-upload">
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
      multiple
    >
      <div class="upload-trigger" :class="{ 'is-uploading': uploading }">
        <el-icon v-if="!uploading" class="upload-icon"><Plus /></el-icon>
        <el-icon v-else class="upload-icon loading"><Loading /></el-icon>
        <span class="upload-text">{{ uploading ? '上传中...' : placeholder }}</span>
      </div>
    </el-upload>
    
    <div v-if="imageUrls.length > 0" class="image-list">
      <div v-for="(url, index) in imageUrls" :key="index" class="image-item">
        <el-image :src="url" fit="cover" class="preview-image" />
        <el-button v-if="!disabled" size="small" text type="danger" @click="handleRemove(index)">
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete, Loading } from '@element-plus/icons-vue'
import type { UploadInstance, UploadRawFile } from 'element-plus'

interface Props {
  modelValue?: string[]
  maxSize?: number
  accept?: string
  placeholder?: string
  disabled?: boolean
  maxCount?: number
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  maxSize: 10,
  accept: 'image/jpeg,image/png,image/gif,image/webp,image/bmp,image/x-icon,image/svg+xml,image/tiff,image/tif,image/psd,image/raw,image/heif,image/heic',
  placeholder: '点击上传图片（可多选）',
  disabled: false,
  maxCount: 9
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string[]): void
  (e: 'success', data: { url: string; fileName: string; size: number }): void
  (e: 'error', message: string): void
}>()

const uploadRef = ref<UploadInstance>()
const uploading = ref(false)
const imageUrls = ref<string[]>([...props.modelValue])

const uploadUrl = '/api/user/products/upload/images'

const headers = computed(() => {
  const token = localStorage.getItem('token')
  return {
    Authorization: token || ''
  }
})

const acceptTypes = computed(() => props.accept)

watch(() => props.modelValue, (val) => {
  imageUrls.value = [...val]
})

const beforeUpload = (file: UploadRawFile) => {
  if (imageUrls.value.length >= props.maxCount) {
    ElMessage.warning(`最多只能上传 ${props.maxCount} 张图片`)
    return false
  }
  
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
    if (Array.isArray(data)) {
      const newUrls = data.map(item => item.url)
      imageUrls.value = [...imageUrls.value, ...newUrls]
      emit('update:modelValue', imageUrls.value)
      ElMessage.success(`成功上传 ${data.length} 张图片`)
    } else {
      imageUrls.value = [...imageUrls.value, data.url]
      emit('update:modelValue', imageUrls.value)
      emit('success', data)
      ElMessage.success('上传成功')
    }
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

const handleRemove = (index: number) => {
  imageUrls.value.splice(index, 1)
  emit('update:modelValue', imageUrls.value)
}
</script>

<style scoped>
.multi-image-upload {
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
  height: 100px;
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
  font-size: 28px;
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

.image-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  background: #F9FAFB;
}

.image-item .el-button {
  position: absolute;
  top: 4px;
  right: 4px;
  background: rgba(255, 255, 255, 0.9);
}

.preview-image {
  width: 100%;
  height: 100%;
}
</style>