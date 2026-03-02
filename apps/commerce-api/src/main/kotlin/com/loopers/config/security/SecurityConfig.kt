package com.loopers.config.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.user.UserService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val userService: UserService,
    private val objectMapper: ObjectMapper,
) {

    @Bean
    @Order(1)
    fun adminFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/api-admin/**")
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .addFilterBefore(AdminAuthenticationFilter(), UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling { it.authenticationEntryPoint(SecurityAuthenticationEntryPoint(objectMapper)) }
        return http.build()
    }

    @Bean
    @Order(2)
    fun userFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/api/**")
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.POST, "/api/v1/users").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/v1/brands/**").permitAll()
                it.requestMatchers("/api/v1/examples/**").permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(
                UserAuthenticationFilter(userService, objectMapper),
                UsernamePasswordAuthenticationFilter::class.java,
            )
            .exceptionHandling { it.authenticationEntryPoint(SecurityAuthenticationEntryPoint(objectMapper)) }
        return http.build()
    }

    @Bean
    @Order(3)
    fun defaultFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .securityMatcher("/**")
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
        return http.build()
    }
}
