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
  private maxReconnectAttempts = 5
  private reconnectDelay = 3000

  connect(token: string): Promise<boolean> {
    return new Promise((resolve, reject) => {
      if (this.ws && this.ws.readyState === WebSocket.OPEN) {
        resolve(true)
        return
      }

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
            console.log('WebSocket message received:', data)
            
            if (data.type === 'message' || data.content) {
              const msg: ChatMessage = data
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
          
          if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++
            console.log(`Attempting reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
            setTimeout(() => {
              this.connect(token)
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
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.setStatus('disconnected')
    this.reconnectAttempts = this.maxReconnectAttempts
    console.log('WebSocket disconnected')
  }

  reconnect(token: string) {
    this.reconnectAttempts = 0
    this.disconnect()
    return this.connect(token)
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
      console.error('WebSocket not connected')
      return false
    }

    const message = {
      type: 'chat.send',
      receiverId,
      content,
      messageType: 'TEXT'
    }

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