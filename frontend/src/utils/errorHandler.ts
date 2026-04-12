/**
 * 错误处理器
 * 统一处理 API 错误并展示给用户
 */

import { ElMessage } from 'element-plus'
import type { ErrorResponse } from '@/api/types'
import {
  getUserFriendlyMessage,
  shouldRedirectToLogin,
  HTTP_STATUS_MAP
} from './error'
import router from '@/router'

/**
 * 处理 API 错误
 * @param error 错误响应对象
 * @param context 错误发生的上下文（可选）
 */
export function handleApiError(error: ErrorResponse | unknown, context?: string) {
  // 开发环境打印错误详情
  if (import.meta.env?.DEV) {
    console.error(`[API Error]${context ? ' ' + context : ''}:`, error)
  }

  // 处理已知格式的错误响应
  if (error && typeof error === 'object' && 'code' in error) {
    const errorResponse = error as ErrorResponse
    const userMessage = getUserFriendlyMessage(
      errorResponse.code,
      errorResponse.message
    )

    // 需要跳转登录的错误
    if (shouldRedirectToLogin(errorResponse.code)) {
      ElMessage.error(userMessage)
      clearAuthAndRedirect()
      return
    }

    // 根据错误类型选择提示方式
    const messageType = getErrorType(errorResponse.code)
    if (messageType === 'error') {
      ElMessage.error(userMessage)
    } else if (messageType === 'warning') {
      ElMessage.warning(userMessage)
    } else {
      ElMessage.info(userMessage)
    }
    return
  }

  // 处理 Axios 错误
  if (error && typeof error === 'object' && 'response' in error) {
    const axiosError = error as {
      response?: {
        status?: number
        data?: ErrorResponse
      }
    }
    handleHttpError(axiosError, context)
    return
  }

  // 处理未知错误
  handleUnknownError(error, context)
}

/**
 * 处理 HTTP 错误
 */
function handleHttpError(
  error: { response?: { status?: number; data?: ErrorResponse } },
  context?: string
) {
  const status = error.response?.status
  const errorData = error.response?.data

  // 优先使用后端返回的错误信息
  if (errorData?.message) {
    handleApiError(errorData, context)
    return
  }

  // 使用 HTTP 状态码映射
  if (status && HTTP_STATUS_MAP[status]) {
    const { type, message } = HTTP_STATUS_MAP[status]
    if (type === 'error') {
      ElMessage.error(message)
    } else if (type === 'warning') {
      ElMessage.warning(message)
    } else {
      ElMessage.info(message)
    }

    // 401 需要跳转登录
    if (status === 401) {
      clearAuthAndRedirect()
    }
    return
  }

  // 默认错误消息
  ElMessage.error('网络请求失败，请稍后重试')
}

/**
 * 处理未知错误
 */
function handleUnknownError(error: unknown, context?: string) {
  // 网络错误
  if (error instanceof TypeError && error.message.includes('fetch')) {
    ElMessage.error('网络连接失败，请检查网络设置')
    return
  }

  // 超时错误
  if (error && typeof error === 'object' && 'code' in error && (error as any).code === 'ECONNABORTED') {
    ElMessage.warning('请求超时，请重试')
    return
  }

  // 其他错误
  ElMessage.error(context ? `${context}失败，请稍后重试` : '操作失败，请稍后重试')
}

/**
 * 清除认证信息并跳转登录页
 */
function clearAuthAndRedirect() {
  localStorage.removeItem('token')
  // 使用 router.push 而不是 window.location，保持 SPA 体验
  router.replace({
    path: '/login',
    query: { redirect: encodeURIComponent(router.currentRoute.value.fullPath) }
  })
}

/**
 * 获取错误类型
 */
function getErrorType(code: string | number): 'error' | 'warning' | 'info' {
  const codeStr = String(code)
  if (codeStr.startsWith('1099')) {
    return 'error'
  }
  if (codeStr.startsWith('1003') || codeStr.startsWith('2002')) {
    return 'error'
  }
  return 'warning'
}
