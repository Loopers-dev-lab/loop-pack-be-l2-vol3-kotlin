package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PaymentReasonCode
import com.loopers.domain.payment.PgPaymentPort
import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.payment.PgPaymentResponse
import com.loopers.domain.payment.PgPaymentStatusResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

@Component
class PgPaymentAdapter(
    private val pgRestClient: RestClient,
) : PgPaymentPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun requestPayment(request: PgPaymentRequest): PgPaymentResponse {
        val pgRequest = PgApiRequest.Payment(
            orderId = request.orderId.toString(),
            cardType = request.cardType,
            cardNo = request.cardNo,
            amount = request.amount.toLong(),
            callbackUrl = request.callbackUrl,
        )

        return try {
            val response = pgRestClient.post()
                .uri("/api/v1/payments")
                .header("X-USER-ID", request.userId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(pgRequest)
                .retrieve()
                .body(PgApiResponse.Payment::class.java)!!

            PgPaymentResponse.Accepted(response.data!!.transactionKey)
        } catch (e: RestClientResponseException) {
            log.warn("PG 결제 요청 실패: status={}, body={}", e.statusCode, e.responseBodyAsString)
            PgPaymentResponse.ImmediateFailure(PaymentReasonCode.PG_INTERNAL_ERROR)
        } catch (e: ResourceAccessException) {
            log.warn("PG 연결 실패: {}", e.message)
            PgPaymentResponse.ImmediateFailure(PaymentReasonCode.PG_INTERNAL_ERROR)
        }
    }

    override fun queryPaymentStatus(transactionKey: String, userId: Long): PgPaymentStatusResponse {
        val response = pgRestClient.get()
            .uri("/api/v1/payments/{transactionKey}", transactionKey)
            .header("X-USER-ID", userId.toString())
            .retrieve()
            .body(PgApiResponse.PaymentDetail::class.java)!!

        val data = response.data!!
        return PgPaymentStatusResponse(
            transactionKey = data.transactionKey,
            status = data.status,
            reason = data.reason,
        )
    }

    override fun isAvailable(): Boolean = true

    fun mapReasonCode(reason: String?): PaymentReasonCode {
        return when {
            reason == null -> PaymentReasonCode.PG_INTERNAL_ERROR
            reason.contains("한도초과") || reason.contains("한도") -> PaymentReasonCode.LIMIT_EXCEEDED
            reason.contains("잘못된 카드") || reason.contains("카드") -> PaymentReasonCode.INVALID_CARD
            else -> PaymentReasonCode.PG_INTERNAL_ERROR
        }
    }
}

object PgApiRequest {
    data class Payment(
        val orderId: String,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
        val callbackUrl: String,
    )
}

object PgApiResponse {
    data class Payment(
        val meta: Meta,
        val data: PaymentData?,
    )

    data class PaymentDetail(
        val meta: Meta,
        val data: PaymentDetailData?,
    )

    data class Meta(
        val result: String,
        val errorCode: String?,
        val message: String?,
    )

    data class PaymentData(
        val transactionKey: String,
        val status: String,
        val reason: String?,
    )

    data class PaymentDetailData(
        val transactionKey: String,
        val orderId: String,
        val cardType: String,
        val cardNo: String,
        val amount: Long,
        val status: String,
        val reason: String?,
    )
}
