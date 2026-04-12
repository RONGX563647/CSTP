package com.aisale.backend.component;

import com.aisale.backend.entity.Address;
import com.aisale.backend.entity.Admin;
import com.aisale.backend.entity.User;
import com.aisale.backend.repository.AddressRepository;
import com.aisale.backend.repository.AdminRepository;
import com.aisale.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("开始初始化测试数据...");

        initAdmins();
        initUsers();
        initAddresses();

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
}
