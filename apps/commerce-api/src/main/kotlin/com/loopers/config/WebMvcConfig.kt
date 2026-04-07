package com.loopers.config

import com.loopers.interfaces.api.auth.AdminAuthArgumentResolver
import com.loopers.interfaces.api.auth.CurrentUserIdArgumentResolver
import com.loopers.interfaces.api.queue.EntryTokenInterceptor
import com.loopers.support.constant.ApiPaths
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val currentUserIdArgumentResolver: CurrentUserIdArgumentResolver,
    private val adminAuthArgumentResolver: AdminAuthArgumentResolver,
    private val entryTokenInterceptor: EntryTokenInterceptor,
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(currentUserIdArgumentResolver)
        resolvers.add(adminAuthArgumentResolver)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(entryTokenInterceptor)
            .addPathPatterns(ApiPaths.Orders.BASE)
    }
}
