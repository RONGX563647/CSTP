<template>
  <MobileLayout :show-header="true" :show-tab-bar="true" header-title="每日签到">
    <div class="checkin-page">
      <!-- 签到卡片 -->
      <div class="checkin-card">
        <div class="checkin-header">
          <div class="greeting">{{ greetingText }}</div>
          <div class="date-text">{{ todayStr }}</div>
        </div>

        <div class="checkin-main" @click="handleCheckIn">
          <div class="checkin-circle" :class="{ checked: checkInStore.todayCheckedIn, animating: animating }">
            <template v-if="checkInStore.todayCheckedIn">
              <el-icon :size="36"><Check /></el-icon>
            </template>
            <template v-else>
              <span class="checkin-text">签到</span>
            </template>
          </div>
          <div class="checkin-hint">
            {{ checkInStore.todayCheckedIn ? '今日已签到' : '点击签到' }}
          </div>
        </div>

        <div class="checkin-stats">
          <div class="stat-item">
            <span class="stat-value">{{ checkInStore.continuousDays }}</span>
            <span class="stat-label">连续签到(天)</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ checkInStore.totalCheckInDays }}</span>
            <span class="stat-label">累计签到(天)</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ todayReward }}</span>
            <span class="stat-label">今日积分</span>
          </div>
        </div>
      </div>

      <!-- 奖励规则 -->
      <div class="reward-section">
        <h3 class="section-title">奖励规则</h3>
        <div class="reward-rules">
          <div class="rule-item" :class="{ active: checkInStore.continuousDays >= 1 }">
            <div class="rule-day">第1天</div>
            <div class="rule-points">+5积分</div>
          </div>
          <div class="rule-item" :class="{ active: checkInStore.continuousDays >= 2 }">
            <div class="rule-day">第2-6天</div>
            <div class="rule-points">+10积分</div>
          </div>
          <div class="rule-item" :class="{ active: checkInStore.continuousDays >= 7 }">
            <div class="rule-day">第7天起</div>
            <div class="rule-points">+20积分</div>
          </div>
        </div>
      </div>

      <!-- 近7天签到情况 -->
      <div class="week-section">
        <h3 class="section-title">最近7天</h3>
        <div class="week-row">
          <div v-for="day in weekDays" :key="day.date" class="week-day">
            <span class="week-label">{{ day.label }}</span>
            <div class="week-dot" :class="{ checked: day.checked, today: day.isToday }">
              <el-icon v-if="day.checked" :size="12"><Check /></el-icon>
            </div>
            <span class="week-date">{{ day.dayNum }}</span>
          </div>
        </div>
      </div>

      <!-- 本月签到日历 -->
      <div class="calendar-section">
        <div class="calendar-header">
          <el-icon class="calendar-nav" @click="changeMonth(-1)"><ArrowLeft /></el-icon>
          <span class="calendar-title">{{ calendarTitle }}</span>
          <el-icon class="calendar-nav" @click="changeMonth(1)"><ArrowRight /></el-icon>
        </div>
        <div class="calendar-weekdays">
          <span v-for="w in ['一', '二', '三', '四', '五', '六', '日']" :key="w" class="weekday">{{ w }}</span>
        </div>
        <div class="calendar-days">
          <div
            v-for="cell in calendarCells"
            :key="cell.key"
            class="calendar-cell"
            :class="{ 'other-month': !cell.currentMonth, 'checked': cell.checked, 'today': cell.isToday }"
          >
            <span v-if="cell.currentMonth">{{ cell.day }}</span>
          </div>
        </div>
      </div>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Check, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import MobileLayout from '@/layouts/MobileLayout.vue'
import { useCheckInStore } from '@/stores/checkin'
import { getCheckInCalendar } from '@/api/checkin'

const checkInStore = useCheckInStore()
const animating = ref(false)
const calendarYear = ref(new Date().getFullYear())
const calendarMonth = ref(new Date().getMonth() + 1)
const calendarDates = ref<string[]>([])

const today = new Date()

