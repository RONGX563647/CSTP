"""
AI Sale Backend API 集成测试
基于 DataInitializer 初始化数据的完整测试链

测试场景：
1. 管理员场景：商品管理、订单管理
2. 张三场景：作为卖家管理商品、查看订单
3. 李四场景：作为买家浏览商品、创建订单、完成交易
"""
import requests
from typing import Optional, Dict, List


# ==================== 配置 ====================
BASE_URL = "http://localhost:8090"

# 初始化数据中的账号
ADMIN_ACCOUNT = {"username": "superadmin", "password": "123456"}
SELLER_ACCOUNT = {"username": "zhangsan", "password": "123456"}  # 卖家：有 5 个商品
BUYER_ACCOUNT = {"username": "lisi", "password": "123456"}      # 买家：有地址、有订单


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
            print(f"  {icon} {name}" + (f" (HTTP {status})" if status else "") + (f" - {error}" if error else ""))


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

    def login(self, username: str, password: str) -> tuple[bool, str]:
        """登录并保存 token"""
        # 判断是管理员还是用户
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

class AdminTestScenario:
    """管理员测试场景"""

    def __init__(self, session: APISession, result: TestResult):
        self.session = session
        self.result = result
        self.product_id: Optional[int] = None

    def run(self):
        print("\n" + "=" * 70)
        print("【场景 1】管理员测试")
        print("=" * 70)

        # 1. 管理员登录
        success, msg = self.session.login(ADMIN_ACCOUNT["username"], ADMIN_ACCOUNT["password"])
        self.result.add("管理员登录", success, error=msg if not success else "")
        print(f"[{'PASS' if success else 'FAIL'}] 管理员登录：{msg}")

        if not success:
            print("  跳过后续管理员测试...")
            return

        # 2. 获取管理员信息
        resp = self.session.request("GET", "/api/admin/auth/me")
        self.result.add("获取管理员信息", resp.status_code == 200, resp.status_code)

        # 3. 商品统计
        resp = self.session.request("GET", "/api/admin/products/stats")
        self.result.add("获取商品统计", resp.status_code == 200, resp.status_code)

        # 4. 获取商品列表
        resp = self.session.request("GET", "/api/admin/products", params={"page": 0, "size": 20})
        self.result.add("获取商品列表", resp.status_code == 200, resp.status_code)
        if resp.status_code == 200:
            data = resp.json()
            products = data.get("data", {}).get("content", [])
            if products:
                self.product_id = products[0]["id"]
                print(f"  获取到 {len(products)} 个商品，第一个商品 ID: {self.product_id}")

        # 5. 获取商品详情
        if self.product_id:
            resp = self.session.request("GET", f"/api/admin/products/{self.product_id}")
            self.result.add("获取商品详情", resp.status_code == 200, resp.status_code)

            # 6. 更新商品
            payload = {
                "name": "管理员更新的商品",
                "description": "测试描述",
                "price": 199.99,
                "originalPrice": 299.99,
                "stock": 50,
                "mainImage": "https://example.com/new.jpg",
                "category": "分类更新",
                "isOnSale": True,
                "isFeatured": True
            }
            resp = self.session.request("PUT", f"/api/admin/products/{self.product_id}", json=payload)
            self.result.add("更新商品", resp.status_code == 200, resp.status_code)

            # 7. 更新库存
            resp = self.session.request("PUT", f"/api/admin/products/{self.product_id}/stock",
                                       params={"stock": 100})
            self.result.add("更新商品库存", resp.status_code == 200, resp.status_code)

            # 8. 更新状态
            resp = self.session.request("PUT", f"/api/admin/products/{self.product_id}/status",
                                       params={"status": "ON_SALE"})
            self.result.add("更新商品状态", resp.status_code == 200, resp.status_code)

            # 9. 设置上下架
            resp = self.session.request("PUT", f"/api/admin/products/{self.product_id}/sale-status",
                                       params={"isOnSale": False})
            self.result.add("设置商品下架", resp.status_code == 200, resp.status_code)

            # 10. 设置推荐
            resp = self.session.request("PUT", f"/api/admin/products/{self.product_id}/featured",
                                       params={"isFeatured": True})
            self.result.add("设置商品推荐", resp.status_code == 200, resp.status_code)

        # 11. 多条件查询
        resp = self.session.request("GET", "/api/admin/products/query",
                                   params={"page": 0, "size": 10, "category": "手机数码"})
        self.result.add("多条件查询商品", resp.status_code == 200, resp.status_code)

        # 12. 获取售罄商品
        resp = self.session.request("GET", "/api/admin/products/out-of-stock",
                                   params={"page": 0, "size": 10})
        self.result.add("获取售罄商品", resp.status_code == 200, resp.status_code)

        # 13. 获取下架商品
        resp = self.session.request("GET", "/api/admin/products/off-sale",
                                   params={"page": 0, "size": 10})
        self.result.add("获取下架商品", resp.status_code == 200, resp.status_code)

        # 14. 获取用户商品
        resp = self.session.request("GET", "/api/admin/products/user/1",
                                   params={"page": 0, "size": 10})
        self.result.add("获取用户商品", resp.status_code == 200, resp.status_code)

        # 15. 订单列表
        resp = self.session.request("GET", "/api/admin/orders",
                                   params={"page": 0, "size": 10})
        self.result.add("获取订单列表", resp.status_code == 200, resp.status_code)

        # 16. 订单详情
        resp = self.session.request("GET", "/api/admin/orders/1")
        self.result.add("获取订单详情", resp.status_code == 200, resp.status_code)

        # 17. 订单统计
        resp = self.session.request("GET", "/api/admin/orders/stats")
        self.result.add("获取订单统计", resp.status_code == 200, resp.status_code)

        # 18. 修改订单状态
        resp = self.session.request("PUT", "/api/admin/orders/1/status",
                                   params={"status": "PENDING_PICKUP"})
        self.result.add("修改订单状态", resp.status_code == 200, resp.status_code)

        # 19. 添加订单备注
        resp = self.session.request("PUT", "/api/admin/orders/1/remark",
                                   params={"remark": "管理员测试备注"})
        self.result.add("添加订单备注", resp.status_code == 200, resp.status_code)


