<template>
  <MobileLayout :show-header="false" :show-tab-bar="!currentChatUser">
    <div class="chat-page">
      <!-- 聊天列表视图 -->
      <template v-if="!currentChatUser">
        <div class="chat-top-bar">
          <h2 class="chat-top-title">消息</h2>
          <el-tag v-if="wsStatus !== 'connected'" :type="statusType" size="small" class="status-tag">
            {{ statusText }}
          </el-tag>
        </div>

        <div class="contacts-list">
          <div
            v-for="partner in chatPartners"
            :key="partner.userId"
            class="contact-item"
            @click="openChat(partner)"
          >
            <div class="contact-avatar">
              <el-avatar :size="48" :src="partner.avatar || undefined">
                {{ partner.username?.charAt(0) }}
              </el-avatar>
              <span v-if="partner.unreadCount > 0" class="unread-badge">
                {{ partner.unreadCount > 99 ? '99+' : partner.unreadCount }}
              </span>
            </div>
            <div class="contact-info">
              <div class="contact-top-row">
                <span class="contact-name">{{ partner.username }}</span>
                <span class="contact-time">{{ formatRelativeTime(partner.lastMessageTime) }}</span>
              </div>
              <div class="contact-preview">
                {{ partner.lastMessage || '暂无消息' }}
              </div>
            </div>
          </div>
          <div v-if="chatPartners.length === 0" class="no-contacts">
            <el-empty description="暂无消息" :image-size="80" />
          </div>
        </div>
      </template>

      <!-- 聊天窗口视图 -->
      <template v-if="currentChatUser">
        <div class="chat-window">
          <div class="wc-header">
            <div class="wc-header-left" @click="closeChat">
              <el-icon :size="20"><ArrowLeft /></el-icon>
            </div>
            <div class="wc-header-title">{{ currentChatUser.username }}</div>
            <div class="wc-header-right"></div>
          </div>

          <div class="wc-messages" ref="messagesRef">
            <div v-if="loadingMessages" class="loading-more">
              <el-icon class="is-loading"><Loading /></el-icon>
            </div>
            <div
              v-for="(msg, index) in messages"
              :key="msg.id"
              class="wc-msg-row"
              :class="{ 'is-self': msg.senderId === currentUserId }"
            >
              <div v-if="shouldShowTime(msg, index)" class="wc-time-divider">
                {{ formatChatTime(msg.createdAt) }}
              </div>
              <div class="wc-msg-bubble-row">
                <!-- 对方消息：左侧显示对方头像 -->
                <template v-if="msg.senderId !== currentUserId">
                  <el-avatar :size="36" class="wc-avatar">
                    {{ msg.senderName?.charAt(0) || '?' }}
                  </el-avatar>
                  <div class="wc-bubble">
                    <span class="wc-bubble-text">{{ msg.content }}</span>
                  </div>
                </template>
                <!-- 我的消息：右侧显示我的头像 -->
                <template v-else>
                  <div class="wc-bubble bubble-self">
                    <span class="wc-bubble-text">{{ msg.content }}</span>
                  </div>
                  <el-avatar :size="36" class="wc-avatar">
                    {{ myAvatarLetter }}
                  </el-avatar>
                </template>
              </div>
            </div>
          </div>

          <div class="wc-input-bar">
            <input
              v-model="inputMessage"
              class="wc-input"
              placeholder="输入消息..."
              @keyup.enter="sendMessage"
              ref="inputRef"
            />
            <button
              class="wc-send-btn"
              :class="{ active: inputMessage.trim() }"
              @click="sendMessage"
              :disabled="!inputMessage.trim()"
            >
              发送
            </button>
          </div>
        </div>
      </template>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import MobileLayout from '@/layouts/MobileLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { sseService, type ChatMessage, type ConnectionStatus } from '@/utils/sse'
import {
  getConversation,
  getChatPartnersDetail,
  sendMessage as apiSendMessage,
  markConversationAsRead,
  getUserPublicInfo,
  type ChatPartner
} from '@/api/chat'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const currentUserId = computed(() => authStore.userInfo?.id)
const myAvatarLetter = computed(() => authStore.userInfo?.username?.charAt(0) || '我')

// 状态
const chatPartners = ref<ChatPartner[]>([])
const currentChatUser = ref<ChatPartner | null>(null)
const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const messagesRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLInputElement | null>(null)
const loadingMessages = ref(false)
const wsStatus = ref<ConnectionStatus>('disconnected')

