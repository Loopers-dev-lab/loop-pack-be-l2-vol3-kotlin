package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PgClient
import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.payment.PgPaymentResult
import com.loopers.domain.payment.PgResultStatus
import com.loopers.domain.payment.PgTransactionDetail
import com.loopers.interfaces.support.config.PgProperties
import feign.FeignException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PgClientImpl(
    private val pgFeignClient: PgFeignClient,
    private val pgStatusQueryClient: PgStatusQueryClient,
    private val pgProperties: PgProperties,
) : PgClient {

    private val log = LoggerFactory.getLogger(PgClientImpl::class.java)

    @CircuitBreaker(name = "pg-payment-request", fallbackMethod = "requestPaymentFallback")
    @Retry(name = "pg-retry")
    override fun requestPayment(request: PgPaymentRequest): PgPaymentResult {
        val response = pgFeignClient.requestPayment(
            userId = "system",
            request = PgFeignPaymentRequest(
                orderId = request.orderId.toString(),
                cardType = request.cardType.name,
                cardNo = request.cardNo,
                amount = request.amount,
                callbackUrl = pgProperties.callbackUrl,
                idempotencyKey = "payment-${request.orderId}",
            ),
        )
        if (response.meta.result != "SUCCESS") {
            return PgPaymentResult(
                transactionKey = null,
                status = PgResultStatus.FAILED,
                reason = response.meta.message ?: "PG 요청 실패: ${response.meta.result}",
            )
        }

        val data = requireNotNull(response.data) { "PG 응답 data가 null입니다." }

        if (data.transactionKey.isBlank()) {
            log.error("PG 성공 응답이지만 transactionKey가 비어있습니다. orderId={}", request.orderId)
            return PgPaymentResult(
                transactionKey = null,
                status = PgResultStatus.FAILED,
                reason = "transactionKey 누락",
            )
        }

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
        if (t is FeignException && t.status() in 400..499) {
            log.warn("PG 클라이언트 오류 (4xx). orderId={}, status={}", request.orderId, t.status())
            return PgPaymentResult(transactionKey = null, status = PgResultStatus.FAILED, reason = "PG 요청 오류: ${t.status()}")
        }
        return try {
            pgStatusQueryClient.getTransactionByOrderId(request.orderId)
                ?.let { PgPaymentResult(it.transactionKey, it.status, it.reason) }
                ?: PgPaymentResult(transactionKey = null, status = PgResultStatus.TIMEOUT)
        } catch (e: Exception) {
            log.error("Fallback 상태 조회 실패. orderId={}, exceptionType={}", request.orderId, e.javaClass.simpleName, e)
            PgPaymentResult(transactionKey = null, status = PgResultStatus.TIMEOUT)
        }
    }

    override fun getTransactionByOrderId(orderId: Long): PgTransactionDetail? {
        return pgStatusQueryClient.getTransactionByOrderId(orderId)
    }
}
