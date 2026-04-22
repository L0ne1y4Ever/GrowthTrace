package com.growthtrace.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    // CORS 已在 SecurityConfig 中统一配置；此处保留作为 MVC 层扩展点（如拦截器、参数解析器等）。
}