// SSE 回调引用
let messageCallback: ((msg: ChatMessage) => void) | null = null
let statusCallback: ((status: ConnectionStatus) => void) | null = null

// 连接状态文案
const statusType = computed(() => {
  switch (wsStatus.value) {
    case 'connected': return 'success'
    case 'connecting': return 'warning'
    case 'error': return 'danger'
    default: return 'info'
  }
})

const statusText = computed(() => {
  switch (wsStatus.value) {
    case 'connected': return '已连接'
    case 'connecting': return '连接中'
    case 'error': return '连接失败'
    default: return '未连接'
  }
})

// ===== 生命周期 =====

onMounted(async () => {
  await ensureSSE()
  await loadChatPartners()

  // 如果路由带 userId 参数，直接打开聊天
  const routeUserId = route.params.userId
  if (routeUserId) {
    const userId = Number(routeUserId)
    if (!isNaN(userId) && userId > 0) {
      await openChatWithUser(userId)
    }
  }
})

onUnmounted(() => {
  if (messageCallback) sseService.removeMessageCallback(messageCallback)
  if (statusCallback) sseService.removeStatusCallback(statusCallback)
  messageCallback = null
  statusCallback = null
})

// ===== SSE 连接 =====

const ensureSSE = async () => {
  statusCallback = (status: ConnectionStatus) => {
    wsStatus.value = status
  }
  sseService.onStatusChange(statusCallback)
  wsStatus.value = sseService.getStatus()

  if (sseService.isConnected()) {
    registerMessageCallback()
    return
  }

  const token = localStorage.getItem('token')
  if (token) {
    try {
      await sseService.connect(token.replace('Bearer ', ''))
      registerMessageCallback()
    } catch (e) {
      console.error('SSE连接失败:', e)
    }
  }
}

const registerMessageCallback = () => {
  if (messageCallback) return

  messageCallback = (msg: ChatMessage) => {
    onSSEMessage(msg)
  }
  sseService.onMessage(messageCallback)
}

// ===== SSE 消息处理 =====

const onSSEMessage = (msg: ChatMessage) => {
  if (!msg.id || !msg.senderId || !msg.content) {
    console.warn('onSSEMessage: invalid message', msg)
    return
  }

  const partnerId = currentChatUser.value?.userId
  console.log('SSE message received:', {
    msgId: msg.id,
    senderId: msg.senderId,
    senderName: msg.senderName,
    receiverId: msg.receiverId,
    currentUserId: currentUserId.value,
    partnerId,
    isCurrentChatMessage: partnerId && (
      (msg.senderId === partnerId && msg.receiverId === currentUserId.value) ||
      (msg.senderId === currentUserId.value && msg.receiverId === partnerId)
    )
  })

  // 判断这条消息是否属于当前打开的聊天
  const isCurrentChatMessage = partnerId && (
    (msg.senderId === partnerId && msg.receiverId === currentUserId.value) ||
    (msg.senderId === currentUserId.value && msg.receiverId === partnerId)
  )

  if (isCurrentChatMessage) {
    // 防止重复添加：检查消息是否已存在于列表中
    const exists = messages.value.some(m => m.id === msg.id)
    console.log('Message belongs to current chat, exists:', exists)

    if (!exists) {
      messages.value.push(msg)
      console.log('Added message to list, total messages:', messages.value.length)
      scrollToBottom()
    }

    // 如果是对方发来的消息，标记已读
    if (msg.senderId === partnerId) {
      markConversationAsRead(partnerId).catch(() => {})
    }
    return
  }

  // 消息不属于当前聊天窗口 -> 更新联系人列表的未读数
  const incomingUserId = msg.senderId === currentUserId.value ? msg.receiverId : msg.senderId
  const partner = chatPartners.value.find(p => p.userId === incomingUserId)

  if (partner) {
    if (msg.senderId !== currentUserId.value) {
      partner.unreadCount++
    }
    partner.lastMessage = msg.content
    partner.lastMessageTime = msg.createdAt
  } else {
    // 新的联系人，刷新列表
    loadChatPartners()
  }
}

// ===== 聊天列表 =====

const loadChatPartners = async () => {
  try {
    const res = await getChatPartnersDetail()
    chatPartners.value = res.data.data || []
  } catch (e) {
    console.error('加载聊天列表失败:', e)
  }
}

