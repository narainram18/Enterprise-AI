package com.enterpriseai.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.enterpriseai.backend.workspace.security.WorkspaceInterceptor;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final WorkspaceInterceptor workspaceInterceptor;

    public WebMvcConfig(WorkspaceInterceptor workspaceInterceptor) {
        this.workspaceInterceptor = workspaceInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(workspaceInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");
    }
}
