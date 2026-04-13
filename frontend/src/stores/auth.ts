import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request, { type ApiResponse } from '@/utils/request'

// 用户信息类型
interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  role: string
  phone?: string
  email?: string
  emailVerified?: boolean
  status?: string
  createdAt?: string
}

// 登录请求参数类型
interface LoginParams {
  username: string
  password: string
}

// 注册请求参数类型
interface RegisterParams {
  username: string
  password: string
  email?: string
  phone?: string
  nickname?: string
}

// 认证响应数据类型
interface AuthResponse {
  id: number
  username: string
  nickname: string
  avatar: string
  role: string
  token: string
  tokenType: string
}

export const useAuthStore = defineStore('auth', () => {
  // State
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const userRole = computed(() => userInfo.value?.role || '')
  const isUser = computed(() => userInfo.value?.role === 'USER')
  const isAdmin = computed(() =>
    userInfo.value?.role === 'ADMIN' || userInfo.value?.role === 'SUPER_ADMIN'
  )

  // Actions
  /**
   * 设置 Token
   */
  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  /**
   * 设置用户信息
   */
  const setUserInfo = (info: UserInfo) => {
    userInfo.value = info
  }

  /**
   * 用户登录
   */
  const login = async (params: LoginParams) => {
    const response = await request.post('/auth/login', { username: params.username, password: params.password })
    const res = response.data as unknown as ApiResponse<AuthResponse>
    const authData = res.data
    setToken(authData.tokenType + ' ' + authData.token)
    setUserInfo({
      id: authData.id,
      username: authData.username,
      nickname: authData.nickname,
      avatar: authData.avatar,
      role: authData.role
    })
    return res
  }

  /**
   * 管理员登录
   */
  const adminLogin = async (params: LoginParams) => {
    const response = await request.post('/admin/auth/login', { username: params.username, password: params.password })
    const res = response.data as unknown as ApiResponse<AuthResponse>
    const authData = res.data
    setToken(authData.tokenType + ' ' + authData.token)
    setUserInfo({
      id: authData.id,
      username: authData.username,
      nickname: authData.nickname,
      avatar: authData.avatar,
      role: authData.role
    })
    return res
  }

  /**
   * 用户注册
   */
  const register = async (params: RegisterParams) => {
    const response = await request.post('/auth/register', params)
    const res = response.data as unknown as ApiResponse<AuthResponse>
    const authData = res.data
    setToken(authData.tokenType + ' ' + authData.token)
    setUserInfo({
      id: authData.id,
      username: authData.username,
      nickname: authData.nickname,
      avatar: authData.avatar,
      role: authData.role
    })
    return res
  }

  /**
   * 获取用户信息
   */
  const getUserInfo = async () => {
    if (!token.value) return

    try {
      const response = await request.get<UserInfo>('/auth/me')
      const res = response.data as unknown as ApiResponse<UserInfo>
      setUserInfo(res.data)
      return res
    } catch (error) {
      console.error('获取用户信息失败:', error)
      throw error
    }
  }

  /**
   * 更新用户信息
   */
  const updateProfile = async (data: { nickname: string; avatar?: string; email?: string; phone?: string }) => {
    try {
      const response = await request.put<UserInfo>('/user/profile', data)
      const res = response.data as unknown as ApiResponse<UserInfo>
      setUserInfo(res.data)
      return res
    } catch (error) {
      console.error('更新用户信息失败:', error)
      throw error
    }
  }

  /**
   * 退出登录
   */
  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return {
    // State
    token,
    userInfo,
    // Getters
    isLoggedIn,
    userRole,
    isUser,
    isAdmin,
    // Actions
    setToken,
    setUserInfo,
    login,
    adminLogin,
    register,
    getUserInfo,
    updateProfile,
    logout
  }
})
