package com.loopers.domain.payment

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PaymentTest {

    @Test
    fun `PG가_요청을_수락하면_PENDING_상태와_transactionKey를_저장한다`() {
        val payment = createPayment()

        payment.markAccepted(transactionKey = "20250816:TR:9577c5", reason = null)

        assertThat(payment.status).isEqualTo(PaymentStatus.PENDING)
        assertThat(payment.pgTransactionKey).isEqualTo("20250816:TR:9577c5")
    }

    @Test
    fun `타임아웃이_발생하면_UNKNOWN_상태로_남긴다`() {
        val payment = createPayment()

        payment.markUnknown("PG 요청 타임아웃")

        assertThat(payment.status).isEqualTo(PaymentStatus.UNKNOWN)
        assertThat(payment.reason).isEqualTo("PG 요청 타임아웃")
    }

    @Test
    fun `최종_성공_상태가_된_후에는_이전_상태로_회귀하지_않는다`() {
        val payment = createPayment()
        payment.applyPgResult(
            transactionKey = "20250816:TR:9577c5",
            status = PgPaymentStatus.SUCCESS,
            reason = "정상 승인",
        )

        payment.applyPgResult(
            transactionKey = "20250816:TR:9577c5",
            status = PgPaymentStatus.PENDING,
            reason = "처리 중",
        )

        assertThat(payment.status).isEqualTo(PaymentStatus.SUCCESS)
        assertThat(payment.reason).isEqualTo("정상 승인")
    }

    private fun createPayment() = Payment(
        orderId = 1L,
        memberId = 1L,
        cardType = CardType.SAMSUNG,
        cardNo = "1234-5678-1234-5678",
        amount = 5000L,
    )
}
