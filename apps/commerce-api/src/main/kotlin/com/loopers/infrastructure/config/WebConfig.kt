package com.loopers.infrastructure.config

import com.loopers.domain.user.UserRepository
import com.loopers.infrastructure.interceptor.LdapAuthInterceptor
import com.loopers.infrastructure.interceptor.UserAuthInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val userRepository: UserRepository,
) : WebMvcConfigurer {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userAuthInterceptor(userRepository: UserRepository, passwordEncoder: PasswordEncoder): UserAuthInterceptor {
        return UserAuthInterceptor(userRepository, passwordEncoder)
    }

    @Bean
    fun ldapAuthInterceptor(): LdapAuthInterceptor {
        return LdapAuthInterceptor()
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        // 일반 API 인증: 사용자 정보, 주문 관련, 상품 좋아요
        registry.addInterceptor(userAuthInterceptor(userRepository, passwordEncoder()))
            .addPathPatterns(
                "/api/*/users/**",
                "/api/*/orders/**",
                "/api/*/products/**/likes",
            )

        // Admin API 인증: LDAP 역할 검증
        registry.addInterceptor(ldapAuthInterceptor())
            .addPathPatterns("/api-admin/**")
    }
}
