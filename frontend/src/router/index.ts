import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  // 重定向
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { requiresAuth: true }
  },
  // 用户认证路由
  {
    path: '/login',
    name: 'UserLogin',
    component: () => import('@/views/user/Login.vue'),
    meta: { guest: true, title: '用户登录' }
  },
  {
    path: '/register',
    name: 'UserRegister',
    component: () => import('@/views/user/Register.vue'),
    meta: { guest: true, title: '用户注册' }
  },
  // 管理员认证路由
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/Login.vue'),
    meta: { guest: true, title: '管理员登录' }
  },
  {
    path: '/admin/register',
    name: 'AdminRegister',
    component: () => import('@/views/admin/Register.vue'),
    meta: { guest: true, title: '管理员注册' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - AiSale`
  } else {
    document.title = 'AiSale'
  }

  // 需要认证的路由
  if (to.meta.requiresAuth) {
    if (!authStore.isLoggedIn) {
      next({ name: 'UserLogin', query: { redirect: to.fullPath } })
    } else {
      next()
    }
  }
  // 仅限访客（已登录用户不能访问登录/注册页）
  else if (to.meta.guest) {
    if (authStore.isLoggedIn) {
      next({ name: 'Home' })
    } else {
      next()
    }
  }
  else {
    next()
  }
})

export default router
