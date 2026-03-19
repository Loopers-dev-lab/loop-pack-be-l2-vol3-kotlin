package com.loopers.infrastructure.payment

import org.slf4j.LoggerFactory
import org.springframework.cloud.openfeign.FallbackFactory
import org.springframework.stereotype.Component

/**
 * PG 클라이언트 Fallback Factory
 *
 * CircuitBreaker OPEN 또는 PG 호출 실패 시 Fallback 동작을 정의한다.
 * Fallback이 호출되면 PgUnavailableException을 던져 상위 서비스에서 처리하도록 한다.
 */
@Component
class PgClientFallbackFactory : FallbackFactory<PgClient> {
    private val log = LoggerFactory.getLogger(PgClientFallbackFactory::class.java)

    override fun create(cause: Throwable): PgClient {
        return object : PgClient {
            override fun requestPayment(
                userId: String,
                request: PgPaymentRequest,
            ): PgApiResponse<PgTransactionResponse> {
                log.warn("PG 결제 요청 실패 - Fallback 동작. orderId={}, cause={}", request.orderId, cause.message)
                throw PgUnavailableException("PG 시스템이 불안정합니다.", cause)
            }

            override fun getTransaction(
                userId: String,
                transactionKey: String,
            ): PgApiResponse<PgTransactionDetailResponse> {
                log.warn("PG 거래 조회 실패 - Fallback 동작. transactionKey={}, cause={}", transactionKey, cause.message)
                throw PgUnavailableException("PG 시스템이 불안정합니다.", cause)
            }

            override fun getTransactionsByOrderId(
                userId: String,
                orderId: String,
            ): PgApiResponse<PgOrderResponse> {
                log.warn("PG 주문 조회 실패 - Fallback 동작. orderId={}, cause={}", orderId, cause.message)
                throw PgUnavailableException("PG 시스템이 불안정합니다.", cause)
            }
        }
    }
}

/**
 * PG 시스템 불가용 예외
 */
class PgUnavailableException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
