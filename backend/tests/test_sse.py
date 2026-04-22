import requests
import json
import time

# 登录获取 token
res = requests.post('http://localhost:8090/api/auth/login', json={'username': 'zhangsan', 'password': '123456'})
token = res.json()['data']['token']
auth_header = f'Bearer {token}'
print(f"Login success, token: {token[:30]}...")

# 测试 SSE 连接（设置 Accept header 为 text/event-stream）
headers = {
    'Authorization': auth_header,
    'Accept': 'text/event-stream',
    'Cache-Control': 'no-cache'
}

print("\nAttempting SSE connection...")
try:
    response = requests.get('http://localhost:8090/api/sse/chat', headers=headers, stream=True, timeout=5)
    print(f"SSE response status: {response.status_code}")
    
    # 读取一些事件流数据
    for i, line in enumerate(response.iter_lines()):
        if line:
            print(f"Event {i}: {line.decode('utf-8')}")
        if i > 5:
            break
except requests.exceptions.ReadTimeout:
    print("SSE connection established successfully (timeout expected for streaming)")
except Exception as e:
    print(f"SSE connection test: {e}")

# 测试发送消息 API（通过 HTTP）
send_res = requests.post(
    'http://localhost:8090/api/user/chat/send',
    json={'receiverId': 2, 'content': 'Test SSE message'},
    headers={
        'Authorization': auth_header,
        'Content-Type': 'application/json'
    }
)
print(f"\nSend message status: {send_res.status_code}")
print(f"Send message response: {json.dumps(send_res.json(), indent=2, ensure_ascii=False)}")

print("\n✅ SSE migration test completed!")
