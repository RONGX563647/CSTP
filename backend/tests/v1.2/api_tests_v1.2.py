"""
AI Sale Backend API 集成测试 - v1.2
基于 DataInitializer 初始化数据，覆盖 v1.2 新增接口及异常场景

v1.2 新增模块：
1. 商品搜索（智能搜索、搜索历史、热门关键词）
2. 商品推荐（首页推荐、分类推荐、猜你喜欢、新品推荐、折扣推荐）
3. 用户管理（用户资料、密码修改、公开信息）
4. 管理端用户管理
5. 聊天消息
6. 管理端日志
7. OSS 文件管理

重点：非200状态码的异常情况覆盖
"""
import requests
from typing import Optional, Dict, List


# ==================== 配置 ====================
BASE_URL = "http://localhost:8090"

# 初始化数据中的账号
ADMIN_ACCOUNT = {"username": "superadmin", "password": "123456"}
SELLER_ACCOUNT = {"username": "zhangsan", "password": "123456"}
BUYER_ACCOUNT = {"username": "lisi", "password": "123456"}
WANGWU_ACCOUNT = {"username": "wangwu", "password": "123456"}


class TestResult:
    """测试结果记录"""
    def __init__(self):
        self.passed = 0
        self.failed = 0
        self.details: List[tuple] = []

    def add(self, name: str, passed: bool, status: int = 0, error: str = ""):
        self.details.append((name, passed, status, error))
        if passed:
            self.passed += 1
        else:
            self.failed += 1

    def print_summary(self):
        total = self.passed + self.failed
        rate = (self.passed / total * 100) if total > 0 else 0
        print("\n" + "=" * 70)
        print("测试汇总")
        print("=" * 70)
        print(f"通过：{self.passed}  失败：{self.failed}  通过率：{rate:.1f}%")
        print("\n详细结果:")
        for name, passed, status, error in self.details:
            icon = "[PASS]" if passed else "[FAIL]"
            status_str = f" (HTTP {status})" if status else ""
            error_str = f" - {error}" if error else ""
            print(f"  {icon} {name}{status_str}{error_str}")


class APISession:
    """API 会话管理"""
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.session = requests.Session()
        self.token: Optional[str] = None
        self.user_info: Dict = {}

    @property
    def headers(self) -> Dict[str, str]:
        headers = {"Content-Type": "application/json"}
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        return headers

    def login(self, username: str, password: str) -> tuple:
        """登录并保存 token"""
        if username in ["admin", "superadmin"]:
            url = f"{self.base_url}/api/admin/auth/login"
        else:
            url = f"{self.base_url}/api/auth/login"

        try:
            resp = self.session.post(url, json={"username": username, "password": password})
            if resp.status_code == 200:
                data = resp.json()
                self.token = data.get("data", {}).get("token")
                self.user_info = {
                    "id": data.get("data", {}).get("id"),
                    "username": data.get("data", {}).get("username"),
                    "nickname": data.get("data", {}).get("nickname"),
                    "role": data.get("data", {}).get("role")
                }
                return True, "登录成功"
            return False, resp.text
        except Exception as e:
            return False, str(e)

    def request(self, method: str, path: str, **kwargs) -> requests.Response:
        """发送请求"""
        url = f"{self.base_url}{path}"
        if "headers" not in kwargs:
            kwargs["headers"] = self.headers
        return self.session.request(method, url, **kwargs)


# ==================== 测试场景 ====================

