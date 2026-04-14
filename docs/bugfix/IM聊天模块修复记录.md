# IM模块即时通讯Bug修复记录

## Bug 1: 消息头像和用户名显示错误

### 问题描述
- 用户 wangwu 发送消息给 lisi
- 在 lisi 的界面：自己发送的消息正确显示（绿色气泡，右侧）
- 在 wangwu 的界面：收到 lisi 的消息，但对方头像显示为 "W" 而不是 lisi 的头像
- wangwu 自己的消息没有在自己界面显示

### 根本原因
1. **头像显示逻辑错误**：原代码使用 `currentChatUser.username` 显示对方头像，但这个值在某些情况下可能不正确或未及时更新
2. **消息来源混淆**：没有正确区分"对方发来的消息"和"自己发送的消息"

### 解决方案
修改 `Chat.vue` 模板，使用消息自带的 `senderName` 显示头像：

```vue
<!-- 修复前：使用 currentChatUser.username -->
<el-avatar>
  {{ currentChatUser.username?.charAt(0) }}
</el-avatar>

<!-- 修复后：使用消息中的发送者名称 -->
<template v-if="msg.senderId !== currentUserId">
  <el-avatar>
    {{ msg.senderName?.charAt(0) || '?' }}
  </el-avatar>
</template>
```

---

## Bug 2: LocalDateTime 序列化格式不匹配

### 问题描述
消息时间显示为 "Invalid Date"，无法正确格式化显示。

### 根本原因
`ChatMessageResponse.createdAt` 是 `LocalDateTime` 类型，Jackson 默认序列化为数组格式 `[2026,4,14,10,30,0]`，而前端 `new Date()` 期望 ISO 字符串格式。

### 解决方案

**后端修复** - 在 `ChatMessageResponse.java` 添加 `@JsonFormat` 注解：

```java
@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
private LocalDateTime createdAt;
```

**前端兼容** - 在 `websocket.ts` 中处理多种时间格式：

```typescript
let createdAt = data.createdAt
if (Array.isArray(createdAt)) {
  // Jackson LocalDateTime 数组格式
  const [y, mo, d, h = 0, mi = 0, s = 0] = createdAt
  createdAt = new Date(y, mo - 1, d, h, mi, s).toISOString()
} else if (typeof createdAt === 'string' && createdAt.includes('T')) {
  // 已经是 ISO 格式
} else {
  createdAt = new Date().toISOString()
}
```

---

## Bug 3: 消息重复添加 (HTTP + WebSocket 回执)

### 问题描述
发送消息时，通过 HTTP 响应添加到消息列表后，WebSocket 回执又触发 `onWebSocketMessage` 再次尝试添加消息，导致重复。

### 根本原因
`ChatService.sendMessage()` 会通过 `ChatWebSocketHandler.sendMessageToUser()` 给发送方也推送一条回执消息。前端 HTTP 响应已添加消息，WebSocket 回执又触发添加。

### 解决方案
在 `onWebSocketMessage` 中使用消息 ID 进行去重检查：

```typescript
const onWebSocketMessage = (msg: ChatMessage) => {
  // 防止重复添加：检查消息是否已存在于列表中
  const exists = messages.value.some(m => m.id === msg.id)

  if (!exists) {
    messages.value.push(msg)
    scrollToBottom()
  }
}
```

---

## Bug 4: 循环依赖问题

### 问题描述
```
The dependencies of some of the beans form a cycle:
   ChatWebSocketHandler -> ChatService -> ChatWebSocketHandler
```

### 根本原因
`ChatWebSocketHandler` 依赖 `ChatService`，而 `ChatService` 又通过 `ApplicationContext.getBean()` 依赖 `ChatWebSocketHandler`。

### 解决方案
使用 Spring `ApplicationEventPublisher` 事件机制解耦：

**1. 创建事件类 `ChatMessageEvent.java`**：
```java
public class ChatMessageEvent extends ApplicationEvent {
    private final Long targetUserId;
    private final ChatMessageResponse message;

    public ChatMessageEvent(Object source, Long targetUserId, ChatMessageResponse message) {
        super(source);
        this.targetUserId = targetUserId;
        this.message = message;
    }
}
```

**2. 创建事件监听器 `ChatMessageEventListener.java`**：
```java
@Component
public class ChatMessageEventListener {
    private final ChatWebSocketHandler chatWebSocketHandler;

    @EventListener
    public void handleChatMessageEvent(ChatMessageEvent event) {
        chatWebSocketHandler.sendMessageToUser(event.getTargetUserId(), event.getMessage());
    }
}
```

