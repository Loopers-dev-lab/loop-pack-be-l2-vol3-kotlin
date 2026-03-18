package com.loopers.interfaces.api.payment

import com.loopers.application.payment.PaymentCriteria
import com.loopers.application.payment.PaymentInfo
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

class PaymentV1Dto {

    @Schema(description = "결제 요청")
    data class CreateRequest(
        @Schema(description = "주문 ID", example = "1")
        val orderId: Long,
        @Schema(description = "카드 종류", example = "SAMSUNG")
        val cardType: String,
        @Schema(description = "카드 번호", example = "1234-5678-9814-1451")
        val cardNo: String,
    ) {
        fun toCriteria() = PaymentCriteria(orderId = orderId, cardType = cardType, cardNo = cardNo)
    }

    @Schema(description = "결제 응답")
    data class PaymentResponse(
        @Schema(description = "결제 ID")
        val id: Long,
        @Schema(description = "주문 ID")
        val orderId: Long,
        @Schema(description = "결제 금액")
        val amount: BigDecimal,
        @Schema(description = "카드 종류")
        val cardType: String,
        @Schema(description = "마스킹된 카드 번호")
        val cardNo: String,
        @Schema(description = "PG 거래 키", example = "20260316:TR:abc123")
        val transactionKey: String?,
        @Schema(description = "결제 상태", example = "INITIATED")
        val status: String,
        @Schema(description = "실패 사유")
        val failReason: String?,
    ) {
        companion object {
            fun from(info: PaymentInfo): PaymentResponse {
                return PaymentResponse(
                    id = info.id,
                    orderId = info.orderId,
                    amount = info.amount,
                    cardType = info.cardType,
                    cardNo = info.cardNo,
                    transactionKey = info.transactionKey,
                    status = info.status.name,
                    failReason = info.failReason,
                )
            }
        }
    }
}