class SellerTestScenario:
    """卖家（张三）测试场景"""

    def __init__(self, session: APISession, result: TestResult):
        self.session = session
        self.result = result
        self.my_product_id: Optional[int] = None
        self.my_address_id: Optional[int] = None

    def run(self):
        print("\n" + "=" * 70)
        print("【场景 2】卖家（张三）测试 - 管理自己的商品")
        print("=" * 70)

        # 1. 张三登录
        success, msg = self.session.login(SELLER_ACCOUNT["username"], SELLER_ACCOUNT["password"])
        self.result.add("张三登录", success, error=msg if not success else "")
        print(f"[{'PASS' if success else 'FAIL'}] 张三登录：{msg}")

        if not success:
            print("  跳过后续卖家测试...")
            return

        print(f"  登录用户：{self.session.user_info}")

        # 2. 获取我的商品列表
        resp = self.session.request("GET", "/api/user/products/my",
                                   params={"page": 0, "size": 10})
        self.result.add("获取我的商品列表", resp.status_code == 200, resp.status_code)
        if resp.status_code == 200:
            data = resp.json()
            products = data.get("data", {}).get("content", [])
            if products:
                self.my_product_id = products[0]["id"]
                print(f"  我有 {len(products)} 个商品，第一个商品 ID: {self.my_product_id}")

        # 3. 获取商品统计
        resp = self.session.request("GET", "/api/user/products/stats")
        self.result.add("获取商品统计", resp.status_code == 200, resp.status_code)

        # 4. 获取商品详情
        if self.my_product_id:
            resp = self.session.request("GET", f"/api/user/products/{self.my_product_id}")
            self.result.add("获取商品详情", resp.status_code == 200, resp.status_code)

            # 5. 更新商品
            payload = {
                "name": "张三更新的商品",
                "description": "更新后的描述",
                "price": 88.88,
                "originalPrice": 150.00,
                "stock": 15,
                "mainImage": "https://example.com/zhangsan.jpg",
                "category": "手机数码",
                "isOnSale": True,
                "isFeatured": False
            }
            resp = self.session.request("PUT", f"/api/user/products/{self.my_product_id}", json=payload)
            self.result.add("更新我的商品", resp.status_code == 200, resp.status_code)

            # 6. 更新库存
            resp = self.session.request("PUT", f"/api/user/products/{self.my_product_id}/stock",
                                       params={"stock": 20})
            self.result.add("更新商品库存", resp.status_code == 200, resp.status_code)

            # 7. 更新状态
            resp = self.session.request("PUT", f"/api/user/products/{self.my_product_id}/status",
                                       params={"status": "ON_SALE"})
            self.result.add("更新商品状态", resp.status_code == 200, resp.status_code)

        # 8. 发布新商品
        payload = {
            "name": "张三发布的新商品",
            "description": "测试发布",
            "price": 66.66,
            "originalPrice": 100.00,
            "stock": 5,
            "mainImage": "https://example.com/new_product.jpg",
            "category": "图书",
            "isOnSale": True,
            "isFeatured": False
        }
        resp = self.session.request("POST", "/api/user/products", json=payload)
        self.result.add("发布新商品", resp.status_code == 200, resp.status_code)
        if resp.status_code == 200:
            new_product = resp.json().get("data", {})
            print(f"  发布成功，新商品 ID: {new_product.get('id')}")

        # 9. 获取我的订单（我卖的）
        resp = self.session.request("GET", "/api/user/orders/my/seller",
                                   params={"page": 0, "size": 10})
        self.result.add("获取我的订单（卖家）", resp.status_code == 200, resp.status_code)
        if resp.status_code == 200:
            data = resp.json()
            orders = data.get("data", {}).get("content", [])
            print(f"  我有 {len(orders)} 个卖家订单")

        # 10. 卖家确认收款（针对待确认订单）
        # 找到一个 PENDING_CONFIRM 状态的订单
        resp = self.session.request("GET", "/api/user/orders/my/seller",
                                   params={"page": 0, "size": 20})
        if resp.status_code == 200:
            data = resp.json()
            orders = data.get("data", {}).get("content", [])
            for order in orders:
                if order.get("status") == "PENDING_CONFIRM":
                    order_id = order.get("id")
                    resp = self.session.request("PUT", f"/api/user/orders/{order_id}/confirm")
                    self.result.add("卖家确认收款", resp.status_code == 200, resp.status_code)
                    print(f"  确认收款订单 ID: {order_id}, 结果：{resp.status_code}")
                    break

        # 11. 获取我的评价（作为卖家收到的）
        resp = self.session.request("GET", "/api/user/orders/reviews/received")
        self.result.add("获取收到的评价", resp.status_code == 200, resp.status_code)


