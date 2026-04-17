<template>
  <MobileLayout :show-header="true" title="我的信誉" :show-tab-bar="true" :show-back="true">
    <div class="reputation-page">
      <!-- 信誉总览卡片 -->
      <div class="reputation-card">
        <div class="reputation-card-bg"></div>
        <div class="reputation-card-content">
          <div class="level-badge">
            <span class="level-icon">{{ reputation?.levelIcon }}</span>
            <span class="level-name">{{ reputation?.levelName }}</span>
          </div>
          <div class="score-section">
            <div class="score-label">信誉分</div>
            <div class="score-value">{{ reputation?.totalScore || 100 }}</div>
          </div>
          <div class="rating-section">
            <div class="rating-stars">
              <el-rate v-model="avgRating" :colors="['#99A9BF', '#F7BA2A', '#FF9900']" disabled show-score text-color="#fff" />
            </div>
            <div class="rating-count">{{ reputation?.totalReviews || 0 }} 条评价</div>
          </div>
        </div>
      </div>

      <!-- 评价统计 -->
      <div class="stats-section">
        <div class="stats-header">评价统计</div>
        <div class="stats-bar">
          <div class="bar-item good">
            <div class="bar-label">好评</div>
            <div class="bar-value">{{ reputation?.goodReviews || 0 }}</div>
            <div class="bar-percent" :style="{ width: goodPercent + '%' }"></div>
          </div>
          <div class="bar-item neutral">
            <div class="bar-label">中评</div>
            <div class="bar-value">{{ reputation?.neutralReviews || 0 }}</div>
            <div class="bar-percent" :style="{ width: neutralPercent + '%' }"></div>
          </div>
          <div class="bar-item bad">
            <div class="bar-label">差评</div>
            <div class="bar-value">{{ reputation?.badReviews || 0 }}</div>
            <div class="bar-percent" :style="{ width: badPercent + '%' }"></div>
          </div>
        </div>
        <div class="good-rate-info">
          <span class="good-rate-label">好评率</span>
          <span class="good-rate-value">{{ (reputation?.goodRate || 0).toFixed(1) }}%</span>
        </div>
      </div>

      <!-- 信誉等级说明 -->
      <div class="level-info">
        <div class="info-header">信誉等级说明</div>
        <div class="level-list">
          <div class="level-item" :class="{ active: reputation?.level === 5 }">
            <span class="level-stars">🌟🌟🌟🌟🌟</span>
            <span class="level-desc">优秀 (≥4.8分, ≥50评)</span>
          </div>
          <div class="level-item" :class="{ active: reputation?.level === 4 }">
            <span class="level-stars">🌟🌟🌟🌟</span>
            <span class="level-desc">良好 (≥4.5分, ≥20评)</span>
          </div>
          <div class="level-item" :class="{ active: reputation?.level === 3 }">
            <span class="level-stars">🌟🌟🌟</span>
            <span class="level-desc">一般 (≥4.0分)</span>
          </div>
          <div class="level-item" :class="{ active: reputation?.level === 2 }">
            <span class="level-stars">🌟🌟</span>
            <span class="level-desc">较差 (<4.0分)</span>
          </div>
          <div class="level-item" :class="{ active: reputation?.level === 1 }">
            <span class="level-stars">🌟</span>
            <span class="level-desc">极差 (<3.0分)</span>
          </div>
        </div>
      </div>

      <!-- 信誉流水 -->
      <div class="records-section">
        <div class="section-header">
          <span class="section-title">信誉明细</span>
        </div>

        <div v-if="!records.length && !loading" class="empty-state">
          <el-empty description="暂无信誉记录" :image-size="80" />
        </div>

        <div v-else class="records-list">
          <div v-for="record in records" :key="record.id" class="record-item">
            <div class="record-left">
              <div class="record-type-icon" :class="getRecordTypeClass(record.type)">
                <span class="icon-text">{{ getRecordTypeIcon(record.type) }}</span>
              </div>
              <div class="record-info">
                <div class="record-desc">{{ record.description }}</div>
                <div v-if="record.content" class="record-content">{{ record.content }}</div>
                <div v-if="record.rating" class="record-rating">
                  <el-rate v-model="record.rating" size="small" disabled />
                </div>
                <div class="record-time">{{ formatTime(record.createdAt) }}</div>
              </div>
            </div>
            <div class="record-right">
              <span class="record-score" :class="record.scoreChange > 0 ? 'positive' : (record.scoreChange < 0 ? 'negative' : 'neutral')">
                {{ record.scoreChange > 0 ? '+' : '' }}{{ record.scoreChange }}
              </span>
              <span class="record-balance">余额: {{ record.balanceAfter }}</span>
            </div>
          </div>
        </div>

        <!-- 加载更多 -->
        <div v-if="hasMore" class="load-more">
          <el-button text type="primary" :loading="loading" @click="loadMore">
            加载更多
          </el-button>
        </div>
      </div>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import MobileLayout from '@/layouts/MobileLayout.vue'
import {
  getMyReputation,
  getReputationRecords,
  type ReputationAccount,
  type ReputationRecord,
  RecordType
} from '@/api/reputation'

const reputation = ref<ReputationAccount | null>(null)
const records = ref<ReputationRecord[]>([])
const loading = ref(false)
const currentPage = ref(0)
const totalRecords = ref(0)
const pageSize = 20

const avgRating = computed(() => reputation.value?.avgRating || 0)