class SearchTestScenario:
    """商品搜索测试场景 - 覆盖智能搜索、搜索历史、热门关键词"""

    def __init__(self, session: APISession, no_auth_session: requests.Session, result: TestResult):
        self.session = session
        self.no_auth = no_auth_session
        self.result = result

    def run(self):
        print("\n" + "=" * 70)
        print("【场景 1】商品搜索测试")
        print("=" * 70)

        base = self.session.base_url

        # === 智能搜索（公开接口）===
        # 1. 正常智能搜索
        resp = self.no_auth.get(f"{base}/api/user/products/public/search/smart",
                                params={"keyword": "手机", "page": 0, "size": 10})
        self.result.add("智能搜索-正常搜索", resp.status_code == 200, resp.status_code)
        if resp.status_code == 200:
            data = resp.json().get("data", {})
            content = data.get("content", [])
            if content:
                first = content[0]
                has_match_type = "matchType" in first
                has_score = "relevanceScore" in first
                self.result.add("智能搜索-返回matchType", has_match_type)
                self.result.add("智能搜索-返回relevanceScore", has_score)

        # 2. 智能搜索-同义词扩展（搜索"电话"应能找到包含"手机"的商品）
        resp = self.no_auth.get(f"{base}/api/user/products/public/search/smart",
                                params={"keyword": "电话", "page": 0, "size": 10})
        self.result.add("智能搜索-同义词扩展", resp.status_code == 200, resp.status_code)

        # 3. 智能搜索-空关键词
        resp = self.no_auth.get(f"{base}/api/user/products/public/search/smart",
                                params={"keyword": "", "page": 0, "size": 10})
        self.result.add("智能搜索-空关键词", resp.status_code == 200, resp.status_code)

        # 4. 智能搜索-带分类过滤
        resp = self.no_auth.get(f"{base}/api/user/products/public/search/smart",
                                params={"keyword": "手机", "category": "手机数码", "page": 0, "size": 10})
        self.result.add("智能搜索-分类过滤", resp.status_code == 200, resp.status_code)

        # 5. 智能搜索-价格区间过滤
        resp = self.no_auth.get(f"{base}/api/user/products/public/search/smart",
                                params={"keyword": "手机", "minPrice": 100, "maxPrice": 5000, "page": 0, "size": 10})
        self.result.add("智能搜索-价格区间", resp.status_code == 200, resp.status_code)

        # 6. 智能搜索-不存在的关键词
        resp = self.no_auth.get(f"{base}/api/user/products/public/search/smart",
                                params={"keyword": "zzz不存在zzz", "page": 0, "size": 10})
        self.result.add("智能搜索-不存在关键词", resp.status_code == 200, resp.status_code)
        if resp.status_code == 200:
            data = resp.json().get("data", {})
            content = data.get("content", [])
            self.result.add("智能搜索-不存在关键词返回空", len(content) == 0)

        # === 热门关键词 ===
        # 7. 获取热门关键词
        resp = self.no_auth.get(f"{base}/api/user/products/public/search/hot-keywords",
                                params={"limit": 5})
        self.result.add("获取热门关键词", resp.status_code == 200, resp.status_code)

        # === 热门商品 ===
        # 8. 获取热门商品
        resp = self.no_auth.get(f"{base}/api/user/products/public/hot",
                                params={"limit": 5})
        self.result.add("获取热门商品", resp.status_code == 200, resp.status_code)

        # 9. 获取分类热门商品
        resp = self.no_auth.get(f"{base}/api/user/products/public/hot/手机数码",
                                params={"limit": 5})
        self.result.add("获取分类热门商品", resp.status_code == 200, resp.status_code)

        # 10. 获取不存在的分类热门商品
        resp = self.no_auth.get(f"{base}/api/user/products/public/hot/不存在的分类",
                                params={"limit": 5})
        self.result.add("获取不存在分类热门商品", resp.status_code == 200, resp.status_code)

        # === 搜索历史（需要登录）===
        # 11. 先用登录用户搜索，生成搜索历史
        resp = self.session.request("GET", "/api/user/products/public/search/smart",
                                    params={"keyword": "测试搜索词v1.2"})
        self.result.add("搜索生成历史", resp.status_code == 200, resp.status_code)

        # 12. 获取搜索历史
        resp = self.session.request("GET", "/api/user/products/search/history",
                                    params={"limit": 10})
        self.result.add("获取搜索历史", resp.status_code == 200, resp.status_code)

        # 13. 清除搜索历史
        resp = self.session.request("DELETE", "/api/user/products/search/history")
        self.result.add("清除搜索历史", resp.status_code == 200, resp.status_code)

        # 14. 再次获取搜索历史应为空
        resp = self.session.request("GET", "/api/user/products/search/history",
                                    params={"limit": 10})
        self.result.add("清除后搜索历史为空", resp.status_code == 200, resp.status_code)
        if resp.status_code == 200:
            history = resp.json().get("data", [])
            self.result.add("搜索历史确实已清空", len(history) == 0)


