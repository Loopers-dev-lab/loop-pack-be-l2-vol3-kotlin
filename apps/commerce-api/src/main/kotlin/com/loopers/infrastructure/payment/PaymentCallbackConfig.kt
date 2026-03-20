package com.loopers.infrastructure.payment

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PaymentCallbackConfig {
    @Bean
    fun pgCallbackSignatureVerifier(
        @Value("\${payment.callback.secret}") secret: String,
    ): PgCallbackSignatureVerifier {
        return PgCallbackSignatureVerifier(secret)
    }
}
