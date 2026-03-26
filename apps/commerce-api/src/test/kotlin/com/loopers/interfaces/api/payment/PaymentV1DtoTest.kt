package com.loopers.interfaces.api.payment

import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PgPaymentStatus
import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PaymentV1DtoTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `callback_request의_필수값이_비어있으면_검증에_실패한다`() {
        val request = PaymentV1Dto.CallbackRequest(
            transactionKey = "",
            orderId = "",
            cardType = null,
            cardNo = "",
            amount = null,
            status = null,
            reason = null,
        )

        val violations = validator.validate(request)

        assertThat(violations.map { it.propertyPath.toString() })
            .containsExactlyInAnyOrder("transactionKey", "orderId", "cardType", "cardNo", "amount", "status")
    }

    @Test
    fun `callback_request가_유효하면_커맨드로_변환할_수_있다`() {
        val request = PaymentV1Dto.CallbackRequest(
            transactionKey = "20250816:TR:9577c5",
            orderId = "1",
            cardType = CardType.SAMSUNG,
            cardNo = "1234-5678-1234-5678",
            amount = 10000L,
            status = PgPaymentStatus.SUCCESS,
            reason = null,
        )

        val violations = validator.validate(request)
        val command = request.toCommand()

        assertThat(violations).isEmpty()
        assertThat(command.transactionKey).isEqualTo("20250816:TR:9577c5")
        assertThat(command.status).isEqualTo(PgPaymentStatus.SUCCESS)
        assertThat(command.reason).isNull()
    }
}
