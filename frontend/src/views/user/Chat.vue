<template>
  <MobileLayout title="即时消息">
    <div class="chat-page">
      <!-- 连接状态 -->
      <div class="connection-status">
        <el-tag :type="statusType">{{ statusText }}</el-tag>
        <el-button v-if="wsStatus !== 'connected'" size="small" @click="handleReconnect">重连</el-button>
      </div>

      <!-- 联系人列表 -->
      <div class="contacts-panel" v-if="!currentChatUser">
        <div class="contacts-header">
          <h3>聊天列表</h3>
          <el-button size="small" @click="showNewChatDialog = true">新建聊天</el-button>
        </div>
        <div class="contacts-list">
          <div 
            v-for="partner in chatPartners" 
            :key="partner.id"
            class="contact-item"
            @click="startChat(partner)"
          >
            <el-avatar :size="40">{{ partner.username?.charAt(0) }}</el-avatar>
            <div class="contact-info">
              <span class="contact-name">{{ partner.username }}</span>
              <span class="contact-preview">{{ partner.lastMessage || '点击开始聊天' }}</span>
            </div>
          </div>
          <div v-if="chatPartners.length === 0" class="no-contacts">
            暂无聊天记录，点击上方按钮开始新聊天
          </div>
        </div>
      </div>

      <!-- 聊天窗口 -->
      <div class="chat-window" v-if="currentChatUser">
        <div class="chat-header">
          <el-button text @click="currentChatUser = null">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <span class="chat-title">{{ currentChatUser.username }}</span>
        </div>

        <div class="messages-container" ref="messagesRef">
          <div 
            v-for="msg in messages" 
            :key="msg.id"
            :class="['message-item', msg.senderId === currentUserId ? 'sent' : 'received']"
          >
            <div class="message-content">{{ msg.content }}</div>
            <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
          </div>
        </div>

        <div class="input-area">
          <el-input 
            v-model="inputMessage"
            placeholder="输入消息..."
            @keyup.enter="sendMessage"
          />
          <el-button type="primary" @click="sendMessage" :disabled="!inputMessage.trim() || wsStatus !== 'connected'">
            发送
          </el-button>
        </div>
      </div>

      <!-- 新建聊天对话框 -->
      <el-dialog v-model="showNewChatDialog" title="选择聊天对象" width="90%">
        <el-input v-model="searchUsername" placeholder="输入用户ID" />
        <div class="user-list">
          <div class="user-item" @click="startNewChat({ id: 2, username: 'lisi' })">
            <el-avatar :size="36">L</el-avatar>
            <span>lisi (ID: 2)</span>
          </div>
          <div class="user-item" @click="startNewChat({ id: 3, username: 'wangwu' })">
            <el-avatar :size="36">W</el-avatar>
            <span>wangwu (ID: 3)</span>
          </div>
        </div>
      </el-dialog>
    </div>
  </MobileLayout>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import MobileLayout from '@/layouts/MobileLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { wsService, type ChatMessage, type ConnectionStatus } from '@/utils/websocket'
import { getConversation, getChatPartners, sendMessage as apiSendMessage } from '@/api/chat'

interface ChatPartner {
  id: number
  username: string
  lastMessage?: string
}

const authStore = useAuthStore()
const currentUserId = computed(() => authStore.userInfo?.id)

const messages = ref<ChatMessage[]>([])
const chatPartners = ref<ChatPartner[]>([])
const currentChatUser = ref<ChatPartner | null>(null)
const inputMessage = ref('')
const messagesRef = ref<HTMLElement | null>(null)
const showNewChatDialog = ref(false)
const searchUsername = ref('')
const wsStatus = ref<ConnectionStatus>('disconnected')

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
    case 'connecting': return '连接中...'
    case 'error': return '连接失败'
    default: return '未连接'
  }
})

onMounted(async () => {
  await loadChatPartners()
  connectWebSocket()
})

onUnmounted(() => {
  wsService.disconnect()
})

const connectWebSocket = async () => {
  const token = localStorage.getItem('token')
  if (!token) return

  try {
    await wsService.connect(token.replace('Bearer ', ''))
    ElMessage.success('WebSocket连接成功')
    
    wsService.onMessage((msg: ChatMessage) => {
      if (currentChatUser.value && msg.senderId === currentChatUser.value.id) {
        messages.value.push(msg)
        scrollToBottom()
      }
      ElMessage.success(`收到来自 ${msg.senderName} 的消息`)
    })

    wsService.onStatusChange((status) => {
      wsStatus.value = status
    })
  } catch (e) {
    console.error('WebSocket连接失败:', e)
    ElMessage.error('WebSocket连接失败')
  }
}

const handleReconnect = () => {
  const token = localStorage.getItem('token')
  if (!token) return
  wsService.reconnect(token.replace('Bearer ', ''))
}

const loadChatPartners = async () => {
  try {
    const res = await getChatPartners()
    const partnerIds = res.data.data || []
    chatPartners.value = partnerIds.map(id => ({
      id,
      username: `用户${id}`
    }))
  } catch (e) {
    console.error('加载聊天列表失败:', e)
  }
}

const startChat = async (partner: ChatPartner) => {
  currentChatUser.value = partner
  messages.value = []
  
  try {
    const res = await getConversation(partner.id)
    messages.value = res.data.data || []
    scrollToBottom()
  } catch (e) {
    console.error('加载聊天记录失败:', e)
  }
}

const startNewChat = (user: ChatPartner) => {
  showNewChatDialog.value = false
  currentChatUser.value = user
  messages.value = []
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || !currentChatUser.value) return

  const content = inputMessage.value.trim()
  inputMessage.value = ''

  try {
    const res = await apiSendMessage({
      receiverId: currentChatUser.value.id,
      content
    })
    
    messages.value.push(res.data.data)
    scrollToBottom()
  } catch (e) {
    ElMessage.error('消息发送失败')
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const formatTime = (time: string) => {
  const date = new Date(time)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.chat-page {
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.connection-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 8px;
  background: white;
  border-bottom: 1px solid #eee;
}

.contacts-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.contacts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: white;
  border-bottom: 1px solid #eee;
}

.contacts-list {
  flex: 1;
  overflow-y: auto;
}

.contact-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #eee;
  cursor: pointer;
}

.contact-item:hover {
  background: #f9f9f9;
}

.contact-info {
  margin-left: 12px;
  flex: 1;
}

.contact-name {
  font-weight: 500;
}

.contact-preview {
  color: #999;
  font-size: 12px;
}

.no-contacts {
  padding: 40px;
  text-align: center;
  color: #999;
}

.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: linear-gradient(135deg, #FDE68A 0%, #F59E0B 100%);
}

.chat-title {
  flex: 1;
  text-align: center;
  font-weight: 600;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f5f5;
}

.message-item {
  margin-bottom: 12px;
  max-width: 70%;
}

.message-item.sent {
  margin-left: auto;
}

.message-item.received {
  margin-right: auto;
}

.message-content {
  padding: 10px 14px;
  border-radius: 8px;
  word-break: break-word;
}

.message-item.sent .message-content {
  background: #F59E0B;
  color: white;
}

.message-item.received .message-content {
  background: white;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}

.message-item.sent .message-time {
  text-align: right;
}

.input-area {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background: white;
  border-top: 1px solid #eee;
}

.input-area .el-input {
  flex: 1;
}

.user-list {
  margin-top: 16px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  cursor: pointer;
  border-radius: 8px;
}

.user-item:hover {
  background: #f5f5f5;
}
</style>