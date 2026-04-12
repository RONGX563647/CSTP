package com.aisale.backend.service;

import com.aisale.backend.entity.Admin;
import com.aisale.backend.entity.User;
import com.aisale.backend.repository.AdminRepository;
import com.aisale.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 先尝试从用户表查询
        return userRepository.findByUsername(username)
                .map(this::buildUserDetails)
                .orElseGet(() -> {
                    // 用户表不存在，尝试从管理员表查询
                    return adminRepository.findByUsername(username)
                            .map(this::buildAdminDetails)
                            .orElseThrow(() -> new UsernameNotFoundException("用户不存在：" + username));
                });
    }

    private UserDetails buildUserDetails(User user) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }

    private UserDetails buildAdminDetails(Admin admin) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        // 根据管理员角色分配权限
        if (admin.getRole() == Admin.AdminRole.SUPER_ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }
        if (admin.getRole() == Admin.AdminRole.ADMIN || admin.getRole() == Admin.AdminRole.SUPER_ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        // 管理员也具有 USER 角色权限，可以访问用户端接口
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        return new org.springframework.security.core.userdetails.User(
                admin.getUsername(),
                admin.getPassword(),
                authorities
        );
    }
}