const greetingText = computed(() => {
  const hour = today.getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const todayStr = computed(() => {
  const y = today.getFullYear()
  const m = String(today.getMonth() + 1).padStart(2, '0')
  const d = String(today.getDate()).padStart(2, '0')
  return `${y}年${m}月${d}日`
})

const todayReward = computed(() => {
  const days = checkInStore.continuousDays
  if (checkInStore.todayCheckedIn) {
    if (days >= 7) return 20
    if (days >= 2) return 10
    return 5
  }
  const nextDays = days + 1
  if (nextDays >= 7) return 20
  if (nextDays >= 2) return 10
  return 5
})

const weekDays = computed(() => {
  const labels = ['一', '二', '三', '四', '五', '六', '日']
  const result = []
  const recentDates = checkInStore.recentCheckInDates

  for (let i = 6; i >= 0; i--) {
    const d = new Date(today)
    d.setDate(d.getDate() - i)
    const dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    const dayOfWeek = d.getDay()
    const labelIndex = dayOfWeek === 0 ? 6 : dayOfWeek - 1

    result.push({
      date: dateStr,
      label: labels[labelIndex],
      dayNum: d.getDate(),
      checked: recentDates.includes(dateStr),
      isToday: i === 0
    })
  }
  return result
})

const calendarTitle = computed(() => `${calendarYear.value}年${calendarMonth.value}月`)

const calendarCells = computed(() => {
  const year = calendarYear.value
  const month = calendarMonth.value
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)
  const daysInMonth = lastDay.getDate()

  // 周一为起始：0=周一, 6=周日
  let startDow = firstDay.getDay() - 1
  if (startDow < 0) startDow = 6

  const prevMonthLastDay = new Date(year, month - 1, 0).getDate()
  const cells = []

  // 上月填充
  for (let i = startDow - 1; i >= 0; i--) {
    cells.push({ key: `prev-${i}`, day: prevMonthLastDay - i, currentMonth: false, checked: false, isToday: false })
  }

  // 当月
  const todayStr = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
  for (let d = 1; d <= daysInMonth; d++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    cells.push({
      key: `curr-${d}`,
      day: d,
      currentMonth: true,
      checked: calendarDates.value.includes(dateStr),
      isToday: dateStr === todayStr
    })
  }

  // 下月填充
  const remaining = 7 - (cells.length % 7)
  if (remaining < 7) {
    for (let i = 1; i <= remaining; i++) {
      cells.push({ key: `next-${i}`, day: i, currentMonth: false, checked: false, isToday: false })
    }
  }

  return cells
})

const handleCheckIn = async () => {
  if (checkInStore.todayCheckedIn || animating.value) return
  animating.value = true
  await checkInStore.doCheckIn()
  setTimeout(() => { animating.value = false }, 600)
  fetchCalendar()
}

const changeMonth = (delta: number) => {
  calendarMonth.value += delta
  if (calendarMonth.value > 12) {
    calendarMonth.value = 1
    calendarYear.value++
  } else if (calendarMonth.value < 1) {
    calendarMonth.value = 12
    calendarYear.value--
  }
  fetchCalendar()
}

const fetchCalendar = async () => {
  try {
    const dates = await getCheckInCalendar(calendarYear.value, calendarMonth.value)
    calendarDates.value = dates || []
  } catch (e) {
    console.error('获取签到日历失败:', e)
  }
}

onMounted(() => {
  checkInStore.fetchStatus()
  fetchCalendar()
})
</script>

<style scoped>
.checkin-page {
  min-height: 100vh;
  background: var(--bg-color);
  padding-bottom: 20px;
}

/* 签到卡片 */
.checkin-card {
  margin: 16px;
  padding: 24px 20px;
  background: linear-gradient(135deg, var(--primary-color), #6c5ce7);
  border-radius: 16px;
  color: #fff;
}

.checkin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.greeting {
  font-size: 18px;
  font-weight: 600;
}

.date-text {
  font-size: 13px;
  opacity: 0.85;
}

.checkin-main {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 16px 0 24px;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.checkin-circle {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  border: 3px solid rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.checkin-circle:not(.checked):active {
  transform: scale(0.92);
}

.checkin-circle.checked {
  background: rgba(255, 255, 255, 0.35);
  border-color: rgba(255, 255, 255, 0.8);
}

.checkin-circle.animating {
  animation: pulse 0.6s ease;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.15); }
  100% { transform: scale(1); }
}

.checkin-text {
  font-size: 20px;
  font-weight: 700;
}

.checkin-hint {
  margin-top: 8px;
  font-size: 13px;
  opacity: 0.85;
}

.checkin-stats {
  display: flex;
  align-items: center;
  justify-content: space-around;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.2);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
}

.stat-label {
  font-size: 11px;
  opacity: 0.75;
}

.stat-divider {
  width: 1px;
  height: 30px;
  background: rgba(255, 255, 255, 0.25);
}

/* 奖励规则 */
.reward-section,
.week-section,
.calendar-section {
  margin: 12px 16px;
  padding: 16px;
  background: var(--bg-card);
  border-radius: 12px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 14px 0;
}

.reward-rules {
  display: flex;
  gap: 10px;
}

.rule-item {
  flex: 1;
  text-align: center;
  padding: 12px 8px;
  background: var(--bg-color);
  border-radius: 10px;
  border: 1px solid var(--border-light);
  transition: all 0.2s;
}

.rule-item.active {
  background: var(--primary-lighter);
  border-color: var(--primary-color);
}

.rule-day {
  font-size: 12px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.rule-item.active .rule-day {
  color: var(--primary-color);
}

.rule-points {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.rule-item.active .rule-points {
  color: var(--primary-color);
}

/* 最近7天 */
.week-row {
  display: flex;
  justify-content: space-between;
}

.week-day {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.week-label {
  font-size: 11px;
  color: var(--text-secondary);
}

.week-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--bg-color);
  border: 2px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.week-dot.checked {
  background: var(--primary-color);
  border-color: var(--primary-color);
}

.week-dot.today {
  border-color: var(--primary-color);
}

.week-date {
  font-size: 12px;
  color: var(--text-primary);
}

/* 日历 */
.calendar-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 12px;
}

.calendar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  min-width: 100px;
  text-align: center;
}

.calendar-nav {
  font-size: 16px;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
  -webkit-tap-highlight-color: transparent;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  margin-bottom: 4px;
}

.weekday {
  font-size: 11px;
  color: var(--text-placeholder);
  padding: 4px 0;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
}

.calendar-cell {
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: var(--text-primary);
  border-radius: 50%;
}

.calendar-cell.other-month {
  color: var(--text-placeholder);
}

.calendar-cell.checked {
  background: var(--primary-color);
  color: #fff;
  font-weight: 600;
}

.calendar-cell.today {
  border: 2px solid var(--primary-color);
}
</style>
