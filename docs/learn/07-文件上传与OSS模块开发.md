# 07 文件上传与 OSS 模块开发

学习时长: 1 天
难度: ⭐⭐☆☆☆
前置知识: 阿里云 OSS、MultipartFile、文件验证

---

## 一、本章概述

本章实现文件上传功能，使用阿里云 OSS 存储商品图片、用户头像等文件。

**核心学习内容:**
- OssService 服务类：上传、下载、删除文件
- 文件验证：检查文件类型、大小限制
- 文件名生成：时间戳 + UUID 防止冲突
- 配置类：OssProperties 读取 OSS 配置

---

## 二、OssService 文件上传服务

### 2.1 技术说明

OssService 是文件上传的核心服务类，封装了阿里云 OSS 的所有操作。

**核心方法:**
- `uploadImage`: 上传图片（返回 ImageUploadResponse）
- `uploadFile`: 上传任意类型文件（返回 URL）
- `downloadFile`: 下载文件（返回 InputStream）
- `deleteFile`: 删除文件（根据 URL）
- `fileExists`: 检查文件是否存在

**文件名生成规则:**
```
目录名/时间戳_UUID.扩展名
例: products/20260422103045_a1b2c3d4.jpg
```

### 2.2 实际源码

文件位置: `backend/src/main/java/com/aisale/backend/service/OssService.java`

```java
package com.aisale.backend.service;

import com.aisale.backend.config.OssProperties;
import com.aisale.backend.dto.ImageUploadResponse;
import com.aisale.backend.exception.BusinessException;
import com.aisale.backend.exception.ErrorCode;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Service
public class OssService {

    @Autowired(required = false)
    private OSS ossClient;

    @Autowired
    private OssProperties ossProperties;

    public boolean isOssEnabled() {
        return ossClient != null;
    }

    /**
     * 上传图片
     */
    public ImageUploadResponse uploadImage(MultipartFile file, String directory) {
        if (!isOssEnabled()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, "OSS服务未配置");
        }
        
        validateFile(file);
        
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String fileName = generateFileName(directory, extension);
        
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            
            PutObjectResult result = ossClient.putObject(
                ossProperties.getBucketName(),
                fileName,
                inputStream,
                metadata
            );
            
            log.info("File uploaded successfully: {}, ETag: {}", fileName, result.getETag());
            
            String url = getFileUrl(fileName);
            return new ImageUploadResponse(url, originalFilename, file.getSize());
        } catch (IOException e) {
            log.error("Failed to upload file: {}", fileName, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, e.getMessage());
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }
        
        if (file.getSize() > ossProperties.getMaxFileSize()) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED, 
                "文件大小超过限制: " + ossProperties.getMaxFileSize() / 1024 / 1024 + "MB");
        }
        
        String extension = getFileExtension(file.getOriginalFilename());
        if (extension == null || !Arrays.asList(ossProperties.getAllowedExtensions()).contains(extension.toLowerCase())) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED, "不支持的文件类型: " + extension);
        }
    }

    /**
     * 生成文件名
     */
    private String generateFileName(String directory, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String fileName = timestamp + "_" + uuid + "." + extension;
        
        if (directory != null && !directory.isEmpty()) {
            return directory + "/" + fileName;
        }
        return fileName;
    }

    /**
     * 生成文件 URL
     */
    private String getFileUrl(String fileName) {
        String baseUrl = ossProperties.getBaseUrl();
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl.endsWith("/") ? baseUrl + fileName : baseUrl + "/" + fileName;
        }
        return "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint() + "/" + fileName;
    }
}
```

---

## 三、OSS 配置类

### 3.1 技术说明

OssProperties 读取 application.yml 中的 OSS 配置。

**配置项:**
- `enabled`: 是否启用 OSS
- `endpoint`: OSS 区域节点
- `bucketName`: 存储桶名称
- `baseUrl`: 自定义域名
- `maxFileSize`: 最大文件大小（默认 10MB）
- `allowedExtensions`: 允许的文件扩展名

### 3.2 实际源码

文件位置: `backend/src/main/java/com/aisale/backend/config/OssProperties.java`

```java
package com.aisale.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    private boolean enabled = false;
    private String accessKeyId;
    private String accessKeySecret;
    private String endpoint;
    private String bucketName;
    private String baseUrl;
    private long maxFileSize = 10 * 1024 * 1024; // 10MB
    private String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "webp"};
}
```

---

## 四、文件上传控制器

### 4.1 实际源码

文件位置: `backend/src/main/java/com/aisale/backend/controller/UserOssController.java`

```java
package com.aisale.backend.controller;

import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.dto.ImageUploadResponse;
import com.aisale.backend.service.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "文件上传", description = "图片、文件上传接口")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserOssController {

    private final OssService ossService;

    @Operation(summary = "上传图片")
    @PostMapping("/upload/image")
    public ApiResponse<ImageUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "products") String directory) {
        ImageUploadResponse response = ossService.uploadImage(file, directory);
        return ApiResponse.success("图片上传成功", response);
    }

    @Operation(summary = "上传头像")
    @PostMapping("/upload/avatar")
    public ApiResponse<ImageUploadResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = ossService.uploadImage(file, "avatars");
        return ApiResponse.success("头像上传成功", response);
    }
}
```

---

## 五、扩展练习

### 练习 1: 实现图片压缩

1. 上传大图时自动压缩
2. 使用 Thumbnailator 库
3. 保持宽高比

### 练习 2: 实现图片水印

1. 上传时自动添加店铺水印
2. 使用 Java ImageIO 处理图片

---

最后更新: 2026-04-22
文档版本: 2.0
