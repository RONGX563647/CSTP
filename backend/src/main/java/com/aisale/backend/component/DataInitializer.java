package com.aisale.backend.component;

import com.aisale.backend.entity.Address;
import com.aisale.backend.entity.Admin;
import com.aisale.backend.entity.Order;
import com.aisale.backend.entity.OrderLog;
import com.aisale.backend.entity.OrderReview;
import com.aisale.backend.entity.Product;
import com.aisale.backend.entity.User;
import com.aisale.backend.repository.AddressRepository;
import com.aisale.backend.repository.AdminRepository;
import com.aisale.backend.repository.OrderLogRepository;
import com.aisale.backend.repository.OrderRepository;
import com.aisale.backend.repository.OrderReviewRepository;
import com.aisale.backend.repository.ProductRepository;
import com.aisale.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 应用启动时数据初始化工具类
 * 用于初始化测试数据：管理员、用户、地址信息
 * 可通过配置 spring.data.init.enabled 控制是否启用 (默认 true)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "spring.data.init.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderReviewRepository orderReviewRepository;
    private final OrderLogRepository orderLogRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("开始初始化测试数据...");

        initAdmins();
        initUsers();
        initAddresses();
        initProducts();
        initOrders();

        log.info("测试数据初始化完成!");
    }

    /**
     * 初始化管理员数据
     */
    private void initAdmins() {
        // 创建超级管理员
        if (!adminRepository.existsByUsername("superadmin")) {
            Admin superAdmin = new Admin();
            superAdmin.setUsername("superadmin");
            superAdmin.setPassword(passwordEncoder.encode("123456"));
            superAdmin.setEmail("superadmin@aisale.com");
            superAdmin.setNickname("超级管理员");
            superAdmin.setRole(Admin.AdminRole.SUPER_ADMIN);
            superAdmin.setStatus(Admin.AdminStatus.ACTIVE);
            adminRepository.save(superAdmin);
            log.info("创建超级管理员：superadmin / 123456");
        }

        // 创建普通管理员
        if (!adminRepository.existsByUsername("admin")) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setEmail("admin@aisale.com");
            admin.setNickname("管理员");
            admin.setRole(Admin.AdminRole.ADMIN);
            admin.setStatus(Admin.AdminStatus.ACTIVE);
            adminRepository.save(admin);
            log.info("创建管理员：admin / 123456");
        }
    }

    /**
     * 初始化用户数据
     */
    private void initUsers() {
        // 创建测试用户 1
        if (!userRepository.existsByUsername("zhangsan")) {
            User user1 = new User();
            user1.setUsername("zhangsan");
            user1.setPassword(passwordEncoder.encode("123456"));
            user1.setEmail("zhangsan@example.com");
            user1.setPhone("13800138001");
            user1.setNickname("张三");
            user1.setStatus(User.UserStatus.ACTIVE);
            user1.setEmailVerified(true);
            userRepository.save(user1);
            log.info("创建用户：zhangsan / 123456");
        }

        // 创建测试用户 2
        if (!userRepository.existsByUsername("lisi")) {
            User user2 = new User();
            user2.setUsername("lisi");
            user2.setPassword(passwordEncoder.encode("123456"));
            user2.setEmail("lisi@example.com");
            user2.setPhone("13800138002");
            user2.setNickname("李四");
            user2.setStatus(User.UserStatus.ACTIVE);
            user2.setEmailVerified(true);
            userRepository.save(user2);
            log.info("创建用户：lisi / 123456");
        }

        // 创建测试用户 3
        if (!userRepository.existsByUsername("wangwu")) {
            User user3 = new User();
            user3.setUsername("wangwu");
            user3.setPassword(passwordEncoder.encode("123456"));
            user3.setEmail("wangwu@example.com");
            user3.setPhone("13800138003");
            user3.setNickname("王五");
            user3.setStatus(User.UserStatus.ACTIVE);
            user3.setEmailVerified(false);
            userRepository.save(user3);
            log.info("创建用户：wangwu / 123456");
        }
    }

    /**
     * 初始化地址数据
     */
    private void initAddresses() {
        // 为张三创建地址
        userRepository.findByUsername("zhangsan").ifPresent(user -> {
            long count = addressRepository.countByUserAndStatus(user, Address.AddressStatus.ACTIVE);
            if (count == 0) {
                // 地址 1 - 默认地址
                Address addr1 = createAddress(user, "张三", "13800138001",
                        "北京市", "北京市", "朝阳区", "中关村大街 1 号", true);
                addressRepository.save(addr1);

                // 地址 2
                Address addr2 = createAddress(user, "张三", "13800138001",
                        "上海市", "上海市", "浦东新区", "张江高科技园区", false);
                addressRepository.save(addr2);

                log.info("为用户 zhangsan 创建 2 个地址");
            }
        });

        // 为李四创建地址
        userRepository.findByUsername("lisi").ifPresent(user -> {
            long count = addressRepository.countByUserAndStatus(user, Address.AddressStatus.ACTIVE);
            if (count == 0) {
                // 地址 1 - 默认地址
                Address addr1 = createAddress(user, "李四", "13800138002",
                        "广东省", "广州市", "天河区", "天河路 100 号", true);
                addressRepository.save(addr1);

                log.info("为用户 lisi 创建 1 个地址");
            }
        });
    }

    /**
     * 创建地址对象
     */
    private Address createAddress(User user, String receiverName, String receiverPhone,
                                   String province, String city, String district,
                                   String detailAddress, Boolean isDefault) {
        Address address = new Address();
        address.setUser(user);
        address.setReceiverName(receiverName);
        address.setReceiverPhone(receiverPhone);
        address.setProvince(province);
        address.setCity(city);
        address.setDistrict(district);
        address.setDetailAddress(detailAddress);
        address.setIsDefault(isDefault);
        address.setStatus(Address.AddressStatus.ACTIVE);
        return address;
    }

    /**
     * 初始化示例商品数据
     */
    private void initProducts() {
        log.info("开始初始化示例商品数据...");

        // 为张三创建商品
        userRepository.findByUsername("zhangsan").ifPresent(user -> {
            long count = productRepository.countBySellerId(user.getId());
            if (count == 0) {
                // 商品 1：二手 iPhone 14 Pro
                createProduct(user, "二手 iPhone 14 Pro", "深空黑色，256GB，国行在保",
                        new BigDecimal("6500.00"), new BigDecimal("8999.00"), 1,
                        "https://picsum.photos/400/400?random=1",
                        "手机数码", "手机", "二手", "苹果");

                // 商品 2：二手 MacBook Air M1
                createProduct(user, "二手 MacBook Air M1", "深空灰，8GB+256GB，2020 款，电池健康度 95%",
                        new BigDecimal("5200.00"), new BigDecimal("7999.00"), 1,
                        "https://picsum.photos/400/400?random=2",
                        "电脑办公", "笔记本", "二手", "苹果");

                // 商品 3：索尼 WH-1000XM4 耳机
                createProduct(user, "索尼 WH-1000XM4 降噪耳机", "黑色，95 新，带原装收纳盒",
                        new BigDecimal("1200.00"), new BigDecimal("2499.00"), 2,
                        "https://picsum.photos/400/400?random=3",
                        "手机数码", "耳机", "二手", "索尼");

                // 商品 4：iPad Pro 11 寸
                createProduct(user, "二手 iPad Pro 11 寸", "2021 款，M1 芯片，128GB，WIFI 版",
                        new BigDecimal("4500.00"), new BigDecimal("6799.00"), 1,
                        "https://picsum.photos/400/400?random=4",
                        "手机数码", "平板", "二手", "苹果");

                // 商品 5：Nintendo Switch OLED
                createProduct(user, "Nintendo Switch OLED 日版", "白色，99 新，箱说全，送游戏卡带",
                        new BigDecimal("1600.00"), new BigDecimal("2099.00"), 1,
                        "https://picsum.photos/400/400?random=5",
                        "娱乐玩具", "游戏机", "二手", "任天堂");

                log.info("为用户 zhangsan 创建 5 个商品");
            }
        });

        // 为李四创建商品
        userRepository.findByUsername("lisi").ifPresent(user -> {
            long count = productRepository.countBySellerId(user.getId());
            if (count == 0) {
                // 商品 6：二手佳能 EOS R50 相机
                createProduct(user, "二手佳能 EOS R50 微单相机", "套机 18-45mm，95 新，快门数 2000+",
                        new BigDecimal("4200.00"), new BigDecimal("5999.00"), 1,
                        "https://picsum.photos/400/400?random=6",
                        "手机数码", "相机", "二手", "佳能");

                // 商品 7：戴森 V10 吸尘器
                createProduct(user, "戴森 V10 Fluffy 吸尘器", "红色，9 成新，吸力强劲",
                        new BigDecimal("1800.00"), new BigDecimal("3290.00"), 1,
                        "https://picsum.photos/400/400?random=7",
                        "家用电器", "吸尘器", "二手", "戴森");

                // 商品 8：罗技 MX Master 3 鼠标
                createProduct(user, "罗技 MX Master 3 无线鼠标", "深空灰，99 新，人体工学设计",
                        new BigDecimal("450.00"), new BigDecimal("799.00"), 3,
                        "https://picsum.photos/400/400?random=8",
                        "电脑办公", "鼠标", "二手", "罗技");

                log.info("为用户 lisi 创建 3 个商品");
            }
        });

        // 为王五创建商品
        userRepository.findByUsername("wangwu").ifPresent(user -> {
            long count = productRepository.countBySellerId(user.getId());
            if (count == 0) {
                // 商品 9：二手 Airpods Pro 2 代
                createProduct(user, "Airpods Pro 2 代", "苹果无线降噪耳机，95 新，带 MagSafe 充电盒",
                        new BigDecimal("1100.00"), new BigDecimal("1899.00"), 2,
                        "https://picsum.photos/400/400?random=9",
                        "手机数码", "耳机", "二手", "苹果");

                // 商品 10：小米 13 Ultra
                createProduct(user, "小米 13 Ultra 二手", "12GB+256GB，徕卡影像，黑色",
                        new BigDecimal("3800.00"), new BigDecimal("5999.00"), 1,
                        "https://picsum.photos/400/400?random=10",
                        "手机数码", "手机", "二手", "小米");

                log.info("为用户 wangwu 创建 2 个商品");
            }
        });

        log.info("商品数据初始化完成，共创建 10 个商品");
    }

    /**
     * 创建商品
     */
    private Product createProduct(User user, String name, String description,
                                   BigDecimal price, BigDecimal originalPrice, Integer stock,
                                   String mainImage, String category, String... tags) {
        Product product = new Product();
        product.setSellerId(user.getId());
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setOriginalPrice(originalPrice);
        product.setStock(stock);
        product.setMainImage(mainImage);
        product.setCategory(category);
        product.setIsOnSale(true);
        product.setIsFeatured(false);
        product.setStatus(stock <= 0 ? Product.ProductStatus.OUT_OF_STOCK : Product.ProductStatus.ON_SALE);
        product.setSalesCount(0);
        product.setViewCount(0);

        // 添加标签
        if (tags != null && tags.length > 0) {
            for (String tag : tags) {
                product.getTags().add(tag);
            }
        }

        return productRepository.save(product);
    }

    /**
     * 初始化订单数据
     */
    private void initOrders() {
        log.info("开始初始化订单数据...");

        User zhangsan = userRepository.findByUsername("zhangsan").orElse(null);
        User lisi = userRepository.findByUsername("lisi").orElse(null);
        User wangwu = userRepository.findByUsername("wangwu").orElse(null);

        if (zhangsan == null || lisi == null || wangwu == null) {
            log.warn("用户数据不完整，跳过订单初始化");
            return;
        }

        // 获取用户 ID
        Long zhangsanId = zhangsan.getId();
        Long lisiId = lisi.getId();
        Long wangwuId = wangwu.getId();

        // 获取商品列表
        var zhangsanProducts = productRepository.findBySellerId(zhangsanId);
        var lisiProducts = productRepository.findBySellerId(lisiId);
        var wangwuProducts = productRepository.findBySellerId(wangwuId);

        if (zhangsanProducts.isEmpty() || lisiProducts.isEmpty()) {
            log.warn("商品数据不完整，跳过订单初始化");
            return;
        }

        // 订单 1: 李四购买张三的 iPhone 14 Pro - 已完成（双方已评价）
        createOrderWithReview(
                lisi, zhangsan, zhangsanProducts.get(0), // iPhone
                Order.OrderStatus.COMPLETED,
                "图书馆门口",
                "周末下午 2 点",
                "希望面交时提前联系",
                true, true // 双方已评价
        );

        // 订单 2: 王五购买张三的 MacBook Air - 待评价（仅买家评价）
        createOrderWithReview(
                wangwu, zhangsan, zhangsanProducts.get(1), // MacBook
                Order.OrderStatus.PENDING_REVIEW,
                "教学楼 A 栋一楼",
                "工作日下午 5 点",
                null,
                true, false // 买家已评价，卖家未评价
        );

        // 订单 3: 张三购买李四的佳能相机 - 待确认（卖家确认收款）
        createOrder(
                zhangsan, lisi, lisiProducts.get(0), // 佳能相机
                Order.OrderStatus.PENDING_CONFIRM,
                "食堂门口",
                "周六上午 10 点",
                "第一次买相机，希望能详细教一下使用"
        );

        // 订单 4: 李四购买王五的 Airpods - 待提货
        createOrder(
                lisi, wangwu, wangwuProducts.get(0), // Airpods
                Order.OrderStatus.PENDING_PICKUP,
                "快递中心",
                "周日下午 3 点",
                null
        );

        // 订单 5: 王五购买张三的索尼耳机 - 待付款
        createOrder(
                wangwu, zhangsan, zhangsanProducts.get(2), // 索尼耳机
                Order.OrderStatus.PENDING_PAYMENT,
                "学生宿舍区",
                "晚上 7 点后",
                "学生党预算有限，能小刀吗"
        );

        // 订单 6: 张三购买李四的戴森吸尘器 - 已取消（买家取消）
        createCancelledOrder(
                zhangsan, lisi, lisiProducts.get(1), // 戴森
                Order.OrderStatus.CANCELLED,
                "临时改变主意，不需要了",
                Order.CancelRole.BUYER
        );

        // 订单 7: 李四购买张三的 iPad Pro - 已完成
        createOrderWithReview(
                lisi, zhangsan, zhangsanProducts.get(3), // iPad
                Order.OrderStatus.COMPLETED,
                "咖啡厅",
                "周六下午 2 点",
                "喜欢喝咖啡的时候谈交易",
                true, true
        );

        // 订单 8: 王五购买李四的罗技鼠标 - 待评价
        createOrderWithReview(
                wangwu, lisi, lisiProducts.get(2), // 罗技鼠标
                Order.OrderStatus.PENDING_REVIEW,
                "实验室楼下",
                "工作日中午",
                null,
                false, false // 双方都未评价
        );

        // 订单 9: 张三购买王五的小米手机 - 待提货
        createOrder(
                zhangsan, wangwu, wangwuProducts.get(1), // 小米手机
                Order.OrderStatus.PENDING_PICKUP,
                "校门口",
                "周日早上 10 点",
                "需要检查手机外观"
        );

        // 订单 10: 李四购买张三的 Switch - 已取消（卖家取消）
        createCancelledOrder(
                lisi, zhangsan, zhangsanProducts.get(4), // Switch
                Order.OrderStatus.CANCELLED,
                "已经有其他买家出价更高",
                Order.CancelRole.SELLER
        );

        log.info("订单数据初始化完成，共创建 10 个订单");
    }

    /**
     * 创建订单（通用方法）
     */
    private Order createOrder(User buyer, User seller, Product product,
                              Order.OrderStatus status,
                              String meetLocation, String meetTimeStr,
                              String buyerRemark) {
        // 检查是否已有订单
        if (orderRepository.hasActiveOrder(product.getId())) {
            log.info("商品 {} 已有未完成订单，跳过", product.getName());
            return null;
        }

        String orderNo = generateOrderNo();
        java.time.LocalDateTime meetTime = null;
        if (meetTimeStr != null) {
            try {
                meetTime = java.time.LocalDateTime.parse(meetTimeStr + "T14:00:00");
            } catch (Exception e) {
                meetTime = java.time.LocalDateTime.now().plusDays(3);
            }
        }

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setBuyerId(buyer.getId());
        order.setSellerId(seller.getId());
        order.setProductId(product.getId());
        order.setProductName(product.getName());
        order.setProductImage(product.getMainImage());
        order.setPrice(product.getPrice());
        order.setQuantity(1);
        order.setTotalAmount(product.getPrice());
        order.setMeetLocation(meetLocation);
        order.setMeetTime(meetTime);
        order.setBuyerRemark(buyerRemark);
        order.setStatus(status);

        // 根据状态设置时间
        if (status == Order.OrderStatus.PENDING_PICKUP ||
            status == Order.OrderStatus.PENDING_CONFIRM ||
            status == Order.OrderStatus.PENDING_REVIEW ||
            status == Order.OrderStatus.COMPLETED) {
            order.setPaymentTime(java.time.LocalDateTime.now().minusDays(2));
        }
        if (status == Order.OrderStatus.PENDING_CONFIRM ||
            status == Order.OrderStatus.PENDING_REVIEW ||
            status == Order.OrderStatus.COMPLETED) {
            order.setPickupTime(java.time.LocalDateTime.now().minusDays(1));
        }
        if (status == Order.OrderStatus.PENDING_REVIEW ||
            status == Order.OrderStatus.COMPLETED) {
            order.setConfirmTime(java.time.LocalDateTime.now());
        }

        order = orderRepository.save(order);
        logOrderAction(order.getId(), buyer.getId(), OrderLog.OperatorRole.BUYER,
                       "CREATE_ORDER", null, status, "订单创建");
        return order;
    }

    /**
     * 创建带评价的订单
     */
    private void createOrderWithReview(User buyer, User seller, Product product,
                                       Order.OrderStatus status,
                                       String meetLocation, String meetTimeStr,
                                       String buyerRemark,
                                       boolean hasBuyerReview, boolean hasSellerReview) {
        Order order = createOrder(buyer, seller, product, status, meetLocation, meetTimeStr, buyerRemark);
        if (order == null) return;

        // 添加买家评价
        if (hasBuyerReview) {
            OrderReview buyerReview = new OrderReview();
            buyerReview.setOrderId(order.getId());
            buyerReview.setReviewerId(buyer.getId());
            buyerReview.setRevieweeId(seller.getId());
            buyerReview.setProductId(product.getId());
            buyerReview.setRating(5);
            buyerReview.setContent("商品很好，和描述一致，卖家态度也很好！");
            buyerReview.setReviewType(OrderReview.ReviewType.BUYER_REVIEW);
            orderReviewRepository.save(buyerReview);
        }

        // 添加卖家评价
        if (hasSellerReview) {
            OrderReview sellerReview = new OrderReview();
            sellerReview.setOrderId(order.getId());
            sellerReview.setReviewerId(seller.getId());
            sellerReview.setRevieweeId(buyer.getId());
            sellerReview.setProductId(product.getId());
            sellerReview.setRating(5);
            sellerReview.setContent("很好的买家，交易愉快！");
            sellerReview.setReviewType(OrderReview.ReviewType.SELLER_REVIEW);
            orderReviewRepository.save(sellerReview);
        }
    }

    /**
     * 创建已取消的订单
     */
    private void createCancelledOrder(User buyer, User seller, Product product,
                                      Order.OrderStatus status,
                                      String cancelReason, Order.CancelRole cancelRole) {
        Order order = createOrder(buyer, seller, product, status, null, null, null);
        if (order == null) return;

        order.setCancelReason(cancelReason);
        order.setCancelRole(cancelRole);
        order.setCancelTime(java.time.LocalDateTime.now());
        orderRepository.save(order);

        logOrderAction(order.getId(), cancelRole == Order.CancelRole.BUYER ? buyer.getId() : seller.getId(),
                       cancelRole == Order.CancelRole.BUYER ? OrderLog.OperatorRole.BUYER : OrderLog.OperatorRole.SELLER,
                       "CANCEL_ORDER", Order.OrderStatus.PENDING_PAYMENT, Order.OrderStatus.CANCELLED, cancelReason);
    }

    /**
     * 记录订单日志
     */
    private void logOrderAction(Long orderId, Long operatorId, OrderLog.OperatorRole role,
                                String action, Order.OrderStatus fromStatus,
                                Order.OrderStatus toStatus, String remark) {
        OrderLog log = new OrderLog();
        log.setOrderId(orderId);
        log.setOperatorId(operatorId);
        log.setOperatorRole(role);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setRemark(remark);
        orderLogRepository.save(log);
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String timestamp = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        java.util.Random random = new java.util.Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return timestamp + sb;
    }
}