// ===== 打开聊天 =====

const openChat = async (partner: ChatPartner) => {
  currentChatUser.value = partner
  loadingMessages.value = true
  messages.value = []
  partner.unreadCount = 0

  try {
    const res = await getConversation(partner.userId)
    messages.value = res.data.data || []
    await markConversationAsRead(partner.userId)
    scrollToBottom()
  } catch (e) {
    console.error('加载聊天记录失败:', e)
  } finally {
    loadingMessages.value = false
  }

  router.replace(`/user/chat/${partner.userId}`)
}

const openChatWithUser = async (userId: number) => {
  if (userId === currentUserId.value) return // 不能和自己聊天

  let partner = chatPartners.value.find(p => p.userId === userId)

  if (!partner) {
    // 获取对方的真实用户信息
    try {
      const res = await getUserPublicInfo(userId)
      const userInfo = res.data.data
      partner = {
        userId,
        username: userInfo.nickname || userInfo.username,
        avatar: userInfo.avatar,
        lastMessage: null,
        lastMessageTime: null,
        unreadCount: 0
      }
    } catch (e) {
      console.error('获取用户信息失败:', e)
      partner = {
        userId,
        username: `用户${userId}`,
        avatar: null,
        lastMessage: null,
        lastMessageTime: null,
        unreadCount: 0
      }
    }
  }

  currentChatUser.value = partner
  loadingMessages.value = true
  messages.value = []

  try {
    const res = await getConversation(userId)
    messages.value = res.data.data || []

    // 从消息记录中提取对方的真实用户名（如果API获取失败）
    if (messages.value.length > 0 && partner.username.includes('用户')) {
      for (const msg of messages.value) {
        if (msg.senderId === userId && msg.senderName) {
          partner.username = msg.senderName
          break
        }
        if (msg.receiverId === userId && msg.receiverName) {
          partner.username = msg.receiverName
          break
        }
      }
    }

    await markConversationAsRead(userId)
    scrollToBottom()
  } catch (e) {
    console.error('加载聊天记录失败:', e)
  } finally {
    loadingMessages.value = false
  }
}

// ===== 关闭聊天 =====

const closeChat = () => {
  currentChatUser.value = null
  messages.value = []
  router.replace('/user/chat')
  loadChatPartners()
}

// ===== 发送消息 =====

const sendMessage = async () => {
  if (!inputMessage.value.trim() || !currentChatUser.value) return

  const content = inputMessage.value.trim()
  const receiverId = currentChatUser.value.userId
  inputMessage.value = ''

  try {
    console.log('Sending message via HTTP:', { receiverId, content })
    const res = await apiSendMessage({ receiverId, content })
    const msg = res.data.data
    console.log('HTTP response received:', msg)

    if (msg && msg.id) {
      const exists = messages.value.some(m => m.id === msg.id)
      console.log('HTTP: message exists in list?', exists)
      if (!exists) {
        messages.value.push(msg)
        console.log('HTTP: message added, total:', messages.value.length)
      }
      scrollToBottom()
    }
  } catch (e) {
    console.error('消息发送失败:', e)
    inputMessage.value = content
  }
}

// ===== 滚动 =====

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

// ===== 时间格式化 =====

const shouldShowTime = (msg: ChatMessage, index: number) => {
  if (index === 0) return true
  const prev = messages.value[index - 1]
  if (!prev || !msg.createdAt || !prev.createdAt) return true
  const diff = new Date(msg.createdAt).getTime() - new Date(prev.createdAt).getTime()
  return isNaN(diff) || diff > 5 * 60 * 1000
}

const formatChatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return ''

  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  const isYesterday = date.toDateString() === yesterday.toDateString()

  const timeStr = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

  if (isToday) return timeStr
  if (isYesterday) return `昨天 ${timeStr}`

  const month = date.getMonth() + 1
  const day = date.getDate()
  const isThisYear = date.getFullYear() === now.getFullYear()
  if (isThisYear) return `${month}/${day} ${timeStr}`
  return `${date.getFullYear()}/${month}/${day} ${timeStr}`
}

const formatRelativeTime = (time: string | null) => {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return ''

  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 24 * 60 * 60 * 1000) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (date.toDateString() === yesterday.toDateString()) return '昨天'

  const isThisYear = date.getFullYear() === now.getFullYear()
  if (isThisYear) return `${date.getMonth() + 1}/${date.getDate()}`
  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
}

