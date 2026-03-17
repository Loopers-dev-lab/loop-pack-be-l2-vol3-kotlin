package com.loopers.infrastructure.payment.pg

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.slf4j.LoggerFactory
import java.math.BigDecimal

@Configuration
class PaymentGatewayConfig {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Mock PG 구현 (개발/테스트용)
     * 실제 PG와 통합할 때는 이를 대체합니다.
     */
    @Bean
    fun paymentGateway(): PgPaymentGateway {
        log.info("Using Mock Payment Gateway")
        return MockPaymentGateway()
    }

    /**
     * Mock Payment Gateway 구현
     */
    class MockPaymentGateway : PgPaymentGateway {
        private val mockLog = LoggerFactory.getLogger(javaClass)

        override fun requestPayment(
            userId: Long,
            transactionId: String,
            orderId: Long,
            amount: BigDecimal,
            cardType: String,
            cardNo: String,
            callbackUrl: String,
        ): PgPaymentGateway.PaymentRequestResult {
            // Mock 구현: 즉시 성공 반환 (개발/테스트용)
            mockLog.info(
                "Mock payment request: userId={}, transactionId={}, orderId={}, amount={}, callback={}",
                userId,
                transactionId,
                orderId,
                amount,
                callbackUrl,
            )
            return PgPaymentGateway.PaymentRequestResult(
                requestId = "MOCK_REQ_${System.currentTimeMillis()}",
                transactionId = transactionId,
                status = "COMPLETED",
                signature = "mock_signature_$transactionId",
            )
        }

        override fun verifySignature(transactionId: String, amount: BigDecimal, signature: String): Boolean {
            // Mock 구현: 항상 true 반환 (개발/테스트용)
            // 실제 PG에서는 HMAC-SHA256 등으로 서명을 검증합니다
            return true
        }
    }
}
