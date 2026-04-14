import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

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

export interface ChatPartner {
  userId: number
  username: string
  avatar: string | null
  lastMessage: string | null
  lastMessageTime: string | null
  unreadCount: number
}

export interface SendMessageRequest {
  receiverId: number
  content: string
  messageType?: string
}

export const sendMessage = (data: SendMessageRequest) => {
  return request<ApiResponse<ChatMessage>>({
    url: '/user/chat/send',
    method: 'post',
    data
  })
}

export const getConversation = (userId: number) => {
  return request<ApiResponse<ChatMessage[]>>({
    url: `/user/chat/conversation/${userId}`,
    method: 'get'
  })
}

export const getChatPartners = () => {
  return request<ApiResponse<number[]>>({
    url: '/user/chat/partners',
    method: 'get'
  })
}

export const getChatPartnersDetail = () => {
  return request<ApiResponse<ChatPartner[]>>({
    url: '/user/chat/partners/detail',
    method: 'get'
  })
}

export const getUnreadMessages = () => {
  return request<ApiResponse<ChatMessage[]>>({
    url: '/user/chat/unread',
    method: 'get'
  })
}

export const markAsRead = (messageId: number) => {
  return request<ApiResponse<void>>({
    url: `/user/chat/read/${messageId}`,
    method: 'put'
  })
}

export const markConversationAsRead = (userId: number) => {
  return request<ApiResponse<void>>({
    url: `/user/chat/conversation/${userId}/read`,
    method: 'put'
  })
}

export const getUserPublicInfo = (userId: number) => {
  return request<ApiResponse<{
    id: number
    username: string
    nickname: string
    avatar: string | null
  }>>({
    url: `/user/public/${userId}`,
    method: 'get'
  })
}
