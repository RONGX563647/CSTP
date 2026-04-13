package com.aisale.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssProperties {

    private String endpoint;

    private String accessKeyId;

    private String accessKeySecret;

    private String bucketName;

    private String baseUrl;

    private Long maxFileSize = 10 * 1024 * 1024L;

    private String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};
}