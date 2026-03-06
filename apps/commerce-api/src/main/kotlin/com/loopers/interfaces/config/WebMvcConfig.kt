package com.loopers.interfaces.config

import com.loopers.interfaces.config.auth.AdminAuthenticationInterceptor
import com.loopers.interfaces.config.auth.AuthenticatedMemberArgumentResolver
import com.loopers.interfaces.config.auth.MemberAuthenticationInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val memberAuthenticationInterceptor: MemberAuthenticationInterceptor,
    private val adminAuthenticationInterceptor: AdminAuthenticationInterceptor,
    private val authenticatedMemberArgumentResolver: AuthenticatedMemberArgumentResolver,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(memberAuthenticationInterceptor)
            .addPathPatterns("/api/**")
        registry.addInterceptor(adminAuthenticationInterceptor)
            .addPathPatterns("/api-admin/**")
    }

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(StringToProductSortRequestConverter())
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authenticatedMemberArgumentResolver)
    }
}