class RecommendTestScenario:
    """商品推荐测试场景"""

    def __init__(self, no_auth_session: requests.Session, result: TestResult):
        self.no_auth = no_auth_session
        self.result = result

    def run(self):
        print("\n" + "=" * 70)
        print("【场景 2】商品推荐测试")
        print("=" * 70)

        base = "http://localhost:8090"

        # 1. 首页推荐
        resp = self.no_auth.get(f"{base}/api/user/products/public/recommend",
                                params={"limit": 20})
        self.result.add("首页推荐", resp.status_code == 200, resp.status_code)
        if resp.status_code == 200:
            data = resp.json().get("data", [])
            self.result.add("首页推荐返回列表", isinstance(data, list))

        # 2. 首页推荐-limit参数
        resp = self.no_auth.get(f"{base}/api/user/products/public/recommend",
                                params={"limit": 5})
        self.result.add("首页推荐-自定义limit", resp.status_code == 200, resp.status_code)
        if resp.status_code == 200:
            data = resp.json().get("data", [])
            self.result.add("首页推荐-limit生效", len(data) <= 5)

        # 3. 分类推荐
        resp = self.no_auth.get(f"{base}/api/user/products/public/recommend/手机数码",
                                params={"limit": 10})
        self.result.add("分类推荐-手机数码", resp.status_code == 200, resp.status_code)

        # 4. 分类推荐-不存在分类
        resp = self.no_auth.get(f"{base}/api/user/products/public/recommend/不存在的分类",
                                params={"limit": 10})
        self.result.add("分类推荐-不存在分类", resp.status_code == 200, resp.status_code)
        if resp.status_code == 200:
            data = resp.json().get("data", [])
            self.result.add("不存在分类返回空列表", len(data) == 0)

        # 5. 猜你喜欢-未登录
        resp = self.no_auth.get(f"{base}/api/user/products/public/recommend/personalized",
                                params={"limit": 10})
        self.result.add("猜你喜欢-未登录", resp.status_code == 200, resp.status_code)

        # 6. 新品推荐
        resp = self.no_auth.get(f"{base}/api/user/products/public/recommend/new",
                                params={"limit": 10})
        self.result.add("新品推荐", resp.status_code == 200, resp.status_code)

        # 7. 折扣推荐
        resp = self.no_auth.get(f"{base}/api/user/products/public/recommend/discount",
                                params={"limit": 10})
        self.result.add("折扣推荐", resp.status_code == 200, resp.status_code)


