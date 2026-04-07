package com.loopers.config

import com.loopers.domain.brand.BrandDomainService
import com.loopers.domain.order.OrderDomainService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

@Configuration
@EnableAsync
class ApplicationConfig {

    @Bean
    fun brandDomainService(): BrandDomainService {
        return BrandDomainService()
    }

    @Bean
    fun orderDomainService(): OrderDomainService {
        return OrderDomainService()
    }
}
