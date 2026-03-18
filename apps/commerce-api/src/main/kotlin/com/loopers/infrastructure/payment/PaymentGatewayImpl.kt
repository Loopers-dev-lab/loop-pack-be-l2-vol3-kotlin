package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentGatewayResponse
import com.loopers.domain.payment.PaymentGatewayTransactionDetail
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.stereotype.Component

@Component
class PaymentGatewayImpl(
    private val pgClient: PgClient,
) : PaymentGateway {

    @CircuitBreaker(name = "pgCircuitBreaker")
    @Retry(name = "pgRetry")
    override fun requestPayment(
        userId: String,
        orderId: String,
        cardType: String,
        cardNo: String,
        amount: Long,
        callbackUrl: String,
    ): PaymentGatewayResponse {
        val response = pgClient.requestPayment(
            userId = userId,
            request = PgPaymentRequest(
                orderId = orderId,
                cardType = cardType,
                cardNo = cardNo,
                amount = amount,
                callbackUrl = callbackUrl,
            ),
        )
        return PaymentGatewayResponse(
            transactionKey = response.transactionKey,
            status = response.status,
            reason = response.reason,
        )
    }

    @CircuitBreaker(name = "pgCircuitBreaker")
    @Retry(name = "pgRetry")
    override fun getTransactionStatus(userId: String, transactionKey: String): PaymentGatewayTransactionDetail {
        val response = pgClient.getTransaction(userId, transactionKey)
        return PaymentGatewayTransactionDetail(
            transactionKey = response.transactionKey,
            orderId = response.orderId,
            status = response.status,
            reason = response.reason,
        )
    }
}
