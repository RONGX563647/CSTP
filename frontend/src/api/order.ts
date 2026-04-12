import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

// ==================== 类型定义 ====================

// 订单状态枚举
export enum OrderStatus {
  PENDING_PAYMENT = 'PENDING_PAYMENT',   // 待付款
  PENDING_PICKUP = 'PENDING_PICKUP',     // 待提货
  PENDING_CONFIRM = 'PENDING_CONFIRM',   // 待确认
  PENDING_REVIEW = 'PENDING_REVIEW',     // 待评价
  COMPLETED = 'COMPLETED',               // 已完成
  CANCELLED = 'CANCELLED',               // 已取消
  REFUNDED = 'REFUNDED'                  // 已退款
}

// 取消方枚举
export enum CancelRole {
  BUYER = 'BUYER',
  SELLER = 'SELLER',
  SYSTEM = 'SYSTEM'
}

// 评价类型枚举
export enum ReviewType {
  BUYER_REVIEW = 'BUYER_REVIEW',
  SELLER_REVIEW = 'SELLER_REVIEW'
}

// 订单接口
export interface Order {
  id: number
  orderNo: string
  buyerId: number
  buyerName?: string
  buyerPhone?: string
  sellerId: number
  sellerName?: string
  sellerPhone?: string
  productId: number
  productName: string
  productImage?: string
  price: number
  quantity: number
  totalAmount: number
  meetLocation?: string
  meetTime?: string
  buyerRemark?: string
  status: OrderStatus
  paymentTime?: string
  pickupTime?: string
  confirmTime?: string
  completeTime?: string
  cancelTime?: string
  cancelReason?: string
  cancelRole?: CancelRole
  adminRemark?: string
  createdAt: string
  updatedAt: string
  hasBuyerReview?: boolean
  hasSellerReview?: boolean
}

// 订单评价接口
export interface OrderReview {
  id: number
  orderId: number
  reviewerId: number
  reviewerName?: string
  revieweeId: number
  revieweeName?: string
  productId?: number
  rating: number
  content: string
  replyContent?: string
  replyTime?: string
  reviewType: ReviewType
  createdAt: string
  updatedAt: string
}

// 订单日志接口
export interface OrderLog {
  id: number
  orderId: number
  operatorId?: number
  operatorRole: 'BUYER' | 'SELLER' | 'SYSTEM' | 'ADMIN'
  action: string
  fromStatus?: OrderStatus
  toStatus?: OrderStatus
  remark?: string
  createTime: string
}

// ==================== 请求参数类型 ====================

