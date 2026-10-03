package com.order.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private MerchantAuthInterceptor merchantAuthInterceptor;

    @Autowired
    private FileConfigProperties fileConfigProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 将本地文件存储目录映射为静态资源，
     * 使前端可通过 {baseUrl}/file/{relativePath} 直接访问上传的图片。
     *
     * 注意：这里只映射专用前缀 /file/**，不能使用 /**，
     * 否则会覆盖 Controller 的路由与 Spring 默认静态资源处理，导致接口与图片都无法访问。
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String basePath = fileConfigProperties.getBasePath();
        if (StringUtils.hasText(basePath)) {
            String location = "file:" + basePath.replace("\\", "/");
            if (!location.endsWith("/")) {
                location = location + "/";
            }
            registry.addResourceHandler("/file/**")
                    .addResourceLocations(location);
        }
    }

    /**
     * 注册店家端统一鉴权拦截器
     * 具体拦截的路径与方法由 MerchantAuthInterceptor 内部的规则表决定，
     * 这里统一挂到 /api/** 上，避免遗漏新增接口。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(merchantAuthInterceptor)
                .addPathPatterns("/api/**")
                .order(1);
    }
}