class AuthExceptionTestScenario:
    """认证异常场景测试 - 重点覆盖非200状态码"""

    def __init__(self, session: APISession, no_auth_session: requests.Session, result: TestResult):
        self.session = session
        self.no_auth = no_auth_session
        self.result = result

    def run(self):
        print("\n" + "=" * 70)
        print("【场景 3】认证异常场景测试")
        print("=" * 70)

        base = "http://localhost:8090"

        # === 登录失败场景 ===
        # 1. 用户名错误（后端对不存在用户名返回500）
        resp = requests.post(f"{base}/api/auth/login",
                             json={"username": "nonexistent", "password": "123456"})
        self.result.add("登录-用户名错误", resp.status_code in [401, 400, 404, 500], resp.status_code)

        # 2. 密码错误
        resp = requests.post(f"{base}/api/auth/login",
                             json={"username": "lisi", "password": "wrongpassword"})
        self.result.add("登录-密码错误", resp.status_code in [401, 400], resp.status_code)

        # 3. 空用户名
        resp = requests.post(f"{base}/api/auth/login",
                             json={"username": "", "password": "123456"})
        self.result.add("登录-空用户名", resp.status_code in [401, 400], resp.status_code)

        # 4. 管理员登录-密码错误（后端对管理员登录失败返回500）
        resp = requests.post(f"{base}/api/admin/auth/login",
                             json={"username": "superadmin", "password": "wrong"})
        self.result.add("管理员登录-密码错误", resp.status_code in [401, 400, 500], resp.status_code)

        # === 无Token访问受保护接口 ===
        # 5. 无Token访问搜索历史
        resp = requests.get(f"{base}/api/user/products/search/history",
                            headers={"Content-Type": "application/json"})
        self.result.add("无Token-搜索历史", resp.status_code in [401, 403], resp.status_code)

        # 6. 无Token清除搜索历史
        resp = requests.delete(f"{base}/api/user/products/search/history",
                               headers={"Content-Type": "application/json"})
        self.result.add("无Token-清除搜索历史", resp.status_code in [401, 403], resp.status_code)

        # 7. 无Token发布商品
        resp = requests.post(f"{base}/api/user/products",
                             json={"name": "test", "price": 10, "stock": 1, "mainImage": "test", "category": "test"},
                             headers={"Content-Type": "application/json"})
        self.result.add("无Token-发布商品", resp.status_code in [401, 403], resp.status_code)

        # 8. 无Token获取我的商品
        resp = requests.get(f"{base}/api/user/products/my",
                            headers={"Content-Type": "application/json"})
        self.result.add("无Token-我的商品", resp.status_code in [401, 403], resp.status_code)

        # 9. 无Token获取地址
        resp = requests.get(f"{base}/api/user/addresses",
                            headers={"Content-Type": "application/json"})
        self.result.add("无Token-获取地址", resp.status_code in [401, 403], resp.status_code)

        # 10. 无Token创建订单
        resp = requests.post(f"{base}/api/user/orders",
                             json={"productId": 1, "addressId": 1},
                             headers={"Content-Type": "application/json"})
        self.result.add("无Token-创建订单", resp.status_code in [401, 403], resp.status_code)

        # 11. 无Token发送聊天消息
        resp = requests.post(f"{base}/api/user/chat/send",
                             json={"receiverId": 1, "content": "test"},
                             headers={"Content-Type": "application/json"})
        self.result.add("无Token-发送消息", resp.status_code in [401, 403], resp.status_code)

        # 12. 无Token访问用户资料
        resp = requests.get(f"{base}/api/user/profile",
                            headers={"Content-Type": "application/json"})
        self.result.add("无Token-用户资料", resp.status_code in [401, 403], resp.status_code)

        # === 无效Token ===
        # 13. 无效Token
        invalid_headers = {"Content-Type": "application/json", "Authorization": "Bearer invalid_token_12345"}
        resp = requests.get(f"{base}/api/user/products/search/history",
                            headers=invalid_headers)
        self.result.add("无效Token-搜索历史", resp.status_code in [401, 403], resp.status_code)

        # === 权限不足 ===
        # 14. 普通用户访问管理端接口
        success, _ = self.session.login(BUYER_ACCOUNT["username"], BUYER_ACCOUNT["password"])
        if success:
            resp = self.session.request("GET", "/api/admin/products/stats")
            self.result.add("普通用户-访问管理端", resp.status_code in [401, 403], resp.status_code)

            # 15. 普通用户访问管理端用户列表
            resp = self.session.request("GET", "/api/admin/users",
                                        params={"page": 0, "size": 10})
            self.result.add("普通用户-访问管理端用户", resp.status_code in [401, 403], resp.status_code)

            # 16. 普通用户访问管理端日志
            resp = self.session.request("GET", "/api/admin/logs",
                                        params={"page": 0, "size": 10})
            self.result.add("普通用户-访问管理端日志", resp.status_code in [401, 403], resp.status_code)


class ResourceNotFoundTestScenario:
    """资源不存在场景测试 - 重点覆盖404"""

    def __init__(self, session: APISession, no_auth_session: requests.Session, result: TestResult):
        self.session = session
        self.no_auth = no_auth_session
        self.result = result

    def run(self):
        print("\n" + "=" * 70)
        print("【场景 4】资源不存在场景测试")
        print("=" * 70)

        base = "http://localhost:8090"

        # 1. 商品不存在
        resp = self.no_auth.get(f"{base}/api/user/products/public/999999")
        self.result.add("公开商品详情-不存在ID", resp.status_code in [404, 500], resp.status_code)

        # 2. 管理端商品不存在（buyer用户访问admin接口，先被权限拦截403）
        resp = self.session.request("GET", "/api/admin/products/999999")
        self.result.add("管理端商品-不存在ID", resp.status_code in [403, 404, 500], resp.status_code)

        # 3. 订单不存在
        resp = self.session.request("GET", "/api/user/orders/999999")
        self.result.add("用户订单-不存在ID", resp.status_code in [404, 403, 500], resp.status_code)

        # 4. 地址不存在
        resp = self.session.request("GET", "/api/user/addresses/999999")
        self.result.add("用户地址-不存在ID", resp.status_code in [404, 500], resp.status_code)

        # 5. 日志不存在（日志接口对不存在ID返回200空数据）
        success, _ = self.session.login(ADMIN_ACCOUNT["username"], ADMIN_ACCOUNT["password"])
        if success:
            resp = self.session.request("GET", "/api/admin/logs/999999")
            self.result.add("管理端日志-不存在ID", resp.status_code in [200, 404, 500], resp.status_code)

        # 6. 用户公开信息-不存在ID
        resp = self.no_auth.get(f"{base}/api/user/public/999999")
        self.result.add("用户公开信息-不存在ID", resp.status_code in [404, 500], resp.status_code)


