package com.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 3 文档配置。
 * 启动后访问：http://localhost:8080/doc.html
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("实习管理 AI 智能体系统 - API")
                        .description("MVP 版本：4 角色 + 4 核心表单 + 三色预警 + AI 问答")
                        .version("v1.0.0-MVP")
                        .contact(new Contact().name("zr-ai-admin").email("admin@zr.edu"))
                        .license(new License().name("Internal Use Only")));
    }
}
