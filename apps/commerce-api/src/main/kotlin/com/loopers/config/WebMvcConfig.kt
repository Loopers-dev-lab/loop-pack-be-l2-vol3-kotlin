package com.loopers.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val adminAuthInterceptor: AdminAuthInterceptor,
    private val userAuthInterceptor: UserAuthInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(adminAuthInterceptor)
            .addPathPatterns("/api-admin/**")
        registry.addInterceptor(userAuthInterceptor)
            .addPathPatterns("/api/**")
    }
}
