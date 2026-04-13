package com.aisale.backend.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OssConfig {

    private final OssProperties ossProperties;

    @Bean
    @ConditionalOnProperty(prefix = "aliyun.oss", name = {"access-key-id", "access-key-secret", "bucket-name"})
    public OSS ossClient() {
        log.info("Initializing Aliyun OSS Client with endpoint: {}", ossProperties.getEndpoint());
        log.info("Bucket name: {}", ossProperties.getBucketName());
        
        return new OSSClientBuilder().build(
            ossProperties.getEndpoint(),
            ossProperties.getAccessKeyId(),
            ossProperties.getAccessKeySecret()
        );
    }
}