**3. 修改 `ChatService.java`**：
```java
private final ApplicationEventPublisher eventPublisher;

public ChatMessageResponse sendMessage(Long senderId, Long receiverId, String content) {
    // ... 保存消息 ...
    
    // 通过事件机制推送，不再直接依赖 ChatWebSocketHandler
    eventPublisher.publishEvent(new ChatMessageEvent(this, receiverId, response));
    eventPublisher.publishEvent(new ChatMessageEvent(this, senderId, response));
    
    return response;
}
```

---

## Bug 5: 多标签页消息推送丢失

### 问题描述
如果用户在多个浏览器标签页打开聊天，只有一个标签页能收到 WebSocket 推送消息。

### 根本原因
`userSessionMap` 使用 `Map<Long, WebSocketSession>`，同一用户的多个 session 互相覆盖。

### 解决方案
改用 `List<WebSocketSession>` 支持多会话：

```java
// 修复前
private final Map<Long, WebSocketSession> userSessionMap = new ConcurrentHashMap<>();

// 修复后
private final Map<Long, List<WebSocketSession>> userSessionsMap = new ConcurrentHashMap<>();

@Override
public void afterConnectionEstablished(WebSocketSession session) {
    Long userId = (Long) session.getAttributes().get("userId");
    userSessionsMap.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(session);
}

public void sendMessageToUser(Long userId, ChatMessageResponse message) {
    List<WebSocketSession> sessions = userSessionsMap.get(userId);
    if (sessions == null || sessions.isEmpty()) return;

    for (WebSocketSession session : sessions) {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(json));
        }
    }
}
```

---

## Bug 6: WebSocket 消息参数校验缺失

### 问题描述
没有校验 WebSocket 接收的消息参数，可能导致空指针异常或无效数据。

### 解决方案
在 `ChatWebSocketHandler.handleTextMessage()` 中添加参数校验：

```java
ChatMessageRequest request = objectMapper.readValue(payload, ChatMessageRequest.class);

// 参数校验
if (request.getReceiverId() == null) {
    session.sendMessage(new TextMessage("{\"error\":\"receiverId is required\"}"));
    return;
}
if (request.getContent() == null || request.getContent().isBlank()) {
    session.sendMessage(new TextMessage("{\"error\":\"content is required\"}"));
    return;
}
// 不能给自己发消息
if (request.getReceiverId().equals(senderId)) {
    session.sendMessage(new TextMessage("{\"error\":\"cannot send message to yourself\"}"));
    return;
}
```

---

## Bug 7: 前端时间显示健壮性不足

### 问题描述
当时间格式解析失败时，显示 "Invalid Date"。

### 解决方案
添加时间格式校验：

```typescript
const formatChatTime = (time: string) => {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return ''  // 检查是否为有效日期

  // ... 格式化逻辑 ...
}

const formatRelativeTime = (time: string | null) => {
  if (!time) return ''
  const date = new Date(time)
  if (isNaN(date.getTime())) return ''  // 检查是否为有效日期

  // ... 格式化逻辑 ...
}
```

---

## Bug 8: JPQL LIMIT 语法不支持

### 问题描述
在 `findLatestMessage` 查询方法中使用 `LIMIT 1`，这是原生 SQL 语法，JPQL 不支持。

### 解决方案
使用 Spring Data 的 `PageRequest` 方法：

```java
// 修复前（JPQL不支持LIMIT）
@Query("SELECT m FROM ChatMessage m WHERE ... LIMIT 1")
ChatMessage findLatestMessage(Long userId1, Long userId2);

// 修复后
@Query("SELECT m FROM ChatMessage m WHERE ... ORDER BY m.createdAt DESC")
List<ChatMessage> findLatestMessage(Long userId1, Long userId2, Pageable pageable);

// 调用时
ChatMessage latestMsg = chatMessageRepository.findLatestMessage(
    currentUserId, partnerId, PageRequest.of(0, 1)
).stream().findFirst().orElse(null);
```

---

## 调试技巧

### 后端日志
添加详细的日志输出便于追踪问题：

```java
log.info("Message saved: id={}, from={}({}), to={}({})",
    savedMessage.getId(), senderId, sender.getUsername(), receiverId, receiver.getUsername());
log.info("Sending WebSocket message to userId {}: {}", userId, json);
```

### 前端日志
```typescript
console.log('WebSocket message received:', {
  msgId: msg.id,
  senderId: msg.senderId,
  senderName: msg.senderName,
  currentUserId: currentUserId.value,
  partnerId
})
```
