import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

// ==================== 类型定义 ====================

export interface OperationLog {
  id: number
  module: string
  action: string
  operatorId: number | null
  operatorName: string | null
  operatorRole: OperatorRole
  description: string
  requestMethod: string
  requestUrl: string
  requestParams: string | null
  status: LogStatus
  message: string | null
  duration: number | null
  ip: string | null
  userAgent: string | null
  bizId: number | null
  bizType: string | null
  createTime: string
}

export enum OperatorRole {
  BUYER = 'BUYER',
  SELLER = 'SELLER',
  ADMIN = 'ADMIN',
  SYSTEM = 'SYSTEM'
}

export enum LogStatus {
  SUCCESS = 'SUCCESS',
  FAIL = 'FAIL'
}

export enum Module {
  USER = 'USER',
  PRODUCT = 'PRODUCT',
  ORDER = 'ORDER',
  ADDRESS = 'ADDRESS',
  CHAT = 'CHAT',
  AUTH = 'AUTH',
  ADMIN = 'ADMIN',
  SYSTEM = 'SYSTEM'
}

export enum ActionType {
  LOGIN = 'LOGIN',
  LOGOUT = 'LOGOUT',
  REGISTER = 'REGISTER',
  CREATE = 'CREATE',
  UPDATE = 'UPDATE',
  DELETE = 'DELETE',
  VIEW = 'VIEW',
  QUERY = 'QUERY',
  EXPORT = 'EXPORT',
  IMPORT = 'IMPORT',
  ENABLE = 'ENABLE',
  DISABLE = 'DISABLE',
  APPROVE = 'APPROVE',
  REJECT = 'REJECT',
  CANCEL = 'CANCEL',
  REFUND = 'REFUND',
  SHIP = 'SHIP',
  CONFIRM = 'CONFIRM'
}

export interface LogQueryRequest {
  page?: number
  size?: number
  operatorId?: number
  operatorRole?: OperatorRole
  module?: string
  action?: string
  status?: LogStatus
  operatorName?: string
  startTime?: string
  endTime?: string
  keyword?: string
}

export interface LogStats {
  todayCount: number
  weekCount: number
  totalCount: number
  moduleStats: Record<string, number>
  statusStats: Record<string, number>
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

// ==================== API 函数 ====================

/**
 * 获取日志列表（分页）
 */
export const getLogList = (params?: LogQueryRequest) => {
  return request<ApiResponse<PageResponse<OperationLog>>>({
    url: '/admin/logs',
    method: 'get',
    params
  })
}

/**
 * 获取日志详情
 */
export const getLogById = (id: number) => {
  return request<ApiResponse<OperationLog>>({
    url: `/admin/logs/${id}`,
    method: 'get'
  })
}

/**
 * 获取日志统计
 */
export const getLogStats = () => {
  return request<ApiResponse<LogStats>>({
    url: '/admin/logs/stats',
    method: 'get'
  })
}

/**
 * 获取最近日志
 */
export const getRecentLogs = (limit: number = 10) => {
  return request<ApiResponse<OperationLog[]>>({
    url: '/admin/logs/recent',
    method: 'get',
    params: { limit }
  })
}

/**
 * 删除旧日志
 */
export const cleanupOldLogs = (before: string) => {
  return request<ApiResponse<void>>({
    url: '/admin/logs/cleanup',
    method: 'delete',
    params: { before }
  })
}

/**
 * 获取模块列表
 */
export const getModules = () => {
  return request<ApiResponse<string[]>>({
    url: '/admin/logs/modules',
    method: 'get'
  })
}

/**
 * 获取操作类型列表
 */
export const getActions = (module?: string) => {
  return request<ApiResponse<string[]>>({
    url: '/admin/logs/actions',
    method: 'get',
    params: { module }
  })
}

// ==================== 工具函数 ====================

/**
 * 获取模块中文名称
 */
export const getModuleText = (module: string): string => {
  const moduleMap: Record<string, string> = {
    'USER': '用户模块',
    'PRODUCT': '商品模块',
    'ORDER': '订单模块',
    'ADDRESS': '地址模块',
    'CHAT': '聊天模块',
    'AUTH': '认证模块',
    'ADMIN': '管理模块',
    'SYSTEM': '系统模块'
  }
  return moduleMap[module] || module
}

/**
 * 获取操作类型中文名称
 */
export const getActionText = (action: string): string => {
  const actionMap: Record<string, string> = {
    'LOGIN': '登录',
    'LOGOUT': '登出',
    'REGISTER': '注册',
    'CREATE': '创建',
    'UPDATE': '更新',
    'DELETE': '删除',
    'VIEW': '查看',
    'QUERY': '查询',
    'EXPORT': '导出',
    'IMPORT': '导入',
    'ENABLE': '启用',
    'DISABLE': '禁用',
    'APPROVE': '审批通过',
    'REJECT': '审批拒绝',
    'CANCEL': '取消',
    'REFUND': '退款',
    'SHIP': '发货',
    'CONFIRM': '确认'
  }
  return actionMap[action] || action
}

/**
 * 获取角色中文名称
 */
export const getRoleText = (role: OperatorRole): string => {
  const roleMap: Record<OperatorRole, string> = {
    [OperatorRole.BUYER]: '买家',
    [OperatorRole.SELLER]: '卖家',
    [OperatorRole.ADMIN]: '管理员',
    [OperatorRole.SYSTEM]: '系统'
  }
  return roleMap[role] || role
}

/**
 * 获取状态中文名称
 */
export const getStatusText = (status: LogStatus): string => {
  return status === LogStatus.SUCCESS ? '成功' : '失败'
}

/**
 * 获取状态标签类型
 */
export const getStatusTagType = (status: LogStatus): string => {
  return status === LogStatus.SUCCESS ? 'success' : 'danger'
}
