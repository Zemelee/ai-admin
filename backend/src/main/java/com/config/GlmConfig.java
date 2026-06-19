package com.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * GLM-4-Air-250414 接入配置，从 ai-admin.glm.* 读取。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai-admin.glm")
public class GlmConfig {

    /** 模型调用端点 */
    private String endpoint;

    /** API Key */
    private String apiKey;

    /** 模型名 */
    private String model = "GLM-4-Air-250414";

    /** 请求超时（秒） */
    private int timeoutSeconds = 30;

    /** 默认 temperature */
    private double temperature = 0.6;

    @Bean("glmRestTemplate")
    public RestTemplate glmRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
