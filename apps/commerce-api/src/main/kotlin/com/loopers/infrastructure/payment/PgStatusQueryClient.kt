package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PgResultStatus
import com.loopers.domain.payment.PgTransactionDetail
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.springframework.stereotype.Component

@Component
class PgStatusQueryClient(
    private val pgFeignClient: PgFeignClient,
) {
    @CircuitBreaker(name = "pgStatusQuery")
    fun getTransactionByOrderId(orderId: Long): PgTransactionDetail? {
        val response = pgFeignClient.getTransactionsByOrderId(
            userId = "system",
            orderId = orderId.toString(),
        )
        val data = response.data ?: return null
        val transaction = data.transactions.firstOrNull() ?: return null

        val status = when (transaction.status) {
            "PENDING" -> return null
            "SUCCESS" -> PgResultStatus.SUCCESS
            "FAILED" -> PgResultStatus.FAILED
            else -> return null
        }

        return PgTransactionDetail(
            transactionKey = transaction.transactionKey,
            orderId = orderId,
            status = status,
            reason = transaction.reason,
        )
    }
}
