import request from '@/utils/request'

export interface CheckInResult {
  id: number
  userId: number
  checkInDate: string
  continuousDays: number
  rewardPoints: number
  todayCheckedIn: boolean
  totalCheckInDays: number
}

export interface CheckInStatus {
  todayCheckedIn: boolean
  continuousDays: number
  totalCheckInDays: number
  lastCheckInDate: string | null
  recentCheckInDates: string[]
}

/**
 * 用户签到
 */
export async function checkIn() {
  const res = await request.post('/user/checkin')
  return res.data
}

/**
 * 获取签到状态
 */
export async function getCheckInStatus() {
  const res = await request.get('/user/checkin/status')
  return res.data
}

/**
 * 获取月度签到日历
 */
export async function getCheckInCalendar(year: number, month: number) {
  const res = await request.get('/user/checkin/calendar', { params: { year, month } })
  return res.data
}
