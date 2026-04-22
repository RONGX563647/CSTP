import requests
import json
import time
import threading

BASE_URL = 'http://localhost:8090'

def test_sse_realtime_push():
    """测试两个用户之间的 SSE 实时消息推送"""
    
    # 登录用户1 (zhangsan)
    res1 = requests.post(f'{BASE_URL}/api/auth/login', json={'username': 'zhangsan', 'password': '123456'})
    token1 = res1.json()['data']['token']
    user1_id = res1.json()['data']['id']
    print(f"用户1 (zhangsan) 登录成功, userId={user1_id}")
    
    # 登录用户2 (lisi)  
    res2 = requests.post(f'{BASE_URL}/api/auth/login', json={'username': 'lisi', 'password': '123456'})
    token2 = res2.json()['data']['token']
    user2_id = res2.json()['data']['id']
    print(f"用户2 (lisi) 登录成功, userId={user2_id}")
    
    received_messages = []
    sse_connected = threading.Event()
    
    def listen_sse():
        """在后台线程中监听 SSE 消息"""
        try:
            url = f'{BASE_URL}/api/sse/chat?token={token2}'
            print(f"\n用户2 开始监听 SSE 消息...")
            print(f"URL: {url[:50]}...")
            
            response = requests.get(url, stream=True, timeout=10)
            print(f"HTTP 状态码: {response.status_code}")
            
            if response.status_code == 200:
                print("用户2 SSE 连接成功!")
                sse_connected.set()
                
                line_count = 0
                for line in response.iter_lines():
                    line_count += 1
                    if line:
                        decoded = line.decode('utf-8')
                        print(f"  行 {line_count}: {decoded}")
                        if decoded.startswith('data:'):
                            data = json.loads(decoded[5:].strip())
                            received_messages.append(data)
                            print(f"  -> 解析到消息: {data.get('content')}")
                    if line_count > 20:
                        break
        except requests.exceptions.Timeout:
            print("SSE 监听超时 (正常)")
        except Exception as e:
            print(f"SSE 监听异常: {e}")
    
    # 启动 SSE 监听线程
    sse_thread = threading.Thread(target=listen_sse, daemon=True)
    sse_thread.start()
    
    # 等待 SSE 连接建立
    if sse_connected.wait(timeout=5):
        print("\n✅ SSE 连接已建立，准备发送消息...")
        time.sleep(2)  # 确保连接完全就绪
        
        # 用户1 发送消息给用户2
        print(f"\n用户1 发送消息给用户2 (userId={user2_id})...")
        send_res = requests.post(
            f'{BASE_URL}/api/user/chat/send',
            json={'receiverId': user2_id, 'content': 'Hello via SSE!'},
            headers={
                'Authorization': f'Bearer {token1}',
                'Content-Type': 'application/json'
            }
        )
        print(f"发送结果: {send_res.status_code} - {send_res.json()['message']}")
        
        # 等待消息推送
        time.sleep(3)
        
        # 检查结果
        if received_messages:
            print(f"\n✅ 成功! 用户2 通过 SSE 收到了 {len(received_messages)} 条消息:")
            for msg in received_messages:
                print(f"   - 来自: {msg.get('senderName')}, 内容: {msg.get('content')}")
        else:
            print("\n❌ 失败! 用户2 未收到 SSE 推送消息")
    else:
        print("❌ SSE 连接超时")
    
    time.sleep(1)
    print("\n✅ 测试完成!")

if __name__ == '__main__':
    test_sse_realtime_push()