class BuyerTestScenario:
    """买家（李四）测试场景 - 完整购物流程"""

    def __init__(self, session: APISession, result: TestResult):
        self.session = session
        self.result = result
        self.address_id: Optional[int] = None
        self.order_id: Optional[int] = None

    def run(self):
        print("\n" + "=" * 70)
        print("【场景 3】买家（李四）测试 - 完整购物流程")
        print("=" * 70)

        # 1. 李四登录
        success, msg = self.session.login(BUYER_ACCOUNT["username"], BUYER_ACCOUNT["password"])
        self.result.add("李四登录", success, error=msg if not success else "")
        print(f"[{'PASS' if success else 'FAIL'}] 李四登录：{msg}")

        if not success:
            print("  跳过后续买家测试...")
            return

        print(f"  登录用户：{self.session.user_info}")

        # 2. 浏览公开商品列表
        resp = self.session.request("GET", "/api/user/products/public/on-sale",
                                   params={"page": 0, "size": 10})
        self.result.add("浏览公开商品", resp.status_code == 200, resp.status_code)

        # 3. 查看推荐商品
        resp = self.session.request("GET", "/api/user/products/public/featured")
        self.result.add("查看推荐商品", resp.status_code == 200, resp.status_code)

        # 4. 按分类浏览
        resp = self.session.request("GET", "/api/user/products/public/category/手机数码",
                                   params={"page": 0, "size": 10})
        self.result.add("按分类浏览", resp.status_code == 200, resp.status_code)

        # 5. 搜索商品
        resp = self.session.request("GET", "/api/user/products/public/search",
                                   params={"page": 0, "size": 10, "name": "iPhone"})
        self.result.add("搜索商品", resp.status_code == 200, resp.status_code)

        # 6. 获取我的地址
        resp = self.session.request("GET", "/api/user/addresses")
        self.result.add("获取我的地址", resp.status_code == 200, resp.status_code)
        if resp.status_code == 200:
            addresses = resp.json().get("data", [])
            if addresses:
                self.address_id = addresses[0]["id"]
                print(f"  我有 {len(addresses)} 个地址，默认地址 ID: {self.address_id}")

        # 7. 添加新地址
        payload = {
            "receiverName": "李四",
            "receiverPhone": "13800138002",
            "province": "广东省",
            "city": "广州市",
            "district": "天河区",
            "detailAddress": "天河路 100 号",
            "isDefault": False
        }
        resp = self.session.request("POST", "/api/user/addresses", json=payload)
        self.result.add("添加新地址", resp.status_code == 200, resp.status_code)
        if resp.status_code == 200:
            new_addr = resp.json().get("data", {})
            self.address_id = new_addr.get("id")
            print(f"  添加地址成功，ID: {self.address_id}")

        # 8. 获取我的订单（我买的）
        resp = self.session.request("GET", "/api/user/orders/my/buyer",
                                   params={"page": 0, "size": 10})
        self.result.add("获取我的订单（买家）", resp.status_code == 200, resp.status_code)
        if resp.status_code == 200:
            data = resp.json()
            orders = data.get("data", {}).get("content", [])
            print(f"  我有 {len(orders)} 个买家订单")

            # 找出一个待付款订单用于测试
            for order in orders:
                if order.get("status") == "PENDING_PAYMENT":
                    self.order_id = order.get("id")
                    break

        # 9. 确认付款（如果有待付款订单）
        if self.order_id:
            resp = self.session.request("PUT", f"/api/user/orders/{self.order_id}/pay")
            self.result.add("确认付款", resp.status_code == 200, resp.status_code)
            print(f"  付款订单 ID: {self.order_id}, 结果：{resp.status_code}")

        # 10. 确认提货（针对待提货订单）
        resp = self.session.request("GET", "/api/user/orders/my/buyer",
                                   params={"page": 0, "size": 20})
        if resp.status_code == 200:
            data = resp.json()
            orders = data.get("data", {}).get("content", [])
            for order in orders:
                if order.get("status") == "PENDING_PICKUP":
                    order_id = order.get("id")
                    resp = self.session.request("PUT", f"/api/user/orders/{order_id}/pickup")
                    self.result.add("确认提货", resp.status_code == 200, resp.status_code)
                    print(f"  提货订单 ID: {order_id}, 结果：{resp.status_code}")
                    break

        # 11. 取消订单（针对待付款状态）
        resp = self.session.request("GET", "/api/user/orders/my/buyer",
                                   params={"page": 0, "size": 20})
        if resp.status_code == 200:
            data = resp.json()
            orders = data.get("data", {}).get("content", [])
            for order in orders:
                if order.get("status") == "PENDING_PAYMENT":
                    order_id = order.get("id")
                    resp = self.session.request("PUT", f"/api/user/orders/{order_id}/cancel",
                                               params={"reason": "测试取消"})
                    self.result.add("取消订单", resp.status_code == 200, resp.status_code)
                    print(f"  取消订单 ID: {order_id}, 结果：{resp.status_code}")
                    break

        # 12. 获取我的评价
        resp = self.session.request("GET", "/api/user/orders/reviews/my")
        self.result.add("获取我的评价", resp.status_code == 200, resp.status_code)

        # 13. 创建新订单（购买张三的商品）
        # 先获取张三的商品
        resp = self.session.request("GET", "/api/user/products/public/on-sale",
                                   params={"page": 0, "size": 5})
        if resp.status_code == 200:
            products = resp.json().get("data", {}).get("content", [])
            if products and self.address_id:
                product_id = products[0]["id"]
                payload = {
                    "productId": product_id,
                    "addressId": self.address_id,
                    "remark": "测试订单，麻烦尽快发货"
                }
                resp = self.session.request("POST", "/api/user/orders", json=payload)
                self.result.add("创建新订单", resp.status_code == 200, resp.status_code)
                if resp.status_code == 200:
                    new_order = resp.json().get("data", {})
                    self.order_id = new_order.get("id")
                    print(f"  创建订单成功，ID: {self.order_id}, 商品 ID: {product_id}")

        # 14. 获取订单详情
        if self.order_id:
            resp = self.session.request("GET", f"/api/user/orders/{self.order_id}")
            self.result.add("获取订单详情", resp.status_code == 200, resp.status_code)

            # 15. 获取订单评价
            resp = self.session.request("GET", f"/api/user/orders/{self.order_id}/reviews")
            self.result.add("获取订单评价", resp.status_code == 200, resp.status_code)

            # 16. 获取订单日志
            resp = self.session.request("GET", f"/api/user/orders/{self.order_id}/logs")
            self.result.add("获取订单日志", resp.status_code in [200, 403], resp.status_code)


