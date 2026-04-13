package com.aisale.backend.controller;

import com.aisale.backend.config.OssProperties;
import com.aisale.backend.dto.ApiResponse;
import com.aisale.backend.service.OssService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "文件上传", description = "阿里云OSS文件上传管理")
@RestController
@RequestMapping("/api/user/oss")
@Slf4j
public class UserOssController {

    @Autowired(required = false)
    private OssService ossService;

    @Autowired
    private OssProperties ossProperties;

    @Operation(summary = "上传文件")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "directory", defaultValue = "images") String directory) {
        
        if (ossService == null) {
            return ApiResponse.error(503, "OSS服务未配置，请配置阿里云OSS环境变量");
        }
        
        log.info("Uploading file: {}, size: {}, directory: {}", 
            file.getOriginalFilename(), file.getSize(), directory);
        
        String fileUrl = ossService.uploadFile(file, directory);
        
        Map<String, String> result = new HashMap<>();
        result.put("url", fileUrl);
        result.put("fileName", file.getOriginalFilename());
        result.put("size", String.valueOf(file.getSize()));
        
        return ApiResponse.success("文件上传成功", result);
    }

    @Operation(summary = "检查文件是否存在")
    @GetMapping("/exists")
    public ApiResponse<Boolean> checkFileExists(@RequestParam("url") String fileUrl) {
        if (ossService == null) {
            return ApiResponse.error(503, "OSS服务未配置");
        }
        boolean exists = ossService.fileExists(fileUrl);
        return ApiResponse.success(exists);
    }

    @Operation(summary = "删除文件")
    @DeleteMapping("/delete")
    public ApiResponse<Void> deleteFile(@RequestParam("url") String fileUrl) {
        if (ossService == null) {
            return ApiResponse.error(503, "OSS服务未配置");
        }
        log.info("Deleting file: {}", fileUrl);
        ossService.deleteFile(fileUrl);
        return ApiResponse.success();
    }

    @Operation(summary = "获取OSS配置信息")
    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getOssConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxFileSize", ossProperties.getMaxFileSize() / 1024 / 1024 + "MB");
        config.put("allowedExtensions", ossProperties.getAllowedExtensions());
        config.put("bucketName", ossProperties.getBucketName());
        config.put("ossEnabled", ossService != null);
        return ApiResponse.success(config);
    }
}