package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentGatewayResponse
import com.loopers.domain.payment.PaymentGatewayTransactionDetail
import org.springframework.stereotype.Component

@Component
class PaymentGatewayImpl(
    private val pgClient: PgClient,
) : PaymentGateway {

    override fun requestPayment(
        userId: String,
        orderId: String,
        cardType: String,
        cardNo: String,
        amount: Long,
        callbackUrl: String,
    ): PaymentGatewayResponse {
        val data = pgClient.requestPayment(
            userId = userId,
            request = PgPaymentRequest(
                orderId = orderId,
                cardType = cardType,
                cardNo = cardNo,
                amount = amount,
                callbackUrl = callbackUrl,
            ),
        ).requireData()
        return PaymentGatewayResponse(
            transactionKey = data.transactionKey,
            status = data.status,
            reason = data.reason,
        )
    }

    override fun getTransactionStatus(userId: String, transactionKey: String): PaymentGatewayTransactionDetail {
        val data = pgClient.getTransaction(userId, transactionKey).requireData()
        return PaymentGatewayTransactionDetail(
            transactionKey = data.transactionKey,
            orderId = data.orderId,
            status = data.status,
            reason = data.reason,
        )
    }

    private fun <T> PgApiResponse<T>.requireData(): T =
        data ?: throw IllegalStateException("PG 응답 데이터가 없습니다.")
}
