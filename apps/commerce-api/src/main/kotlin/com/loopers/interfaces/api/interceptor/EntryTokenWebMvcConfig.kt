package com.loopers.interfaces.api.interceptor

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class EntryTokenWebMvcConfig(
    private val entryTokenInterceptor: EntryTokenInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(entryTokenInterceptor)
            .addPathPatterns("/api/v1/orders")
            .addPathPatterns("/api/v1/orders/**")
    }
}