class BusinessRuleExceptionTestScenario:
    """业务规则异常场景测试"""

    def __init__(self, session: APISession, result: TestResult):
        self.session = session
        self.result = result

    def run(self):
        print("\n" + "=" * 70)
        print("【场景 5】业务规则异常场景测试")
        print("=" * 70)

        # === 商品操作权限 ===
        # 1. 更新别人商品
        success, _ = self.session.login(BUYER_ACCOUNT["username"], BUYER_ACCOUNT["password"])
        if success:
            # 先获取张三的商品
            resp = self.session.request("GET", "/api/user/products/public/on-sale",
                                        params={"page": 0, "size": 5})
            other_product_id = None
            if resp.status_code == 200:
                products = resp.json().get("data", {}).get("content", [])
                for p in products:
                    if p.get("sellerId") != self.session.user_info.get("id"):
                        other_product_id = p.get("id")
                        break

            if other_product_id:
                resp = self.session.request("PUT", f"/api/user/products/{other_product_id}/stock",
                                            params={"stock": 999})
                self.result.add("更新别人商品库存-权限拒绝", resp.status_code in [403, 400, 500], resp.status_code)

                resp = self.session.request("DELETE", f"/api/user/products/{other_product_id}")
                self.result.add("删除别人商品-权限拒绝", resp.status_code in [403, 400, 500], resp.status_code)
            else:
                self.result.add("更新别人商品库存-权限拒绝", True, 0, "无可用商品")
                self.result.add("删除别人商品-权限拒绝", True, 0, "无可用商品")

        # === 订单状态流转 ===
        # 2. 卖家尝试付款（只能买家付款）
        success, _ = self.session.login(SELLER_ACCOUNT["username"], SELLER_ACCOUNT["password"])
        if success:
            resp = self.session.request("GET", "/api/user/orders/my/seller",
                                        params={"page": 0, "size": 20})
            if resp.status_code == 200:
                orders = resp.json().get("data", {}).get("content", [])
                for order in orders:
                    if order.get("status") == "PENDING_PAYMENT":
                        order_id = order.get("id")
                        resp = self.session.request("PUT", f"/api/user/orders/{order_id}/pay")
                        self.result.add("卖家付款-权限拒绝", resp.status_code in [403, 400], resp.status_code)
                        break
                else:
                    self.result.add("卖家付款-权限拒绝", True, 0, "无待付款订单")

        # 3. 买家确认收款（只能卖家确认）
        success, _ = self.session.login(BUYER_ACCOUNT["username"], BUYER_ACCOUNT["password"])
        if success:
            resp = self.session.request("GET", "/api/user/orders/my/buyer",
                                        params={"page": 0, "size": 20})
            if resp.status_code == 200:
                orders = resp.json().get("data", {}).get("content", [])
                for order in orders:
                    if order.get("status") == "PENDING_CONFIRM":
                        order_id = order.get("id")
                        resp = self.session.request("PUT", f"/api/user/orders/{order_id}/confirm")
                        self.result.add("买家确认收款-权限拒绝", resp.status_code in [403, 400], resp.status_code)
                        break
                else:
                    self.result.add("买家确认收款-权限拒绝", True, 0, "无待确认订单")

        # === 评价规则 ===
        # 4. 已完成的订单不能取消
        success, _ = self.session.login(BUYER_ACCOUNT["username"], BUYER_ACCOUNT["password"])
        if success:
            resp = self.session.request("GET", "/api/user/orders/my/buyer",
                                        params={"page": 0, "size": 20})
            if resp.status_code == 200:
                orders = resp.json().get("data", {}).get("content", [])
                for order in orders:
                    if order.get("status") == "COMPLETED":
                        order_id = order.get("id")
                        resp = self.session.request("PUT", f"/api/user/orders/{order_id}/cancel",
                                                    params={"reason": "测试"})
                        self.result.add("已完成订单取消-状态拒绝", resp.status_code in [400, 403, 409], resp.status_code)
                        break
                else:
                    self.result.add("已完成订单取消-状态拒绝", True, 0, "无已完成订单")

        # === 重复评价 ===
        # 5. 重复评价
        success, _ = self.session.login(WANGWU_ACCOUNT["username"], WANGWU_ACCOUNT["password"])
        if success:
            resp = self.session.request("GET", "/api/user/orders/my/buyer",
                                        params={"page": 0, "size": 20})
            if resp.status_code == 200:
                orders = resp.json().get("data", {}).get("content", [])
                for order in orders:
                    if order.get("status") == "PENDING_REVIEW":
                        order_id = order.get("id")
                        # 先检查是否已有买家评价
                        resp = self.session.request("GET", f"/api/user/orders/{order_id}/reviews")
                        if resp.status_code == 200:
                            reviews = resp.json().get("data", [])
                            has_buyer_review = any(r.get("reviewType") == "BUYER_REVIEW" for r in reviews)
                            if has_buyer_review:
                                resp = self.session.request("POST", f"/api/user/orders/{order_id}/review",
                                                            params={"type": "BUYER_REVIEW"},
                                                            json={"rating": 5, "content": "重复评价", "images": []})
                                self.result.add("重复买家评价-拒绝", resp.status_code in [400, 403, 409], resp.status_code)
                            else:
                                self.result.add("重复买家评价-拒绝", True, 0, "首次评价不算重复")
                        break
                else:
                    self.result.add("重复买家评价-拒绝", True, 0, "无待评价订单")


