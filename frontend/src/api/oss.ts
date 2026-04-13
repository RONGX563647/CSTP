import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

export interface ImageUploadResponse {
  url: string
  fileName: string
  size: number
}

export interface OssConfig {
  bucketName: string
  allowedExtensions: string[]
  ossEnabled: boolean
  maxFileSize: string
}

export const uploadAvatar = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request<ApiResponse<ImageUploadResponse>>({
    url: '/user/avatar',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const uploadProductMainImage = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request<ApiResponse<ImageUploadResponse>>({
    url: '/user/products/upload/main-image',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const uploadProductImages = (files: File[]) => {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })
  return request<ApiResponse<ImageUploadResponse[]>>({
    url: '/user/products/upload/images',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const getOssConfig = () => {
  return request<ApiResponse<OssConfig>>({
    url: '/user/oss/config',
    method: 'get'
  })
}