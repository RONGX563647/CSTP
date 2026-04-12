import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  // 重定向
  {
    path: '/',
    redirect: '/user/home'
  },
  // 用户端路由
  {
    path: '/user/home',
    name: 'UserHome',
    component: () => import('@/views/user/Home.vue'),
    meta: { requiresAuth: true, title: '首页' }
  },
  {
    path: '/user/addresses',
    name: 'UserAddressList',
    component: () => import('@/views/user/AddressList.vue'),
    meta: { requiresAuth: true, title: '收货地址' }
  },
  {
    path: '/user/addresses/new',
    name: 'UserAddressNew',
    component: () => import('@/views/user/AddressForm.vue'),
    meta: { requiresAuth: true, title: '新建地址' }
  },
  {
    path: '/user/addresses/:id',
    name: 'UserAddressEdit',
    component: () => import('@/views/user/AddressForm.vue'),
    meta: { requiresAuth: true, title: '编辑地址' }
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
    path: '/admin',
    redirect: '/admin/login'
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
      next({ name: 'UserHome' })
    } else {
      next()
    }
  }
  else {
    next()
  }
})

export default router
