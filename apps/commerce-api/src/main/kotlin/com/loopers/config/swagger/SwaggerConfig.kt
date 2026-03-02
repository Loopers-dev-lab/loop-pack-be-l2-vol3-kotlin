package com.loopers.config.swagger

import com.loopers.support.constant.HttpHeaders
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        val loginIdScheme = SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .`in`(SecurityScheme.In.HEADER)
            .name(HttpHeaders.LOGIN_ID)

        val loginPwScheme = SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .`in`(SecurityScheme.In.HEADER)
            .name(HttpHeaders.LOGIN_PW)

        val ldapScheme = SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .`in`(SecurityScheme.In.HEADER)
            .name(HttpHeaders.LDAP)

        return OpenAPI()
            .components(
                Components()
                    .addSecuritySchemes(HttpHeaders.LOGIN_ID, loginIdScheme)
                    .addSecuritySchemes(HttpHeaders.LOGIN_PW, loginPwScheme)
                    .addSecuritySchemes(HttpHeaders.LDAP, ldapScheme),
            )
            .addSecurityItem(
                SecurityRequirement()
                    .addList(HttpHeaders.LOGIN_ID)
                    .addList(HttpHeaders.LOGIN_PW)
                    .addList(HttpHeaders.LDAP),
            )
    }
}
