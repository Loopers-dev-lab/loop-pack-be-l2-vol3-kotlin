package com.loopers.interfaces.support.config

import com.loopers.interfaces.support.auth.AuthUserArgumentResolver
import com.loopers.interfaces.support.interceptor.AdminInterceptor
import com.loopers.interfaces.support.interceptor.AuthInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val authInterceptor: AuthInterceptor,
    private val adminInterceptor: AdminInterceptor,
    private val authUserArgumentResolver: AuthUserArgumentResolver,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns(
                "/api/v1/users/**",
                "/api/v1/products/*/likes",
                "/api/v1/orders/**",
            )
            .excludePathPatterns("/api/v1/users/sign-up")

        registry.addInterceptor(adminInterceptor)
            .addPathPatterns("/api-admin/**")
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authUserArgumentResolver)
    }
}
