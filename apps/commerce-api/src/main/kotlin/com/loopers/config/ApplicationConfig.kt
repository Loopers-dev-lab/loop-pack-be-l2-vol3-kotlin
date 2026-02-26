package com.loopers.config

import com.loopers.domain.brand.BrandDomainService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationConfig {

    @Bean
    fun brandDomainService(): BrandDomainService {
        return BrandDomainService()
    }
}
