package com.cinema.movies.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
    private ImageSize imageSize = new ImageSize();

    @Data
    public static class ImageSize {
        private Long max = 5242880L; // 5MB default
    }
}
