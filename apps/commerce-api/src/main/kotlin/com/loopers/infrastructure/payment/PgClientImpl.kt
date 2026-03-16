package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PgClient
import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.payment.PgPaymentResult
import com.loopers.domain.payment.PgResultStatus
import com.loopers.domain.payment.PgTransactionDetail
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PgClientImpl(
    private val pgFeignClient: PgFeignClient,
    private val pgStatusQueryClient: PgStatusQueryClient,
) : PgClient {

    private val log = LoggerFactory.getLogger(PgClientImpl::class.java)

    @CircuitBreaker(name = "pgPayment", fallbackMethod = "requestPaymentFallback")
    @Retry(name = "pgRetry")
    override fun requestPayment(request: PgPaymentRequest): PgPaymentResult {
        val response = pgFeignClient.requestPayment(
            userId = "system",
            request = PgFeignPaymentRequest(
                orderId = request.orderId.toString(),
                cardType = request.cardType.name,
                cardNo = request.cardNo,
                amount = request.amount,
                callbackUrl = request.callbackUrl,
            ),
        )
        val data = requireNotNull(response.data) { "PG 응답 data가 null입니다." }

        return PgPaymentResult(
            transactionKey = data.transactionKey,
            status = PgResultStatus.SUCCESS,
            reason = data.reason,
        )
    }

    private fun requestPaymentFallback(request: PgPaymentRequest, t: Throwable): PgPaymentResult {
        if (t is CallNotPermittedException) {
            return PgPaymentResult(transactionKey = null, status = PgResultStatus.TIMEOUT)
        }
        return try {
            pgStatusQueryClient.getTransactionByOrderId(request.orderId)
                ?.let { PgPaymentResult(it.transactionKey, it.status, it.reason) }
                ?: PgPaymentResult(transactionKey = null, status = PgResultStatus.TIMEOUT)
        } catch (e: Exception) {
            log.warn("Fallback 상태 조회 실패. orderId={}: {}", request.orderId, e.message)
            PgPaymentResult(transactionKey = null, status = PgResultStatus.TIMEOUT)
        }
    }

    override fun getTransactionByOrderId(orderId: Long): PgTransactionDetail? {
        return pgStatusQueryClient.getTransactionByOrderId(orderId)
    }
}
