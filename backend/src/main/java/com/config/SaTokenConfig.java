package com.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 拦截器配置：仅做登录态校验，角色校验下沉到 Controller 注解 @SaCheckRole。
 * 放行：登录、登出、CORS 预检请求、Knife4j、静态资源、健康检查。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
                    if ("OPTIONS".equalsIgnoreCase(SaHolder.getRequest().getMethod())) {
                        return;
                    }
                    SaRouter.match("/**")
                            .notMatch("/auth/login")
                            .notMatch("/auth/logout")
                            .notMatch("/error")
                            .notMatch("/doc.html")
                            .notMatch("/webjars/**")
                            .notMatch("/v3/api-docs/**")
                            .notMatch("/swagger-resources/**")
                            .notMatch("/favicon.ico")
                            .check(r -> StpUtil.checkLogin());
                }))
                .addPathPatterns("/**");
    }
}
