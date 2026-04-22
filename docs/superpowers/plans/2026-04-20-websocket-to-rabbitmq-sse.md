# WebSocket 迁移到 RabbitMQ + SSE 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 WebSocket 聊天消息推送替换为 RabbitMQ + SSE 方案，实现消息推送解耦。

**Architecture:** 前端通过 SSE 连接接收消息，后端通过 RabbitMQ 队列解耦消息生产与消费，SSESessionManager 管理用户连接。

**Tech Stack:** Spring Boot 3.3.4, Spring AMQP (RabbitMQ), SSE (Server-Sent Events)

---

## 文件结构

```
backend/src/main/java/com/aisale/backend/
├── config/
│   ├── RabbitMQConfig.java          # 新增：RabbitMQ 配置
│   └── WebSocketConfig.java         # 删除：废弃 WebSocket
├── controller/
│   ├── SSEChatController.java       # 新增：SSE 连接端点
│   └── UserChatController.java      # 保留：消息发送接口
├── service/
│   ├── SSESessionManager.java       # 新增：SSE 会话管理
│   ├── ChatMessageProducer.java     # 新增：RabbitMQ 生产者
│   ├── ChatMessageConsumer.java     # 新增：RabbitMQ 消费者
│   ├── ChatService.java             # 修改：移除 Event，调用 Producer
│   └── ChatMessageEvent.java        # 删除：废弃 Event
├── websocket/                        # 删除：整个目录
│   ├── ChatWebSocketHandler.java
│   ├── ChatMessageEventListener.java
│   ├── JwtHandshakeInterceptor.java
│   ├── WebSocketAuthChannelInterceptor.java
│   └── WebSocketPrincipal.java
│   └── WebSocketMessageController.java
├── dto/
│   └── SSEMessageDTO.java           # 新增：SSE 消息格式（可选，可复用 ChatMessageResponse）
└── pom.xml                          # 修改：添加 AMQP 依赖
```

---

## Task 1: 添加 RabbitMQ 依赖和配置

**Files:**
- Modify: `backend/pom.xml`
- Create: `backend/src/main/java/com/aisale/backend/config/RabbitMQConfig.java`

- [ ] **Step 1: 添加 RabbitMQ 依赖到 pom.xml**

在 `pom.xml` 的 `<dependencies>` 部分，WebSocket 依赖后添加：

```xml
<!-- RabbitMQ (AMQP) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

位置：在第 160-164 行 WebSocket 依赖之后添加。

- [ ] **Step 2: 创建 RabbitMQConfig.java**

```java
package com.aisale.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String CHAT_QUEUE = "chat.message.queue";
    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String CHAT_ROUTING_KEY = "chat.message";

    @Bean
    public Queue chatQueue() {
        return new Queue(CHAT_QUEUE, true); // 持久化队列
    }

    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange(CHAT_EXCHANGE);
    }

    @Bean
    public Binding chatBinding() {
        return BindingBuilder.bind(chatQueue())
            .to(chatExchange())
            .with(CHAT_ROUTING_KEY);
    }
}
```

- [ ] **Step 3: 添加 application.yml RabbitMQ 配置（可选）**

如果需要，在 `application.yml` 添加：

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

- [ ] **Step 4: 提交**

```bash
git add backend/pom.xml backend/src/main/java/com/aisale/backend/config/RabbitMQConfig.java
git commit -m "feat: 添加 RabbitMQ 依赖和配置

- 添加 spring-boot-starter-amqp 依赖
- 创建 RabbitMQConfig 配置队列和交换机

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 2: 创建 SSESessionManager

**Files:**
- Create: `backend/src/main/java/com/aisale/backend/service/SSESessionManager.java`

- [ ] **Step 1: 创建 SSESessionManager.java**

