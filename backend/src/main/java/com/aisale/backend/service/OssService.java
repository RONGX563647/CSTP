package com.aisale.backend.service;

import com.aisale.backend.config.OssProperties;
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

    public String uploadFile(MultipartFile file, String directory) {
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
            
            return getFileUrl(fileName);
        } catch (IOException e) {
            log.error("Failed to upload file: {}", fileName, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, e.getMessage());
        }
    }

    public InputStream downloadFile(String fileUrl) {
        String fileName = extractFileNameFromUrl(fileUrl);
        
        try {
            OSSObject ossObject = ossClient.getObject(ossProperties.getBucketName(), fileName);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            log.error("Failed to download file: {}", fileName, e);
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_ERROR, e.getMessage());
        }
    }

    public void deleteFile(String fileUrl) {
        String fileName = extractFileNameFromUrl(fileUrl);
        
        try {
            ossClient.deleteObject(ossProperties.getBucketName(), fileName);
            log.info("File deleted successfully: {}", fileName);
        } catch (Exception e) {
            log.error("Failed to delete file: {}", fileName, e);
            throw new BusinessException(ErrorCode.FILE_DELETE_ERROR, e.getMessage());
        }
    }

    public boolean fileExists(String fileUrl) {
        String fileName = extractFileNameFromUrl(fileUrl);
        return ossClient.doesObjectExist(ossProperties.getBucketName(), fileName);
    }

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

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return null;
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private String generateFileName(String directory, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String fileName = timestamp + "_" + uuid + "." + extension;
        
        if (directory != null && !directory.isEmpty()) {
            return directory + "/" + fileName;
        }
        return fileName;
    }

    private String getFileUrl(String fileName) {
        String baseUrl = ossProperties.getBaseUrl();
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl.endsWith("/") ? baseUrl + fileName : baseUrl + "/" + fileName;
        }
        return "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint() + "/" + fileName;
    }

    private String extractFileNameFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_ERROR, "文件URL不能为空");
        }
        
        int lastSlashIndex = fileUrl.lastIndexOf("/");
        if (lastSlashIndex == -1) {
            return fileUrl;
        }
        return fileUrl.substring(lastSlashIndex + 1);
    }
}