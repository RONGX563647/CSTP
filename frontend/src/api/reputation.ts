import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

// ==================== 类型定义 ====================

// 信誉等级枚举
export enum ReputationLevel {
  EXCELLENT = 5,  // 优秀
  GOOD = 4,       // 良好
  NORMAL = 3,     // 一般
  POOR = 2,       // 较差
  BAD = 1         // 极差
}

// 信誉记录类型枚举
export enum RecordType {
  REVIEW_ADD = 'REVIEW_ADD',      // 收到新评价
  REVIEW_REPLY = 'REVIEW_REPLY',  // 评价被回复
  ADMIN_ADJUST = 'ADMIN_ADJUST'   // 管理员调整
}

// 信誉账户接口
export interface ReputationAccount {
  id: number
  userId: number
  totalScore: number        // 总信誉分
  totalReviews: number      // 评价总数
  goodReviews: number       // 好评数
  neutralReviews: number    // 中评数
  badReviews: number        // 差评数
  goodRate: number          // 好评率
  avgRating: number         // 平均评分
  level: number             // 信誉等级
  levelName: string         // 等级名称
  levelIcon: string         // 等级图标
  createdAt: string
  updatedAt: string
}

// 信誉记录接口
export interface ReputationRecord {
  id: number
  userId: number
  reviewId?: number
  type: RecordType
  scoreChange: number       // 信誉变化
  balanceAfter: number      // 变化后信誉分
  rating?: number           // 本次评分
  content?: string          // 评价内容摘要
  description: string       // 描述
  createdAt: string
}

// 管理员调整请求
export interface AdminReputationAdjustRequest {
  score: number             // 调整分数（正/负）
  reason: string            // 调整原因
}

// ==================== 用户端信誉 API ====================

/**
 * 获取我的信誉概览
 */
export const getMyReputation = () => {
  return request<ApiResponse<ReputationAccount>>({
    url: '/user/reputation',
    method: 'get'
  })
}

/**
 * 获取信誉流水记录
 */
export const getReputationRecords = (page?: number, size?: number) => {
  return request<ApiResponse<{
    content: ReputationRecord[]
    totalElements: number
    totalPages: number
    number: number
    size: number
  }>>({
    url: '/user/reputation/records',
    method: 'get',
    params: { page, size }
  })
}

/**
 * 获取指定用户信誉信息（公开）
 */
export const getUserReputation = (userId: number) => {
  return request<ApiResponse<ReputationAccount>>({
    url: `/user/reputation/public/${userId}`,
    method: 'get'
  })
}

// ==================== 管理端信誉 API ====================

/**
 * 获取用户信誉详情（管理端）
 */
export const getAdminUserReputation = (userId: number) => {
  return request<ApiResponse<ReputationAccount>>({
    url: `/admin/users/${userId}/reputation`,
    method: 'get'
  })
}

/**
 * 调整用户信誉分（管理端）
 */
export const adjustUserReputation = (userId: number, data: AdminReputationAdjustRequest) => {
  return request<ApiResponse<ReputationRecord>>({
    url: `/admin/users/${userId}/reputation/adjust`,
    method: 'post',
    data
  })
}

// ==================== 工具函数 ====================

/**
 * 获取信誉等级文本
 */
export const getLevelText = (level: number): string => {
  const levelMap: Record<number, string> = {
    [ReputationLevel.EXCELLENT]: '优秀',
    [ReputationLevel.GOOD]: '良好',
    [ReputationLevel.NORMAL]: '一般',
    [ReputationLevel.POOR]: '较差',
    [ReputationLevel.BAD]: '极差'
  }
  return levelMap[level] || '一般'
}

/**
 * 获取信誉等级颜色
 */
export const getLevelColor = (level: number): string => {
  const colorMap: Record<number, string> = {
    [ReputationLevel.EXCELLENT]: '#67C23A',  // 绿色
    [ReputationLevel.GOOD]: '#409EFF',       // 蓝色
    [ReputationLevel.NORMAL]: '#E6A23C',     // 黄色
    [ReputationLevel.POOR]: '#F56C6C',       // 红色
    [ReputationLevel.BAD]: '#909399'         // 灰色
  }
  return colorMap[level] || '#E6A23C'
}

/**
 * 获取信誉变化文本
 */
export const getScoreChangeText = (change: number): string => {
  if (change > 0) return `+${change}`
  if (change < 0) return `${change}`
  return '0'
}

/**
 * 获取记录类型文本
 */
export const getRecordTypeText = (type: RecordType): string => {
  const typeMap: Record<RecordType, string> = {
    [RecordType.REVIEW_ADD]: '收到评价',
    [RecordType.REVIEW_REPLY]: '评价回复',
    [RecordType.ADMIN_ADJUST]: '管理员调整'
  }
  return typeMap[type] || '其他'
}