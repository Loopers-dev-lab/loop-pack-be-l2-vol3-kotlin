package com.loopers.interfaces.api.admin

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AdminWebMvcConfig(
    private val adminAuthInterceptor: AdminAuthInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(adminAuthInterceptor)
            .addPathPatterns("/api-admin/**")
    }
}
