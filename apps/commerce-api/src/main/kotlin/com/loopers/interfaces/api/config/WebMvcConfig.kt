package com.loopers.interfaces.api.config

import com.loopers.interfaces.api.auth.AuthenticatedUserArgumentResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebMvcConfig(
    private val authenticatedUserArgumentResolver: AuthenticatedUserArgumentResolver,
) : WebMvcConfigurer {

    /**
     * Appends the configured AuthenticatedUserArgumentResolver to the MVC argument resolver chain.
     *
     * @param resolvers The mutable list of HandlerMethodArgumentResolver instances used by Spring MVC; the configured resolver is added to this list.
     */
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(authenticatedUserArgumentResolver)
    }
}