# 07 文件上传与OSS模块开发

学习时长: 1-2 天
难度: ⭐⭐⭐☆☆
前置知识: 阿里云OSS、文件上传、Multipart请求

---

## 一、本章概述

本章将实现文件上传功能，支持图片上传到阿里云OSS。

**学习内容:**
- 阿里云OSS配置
- 文件上传服务
- 图片验证
- 上传响应DTO
- 前端上传组件

---

## 二、OSS配置

文件位置: `backend/src/main/java/com/aisale/backend/config/OssConfig.java`

```java
package com.aisale.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;
    private String baseUrl;
    private long maxFileSize = 10 * 1024 * 1024; // 10MB
    private String allowedExtensions = "jpg,jpeg,png,gif,bmp,webp";
}
```

---

## 三、OSS服务

文件位置: `backend/src/main/java/com/aisale/backend/service/OssService.java`

```java
package com.aisale.backend.service;

import com.aisale.backend.config.OssConfig;
import com.aisale.backend.dto.ImageUploadResponse;
import com.aisale.backend.exception.business.BusinessException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OssService {

    private final OSS ossClient;
    private final OssConfig ossConfig;

    public ImageUploadResponse uploadImage(MultipartFile file, String folder) {
        // 验证文件
        validateFile(file);

        try {
            // 生成文件名
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null ? 
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String fileName = folder + "/" + UUID.randomUUID() + extension;

            // 上传到OSS
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());

            ossClient.putObject(ossConfig.getBucketName(), fileName, 
                file.getInputStream(), metadata);

            // 生成访问URL
            String url = ossConfig.getBaseUrl() + "/" + fileName;

            return ImageUploadResponse.builder()
                    .url(url)
                    .fileName(fileName)
                    .originalName(originalFilename)
                    .size(file.getSize())
                    .build();

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException(500, "文件上传失败");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }

        // 验证文件大小
        if (file.getSize() > ossConfig.getMaxFileSize()) {
            throw new BusinessException(400, "文件大小不能超过10MB");
        }

        // 验证文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(400, "文件名不能为空");
        }

        String extension = originalFilename.substring(
            originalFilename.lastIndexOf(".") + 1).toLowerCase();
        List<String> allowed = Arrays.asList(
            ossConfig.getAllowedExtensions().split(","));

        if (!allowed.contains(extension)) {
            throw new BusinessException(400, "不支持的文件类型");
        }
    }
}
```

---

## 四、上传响应DTO

```java
package com.aisale.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {
    private String url;
    private String fileName;
    private String originalName;
    private long size;
}
```

---

## 五、上传控制器

```java
package com.aisale.backend.controller;

import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.dto.ImageUploadResponse;
import com.aisale.backend.service.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;

@Tag(name = "文件上传", description = "图片上传到OSS")
@RestController
@RequestMapping("/api/user/upload")
@RequiredArgsConstructor
public class UploadController {

    private final OssService ossService;

    @Operation(summary = "上传单张图片")
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file) {
        ImageUploadResponse response = ossService.uploadImage(file, "images");
        return ApiResponse.success("上传成功", response);
    }

    @Operation(summary = "批量上传图片")
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<ImageUploadResponse>> uploadImages(
            @RequestParam("files") MultipartFile[] files) {
        List<ImageUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            ImageUploadResponse response = ossService.uploadImage(file, "images");
            responses.add(response);
        }
        return ApiResponse.success("上传成功", responses);
    }
}
```

---

## 六、前端上传组件

```vue
<template>
  <div class="upload-container">
    <el-upload
      action="/api/user/upload/image"
      :headers="headers"
      :on-success="handleSuccess"
      :on-error="handleError"
      :before-upload="beforeUpload"
      list-type="picture-card"
      :limit="5"
      accept="image/*"
    >
      <el-icon><Plus /></el-icon>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const headers = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

const beforeUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过10MB')
    return false
  }
  return true
}

const handleSuccess = (response: any) => {
  ElMessage.success('上传成功')
  console.log('上传结果:', response)
}

const handleError = () => {
  ElMessage.error('上传失败')
}
</script>
```

---

最后更新: 2026-04-22
文档版本: 1.0