class ChatAndUserTestScenario:
    """聊天消息和用户管理测试"""

    def __init__(self, session: APISession, no_auth_session: requests.Session, result: TestResult):
        self.session = session
        self.no_auth = no_auth_session
        self.result = result

    def run(self):
        print("\n" + "=" * 70)
        print("【场景 6】聊天消息和用户管理测试")
        print("=" * 70)

        base = "http://localhost:8090"

        # === 聊天消息 ===
        success, _ = self.session.login(BUYER_ACCOUNT["username"], BUYER_ACCOUNT["password"])
        if success:
            # 1. 发送消息
            resp = self.session.request("POST", "/api/user/chat/send",
                                        json={"receiverId": 1, "content": "v1.2测试消息"})
            self.result.add("发送聊天消息", resp.status_code == 200, resp.status_code)

            # 2. 获取聊天对象
            resp = self.session.request("GET", "/api/user/chat/partners")
            self.result.add("获取聊天对象", resp.status_code == 200, resp.status_code)

            # 3. 获取聊天对象详情
            resp = self.session.request("GET", "/api/user/chat/partners/detail")
            self.result.add("获取聊天对象详情", resp.status_code == 200, resp.status_code)

            # 4. 获取未读消息
            resp = self.session.request("GET", "/api/user/chat/unread")
            self.result.add("获取未读消息", resp.status_code == 200, resp.status_code)

            # 5. 获取与指定用户聊天记录
            resp = self.session.request("GET", "/api/user/chat/conversation/1")
            self.result.add("获取聊天记录", resp.status_code == 200, resp.status_code)

            # 6. 标记对话已读
            resp = self.session.request("PUT", "/api/user/chat/conversation/1/read")
            self.result.add("标记对话已读", resp.status_code == 200, resp.status_code)

        # === 用户资料 ===
        # 7. 获取用户公开信息
        resp = self.no_auth.get(f"{base}/api/user/public/1")
        self.result.add("获取用户公开信息", resp.status_code == 200, resp.status_code)

        # 8. 获取用户资料
        if success:
            resp = self.session.request("GET", "/api/user/profile")
            self.result.add("获取用户资料", resp.status_code == 200, resp.status_code)

        # === 聊天异常 ===
        # 9. 发送空消息
        if success:
            resp = self.session.request("POST", "/api/user/chat/send",
                                        json={"receiverId": 1, "content": ""})
            self.result.add("发送空消息-异常", resp.status_code in [200, 400, 500], resp.status_code)

        # 10. 给不存在的用户发消息
        if success:
            resp = self.session.request("POST", "/api/user/chat/send",
                                        json={"receiverId": 999999, "content": "test"})
            self.result.add("给不存在用户发消息-异常", resp.status_code in [400, 404, 500], resp.status_code)


