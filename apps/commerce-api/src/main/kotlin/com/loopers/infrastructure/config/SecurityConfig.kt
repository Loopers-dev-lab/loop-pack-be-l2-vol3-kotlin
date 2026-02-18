package com.loopers.infrastructure.config

import com.loopers.infrastructure.filter.AuthenticationFilter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val authenticationFilter: AuthenticationFilter,
) {

    object ApiPaths {
        const val SIGN_UP = "/api/*/users"
        const val EXAMPLES = "/api/*/examples/**"
        const val BRANDS = "/api/*/brands/**"
        const val ADMIN_BRANDS = "/api-admin/*/brands/**"
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .authorizeHttpRequests {
                it
                    .requestMatchers(HttpMethod.POST, ApiPaths.SIGN_UP).permitAll()
                    .requestMatchers(ApiPaths.EXAMPLES).permitAll()
                    .requestMatchers(ApiPaths.BRANDS).permitAll()
                    .requestMatchers(ApiPaths.ADMIN_BRANDS).permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint())
            }
            .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun authenticationEntryPoint(): AuthenticationEntryPoint {
        return AuthenticationEntryPoint { _, response, _ ->
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "인증이 필요합니다")
        }
    }
}
