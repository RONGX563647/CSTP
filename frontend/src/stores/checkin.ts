import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { checkIn as checkInApi, getCheckInStatus as getStatusApi } from '@/api/checkin'
import type { CheckInResult, CheckInStatus } from '@/api/checkin'
import { ElMessage } from 'element-plus'

export const useCheckInStore = defineStore('checkin', () => {
  const status = ref<CheckInStatus | null>(null)
  const loading = ref(false)

  const todayCheckedIn = computed(() => status.value?.todayCheckedIn ?? false)
  const continuousDays = computed(() => status.value?.continuousDays ?? 0)
  const totalCheckInDays = computed(() => status.value?.totalCheckInDays ?? 0)
  const recentCheckInDates = computed(() => status.value?.recentCheckInDates ?? [])

  const fetchStatus = async () => {
    try {
      loading.value = true
      status.value = await getStatusApi()
    } catch (error: any) {
      console.error('获取签到状态失败:', error)
    } finally {
      loading.value = false
    }
  }

  const doCheckIn = async (): Promise<CheckInResult | null> => {
    try {
      loading.value = true
      const result = await checkInApi()
      // 刷新状态
      await fetchStatus()
      ElMessage.success(`签到成功！连续签到${result.continuousDays}天，获得${result.rewardPoints}积分`)
      return result
    } catch (error: any) {
      if (error?.response?.data?.code === '70010001') {
        ElMessage.warning('今日已签到')
      } else {
        ElMessage.error(error?.response?.data?.message || '签到失败')
      }
      return null
    } finally {
      loading.value = false
    }
  }

  return {
    status,
    loading,
    todayCheckedIn,
    continuousDays,
    totalCheckInDays,
    recentCheckInDates,
    fetchStatus,
    doCheckIn
  }
})
