package com;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 实习管理 AI 智能体系统 启动类
 *
 * 注意：scanBasePackages 显式限定为本项目包，避免 @SpringBootApplication 默认扫描 "com"
 * 时把第三方 jar 中位于 com.github.xxx 下的 @Component / @ConfigurationProperties 类
 * （例如 Knife4jProperties）当作本工程组件重复注册，导致 Bean 冲突。
 */
@SpringBootApplication(scanBasePackages = {
        "com.config",
        "com.common",
        "com.controller",
        "com.service",
        "com.entity",
        "com.dto"
})
@EnableScheduling
@MapperScan("com.mapper")
public class AiAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAdminApplication.class, args);
    }
}
