import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 用户信息类型
interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  role: string
  phone?: string
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
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(params)
    })
    const result = await response.json()

    if (result.code === 200 && result.data) {
      const authData = result.data
      setToken(authData.tokenType + ' ' + authData.token)
      setUserInfo({
        id: authData.id,
        username: authData.username,
        nickname: authData.nickname,
        avatar: authData.avatar,
        role: authData.role
      })
      return result
    }
    throw new Error(result.message || '登录失败')
  }

  /**
   * 管理员登录
   */
  const adminLogin = async (params: LoginParams) => {
    const response = await fetch('/api/admin/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(params)
    })
    const result = await response.json()

    if (result.code === 200 && result.data) {
      const authData = result.data
      setToken(authData.tokenType + ' ' + authData.token)
      setUserInfo({
        id: authData.id,
        username: authData.username,
        nickname: authData.nickname,
        avatar: authData.avatar,
        role: authData.role
      })
      return result
    }
    throw new Error(result.message || '登录失败')
  }

  /**
   * 用户注册
   */
  const register = async (params: RegisterParams) => {
    const response = await fetch('/api/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(params)
    })
    const result = await response.json()

    if (result.code === 200 && result.data) {
      const authData = result.data
      setToken(authData.tokenType + ' ' + authData.token)
      setUserInfo({
        id: authData.id,
        username: authData.username,
        nickname: authData.nickname,
        avatar: authData.avatar,
        role: authData.role
      })
      return result
    }
    throw new Error(result.message || '注册失败')
  }

  /**
   * 获取用户信息
   */
  const getUserInfo = async () => {
    if (!token.value) return

    try {
      const response = await fetch('/api/auth/me', {
        method: 'GET',
        headers: {
          'Authorization': token.value
        }
      })
      const result = await response.json()

      if (result.code === 200 && result.data) {
        setUserInfo(result.data)
        return result
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
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
    logout
  }
})
