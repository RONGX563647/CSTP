<template>
  <MobileLayout :show-header="true" title="我的积分" :show-tab-bar="true" :show-back="true">
    <div class="points-page">
      <!-- 积分总览卡片 -->
      <div class="points-card">
        <div class="points-card-bg"></div>
        <div class="points-card-content">
          <div class="points-label">可用积分</div>
          <div class="points-value">{{ pointsStore.availablePoints }}</div>
          <div class="points-meta">
            <div class="meta-item">
              <span class="meta-label">累计获得</span>
              <span class="meta-value">{{ pointsStore.totalPoints }}</span>
            </div>
            <div class="meta-divider"></div>
            <div class="meta-item">
              <span class="meta-label">已使用</span>
              <span class="meta-value">{{ pointsStore.usedPoints }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 积分来源统计 -->
      <div class="stats-row">
        <div class="stat-item">
          <el-icon :size="20" color="#6c5ce7"><Calendar /></el-icon>
          <span class="stat-label">签到</span>
          <span class="stat-value">{{ pointsStore.account?.checkInCount || 0 }}次</span>
        </div>
        <div class="stat-item">
          <el-icon :size="20" color="#00b894"><ShoppingBag /></el-icon>
          <span class="stat-label">订单奖励</span>
          <span class="stat-value">{{ pointsStore.account?.orderCount || 0 }}次</span>
        </div>
      </div>

      <!-- 积分流水 -->
      <div class="records-section">
        <div class="section-header">
          <span class="section-title">积分明细</span>
          <el-select v-model="filterType" placeholder="全部类型" size="small" clearable style="width: 120px" @change="handleFilterChange">
            <el-option label="全部类型" value="" />
            <el-option label="签到奖励" value="CHECK_IN" />
            <el-option label="订单奖励" value="ORDER_COMPLETE" />
            <el-option label="积分兑换" value="EXCHANGE" />
            <el-option label="管理员调整" value="ADMIN_ADD" />
          </el-select>
        </div>

        <div v-if="!pointsStore.records?.length && !pointsStore.loading" class="empty-state">
          <el-empty description="暂无积分记录" :image-size="80" />
        </div>

        <div v-else class="records-list">
          <div v-for="record in pointsStore.records" :key="record.id" class="record-item">
            <div class="record-left">
              <div class="record-type-icon" :class="getPointTypeClass(record.type)">
                <el-icon :size="16">
                  <component :is="getPointTypeIcon(record.type)" />
                </el-icon>
              </div>
              <div class="record-info">
                <div class="record-desc">{{ record.description || getTypeName(record.type) }}</div>
                <div class="record-time">{{ formatTime(record.createdAt) }}</div>
              </div>
            </div>
            <div class="record-right">
              <span class="record-points" :class="record.points > 0 ? 'positive' : 'negative'">
                {{ record.points > 0 ? '+' : '' }}{{ record.points }}
              </span>
            </div>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMore" class="load-more">
          <el-button text type="primary" :loading="pointsStore.loading" @click="loadMore">
            加载更多
          </el-button>
        </div>
      </div>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Calendar, ShoppingBag, Goods, Setting, Star } from '@element-plus/icons-vue'
import MobileLayout from '@/layouts/MobileLayout.vue'
import { usePointsStore } from '@/stores/points'

const pointsStore = usePointsStore()
const filterType = ref('')
const currentPage = ref(0)
const pageSize = 20

const hasMore = computed(() => {
  return (pointsStore.records?.length ?? 0) < pointsStore.totalRecords
})

const getTypeName = (type: string) => {
  const map: Record<string, string> = {
    CHECK_IN: '签到奖励',
    ORDER_COMPLETE: '订单奖励',
    ORDER_CANCEL: '订单取消扣回',
    ADMIN_ADD: '管理员增加',
    ADMIN_DEDUCT: '管理员扣减',
    EXCHANGE: '积分兑换'
  }
  return map[type] || type
}

const getPointTypeClass = (type: string) => {
  if (['CHECK_IN', 'ORDER_COMPLETE', 'ADMIN_ADD'].includes(type)) return 'type-earn'
  return 'type-spend'
}

const getPointTypeIcon = (type: string) => {
  const map: Record<string, any> = {
    CHECK_IN: Calendar,
    ORDER_COMPLETE: ShoppingBag,
    ORDER_CANCEL: ShoppingBag,
    ADMIN_ADD: Setting,
    ADMIN_DEDUCT: Setting,
    EXCHANGE: Star
  }
  return map[type] || Goods
}

const formatTime = (dateStr: string) => {
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

const fetchRecords = (reset = false) => {
  if (reset) currentPage.value = 0
  pointsStore.fetchRecords({
    type: filterType.value || undefined,
    page: currentPage.value,
    size: pageSize
  })
}

const handleFilterChange = (val: string | number | boolean | null | undefined) => {
  filterType.value = val ? String(val) : ''
  fetchRecords(true)
}

const loadMore = () => {
  currentPage.value++
  pointsStore.fetchRecords({
    type: filterType.value || undefined,
    page: currentPage.value,
    size: pageSize,
    append: true
  })
}

onMounted(async () => {
  await pointsStore.fetchAccount()
  fetchRecords(true)
})
</script>

<style scoped>
.points-page {
  min-height: 100vh;
  background: var(--bg-color);
}

/* 积分总览卡片 */
.points-card {
  position: relative;
  margin: 12px;
  border-radius: 16px;
  overflow: hidden;
}

.points-card-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #6c5ce7, #a29bfe, #74b9ff);
  opacity: 0.95;
}

.points-card-content {
  position: relative;
  z-index: 1;
  padding: 24px 20px;
  color: #fff;
  text-align: center;
}

.points-label {
  font-size: 13px;
  opacity: 0.85;
  margin-bottom: 4px;
}

.points-value {
  font-size: 42px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 16px;
}

.points-meta {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.meta-label {
  font-size: 11px;
  opacity: 0.75;
}

.meta-value {
  font-size: 16px;
  font-weight: 600;
}

.meta-divider {
  width: 1px;
  height: 24px;
  background: rgba(255, 255, 255, 0.3);
}

/* 积分来源统计 */
.stats-row {
  display: flex;
  gap: 8px;
  padding: 0 12px;
  margin-bottom: 12px;
}

.stat-item {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: var(--bg-card);
  border-radius: 12px;
}

.stat-label {
  font-size: 13px;
  color: var(--text-secondary);
}

.stat-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-left: auto;
}

/* 积分明细 */
.records-section {
  background: var(--bg-card);
  margin: 0 12px;
  border-radius: 12px;
  padding: 16px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.empty-state {
  padding: 20px 0;
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-light);
}

.record-item:last-child {
  border-bottom: none;
}

.record-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.record-type-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
}

.record-type-icon.type-earn {
  background: #e8f5e9;
  color: #43a047;
}

.record-type-icon.type-spend {
  background: #fbe9e7;
  color: #e53935;
}

.record-desc {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.4;
}

.record-time {
  font-size: 12px;
  color: var(--text-placeholder);
  margin-top: 2px;
}

.record-points {
  font-size: 16px;
  font-weight: 600;
}

.record-points.positive {
  color: #43a047;
}

.record-points.negative {
  color: #e53935;
}

.load-more {
  text-align: center;
  padding: 8px 0;
}
</style>