```java
package com.aisale.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SSESessionManager {

    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 注册用户 SSE 连接
     */
    public SseEmitter register(Long userId) {
        // 30分钟超时
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.put(userId, emitter);
        
        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.info("SSE connection completed for userId={}", userId);
        });
        
        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.info("SSE connection timeout for userId={}", userId);
        });
        
        emitter.onError(e -> {
            emitters.remove(userId);
            log.error("SSE connection error for userId={}: {}", userId, e.getMessage());
        });

        log.info("SSE registered for userId={}", userId);
        return emitter;
    }

    /**
     * 向指定用户发送消息
     */
    public void sendToUser(Long userId, Object message) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("chat")
                    .data(message));
                log.debug("SSE message sent to userId={}", userId);
            } catch (Exception e) {
                log.error("SSE send failed for userId={}: {}", userId, e.getMessage());
                emitters.remove(userId);
            }
        } else {
            log.debug("User userId={} is offline, message will be stored", userId);
        }
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        return emitters.containsKey(userId);
    }

    /**
     * 移除用户连接
     */
    public void remove(Long userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE connection removed for userId={}", userId);
        }
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineCount() {
        return emitters.size();
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/aisale/backend/service/SSESessionManager.java
git commit -m "feat: 创建 SSESessionManager 管理 SSE 连接

- 支持用户注册/移除连接
- 支持向指定用户推送消息
- 支持检查用户在线状态

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 3: 创建 RabbitMQ 消息生产者

**Files:**
- Create: `backend/src/main/java/com/aisale/backend/service/ChatMessageProducer.java`

- [ ] **Step 1: 创建 ChatMessageProducer.java**

```java
package com.aisale.backend.service;

