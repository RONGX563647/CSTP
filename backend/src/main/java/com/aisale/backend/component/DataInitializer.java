package com.aisale.backend.component;

import com.aisale.backend.entity.Address;
import com.aisale.backend.entity.Admin;
import com.aisale.backend.entity.Product;
import com.aisale.backend.entity.User;
import com.aisale.backend.repository.AddressRepository;
import com.aisale.backend.repository.AdminRepository;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("开始初始化测试数据...");

        initAdmins();
        initUsers();
        initAddresses();
        initProducts();

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
}