class PublicAPITest:
    """公共接口测试（无需登录）"""

    def __init__(self, base_url: str, result: TestResult):
        self.base_url = base_url
        self.result = result
        self.session = requests.Session()

    def run(self):
        print("\n" + "=" * 70)
        print("【场景 4】公共接口测试（无需登录）")
        print("=" * 70)

        # 1. 商品列表
        resp = self.session.get(f"{self.base_url}/api/user/products/public/on-sale",
                               params={"page": 0, "size": 10})
        self.result.add("公共商品列表", resp.status_code == 200, resp.status_code)

        # 2. 商品详情
        if resp.status_code == 200:
            products = resp.json().get("data", {}).get("content", [])
            if products:
                product_id = products[0]["id"]
                resp = self.session.get(f"{self.base_url}/api/user/products/public/{product_id}")
                self.result.add("公共商品详情", resp.status_code == 200, resp.status_code)

        # 3. 推荐商品
        resp = self.session.get(f"{self.base_url}/api/user/products/public/featured")
        self.result.add("推荐商品列表", resp.status_code == 200, resp.status_code)

        # 4. 分类商品
        resp = self.session.get(f"{self.base_url}/api/user/products/public/category/手机数码",
                               params={"page": 0, "size": 10})
        self.result.add("分类商品列表", resp.status_code == 200, resp.status_code)

        # 5. 搜索商品
        resp = self.session.get(f"{self.base_url}/api/user/products/public/search",
                               params={"page": 0, "size": 10, "name": ""})
        self.result.add("搜索商品", resp.status_code == 200, resp.status_code)