import com.aisale.backend.config.RabbitMQConfig;
import com.aisale.backend.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送聊天消息到 RabbitMQ
     */
    public void sendChatMessage(Long targetUserId, ChatMessageResponse message) {
        try {
            ChatMessagePayload payload = new ChatMessagePayload(targetUserId, message);
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                RabbitMQConfig.CHAT_ROUTING_KEY,
                payload
            );
            log.info("Message sent to RabbitMQ: targetUserId={}, messageId={}", 
                targetUserId, message.getId());
        } catch (Exception e) {
            log.error("Failed to send message to RabbitMQ: {}", e.getMessage());
        }
    }

    /**
     * 消息载体（内部类）
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ChatMessagePayload {
        private Long targetUserId;
        private ChatMessageResponse message;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/aisale/backend/service/ChatMessageProducer.java
git commit -m "feat: 创建 ChatMessageProducer 发送消息到 RabbitMQ

- 使用 RabbitTemplate 发送消息
- 封装 ChatMessagePayload 消息载体

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 4: 创建 RabbitMQ 消息消费者

**Files:**
- Create: `backend/src/main/java/com/aisale/backend/service/ChatMessageConsumer.java`

- [ ] **Step 1: 创建 ChatMessageConsumer.java**

```java
package com.aisale.backend.service;

import com.aisale.backend.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatMessageConsumer {

    private final SSESessionManager sseSessionManager;

    /**
     * 监听聊天消息队列
     */
    @RabbitListener(queues = RabbitMQConfig.CHAT_QUEUE)
    public void handleChatMessage(ChatMessageProducer.ChatMessagePayload payload) {
        Long targetUserId = payload.getTargetUserId();
        Object message = payload.getMessage();

        log.info("Received message from RabbitMQ: targetUserId={}", targetUserId);

        // 通过 SSE 推送给在线用户
        sseSessionManager.sendToUser(targetUserId, message);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/aisale/backend/service/ChatMessageConsumer.java
git commit -m "feat: 创建 ChatMessageConsumer 消费 RabbitMQ 消息

- 使用 @RabbitListener 监听队列
- 通过 SSESessionManager 推送给用户

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 5: 创建 SSE Controller

**Files:**
- Create: `backend/src/main/java/com/aisale/backend/controller/SSEChatController.java`

- [ ] **Step 1: 创建 SSEChatController.java**

```java
package com.aisale.backend.controller;

import com.aisale.backend.service.SSESessionManager;
import com.aisale.backend.util.JwtRequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "SSE 聊天", description = "Server-Sent Events 聊天消息推送")
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Slf4j
public class SSEChatController {

    private final SSESessionManager sseSessionManager;
    private final JwtRequestUtils jwtRequestUtils;

    @Operation(summary = "建立 SSE 连接")
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(HttpServletRequest request) {
        Long userId = jwtRequestUtils.getCurrentUserId(request);
        log.info("SSE connection request from userId={}", userId);
        return sseSessionManager.register(userId);
    }

    @Operation(summary = "断开 SSE 连接")
    @DeleteMapping("/chat")
    public void disconnect(HttpServletRequest request) {
        Long userId = jwtRequestUtils.getCurrentUserId(request);
        log.info("SSE disconnect request from userId={}", userId);
        sseSessionManager.remove(userId);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/aisale/backend/controller/SSEChatController.java
git commit -m "feat: 创建 SSEChatController 提供 SSE 连接端点

- GET /api/sse/chat 建立 SSE 连接
- DELETE /api/sse/chat 断开连接

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 6: 修改 ChatService

**Files:**
- Modify: `backend/src/main/java/com/aisale/backend/service/ChatService.java`

- [ ] **Step 1: 修改 ChatService.java**

原文件需要修改以下内容：

**1. 移除 ApplicationEventPublisher 依赖**

将第 28 行：
```java
private final ApplicationEventPublisher eventPublisher;
```

替换为：
```java
private final ChatMessageProducer chatMessageProducer;
```

**2. 移除 import**

删除：
```java
import org.springframework.context.ApplicationEventPublisher;
```

添加：
```java
import com.aisale.backend.service.ChatMessageProducer;
```

**3. 修改 sendMessage 方法**

将第 53-56 行：
```java
// 通过 Spring 事件机制异步推送给接收方（避免循环依赖）
eventPublisher.publishEvent(new ChatMessageEvent(this, receiverId, response));
// 同时也给发送方回执（支持发送方多标签页场景）
eventPublisher.publishEvent(new ChatMessageEvent(this, senderId, response));
```

替换为：
```java
// 通过 RabbitMQ 推送给接收方和发送方
chatMessageProducer.sendChatMessage(receiverId, response);
chatMessageProducer.sendChatMessage(senderId, response);
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/aisale/backend/service/ChatService.java
git commit -m "refactor: ChatService 使用 RabbitMQ 替代 Event 发布消息

- 移除 ApplicationEventPublisher
- 使用 ChatMessageProducer 发送消息到队列

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 7: 删除废弃的 WebSocket 相关文件

**Files:**
- Delete: `backend/src/main/java/com/aisale/backend/config/WebSocketConfig.java`
- Delete: `backend/src/main/java/com/aisale/backend/websocket/` 整个目录
- Delete: `backend/src/main/java/com/aisale/backend/service/ChatMessageEvent.java`

- [ ] **Step 1: 删除 WebSocket 相关文件**

```bash
rm -rf backend/src/main/java/com/aisale/backend/websocket/
rm backend/src/main/java/com/aisale/backend/config/WebSocketConfig.java
rm backend/src/main/java/com/aisale/backend/service/ChatMessageEvent.java
```

- [ ] **Step 2: 移除 pom.xml 中的 WebSocket 依赖（可选保留）**

如果不再需要 WebSocket，可移除第 160-164 行：
```xml
<!-- WebSocket -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

- [ ] **Step 3: 提交**

```bash
git add -A
git commit -m "refactor: 删除废弃的 WebSocket 相关文件

- 删除 websocket 目录下所有文件
- 删除 WebSocketConfig.java
- 删除 ChatMessageEvent.java

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 8: 更新 SecurityConfig 放开 SSE 端点

**Files:**
- Modify: `backend/src/main/java/com/aisale/backend/security/SecurityConfig.java`

- [ ] **Step 1: 添加 SSE 端点到安全配置**

在 SecurityConfig.java 中，确保 `/api/sse/**` 端点需要认证（与 WebSocket 类似）。

如果使用 `.requestMatchers()` 配置，添加：
```java
.requestMatchers("/api/sse/**").authenticated()
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/aisale/backend/security/SecurityConfig.java
git commit -m "security: 更新 SecurityConfig 添加 SSE 端点配置

Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>"
```

---

## Task 9: 运行测试验证

- [ ] **Step 1: 运行所有测试**

```bash
cd backend && mvn test
```

预期：所有测试通过。

- [ ] **Step 2: 检查 RabbitMQ 连接（如果已安装）**

启动应用，检查日志中是否有 RabbitMQ 连接成功信息。

- [ ] **Step 3: 提交最终状态**

```bash
git status
git log --oneline -5
```

---

## 自审检查清单

| 检查项 | 状态 |
|-------|------|
| Spec覆盖率 | ✅ 所有设计要求有对应任务 |
| 无占位符 | ✅ 所有代码完整 |
| 类型一致性 | ✅ ChatMessagePayload 在 Producer 和 Consumer 中定义一致 |
| 文件路径正确 | ✅ 所有路径符合项目结构 |

---

## 执行选项

**Plan complete and saved to `docs/superpowers/plans/2026-04-20-websocket-to-rabbitmq-sse.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**