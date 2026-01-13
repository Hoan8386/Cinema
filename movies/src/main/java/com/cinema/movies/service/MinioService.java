package com.cinema.movies.service;

import com.cinema.movies.config.MinioProperties;
import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
@Slf4j
public class MinioService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioService(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    /**
     * Upload file to MinIO
     * 
     * @param file MultipartFile to upload
     * @return URL of uploaded file
     */
    public String uploadFile(MultipartFile file) throws Exception {
        validateFile(file);

        // Tạo tên file unique
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String fileName = UUID.randomUUID().toString() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            String fileUrl = String.format("%s/%s/%s",
                    minioProperties.getEndpoint(),
                    minioProperties.getBucketName(),
                    fileName);

            log.info("Uploaded file: {} -> {}", originalFilename, fileUrl);
            return fileUrl;

        } catch (Exception e) {
            log.error("Failed to upload file: {}", originalFilename, e);
            throw new Exception("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Download file from MinIO
     * 
     * @param fileName Name of file to download
     * @return InputStream of file
     */
    public InputStream downloadFile(String fileName) throws Exception {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .build());
        } catch (Exception e) {
            log.error("Failed to download file: {}", fileName, e);
            throw new Exception("Failed to download file: " + e.getMessage());
        }
    }

    /**
     * Delete file from MinIO
     * 
     * @param fileName Name of file to delete
     */
    public void deleteFile(String fileName) throws Exception {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .build());
            log.info("Deleted file: {}", fileName);
        } catch (Exception e) {
            log.error("Failed to delete file: {}", fileName, e);
            throw new Exception("Failed to delete file: " + e.getMessage());
        }
    }

    /**
     * Delete file by URL
     * 
     * @param fileUrl URL of file to delete
     */
    public void deleteFileByUrl(String fileUrl) throws Exception {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        // Extract filename from URL
        // Format: http://localhost:9000/cinema-movies/filename.jpg
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        deleteFile(fileName);
    }

    /**
     * Get presigned URL for viewing file (valid for 7 days)
     * 
     * @param fileName Name of file
     * @return Presigned URL
     */
    public String getPresignedUrl(String fileName) throws Exception {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(io.minio.http.Method.GET)
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .expiry(60 * 60 * 24 * 7) // 7 days
                            .build());
        } catch (Exception e) {
            log.error("Failed to get presigned URL for file: {}", fileName, e);
            throw new Exception("Failed to get presigned URL: " + e.getMessage());
        }
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new Exception("File is empty");
        }

        // Check file size
        if (file.getSize() > minioProperties.getImageSize().getMax()) {
            throw new Exception(String.format("File size exceeds maximum limit of %d bytes",
                    minioProperties.getImageSize().getMax()));
        }

        // Check file type (only images)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new Exception("Only image files are allowed");
        }
    }

    /**
     * Check if file exists
     */
    public boolean fileExists(String fileName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(fileName)
                            .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
