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

class SSEService {
  private eventSource: EventSource | null = null
  private status: ConnectionStatus = 'disconnected'
  private messageCallbacks: MessageCallback[] = []
  private statusCallbacks: ((status: ConnectionStatus) => void)[] = []
  private reconnectAttempts = 0
  private maxReconnectAttempts = 10
  private reconnectDelay = 3000
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private savedToken: string | null = null
  private isOpen = false

  connect(token: string): Promise<boolean> {
    return new Promise((resolve) => {
      if (this.eventSource && this.status === 'connected') {
        resolve(true)
        return
      }

      this.savedToken = token
      this.close()
      this.setStatus('connecting')
      this.isOpen = false

      const url = `/api/sse/chat?token=${encodeURIComponent(token)}`
      console.log('SSE connecting to:', url)
      
      this.eventSource = new EventSource(url)
      console.log('EventSource created, readyState:', this.eventSource.readyState)

      const connectTimeout = setTimeout(() => {
        if (!this.isOpen) {
          console.warn('SSE connection timeout after 5s')
          this.setStatus('error')
          this.eventSource?.close()
          this.eventSource = null
          resolve(false)
        }
      }, 5000)

      this.eventSource.addEventListener('connected', (event) => {
        console.log('SSE connected event from server:', event.data)
        this.isOpen = true
        clearTimeout(connectTimeout)
        this.setStatus('connected')
        this.reconnectAttempts = 0
        resolve(true)
      })

      this.eventSource.onopen = () => {
        console.log('SSE onopen fired, readyState:', this.eventSource?.readyState)
      }

      this.eventSource.addEventListener('chat', (event) => {
        try {
          const data = JSON.parse(event.data)
          console.log('SSE chat message received:', data)
          
          let createdAt = data.createdAt
          if (Array.isArray(createdAt)) {
            const [y, mo, d, h = 0, mi = 0, s = 0] = createdAt
            createdAt = new Date(y, mo - 1, d, h, mi, s).toISOString()
          } else if (typeof createdAt === 'number') {
            createdAt = new Date(createdAt).toISOString()
          } else if (typeof createdAt === 'string' && createdAt.includes('T')) {
          } else if (createdAt) {
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
        } catch (e) {
          console.error('Failed to parse SSE message:', e)
        }
      })

      this.eventSource.onerror = (error) => {
        console.error('SSE connection error:', error)
        
        if (!this.isOpen) {
          clearTimeout(connectTimeout)
          this.setStatus('error')
          resolve(false)
        }

        if (this.eventSource) {
          if (this.eventSource.readyState === EventSource.CONNECTING) {
            this.setStatus('connecting')
          } else {
            console.log('SSE closed, attempting reconnect...')
            this.eventSource.close()
            this.eventSource = null
            this.setStatus('disconnected')
            this.isOpen = false
            
            if (this.reconnectAttempts < this.maxReconnectAttempts && this.savedToken) {
              this.reconnectAttempts++
              console.log(`SSE reconnect attempt (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
              this.reconnectTimer = setTimeout(() => {
                this.connect(this.savedToken!)
              }, this.reconnectDelay)
            }
          }
        }
      }
    })
  }

  disconnect() {
    this.close()
    this.setStatus('disconnected')
    console.log('SSE disconnected')
  }

  private close() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }
    this.isOpen = false
  }

  reconnect(token?: string) {
    this.reconnectAttempts = 0
    if (this.eventSource) {
      this.close()
    }
    if (!token && !this.savedToken) return Promise.reject('No token')
    return this.connect(token || this.savedToken!)
  }

  isConnected(): boolean {
    return this.eventSource !== null && this.status === 'connected'
  }

  getStatus(): ConnectionStatus {
    return this.status
  }

  private setStatus(status: ConnectionStatus) {
    this.status = status
    this.statusCallbacks.forEach(cb => cb(status))
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

export const sseService = new SSEService()