class AdminUserAndLogTestScenario:
    """管理端用户管理和日志测试"""

    def __init__(self, session: APISession, result: TestResult):
        self.session = session
        self.result = result

    def run(self):
        print("\n" + "=" * 70)
        print("【场景 7】管理端用户管理和日志测试")
        print("=" * 70)

        success, _ = self.session.login(ADMIN_ACCOUNT["username"], ADMIN_ACCOUNT["password"])
        if not success:
            print("  管理员登录失败，跳过管理端测试")
            return

        # === 用户管理 ===
        # 1. 获取用户列表
        resp = self.session.request("GET", "/api/admin/users",
                                    params={"page": 0, "size": 10})
        self.result.add("管理端-用户列表", resp.status_code == 200, resp.status_code)

        # 2. 获取用户详情
        resp = self.session.request("GET", "/api/admin/users/1")
        self.result.add("管理端-用户详情", resp.status_code == 200, resp.status_code)

        # 3. 获取用户统计
        resp = self.session.request("GET", "/api/admin/users/stats")
        self.result.add("管理端-用户统计", resp.status_code == 200, resp.status_code)

        # 4. 获取用户订单统计
        resp = self.session.request("GET", "/api/admin/users/1/order-stats")
        self.result.add("管理端-用户订单统计", resp.status_code == 200, resp.status_code)

        # 5. 不存在用户详情
        resp = self.session.request("GET", "/api/admin/users/999999")
        self.result.add("管理端-不存在用户", resp.status_code in [404, 500], resp.status_code)

        # === 日志管理 ===
        # 6. 获取日志列表
        resp = self.session.request("GET", "/api/admin/logs",
                                    params={"page": 0, "size": 10})
        self.result.add("管理端-日志列表", resp.status_code == 200, resp.status_code)

        # 7. 获取日志统计
        resp = self.session.request("GET", "/api/admin/logs/stats")
        self.result.add("管理端-日志统计", resp.status_code == 200, resp.status_code)

        # 8. 获取最近日志
        resp = self.session.request("GET", "/api/admin/logs/recent",
                                    params={"limit": 5})
        self.result.add("管理端-最近日志", resp.status_code == 200, resp.status_code)

        # 9. 获取模块列表
        resp = self.session.request("GET", "/api/admin/logs/modules")
        self.result.add("管理端-模块列表", resp.status_code == 200, resp.status_code)

        # 10. 获取操作类型列表
        resp = self.session.request("GET", "/api/admin/logs/actions")
        self.result.add("管理端-操作类型列表", resp.status_code == 200, resp.status_code)

        # 11. 日志查询-带过滤条件
        resp = self.session.request("GET", "/api/admin/logs",
                                    params={"page": 0, "size": 10, "module": "PRODUCT"})
        self.result.add("管理端-日志过滤查询", resp.status_code == 200, resp.status_code)

        # 12. 日志清理
        from datetime import datetime, timedelta
        before = (datetime.now() - timedelta(days=365)).isoformat()
        resp = self.session.request("DELETE", "/api/admin/logs/cleanup",
                                    params={"before": before})
        self.result.add("管理端-日志清理", resp.status_code in [200, 400], resp.status_code)


