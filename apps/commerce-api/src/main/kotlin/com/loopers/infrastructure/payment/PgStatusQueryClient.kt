package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PgResultStatus
import com.loopers.domain.payment.PgTransactionDetail
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PgStatusQueryClient(
    private val pgFeignClient: PgFeignClient,
) {
    private val log = LoggerFactory.getLogger(PgStatusQueryClient::class.java)

    @CircuitBreaker(name = "pg-status-query", fallbackMethod = "getTransactionByOrderIdFallback")
    fun getTransactionByOrderId(orderId: Long): PgTransactionDetail? {
        val response = pgFeignClient.getTransactionsByOrderId(
            userId = "system",
            orderId = orderId.toString(),
        )
        val data = response.data ?: return null
        val transaction = data.transactions.lastOrNull() ?: return null

        val status = when (transaction.status) {
            "PENDING" -> return null
            "SUCCESS" -> PgResultStatus.SUCCESS
            "FAILED" -> PgResultStatus.FAILED
            else -> {
                log.warn("미정의 PG 상태값: status={}, orderId={}", transaction.status, orderId)
                return null
            }
        }

        return PgTransactionDetail(
            transactionKey = transaction.transactionKey,
            orderId = orderId,
            status = status,
            reason = transaction.reason,
        )
    }

    private fun getTransactionByOrderIdFallback(orderId: Long, t: Throwable): PgTransactionDetail? {
        if (t is CallNotPermittedException) {
            log.warn("PG 상태 조회 서킷브레이커 OPEN. orderId={}", orderId)
            return null
        }
        throw t
    }
}
