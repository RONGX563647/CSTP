package com.aisale.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j 配置 (基于 springdoc-openapi)
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AiSale API")
                        .description("AI 销售后台管理系统 API 文档")
                        .version("1.0")
                        .contact(new Contact()
                                .name("AiSale Team")
                                .email("support@aisale.com")));
    }

    @Bean
    public GlobalOpenApiCustomizer globalOpenApiCustomizer() {
        return openApi -> {
            // 此配置主要用于 OpenAPI 规范生成
            // Knife4j 的 afterScript 需要在 application.yml 中配置
        };
    }
}
