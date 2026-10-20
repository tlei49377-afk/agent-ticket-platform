package com.atp.server.config;

import com.atp.server.interceptor.JwtTokenInterceptor;
import com.atp.server.properties.UploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final JwtTokenInterceptor jwtTokenInterceptor;
    private final UploadProperties uploadProperties;

    /**
     * 注册 JWT 拦截器。登录、附件、健康检查、接口文档这些路径放行
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtTokenInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/logout",
                        "/actuator/**",
                        "/v3/api-docs/**",
                        "/error",
                        // 附件是给 <img src> 直接访问的，浏览器带不了请求头里的 token
                        "/files/**"
                );
    }

    /**
     * 把磁盘上的附件目录挂成可访问的 URL
     * @param registry
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 把磁盘上的附件目录挂到 /files/** 上，URL 与文件路径的对应关系只在这里写一次
        String location = Paths.get(uploadProperties.getUploadDir())
                .toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(uploadProperties.getUrlPrefix() + "/**")
                .addResourceLocations(location);
    }

    /**
     * 跨域配置，前端本地开发直连后端时要用
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
