package com.loopers.domain.payment

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PaymentTest {

    @Test
    fun `create로 생성한 Payment의 persistenceId는 null이어야 한다`() {
        val payment = createPayment()

        assertThat(payment.persistenceId).isNull()
    }

    @Test
    fun `create로 생성한 Payment의 상태는 PENDING이어야 한다`() {
        val payment = createPayment()

        assertThat(payment.status).isEqualTo(PaymentStatus.PENDING)
    }

    @Test
    fun `create로 생성한 Payment의 transactionKey는 null이어야 한다`() {
        val payment = createPayment()

        assertThat(payment.transactionKey).isNull()
    }

    @Test
    fun `create로 생성한 Payment의 failureReason은 null이어야 한다`() {
        val payment = createPayment()

        assertThat(payment.failureReason).isNull()
    }

    @Test
    fun `금액이 0 이하이면 create가 실패해야 한다`() {
        assertThatThrownBy {
            Payment.create(
                refOrderId = ORDER_ID,
                refUserId = USER_ID,
                cardType = CardType.SAMSUNG,
                cardNo = CARD_NO,
                amount = 0,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `카드 번호가 빈 문자열이면 create가 실패해야 한다`() {
        assertThatThrownBy {
            Payment.create(
                refOrderId = ORDER_ID,
                refUserId = USER_ID,
                cardType = CardType.SAMSUNG,
                cardNo = "",
                amount = AMOUNT,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `PENDING 상태에서 markRequested가 REQUESTED 상태를 반환해야 한다`() {
        val payment = createPayment()

        val requested = payment.markRequested(TRANSACTION_KEY)

        assertThat(requested.status).isEqualTo(PaymentStatus.REQUESTED)
        assertThat(requested.transactionKey).isEqualTo(TRANSACTION_KEY)
    }

    @Test
    fun `markRequested 호출시 새 Payment 인스턴스를 반환해야 한다`() {
        val payment = createPayment()

        val requested = payment.markRequested(TRANSACTION_KEY)

        assertThat(requested).isNotSameAs(payment)
    }

    @Test
    fun `REQUESTED 상태에서 markRequested가 실패해야 한다`() {
        val payment = createPayment().markRequested(TRANSACTION_KEY)

        assertThatThrownBy { payment.markRequested("another-key") }
            .isInstanceOf(PaymentException::class.java)
    }

    @Test
    fun `REQUESTED 상태에서 approve가 SUCCESS 상태를 반환해야 한다`() {
        val payment = createPayment().markRequested(TRANSACTION_KEY)

        val approved = payment.approve()

        assertThat(approved.status).isEqualTo(PaymentStatus.SUCCESS)
    }

    @Test
    fun `approve 호출시 새 Payment 인스턴스를 반환해야 한다`() {
        val payment = createPayment().markRequested(TRANSACTION_KEY)

        val approved = payment.approve()

        assertThat(approved).isNotSameAs(payment)
    }

    @Test
    fun `PENDING 상태에서 approve가 실패해야 한다`() {
        val payment = createPayment()

        assertThatThrownBy { payment.approve() }
            .isInstanceOf(PaymentException::class.java)
    }

    @Test
    fun `SUCCESS 상태에서 approve가 실패해야 한다`() {
        val payment = createPayment().markRequested(TRANSACTION_KEY).approve()

        assertThatThrownBy { payment.approve() }
            .isInstanceOf(PaymentException::class.java)
    }

    @Test
    fun `PENDING 상태에서 fail이 FAILED 상태를 반환해야 한다`() {
        val payment = createPayment()

        val failed = payment.fail(FAIL_REASON)

        assertThat(failed.status).isEqualTo(PaymentStatus.FAILED)
        assertThat(failed.failureReason).isEqualTo(FAIL_REASON)
    }

    @Test
    fun `REQUESTED 상태에서 fail이 FAILED 상태를 반환해야 한다`() {
        val payment = createPayment().markRequested(TRANSACTION_KEY)

        val failed = payment.fail(FAIL_REASON)

        assertThat(failed.status).isEqualTo(PaymentStatus.FAILED)
    }

    @Test
    fun `SUCCESS 상태에서 fail이 실패해야 한다`() {
        val payment = createPayment().markRequested(TRANSACTION_KEY).approve()

        assertThatThrownBy { payment.fail(FAIL_REASON) }
            .isInstanceOf(PaymentException::class.java)
    }

    @Test
    fun `FAILED 상태에서 fail이 실패해야 한다`() {
        val payment = createPayment().fail(FAIL_REASON)

        assertThatThrownBy { payment.fail(FAIL_REASON) }
            .isInstanceOf(PaymentException::class.java)
    }

    @Test
    fun `SUCCESS 상태는 terminal이어야 한다`() {
        val payment = createPayment().markRequested(TRANSACTION_KEY).approve()

        assertThat(payment.isTerminal()).isTrue()
    }

    @Test
    fun `FAILED 상태는 terminal이어야 한다`() {
        val payment = createPayment().fail(FAIL_REASON)

        assertThat(payment.isTerminal()).isTrue()
    }

    @Test
    fun `PENDING 상태는 terminal이 아니어야 한다`() {
        val payment = createPayment()

        assertThat(payment.isTerminal()).isFalse()
    }

    @Test
    fun `REQUESTED 상태는 terminal이 아니어야 한다`() {
        val payment = createPayment().markRequested(TRANSACTION_KEY)

        assertThat(payment.isTerminal()).isFalse()
    }

    @Test
    fun `본인의 결제에 대해 assertOwnedBy는 예외를 던지지 않아야 한다`() {
        val payment = createPayment()

        payment.assertOwnedBy(USER_ID)
    }

    @Test
    fun `타인의 결제에 대해 assertOwnedBy는 PaymentException을 던져야 한다`() {
        val payment = createPayment()

        assertThatThrownBy { payment.assertOwnedBy(OTHER_USER_ID) }
            .isInstanceOf(PaymentException::class.java)
    }

    @Test
    fun `isOwnedBy는 본인이면 true를 반환해야 한다`() {
        val payment = createPayment()

        assertThat(payment.isOwnedBy(USER_ID)).isTrue()
        assertThat(payment.isOwnedBy(OTHER_USER_ID)).isFalse()
    }

    @Test
    fun `reconstitute로 생성한 Payment는 persistenceId를 가져야 한다`() {
        val payment = Payment.reconstitute(
            persistenceId = 1L,
            refOrderId = ORDER_ID,
            refUserId = USER_ID,
            transactionKey = TRANSACTION_KEY,
            cardType = CardType.SAMSUNG,
            cardNo = CARD_NO,
            amount = AMOUNT,
            status = PaymentStatus.REQUESTED,
            failureReason = null,
        )

        assertThat(payment.persistenceId).isEqualTo(1L)
    }

    private fun createPayment(): Payment = Payment.create(
        refOrderId = ORDER_ID,
        refUserId = USER_ID,
        cardType = CardType.SAMSUNG,
        cardNo = CARD_NO,
        amount = AMOUNT,
    )

    companion object {
        private const val ORDER_ID = 1L
        private const val USER_ID = 1L
        private const val CARD_NO = "1234-5678-9012-3456"
        private const val AMOUNT = 50000L
        private const val TRANSACTION_KEY = "tx-abc-123"
        private const val FAIL_REASON = "잔액 부족"
        private const val OTHER_USER_ID = 999L
    }
}
