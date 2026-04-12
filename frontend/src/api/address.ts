import request from '@/utils/request'

export interface Address {
  id?: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault: boolean
  fullAddress?: string
}

/**
 * 获取用户地址列表
 */
export async function getUserAddresses() {
  const res = await request.get('/user/addresses')
  return res.data
}

/**
 * 获取单个地址详情
 */
export async function getAddressById(id: number) {
  const res = await request.get(`/user/addresses/${id}`)
  return res.data
}

/**
 * 创建新地址
 */
export async function createAddress(data: Address) {
  const res = await request.post('/user/addresses', data)
  return res.data
}

/**
 * 更新地址
 */
export async function updateAddress(id: number, data: Address) {
  const res = await request.put(`/user/addresses/${id}`, data)
  return res.data
}

/**
 * 删除地址
 */
export async function deleteAddress(id: number) {
  const res = await request.delete(`/user/addresses/${id}`)
  return res.data
}

/**
 * 设置默认地址
 */
export async function setDefaultAddress(id: number) {
  const res = await request.put(`/user/addresses/${id}/default`)
  return res.data
}