# ==================== 主函数 ====================

def main():
    print("=" * 70)
    print("AI Sale Backend API 集成测试")
    print("基于 DataInitializer 初始化数据")
    print("=" * 70)
    print(f"测试服务器：{BASE_URL}")
    print("\n初始化数据:")
    print(f"  管理员：{ADMIN_ACCOUNT['username']} / {ADMIN_ACCOUNT['password']}")
    print(f"  卖家：  {SELLER_ACCOUNT['username']} / {SELLER_ACCOUNT['password']}")
    print(f"  买家：  {BUYER_ACCOUNT['username']} / {BUYER_ACCOUNT['password']}")

    result = TestResult()

    # 场景 1: 管理员测试
    admin_session = APISession(BASE_URL)
    AdminTestScenario(admin_session, result).run()

    # 场景 2: 卖家测试
    seller_session = APISession(BASE_URL)
    SellerTestScenario(seller_session, result).run()

    # 场景 3: 买家测试
    buyer_session = APISession(BASE_URL)
    BuyerTestScenario(buyer_session, result).run()

    # 场景 4: 公共接口测试
    PublicAPITest(BASE_URL, result).run()

    # 打印汇总
    result.print_summary()

    return result.failed == 0


if __name__ == "__main__":
    import sys
    success = main()
    sys.exit(0 if success else 1)
