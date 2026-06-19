package com.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置，从 application.yml 的 ai-admin.minio.* 读取。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai-admin.minio")
public class MinioConfig {

    /** 访问端点 */
    private String endpoint;

    /** 公网（浏览器）访问端点：用于生成可被前端直连的预签名 URL */
    private String publicEndpoint;

    /** Access Key */
    private String accessKey;

    /** Secret Key */
    private String secretKey;

    /** 默认桶 */
    private String bucket;

    /** 预签名 URL 有效期（秒） */
    private int presignedExpirySeconds = 7200;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
