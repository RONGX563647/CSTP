import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

// 类型定义
export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
  phone?: string
  nickname?: string
}

export interface AuthResponse {
  token: string
  tokenType: string
  id: number
  username: string
  nickname: string
  avatar: string
  role: string
}

export interface UserInfo {
  id: number
  username: string
  email?: string
  phone?: string
  nickname: string
  avatar?: string
  status: string
}

export interface UpdateProfileRequest {
  nickname: string
  avatar?: string
  email?: string
  phone?: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

/**
 * 用户登录
 */
export const userLogin = (data: LoginRequest) => {
  return request<ApiResponse<AuthResponse>>({
    url: '/auth/login',
    method: 'post',
    data
  })
}

/**
 * 用户注册
 */
export const userRegister = (data: RegisterRequest) => {
  return request<ApiResponse<AuthResponse>>({
    url: '/auth/register',
    method: 'post',
    data
  })
}

/**
 * 获取当前用户信息
 */
export const getUserInfo = () => {
  return request<ApiResponse<UserInfo>>({
    url: '/auth/me',
    method: 'get'
  })
}

/**
 * 管理员登录
 */
export const adminLogin = (data: LoginRequest) => {
  return request<ApiResponse<AuthResponse>>({
    url: '/admin/auth/login',
    method: 'post',
    data
  })
}

/**
 * 更新用户信息
 */
export const updateProfile = (data: UpdateProfileRequest) => {
  return request<ApiResponse<UserInfo>>({
    url: '/user/profile',
    method: 'put',
    data
  })
}

/**
 * 修改密码
 */
export const changePassword = (data: ChangePasswordRequest) => {
  return request<ApiResponse<void>>({
    url: '/user/password',
    method: 'put',
    data
  })
}

/**
 * 删除账号
 */
export const deleteAccount = (password: string) => {
  return request<ApiResponse<void>>({
    url: '/user/delete-account',
    method: 'post',
    params: { password }
  })
}