export interface OrderListParams {
  page?: number
  size?: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

export interface OrderCreateRequest {
  productId: number
  quantity?: number
  meetLocation?: string
  meetTime?: string
  buyerRemark?: string
}

export interface OrderReviewRequest {
  rating: number
  content: string
}

export interface OrderStats {
  totalOrders: number
  pendingPayment: number
  pendingPickup: number
  pendingConfirm: number
  pendingReview: number
  completed: number
  cancelled: number
}

// ==================== 用户端订单 API ====================

/**
 * 创建订单
 */
export const createOrder = (data: OrderCreateRequest) => {
  return request<ApiResponse<Order>>({
    url: '/user/orders',
    method: 'post',
    data
  })
}

/**
 * 获取我的订单列表（我买的）
 */
export const getMyBuyerOrders = (params?: OrderListParams) => {
  return request<ApiResponse<{
    content: Order[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>>({
    url: '/user/orders/my/buyer',
    method: 'get',
    params
  })
}

/**
 * 获取我的订单列表（我卖的）
 */
export const getMySellerOrders = (params?: OrderListParams) => {
  return request<ApiResponse<{
    content: Order[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>>({
    url: '/user/orders/my/seller',
    method: 'get',
    params
  })
}

/**
 * 获取订单详情
 */
export const getOrderById = (id: number) => {
  return request<ApiResponse<Order>>({
    url: `/user/orders/${id}`,
    method: 'get'
  })
}

/**
 * 买家确认付款
 */
export const payOrder = (id: number) => {
  return request<ApiResponse<Order>>({
    url: `/user/orders/${id}/pay`,
    method: 'put'
  })
}

/**
 * 买家确认提货
 */
export const confirmPickup = (id: number) => {
  return request<ApiResponse<Order>>({
    url: `/user/orders/${id}/pickup`,
    method: 'put'
  })
}

/**
 * 卖家确认收款
 */
export const confirmPayment = (id: number) => {
  return request<ApiResponse<Order>>({
    url: `/user/orders/${id}/confirm`,
    method: 'put'
  })
}

/**
 * 取消订单
 */
export const cancelOrder = (id: number, reason?: string) => {
  return request<ApiResponse<Order>>({
    url: `/user/orders/${id}/cancel`,
    method: 'put',
    params: reason ? { reason } : {}
  })
}

/**
 * 提交评价
 */
export const createReview = (orderId: number, data: OrderReviewRequest, type: ReviewType) => {
  return request<ApiResponse<OrderReview>>({
    url: `/user/orders/${orderId}/review`,
    method: 'post',
    params: { type },
    data
  })
}

/**
 * 获取订单评价列表
 */
export const getOrderReviews = (orderId: number) => {
  return request<ApiResponse<OrderReview[]>>({
    url: `/user/orders/${orderId}/reviews`,
    method: 'get'
  })
}

/**
 * 回复评价
 */
export const replyReview = (reviewId: number, content: string) => {
  return request<ApiResponse<OrderReview>>({
    url: `/user/orders/review/${reviewId}/reply`,
    method: 'put',
    data: { content }
  })
}

/**
 * 获取我的评价列表
 */
export const getMyReviews = () => {
  return request<ApiResponse<OrderReview[]>>({
    url: '/user/orders/reviews/my',
    method: 'get'
  })
}

/**
 * 获取我收到的评价
 */
export const getReceivedReviews = () => {
  return request<ApiResponse<OrderReview[]>>({
    url: '/user/orders/reviews/received',
    method: 'get'
  })
}

// ==================== 管理端订单 API ====================

/**
 * 获取所有订单列表
 */
export const getAllOrders = (params?: OrderListParams) => {
  return request<ApiResponse<{
    content: Order[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>>({
    url: '/admin/orders',
    method: 'get',
    params
  })
}

/**
 * 获取订单详情
 */
export const getAdminOrderById = (id: number) => {
  return request<ApiResponse<Order>>({
    url: `/admin/orders/${id}`,
    method: 'get'
  })
}

/**
 * 多条件查询订单
 */
export const searchOrders = (params?: {
  orderNo?: string
  buyerId?: number
  sellerId?: number
  status?: OrderStatus
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}) => {
  return request<ApiResponse<{
    content: Order[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>>({
    url: '/admin/orders/query',
    method: 'get',
    params
  })
}

/**
 * 管理员修改订单状态
 */
export const updateOrderStatus = (id: number, status: OrderStatus) => {
  return request<ApiResponse<Order>>({
    url: `/admin/orders/${id}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 添加管理员备注
 */
export const updateAdminRemark = (id: number, remark: string) => {
  return request<ApiResponse<Order>>({
    url: `/admin/orders/${id}/remark`,
    method: 'put',
    params: { remark }
  })
}

/**
 * 获取订单统计
 */
export const getOrderStats = () => {
  return request<ApiResponse<OrderStats>>({
    url: '/admin/orders/stats',
    method: 'get'
  })
}

/**
 * 获取订单日志
 */
export const getOrderLogs = (id: number) => {
  return request<ApiResponse<OrderLog[]>>({
    url: `/admin/orders/${id}/logs`,
    method: 'get'
  })
}

// ==================== 工具函数 ====================

/**
 * 获取订单状态文本
 */
export const getOrderStatusText = (status: OrderStatus): string => {
  const statusMap: Record<OrderStatus, string> = {
    [OrderStatus.PENDING_PAYMENT]: '待付款',
    [OrderStatus.PENDING_PICKUP]: '待提货',
    [OrderStatus.PENDING_CONFIRM]: '待确认',
    [OrderStatus.PENDING_REVIEW]: '待评价',
    [OrderStatus.COMPLETED]: '已完成',
    [OrderStatus.CANCELLED]: '已取消',
    [OrderStatus.REFUNDED]: '已退款'
  }
  return statusMap[status]
}

/**
 * 获取订单状态颜色
 */
export const getOrderStatusColor = (status: OrderStatus): string => {
  const colorMap: Record<OrderStatus, string> = {
    [OrderStatus.PENDING_PAYMENT]: 'warning',
    [OrderStatus.PENDING_PICKUP]: 'primary',
    [OrderStatus.PENDING_CONFIRM]: 'success',
    [OrderStatus.PENDING_REVIEW]: 'info',
    [OrderStatus.COMPLETED]: 'success',
    [OrderStatus.CANCELLED]: 'danger',
    [OrderStatus.REFUNDED]: 'info'
  }
  return colorMap[status]
}
