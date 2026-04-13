import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

// ==================== 类型定义 ====================

export interface User {
  id: number
  username: string
  nickname: string
  avatar: string
  email: string
  phone: string
  status: UserStatus
  emailVerified: boolean
  createdAt: string
  updatedAt: string
}

export enum UserStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  BANNED = 'BANNED'
}

export interface UserQueryRequest {
  page?: number
  size?: number
  username?: string
  nickname?: string
  email?: string
  phone?: string
  status?: UserStatus
}

export interface UserStats {
  totalUsers: number
  activeUsers: number
  inactiveUsers: number
  bannedUsers: number
}

// ==================== 管理端用户 API ====================

/**
 * 获取用户列表
 */
export const getUserList = (params?: UserQueryRequest) => {
  return request<ApiResponse<{
    content: User[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>>({
    url: '/admin/users',
    method: 'get',
    params
  })
}

/**
 * 获取用户详情
 */
export const getUserById = (id: number) => {
  return request<ApiResponse<User>>({
    url: `/admin/users/${id}`,
    method: 'get'
  })
}

/**
 * 更新用户状态
 */
export const updateUserStatus = (id: number, status: UserStatus) => {
  return request<ApiResponse<User>>({
    url: `/admin/users/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 重置用户密码
 */
export const resetUserPassword = (id: number, newPassword: string) => {
  return request<ApiResponse<void>>({
    url: `/admin/users/${id}/reset-password`,
    method: 'put',
    params: { newPassword }
  })
}

/**
 * 删除用户
 */
export const deleteUser = (id: number) => {
  return request<ApiResponse<void>>({
    url: `/admin/users/${id}`,
    method: 'delete'
  })
}

/**
 * 获取用户统计
 */
export const getUserStats = () => {
  return request<ApiResponse<UserStats>>({
    url: '/admin/users/stats',
    method: 'get'
  })
}

/**
 * 获取用户订单统计
 */
export const getUserOrderStats = (id: number) => {
  return request<ApiResponse<{
    buyerOrders: number
    sellerOrders: number
    completedOrders: number
  }>>({
    url: `/admin/users/${id}/order-stats`,
    method: 'get'
  })
}
