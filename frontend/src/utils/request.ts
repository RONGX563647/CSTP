import axios, { AxiosInstance, AxiosResponse } from 'axios'
import { handleApiError } from '@/utils/errorHandler'
import type { ErrorResponse } from '@/api/types'

// 响应数据结构
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 创建 Axios 实例
const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 自动添加 Token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器 - 统一错误处理
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    const res = response.data

    // 如果响应码不是 200，使用错误处理器显示错误
    if (res.code !== 200) {
      handleApiError({ code: res.code, message: res.message } as ErrorResponse)
      return Promise.reject(res)
    }

    return response
  },
  (error) => {
    // 使用错误处理器统一处理
    handleApiError(error)
    return Promise.reject(error)
  }
)

export default request