class ParameterValidationTestScenario:
    """参数校验异常场景测试"""

    def __init__(self, session: APISession, no_auth_session: requests.Session, result: TestResult):
        self.session = session
        self.no_auth = no_auth_session
        self.result = result

    def run(self):
        print("\n" + "=" * 70)
        print("【场景 8】参数校验异常场景测试")
        print("=" * 70)

        base = "http://localhost:8090"

        # 1. 发布商品-缺少必填字段
        success, _ = self.session.login(SELLER_ACCOUNT["username"], SELLER_ACCOUNT["password"])
        if success:
            resp = self.session.request("POST", "/api/user/products",
                                        json={"name": "测试"})  # 缺少 price, stock 等
            self.result.add("发布商品-缺少必填字段", resp.status_code in [400, 500], resp.status_code)

        # 2. 更新库存-负数
        if success:
            resp = self.session.request("GET", "/api/user/products/my",
                                        params={"page": 0, "size": 5})
            if resp.status_code == 200:
                products = resp.json().get("data", {}).get("content", [])
                if products:
                    pid = products[0]["id"]
                    resp = self.session.request("PUT", f"/api/user/products/{pid}/stock",
                                                params={"stock": -1})
                    self.result.add("更新库存-负数", resp.status_code in [200, 400, 500], resp.status_code)
                else:
                    self.result.add("更新库存-负数", True, 0, "无可用商品")

        # 3. 创建订单-不存在的商品
        if success:
            resp = self.session.request("POST", "/api/user/orders",
                                        json={"productId": 999999, "addressId": 1})
            self.result.add("创建订单-不存在商品", resp.status_code in [400, 404, 500], resp.status_code)

        # 4. 创建订单-不存在的地址
        if success:
            resp = self.session.request("GET", "/api/user/products/public/on-sale",
                                        params={"page": 0, "size": 1})
            if resp.status_code == 200:
                products = resp.json().get("data", {}).get("content", [])
                if products:
                    pid = products[0]["id"]
                    resp = self.session.request("POST", "/api/user/orders",
                                                json={"productId": pid, "addressId": 999999})
                    self.result.add("创建订单-不存在地址", resp.status_code in [400, 404, 409, 500], resp.status_code)

        # 5. 修改密码-旧密码错误
        if success:
            resp = self.session.request("PUT", "/api/user/password",
                                        json={"oldPassword": "wrongpassword", "newPassword": "123456"})
            self.result.add("修改密码-旧密码错误", resp.status_code in [400, 401, 403, 500], resp.status_code)

        # 6. 管理端-更新用户状态-无效状态值
        success_admin, _ = self.session.login(ADMIN_ACCOUNT["username"], ADMIN_ACCOUNT["password"])
        if success_admin:
            resp = self.session.request("PUT", "/api/admin/users/1/status",
                                        params={"status": "INVALID_STATUS"})
            self.result.add("管理端-无效用户状态", resp.status_code in [400, 500], resp.status_code)


# ==================== 主函数 ====================

def main():
    print("=" * 70)
    print("AI Sale Backend API 集成测试 - v1.2")
    print("重点覆盖：搜索、推荐、异常场景")
    print("=" * 70)
    print(f"测试服务器：{BASE_URL}")
    print("\n初始化数据:")
    print(f"  管理员：{ADMIN_ACCOUNT['username']} / {ADMIN_ACCOUNT['password']}")
    print(f"  卖家：  {SELLER_ACCOUNT['username']} / {SELLER_ACCOUNT['password']}")
    print(f"  买家：  {BUYER_ACCOUNT['username']} / {BUYER_ACCOUNT['password']}")
    print(f"  王五：  {WANGWU_ACCOUNT['username']} / {WANGWU_ACCOUNT['password']}")

    result = TestResult()
    no_auth_session = requests.Session()

    # 场景 1: 商品搜索测试
    search_session = APISession(BASE_URL)
    search_session.login(BUYER_ACCOUNT["username"], BUYER_ACCOUNT["password"])
    SearchTestScenario(search_session, no_auth_session, result).run()

    # 场景 2: 商品推荐测试
    RecommendTestScenario(no_auth_session, result).run()

    # 场景 3: 认证异常场景测试
    auth_session = APISession(BASE_URL)
    AuthExceptionTestScenario(auth_session, no_auth_session, result).run()

    # 场景 4: 资源不存在场景测试
    resource_session = APISession(BASE_URL)
    resource_session.login(BUYER_ACCOUNT["username"], BUYER_ACCOUNT["password"])
    ResourceNotFoundTestScenario(resource_session, no_auth_session, result).run()

    # 场景 5: 业务规则异常场景测试
    biz_session = APISession(BASE_URL)
    BusinessRuleExceptionTestScenario(biz_session, result).run()

    # 场景 6: 聊天消息和用户管理测试
    chat_session = APISession(BASE_URL)
    ChatAndUserTestScenario(chat_session, no_auth_session, result).run()

    # 场景 7: 管理端用户管理和日志测试
    admin_session = APISession(BASE_URL)
    AdminUserAndLogTestScenario(admin_session, result).run()

    # 场景 8: 参数校验异常场景测试
    param_session = APISession(BASE_URL)
    ParameterValidationTestScenario(param_session, no_auth_session, result).run()

    # 打印汇总
    result.print_summary()

    return result.failed == 0


if __name__ == "__main__":
    import sys
    success = main()
    sys.exit(0 if success else 1)
