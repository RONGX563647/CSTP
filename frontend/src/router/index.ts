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
  // 用户端商品路由
  {
    path: '/user/products',
    name: 'UserProductList',
    component: () => import('@/views/user/ProductList.vue'),
    meta: { requiresAuth: false, title: '商品列表' }
  },
  {
    path: '/user/products/:id',
    name: 'UserProductDetail',
    component: () => import('@/views/user/ProductDetail.vue'),
    meta: { requiresAuth: false, title: '商品详情' }
  },
  {
    path: '/user/products/my',
    name: 'UserMyProducts',
    component: () => import('@/views/user/MyProducts.vue'),
    meta: { requiresAuth: true, title: '我的商品' }
  },
  {
    path: '/user/products/new',
    name: 'UserProductNew',
    component: () => import('@/views/user/ProductForm.vue'),
    meta: { requiresAuth: true, title: '发布商品' }
  },
  {
    path: '/user/products/:id/edit',
    name: 'UserProductEdit',
    component: () => import('@/views/user/ProductForm.vue'),
    meta: { requiresAuth: true, title: '编辑商品' }
  },
  // 用户端订单路由
  {
    path: '/user/orders/buyer',
    name: 'UserOrderList',
    component: () => import('@/views/user/OrderList.vue'),
    meta: { requiresAuth: true, title: '我的订单' }
  },
  {
    path: '/user/orders/seller',
    name: 'UserSellerOrderList',
    component: () => import('@/views/user/OrderList.vue'),
    meta: { requiresAuth: true, title: '我卖的订单' }
  },
  {
    path: '/user/orders/:id',
    name: 'UserOrderDetail',
    component: () => import('@/views/user/OrderDetail.vue'),
    meta: { requiresAuth: true, title: '订单详情' }
  },
  {
    path: '/user/orders/new/:productId',
    name: 'UserOrderForm',
    component: () => import('@/views/user/OrderForm.vue'),
    meta: { requiresAuth: true, title: '确认订单' }
  },
  {
    path: '/user/orders/:id/review',
    name: 'UserOrderReview',
    component: () => import('@/views/user/OrderReviewForm.vue'),
    meta: { requiresAuth: true, title: '订单评价' }
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
  // 管理端路由
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/Login.vue'),
    meta: { guest: true, title: '管理员登录' }
  },
  // 管理端商品管理路由
  {
    path: '/admin',
    redirect: '/admin/login'
  },
  {
    path: '/admin/products',
    name: 'AdminProductList',
    component: () => import('@/views/admin/ProductList.vue'),
    meta: { requiresAuth: true, role: 'admin', title: '商品管理' }
  },
  {
    path: '/admin/products/new',
    name: 'AdminProductNew',
    component: () => import('@/views/admin/ProductForm.vue'),
    meta: { requiresAuth: true, role: 'admin', title: '新增商品' }
  },
  {
    path: '/admin/products/:id/edit',
    name: 'AdminProductEdit',
    component: () => import('@/views/admin/ProductForm.vue'),
    meta: { requiresAuth: true, role: 'admin', title: '编辑商品' }
  },
  {
    path: '/admin/products/user/:userId',
    name: 'AdminUserProducts',
    component: () => import('@/views/admin/UserProducts.vue'),
    meta: { requiresAuth: true, role: 'admin', title: '用户商品' }
  },
  // 管理端订单路由
  {
    path: '/admin/orders',
    name: 'AdminOrderList',
    component: () => import('@/views/admin/OrderList.vue'),
    meta: { requiresAuth: true, role: 'admin', title: '订单管理' }
  },
  {
    path: '/admin/orders/:id',
    name: 'AdminOrderDetail',
    component: () => import('@/views/admin/OrderDetail.vue'),
    meta: { requiresAuth: true, role: 'admin', title: '订单详情' }
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
