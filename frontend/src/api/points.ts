import request from '@/utils/request'

export interface PointAccount {
  id: number
  userId: number
  totalPoints: number
  availablePoints: number
  usedPoints: number
  checkInPoints: number
  orderPoints: number
}

export interface PointRecord {
  id: number
  type: string
  points: number
  balanceAfter: number
  relatedId: number | null
  description: string
  createdAt: string
}

export interface PointRecordPage {
  content: PointRecord[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

/**
 * 获取积分账户概览
 */
export async function getPointAccount() {
  const res = await request.get('/user/points/account')
  return res.data
}

/**
 * 获取积分流水记录
 */
export async function getPointRecords(params?: {
  type?: string
  page?: number
  size?: number
}) {
  const res = await request.get('/user/points/records', { params })
  return res.data
}

/**
 * 管理员调整用户积分
 */
export async function adminAdjustPoints(userId: number, data: {
  points: number
  reason: string
}) {
  const res = await request.post(`/admin/points/adjust/${userId}`, data)
  return res.data
}
