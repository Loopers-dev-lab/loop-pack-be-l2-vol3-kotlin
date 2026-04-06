package com.loopers.interfaces.support.config

import com.loopers.interfaces.support.HEADER_ENTRY_TOKEN
import com.loopers.interfaces.support.HEADER_LOGIN_ID
import com.loopers.interfaces.support.HEADER_LOGIN_PW
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
        return OpenAPI()
            .components(
                Components()
                    .addSecuritySchemes(
                        HEADER_LOGIN_ID,
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.HEADER)
                            .name(HEADER_LOGIN_ID)
                            .description("로그인 ID"),
                    )
                    .addSecuritySchemes(
                        HEADER_LOGIN_PW,
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.HEADER)
                            .name(HEADER_LOGIN_PW)
                            .description("비밀번호"),
                    )
                    .addSecuritySchemes(
                        HEADER_ENTRY_TOKEN,
                        SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .`in`(SecurityScheme.In.HEADER)
                            .name(HEADER_ENTRY_TOKEN)
                            .description("대기열 입장 토큰"),
                    ),
            )
            .security(
                listOf(
                    SecurityRequirement()
                        .addList(HEADER_LOGIN_ID)
                        .addList(HEADER_LOGIN_PW),
                ),
            )
    }
}
