import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'
import { Product, ProductStatus } from './types'

// ==================== 请求参数类型 ====================

export interface ProductListParams {
  page?: number
  size?: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
  name?: string
  category?: string
  minPrice?: number
  maxPrice?: number
  status?: ProductStatus
  sellerId?: number
}

export interface ProductCreateRequest {
  name: string
  description?: string
  price: number
  originalPrice?: number
  stock: number
  mainImage: string
  images?: string[]
  category: string
  tags?: string[]
  isOnSale?: boolean
  isFeatured?: boolean
}

export interface ProductStats {
  totalProducts: number
  onSaleProducts: number
}

export interface UserProductStats {
  totalProducts: number
}

// ==================== 用户端公共 API（无需登录）====================

/**
 * 获取公开在售商品列表
 */
export const getPublicOnSaleProducts = (params?: ProductListParams) => {
  return request<ApiResponse<{
    content: Product[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>>({
    url: '/user/products/public/on-sale',
    method: 'get',
    params
  })
}

/**
 * 获取公开商品详情
 */
export const getPublicProductById = (id: number) => {
  return request<ApiResponse<Product>>({
    url: `/user/products/public/${id}`,
    method: 'get'
  })
}

/**
 * 获取推荐商品
 */
export const getPublicFeaturedProducts = () => {
  return request<ApiResponse<Product[]>>({
    url: '/user/products/public/featured',
    method: 'get'
  })
}

/**
 * 按分类获取商品
 */
export const getProductsByCategory = (category: string, params?: { page?: number; size?: number }) => {
  return request<ApiResponse<{
    content: Product[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>>({
    url: `/user/products/public/category/${category}`,
    method: 'get',
    params
  })
}

/**
 * 搜索商品（公开）
 */
export const searchPublicProducts = (params?: ProductListParams) => {
  return request<ApiResponse<{
    content: Product[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>>({
    url: '/user/products/public/search',
    method: 'get',
    params
  })
}

// ==================== 用户端已登录 API ====================

/**
 * 获取我的商品列表
 */
export const getMyProducts = (params?: ProductListParams) => {
  return request<ApiResponse<{
    content: Product[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>>({
    url: '/user/products/my',
    method: 'get',
    params
  })
}

/**
 * 获取我的商品详情
 */
export const getMyProductById = (id: number) => {
  return request<ApiResponse<Product>>({
    url: `/user/products/${id}`,
    method: 'get'
  })
}

/**
 * 发布商品
 */
export const createProduct = (data: ProductCreateRequest) => {
  return request<ApiResponse<Product>>({
    url: '/user/products',
    method: 'post',
    data
  })
}

/**
 * 更新商品
 */
export const updateProduct = (id: number, data: ProductCreateRequest) => {
  return request<ApiResponse<Product>>({
    url: `/user/products/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除商品
 */
export const deleteProduct = (id: number) => {
  return request<ApiResponse<void>>({
    url: `/user/products/${id}`,
    method: 'delete'
  })
}

/**
 * 更新商品库存
 */
export const updateStock = (id: number, stock: number) => {
  return request<ApiResponse<Product>>({
    url: `/user/products/${id}/stock`,
    method: 'put',
    params: { stock }
  })
}

/**
 * 更新商品状态
 */
export const updateProductStatus = (id: number, status: ProductStatus) => {
  return request<ApiResponse<Product>>({
    url: `/user/products/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 获取我的商品统计
 */
export const getMyProductStats = () => {
  return request<ApiResponse<ProductStats>>({
    url: '/user/products/stats',
    method: 'get'
  })
}

// ==================== 管理端 API ====================

/**
 * 获取所有商品（管理端）
 */
export const getAllProducts = (params?: ProductListParams) => {
  return request<ApiResponse<{
    content: Product[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>>({
    url: '/admin/products',
    method: 'get',
    params
  })
}

/**
 * 获取商品详情（管理端）
 */
export const getProductById = (id: number) => {
  return request<ApiResponse<Product>>({
    url: `/admin/products/${id}`,
    method: 'get'
  })
}

/**
 * 创建商品（管理端）
 */
export const createProductForAdmin = (data: ProductCreateRequest) => {
  return request<ApiResponse<Product>>({
    url: '/admin/products',
    method: 'post',
    data
  })
}

/**
 * 更新商品（管理端）
 */
export const updateProductForAdmin = (id: number, data: ProductCreateRequest) => {
  return request<ApiResponse<Product>>({
    url: `/admin/products/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除商品（管理端）
 */
export const deleteProductForAdmin = (id: number) => {
  return request<ApiResponse<void>>({
    url: `/admin/products/${id}`,
    method: 'delete'
  })
}

/**
 * 查看指定用户的商品
 */
export const getProductsByUser = (userId: number, params?: { page?: number; size?: number }) => {
  return request<ApiResponse<{
    content: Product[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>>({
    url: `/admin/products/user/${userId}`,
    method: 'get',
    params
  })
}

/**
 * 获取用户商品统计
 */
export const getUserProductStats = (userId: number) => {
  return request<ApiResponse<UserProductStats>>({
    url: `/admin/products/user/${userId}/stats`,
    method: 'get'
  })
}

/**
 * 设置商品上下架状态
 */
export const setSaleStatus = (id: number, isOnSale: boolean) => {
  return request<ApiResponse<Product>>({
    url: `/admin/products/${id}/sale-status`,
    method: 'put',
    params: { isOnSale }
  })
}

/**
 * 设置商品推荐状态
 */
export const setFeaturedStatus = (id: number, isFeatured: boolean) => {
  return request<ApiResponse<Product>>({
    url: `/admin/products/${id}/featured`,
    method: 'put',
    params: { isFeatured }
  })
}

/**
 * 多条件查询商品
 */
export const queryProducts = (params?: ProductListParams) => {
  return request<ApiResponse<{
    content: Product[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>>({
    url: '/admin/products/query',
    method: 'get',
    params
  })
}

/**
 * 查询售罄商品
 */
export const getOutOfStockProducts = (params?: { page?: number; size?: number }) => {
  return request<ApiResponse<{
    content: Product[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>>({
    url: '/admin/products/out-of-stock',
    method: 'get',
    params
  })
}

/**
 * 查询下架商品
 */
export const getOffSaleProducts = (params?: { page?: number; size?: number }) => {
  return request<ApiResponse<{
    content: Product[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>>({
    url: '/admin/products/off-sale',
    method: 'get',
    params
  })
}

/**
 * 获取商品统计（管理端）
 */
export const getProductStats = () => {
  return request<ApiResponse<ProductStats>>({
    url: '/admin/products/stats',
    method: 'get'
  })
}