// ===== 路由变化监听 =====

watch(() => route.params.userId, (newVal) => {
  if (!newVal && currentChatUser.value) {
    currentChatUser.value = null
    messages.value = []
    loadChatPartners()
  }
})
</script>

<style scoped>
.chat-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-color);
}

/* 顶部栏 */
.chat-top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-light);
}

.chat-top-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}

.status-tag {
  font-size: 11px;
}

/* 联系人列表 */
.contacts-list {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.contact-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-light);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  transition: background 0.15s;
}

.contact-item:active {
  background: #F0F0F0;
}

.contact-avatar {
  position: relative;
  flex-shrink: 0;
}

.unread-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  line-height: 16px;
  text-align: center;
  font-size: 10px;
  color: #FFFFFF;
  background: #F5222D;
  border-radius: 10px;
  font-weight: 500;
}

.contact-info {
  flex: 1;
  margin-left: 12px;
  overflow: hidden;
}

.contact-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.contact-name {
  font-weight: 500;
  font-size: 15px;
  color: var(--text-primary);
}

.contact-time {
  font-size: 11px;
  color: var(--text-placeholder);
  flex-shrink: 0;
  margin-left: 8px;
}

.contact-preview {
  margin-top: 4px;
  font-size: 13px;
  color: var(--text-placeholder);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.no-contacts {
  padding: 60px 0;
}

/* 聊天窗口 */
.chat-window {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  background: #EDEDED;
  z-index: 200;
}

.wc-header {
  display: flex;
  align-items: center;
  height: 48px;
  background: #EDEDED;
  border-bottom: 1px solid #D9D9D9;
  padding: 0 12px;
  flex-shrink: 0;
}

.wc-header-left {
  width: 40px;
  display: flex;
  align-items: center;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  color: var(--text-primary);
}

.wc-header-title {
  flex: 1;
  text-align: center;
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
}

.wc-header-right {
  width: 40px;
}

.wc-messages {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 8px 12px 12px;
}

.loading-more {
  display: flex;
  justify-content: center;
  padding: 12px 0;
  color: var(--text-placeholder);
}

.wc-time-divider {
  text-align: center;
  padding: 8px 0 12px;
  font-size: 11px;
  color: #B2B2B2;
}

.wc-msg-row {
  margin-bottom: 16px;
}

.wc-msg-bubble-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.wc-msg-row.is-self .wc-msg-bubble-row {
  flex-direction: row-reverse;
}

.wc-avatar {
  flex-shrink: 0;
  background: #C9C9C9;
}

.wc-bubble {
  max-width: 65%;
  position: relative;
}

.wc-bubble .wc-bubble-text {
  display: inline-block;
  padding: 9px 13px;
  font-size: 15px;
  line-height: 1.5;
  word-break: break-word;
  border-radius: 4px;
  background: #FFFFFF;
  color: var(--text-primary);
}

.wc-bubble:not(.bubble-self)::before {
  content: '';
  position: absolute;
  top: 10px;
  left: -6px;
  width: 0;
  height: 0;
  border-top: 5px solid transparent;
  border-bottom: 5px solid transparent;
  border-right: 6px solid #FFFFFF;
}

.wc-bubble.bubble-self .wc-bubble-text {
  background: #95EC69;
  color: #000000;
}

.wc-bubble.bubble-self::after {
  content: '';
  position: absolute;
  top: 10px;
  right: -6px;
  width: 0;
  height: 0;
  border-top: 5px solid transparent;
  border-bottom: 5px solid transparent;
  border-left: 6px solid #95EC69;
}

.wc-input-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #F7F7F7;
  border-top: 1px solid #D9D9D9;
  flex-shrink: 0;
}

.wc-input {
  flex: 1;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #D9D9D9;
  border-radius: 4px;
  font-size: 15px;
  background: #FFFFFF;
  outline: none;
  color: var(--text-primary);
}

.wc-input:focus {
  border-color: #B0B0B0;
}

.wc-send-btn {
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  background: #D9D9D9;
  color: #FFFFFF;
  transition: background 0.2s;
  flex-shrink: 0;
  -webkit-tap-highlight-color: transparent;
}

.wc-send-btn.active {
  background: #07C160;
  color: #FFFFFF;
}

.wc-send-btn:disabled {
  cursor: default;
}
</style>
