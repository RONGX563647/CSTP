package com.aisale.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Spring Security 配置
 *
 * 开发环境：开放所有接口，无需认证
 * 生产环境：启用 JWT 认证
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 开发环境安全配置
     * 开放所有接口，禁用 CSRF，允许 H2 Console
     */
    @Bean
    @Profile("dev")
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .anyRequest().permitAll()  // 开发环境开放所有接口
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))  // 允许 H2 Console 使用 frame
            .build();
    }

    /**
     * 生产环境安全配置
     * 所有接口需要认证
     */
    @Bean
    @Profile("!dev")
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").denyAll()  // 生产环境禁用 H2
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").authenticated()
                .anyRequest().authenticated()
            )
            .httpBasic(withDefaults())
            .build();
    }
}
