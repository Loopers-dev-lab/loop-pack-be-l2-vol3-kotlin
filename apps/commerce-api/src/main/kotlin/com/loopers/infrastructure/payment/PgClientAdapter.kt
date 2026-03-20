package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PgClient
import com.loopers.domain.payment.PgPaymentDetailResponse
import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.payment.PgPaymentResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class PgClientAdapter(
    private val pgFeignClient: PgFeignClient,
) : PgClient {
    override fun requestPayment(request: PgPaymentRequest): PgPaymentResponse {
        try {
            val response = pgFeignClient.requestPayment(
                userId = request.userId.toString(),
                request = PgPaymentRequestDto(
                    orderId = request.orderId,
                    cardType = request.cardType,
                    cardNo = request.cardNo,
                    amount = request.amount,
                    callbackUrl = request.callbackUrl,
                ),
            )
            val data = response.data
                ?: throw CoreException(ErrorType.INTERNAL_ERROR, "PG 응답 데이터가 없습니다.")
            return PgPaymentResponse(
                transactionKey = data.transactionKey,
                status = data.status,
                reason = data.reason,
            )
        } catch (e: CoreException) {
            throw e
        } catch (e: Exception) {
            throw CoreException(ErrorType.INTERNAL_ERROR, "PG 연동 오류: ${e.message}")
        }
    }

    override fun getPaymentDetail(transactionKey: String, userId: Long): PgPaymentDetailResponse {
        try {
            val response = pgFeignClient.getPayment(
                userId = userId.toString(),
                transactionKey = transactionKey,
            )
            val data = response.data
                ?: throw CoreException(ErrorType.INTERNAL_ERROR, "PG 응답 데이터가 없습니다.")
            return PgPaymentDetailResponse(
                transactionKey = data.transactionKey,
                orderId = data.orderId,
                cardType = data.cardType,
                cardNo = data.cardNo,
                amount = data.amount,
                status = data.status,
                reason = data.reason,
            )
        } catch (e: CoreException) {
            throw e
        } catch (e: Exception) {
            throw CoreException(ErrorType.INTERNAL_ERROR, "PG 연동 오류: ${e.message}")
        }
    }
}
