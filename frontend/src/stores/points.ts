import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { getPointAccount as getAccountApi, getPointRecords as getRecordsApi } from '@/api/points'
import type { PointAccount, PointRecord, PointRecordPage } from '@/api/points'

export const usePointsStore = defineStore('points', () => {
  const account = ref<PointAccount | null>(null)
  const records = ref<PointRecord[]>([])
  const totalRecords = ref(0)
  const loading = ref(false)

  const availablePoints = computed(() => account.value?.availablePoints ?? 0)
  const totalPoints = computed(() => account.value?.totalPoints ?? 0)
  const usedPoints = computed(() => account.value?.usedPoints ?? 0)

  const fetchAccount = async () => {
    try {
      loading.value = true
      account.value = await getAccountApi()
    } catch (error: any) {
      console.error('获取积分账户失败:', error)
    } finally {
      loading.value = false
    }
  }

  const fetchRecords = async (params?: { type?: string; page?: number; size?: number }) => {
    try {
      loading.value = true
      const page: PointRecordPage = await getRecordsApi(params)
      records.value = page?.content ?? []
      totalRecords.value = page?.totalElements ?? 0
    } catch (error: any) {
      console.error('获取积分流水失败:', error)
    } finally {
      loading.value = false
    }
  }

  return {
    account,
    records,
    totalRecords,
    loading,
    availablePoints,
    totalPoints,
    usedPoints,
    fetchAccount,
    fetchRecords
  }
})
