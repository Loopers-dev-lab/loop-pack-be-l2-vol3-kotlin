package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentGatewayResponse
import com.loopers.domain.payment.PaymentGatewayTransactionDetail
import io.github.resilience4j.bulkhead.annotation.Bulkhead
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PaymentGatewayImpl(
    private val pgClient: PgClient,
) : PaymentGateway {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bulkhead(name = "pg-simulator")
    override fun requestPayment(
        userId: String,
        orderId: String,
        cardType: String,
        cardNo: String,
        amount: Long,
        callbackUrl: String,
    ): PaymentGatewayResponse? {
        return try {
            val data = pgClient.requestPayment(
                userId = userId,
                request = PgPaymentRequest(
                    orderId = orderId,
                    cardType = cardType,
                    cardNo = cardNo,
                    amount = amount,
                    callbackUrl = callbackUrl,
                ),
            ).data ?: return null
            PaymentGatewayResponse(
                transactionKey = data.transactionKey,
                status = data.status,
                reason = data.reason,
            )
        } catch (e: Exception) {
            log.warn("PG 결제 요청 실패 (orderId={}): {}", orderId, e.message)
            null
        }
    }

    @Bulkhead(name = "pg-simulator")
    @Retry(name = "pg-query")
    override fun getTransactionsByOrderId(userId: String, orderId: String): List<PaymentGatewayResponse> {
        return try {
            val data = pgClient.getTransactionsByOrderId(userId, orderId).data ?: return emptyList()
            data.transactions.map {
                PaymentGatewayResponse(
                    transactionKey = it.transactionKey,
                    status = it.status,
                    reason = it.reason,
                )
            }
        } catch (e: Exception) {
            log.warn("PG 주문 조회 실패 (orderId={}): {}", orderId, e.message)
            emptyList()
        }
    }

    @Bulkhead(name = "pg-simulator")
    @Retry(name = "pg-query")
    override fun getTransactionStatus(userId: String, transactionKey: String): PaymentGatewayTransactionDetail? {
        return try {
            val data = pgClient.getTransaction(userId, transactionKey).data ?: return null
            PaymentGatewayTransactionDetail(
                transactionKey = data.transactionKey,
                orderId = data.orderId,
                status = data.status,
                reason = data.reason,
            )
        } catch (e: Exception) {
            log.warn("PG 상태 조회 실패 (transactionKey={}): {}", transactionKey, e.message)
            null
        }
    }
}
