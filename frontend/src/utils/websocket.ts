export interface ChatMessage {
  id: number
  senderId: number
  senderName: string
  receiverId: number
  receiverName: string
  content: string
  messageType: string
  isRead: boolean
  createdAt: string
}

export interface MessageCallback {
  (message: ChatMessage): void
}

export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'error'

class WebSocketService {
  private ws: WebSocket | null = null
  private status: ConnectionStatus = 'disconnected'
  private messageCallbacks: MessageCallback[] = []
  private statusCallbacks: ((status: ConnectionStatus) => void)[] = []
  private reconnectAttempts = 0
  private maxReconnectAttempts = 10
  private reconnectDelay = 3000
  private savedToken: string | null = null

  connect(token: string): Promise<boolean> {
    return new Promise((resolve, reject) => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        resolve(true)
        return
      }

      this.savedToken = token
      this.setStatus('connecting')
      const wsUrl = `ws://localhost:8090/ws/chat?token=${token}`

      try {
        this.ws = new WebSocket(wsUrl)

        this.ws.onopen = () => {
          console.log('WebSocket connected')
          this.setStatus('connected')
          this.reconnectAttempts = 0
          resolve(true)
        }

        this.ws.onmessage = (event) => {
          try {
            const data = JSON.parse(event.data)
            console.log('WebSocket onmessage raw:', data)
            if (data.error) {
              console.warn('WebSocket error message:', data.error)
              return
            }
            // 只处理包含 senderId 和 content 的聊天消息
            if (data.senderId && data.content) {
              // 兼容多种 createdAt 格式：ISO字符串、数组[2026,4,14,10,30]、数字时间戳
              let createdAt = data.createdAt
              if (Array.isArray(createdAt)) {
                // Jackson LocalDateTime 数组格式: [year, month, day, hour, minute, second, nano]
                const [y, mo, d, h = 0, mi = 0, s = 0] = createdAt
                createdAt = new Date(y, mo - 1, d, h, mi, s).toISOString()
              } else if (typeof createdAt === 'number') {
                createdAt = new Date(createdAt).toISOString()
              } else if (typeof createdAt === 'string' && createdAt.includes('T')) {
                // 已经是 ISO 格式，直接使用
              } else if (createdAt) {
                // 其他字符串格式，尝试解析
                const parsed = new Date(createdAt)
                createdAt = isNaN(parsed.getTime()) ? new Date().toISOString() : parsed.toISOString()
              } else {
                createdAt = new Date().toISOString()
              }

              const msg: ChatMessage = {
                id: data.id,
                senderId: data.senderId,
                senderName: data.senderName || '',
                receiverId: data.receiverId,
                receiverName: data.receiverName || '',
                content: data.content,
                messageType: data.messageType || 'TEXT',
                isRead: data.isRead || false,
                createdAt
              }
              this.messageCallbacks.forEach(cb => cb(msg))
            }
          } catch (e) {
            console.error('Failed to parse WebSocket message:', e)
          }
        }

        this.ws.onerror = (error) => {
          console.error('WebSocket error:', error)
          this.setStatus('error')
          reject(error)
        }

        this.ws.onclose = () => {
          console.log('WebSocket closed')
          this.setStatus('disconnected')
          this.ws = null

          if (this.reconnectAttempts < this.maxReconnectAttempts && this.savedToken) {
            this.reconnectAttempts++
            console.log(`Attempting reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
            setTimeout(() => {
              if (this.savedToken) {
                this.connect(this.savedToken)
              }
            }, this.reconnectDelay)
          }
        }
      } catch (e) {
        this.setStatus('error')
        reject(e)
      }
    })
  }

  disconnect() {
    this.savedToken = null
    this.reconnectAttempts = this.maxReconnectAttempts
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.setStatus('disconnected')
    console.log('WebSocket disconnected')
  }

  reconnect(token?: string) {
    this.reconnectAttempts = 0
    const t = token || this.savedToken
    if (!t) return Promise.reject('No token')
    if (this.ws) {
      this.savedToken = null
      this.ws.close()
      this.ws = null
    }
    return this.connect(t)
  }

  isConnected(): boolean {
    return this.ws !== null && this.ws.readyState === WebSocket.OPEN
  }

  getStatus(): ConnectionStatus {
    return this.status
  }

  private setStatus(status: ConnectionStatus) {
    this.status = status
    this.statusCallbacks.forEach(cb => cb(status))
  }

  sendMessage(receiverId: number, content: string): boolean {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket not connected, cannot send message')
      return false
    }

    const message = {
      type: 'chat.send',
      receiverId,
      content,
      messageType: 'TEXT'
    }

    console.log('Sending via WebSocket:', message)
    this.ws.send(JSON.stringify(message))
    return true
  }

  onMessage(callback: MessageCallback) {
    this.messageCallbacks.push(callback)
  }

  removeMessageCallback(callback: MessageCallback) {
    this.messageCallbacks = this.messageCallbacks.filter(cb => cb !== callback)
  }

  onStatusChange(callback: (status: ConnectionStatus) => void) {
    this.statusCallbacks.push(callback)
  }

  removeStatusCallback(callback: (status: ConnectionStatus) => void) {
    this.statusCallbacks = this.statusCallbacks.filter(cb => cb !== callback)
  }
}

export const wsService = new WebSocketService()
