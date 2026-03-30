package com.loopers.interfaces.support.config

import com.loopers.interfaces.support.auth.AuthUserArgumentResolver
import com.loopers.interfaces.support.interceptor.AdminInterceptor
import com.loopers.interfaces.support.interceptor.AuthInterceptor
import com.loopers.interfaces.support.interceptor.OptionalAuthInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val authInterceptor: AuthInterceptor,
    private val optionalAuthInterceptor: OptionalAuthInterceptor,
    private val adminInterceptor: AdminInterceptor,
    private val authUserArgumentResolver: AuthUserArgumentResolver,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(authInterceptor)
            .addPathPatterns(
                "/api/v1/users/**",
                "/api/v1/products/*/likes",
                "/api/v1/orders/**",
                "/api/v1/coupons/**",
                "/api/v1/payments/**",
                "/api/v1/queue/**",
            )
            .excludePathPatterns(
                "/api/v1/users/sign-up",
                "/api/v1/payments/callback",
            )

        registry.addInterceptor(optionalAuthInterceptor)
            .addPathPatterns("/api/v1/products/**")
            .excludePathPatterns("/api/v1/products/*/likes")

        registry.addInterceptor(adminInterceptor)
            .addPathPatterns("/api-admin/**")
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authUserArgumentResolver)
    }
}