const goodPercent = computed(() => {
  const total = reputation.value?.totalReviews || 0
  if (total === 0) return 0
  return (reputation.value?.goodReviews || 0) * 100 / total
})

const neutralPercent = computed(() => {
  const total = reputation.value?.totalReviews || 0
  if (total === 0) return 0
  return (reputation.value?.neutralReviews || 0) * 100 / total
})

const badPercent = computed(() => {
  const total = reputation.value?.totalReviews || 0
  if (total === 0) return 0
  return (reputation.value?.badReviews || 0) * 100 / total
})

const hasMore = computed(() => records.value.length < totalRecords.value)

const getRecordTypeClass = (type: string) => {
  if (type === 'REVIEW_ADD') {
    return 'type-review'
  }
  return 'type-admin'
}

const getRecordTypeIcon = (type: string) => {
  const map: Record<string, string> = {
    REVIEW_ADD: '评',
    REVIEW_REPLY: '复',
    ADMIN_ADJUST: '调'
  }
  return map[type] || '?'
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

const fetchReputation = async () => {
  try {
    const response = await getMyReputation()
    reputation.value = response.data.data
  } catch (error) {
    console.error('获取信誉信息失败:', error)
  }
}

const fetchRecords = async (reset = false) => {
  if (reset) {
    currentPage.value = 0
    records.value = []
  }

  loading.value = true
  try {
    const response = await getReputationRecords(currentPage.value, pageSize)
    const data = response.data.data
    if (reset) {
      records.value = data.content
    } else {
      records.value = [...records.value, ...data.content]
    }
    totalRecords.value = data.totalElements
  } catch (error) {
    console.error('获取信誉记录失败:', error)
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  currentPage.value++
  fetchRecords()
}

onMounted(async () => {
  await fetchReputation()
  await fetchRecords(true)
})
</script>

<style scoped>
.reputation-page {
  min-height: 100vh;
  background: var(--bg-color);
  padding-bottom: 20px;
}

/* 信誉总览卡片 */
.reputation-card {
  position: relative;
  margin: 12px;
  border-radius: 16px;
  overflow: hidden;
}

.reputation-card-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #fdcb6e, #e17055, #d63031);
  opacity: 0.95;
}

.reputation-card-content {
  position: relative;
  z-index: 1;
  padding: 24px 20px;
  color: #fff;
  text-align: center;
}

.level-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255, 255, 255, 0.2);
  padding: 8px 16px;
  border-radius: 20px;
  margin-bottom: 16px;
}

.level-icon {
  font-size: 18px;
}

.level-name {
  font-size: 14px;
  font-weight: 500;
}

.score-section {
  margin-bottom: 16px;
}

.score-label {
  font-size: 13px;
  opacity: 0.85;
  margin-bottom: 4px;
}

.score-value {
  font-size: 42px;
  font-weight: 700;
  line-height: 1.2;
}

.rating-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.rating-count {
  font-size: 12px;
  opacity: 0.85;
}

/* 评价统计 */
.stats-section {
  background: var(--bg-card);
  margin: 0 12px 12px;
  border-radius: 12px;
  padding: 16px;
}

.stats-header {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.stats-bar {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.bar-item {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  background: var(--bg-color);
  border-radius: 8px;
  overflow: hidden;
}

.bar-percent {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  opacity: 0.2;
  border-radius: 8px;
}

.bar-item.good .bar-percent {
  background: #67C23A;
}

.bar-item.neutral .bar-percent {
  background: #E6A23C;
}

.bar-item.bad .bar-percent {
  background: #F56C6C;
}

.bar-label {
  font-size: 14px;
  color: var(--text-secondary);
  z-index: 1;
}

.bar-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  z-index: 1;
}

.good-rate-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}

.good-rate-label {
  font-size: 14px;
  color: var(--text-secondary);
}

.good-rate-value {
  font-size: 18px;
  font-weight: 600;
  color: #67C23A;
}

/* 信誉等级说明 */
.level-info {
  background: var(--bg-card);
  margin: 0 12px 12px;
  border-radius: 12px;
  padding: 16px;
}

.info-header {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.level-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.level-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--bg-color);
}

.level-item.active {
  background: #fef3cd;
  border: 1px solid #ffc107;
}

.level-stars {
  font-size: 14px;
}

.level-desc {
  font-size: 13px;
  color: var(--text-secondary);
}

/* 信誉明细 */
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
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-light);
}

.record-item:last-child {
  border-bottom: none;
}

.record-left {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.record-type-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
}

.record-type-icon.type-review {
  background: #e3f2fd;
  color: #1976d2;
}

.record-type-icon.type-admin {
  background: #fce4ec;
  color: #c2185b;
}

.icon-text {
  font-size: 14px;
}

.record-info {
  flex: 1;
}

.record-desc {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.4;
}

.record-content {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
  padding: 4px 8px;
  background: var(--bg-color);
  border-radius: 4px;
}

.record-rating {
  margin-top: 4px;
}

.record-time {
  font-size: 12px;
  color: var(--text-placeholder);
  margin-top: 4px;
}

.record-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.record-score {
  font-size: 16px;
  font-weight: 600;
}

.record-score.positive {
  color: #67C23A;
}

.record-score.negative {
  color: #F56C6C;
}

.record-score.neutral {
  color: var(--text-secondary);
}

.record-balance {
  font-size: 12px;
  color: var(--text-placeholder);
}

.load-more {
  text-align: center;
  padding: 8px 0;
}
</style>