package com.loopers.application.payment

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PaymentCallbackFacadeUnitTest {

    private val mockPaymentService = mockk<PaymentService>()
    private val mockOrderService = mockk<OrderService>()

    private val callbackFacade = PaymentCallbackFacade(mockPaymentService, mockOrderService)

    // ─── handleCallback SUCCESS ───

    @Test
    fun `handleCallback() should confirm paid and update order on SUCCESS`() {
        val payment = createPayment(transactionKey = "txn-123", status = PaymentStatus.PENDING)
        val order = createOrder()

        every { mockPaymentService.getByTransactionKey("txn-123") } returns payment
        every { mockPaymentService.updatePaymentStatus(any()) } just Runs
        every { mockOrderService.updateStatus(eq(1L), any()) } answers {
            val action = secondArg<(Order) -> Unit>()
            action(order)
            order
        }

        val result = callbackFacade.handleCallback(
            PaymentCallbackCommand(transactionKey = "txn-123", status = "SUCCESS", reason = null),
        )

        assertThat(result.status).isEqualTo(PaymentStatus.PAID)
        assertThat(order.status).isEqualTo(OrderStatus.PAID)
        verify { mockPaymentService.updatePaymentStatus(any()) }
        verify { mockOrderService.updateStatus(eq(1L), any()) }
    }

    // ─── handleCallback FAILED ───

    @Test
    fun `handleCallback() should confirm failed and NOT update order on FAILED`() {
        val payment = createPayment(transactionKey = "txn-456", status = PaymentStatus.PENDING)

        every { mockPaymentService.getByTransactionKey("txn-456") } returns payment
        every { mockPaymentService.updatePaymentStatus(any()) } just Runs

        val result = callbackFacade.handleCallback(
            PaymentCallbackCommand(transactionKey = "txn-456", status = "FAILED", reason = "insufficient funds"),
        )

        assertThat(result.status).isEqualTo(PaymentStatus.FAILED)
        assertThat(result.reason).isEqualTo("insufficient funds")
        verify { mockPaymentService.updatePaymentStatus(any()) }
        verify(exactly = 0) { mockOrderService.updateStatus(any(), any()) }
    }

    // ─── handleCallback idempotency ───

    @Test
    fun `handleCallback() should skip if payment is already PAID (idempotency)`() {
        val payment = createPayment(transactionKey = "txn-789", status = PaymentStatus.PAID)

        every { mockPaymentService.getByTransactionKey("txn-789") } returns payment

        val result = callbackFacade.handleCallback(
            PaymentCallbackCommand(transactionKey = "txn-789", status = "SUCCESS", reason = null),
        )

        assertThat(result.status).isEqualTo(PaymentStatus.PAID)
        verify(exactly = 0) { mockPaymentService.updatePaymentStatus(any()) }
        verify(exactly = 0) { mockOrderService.updateStatus(any(), any()) }
    }

    @Test
    fun `handleCallback() should skip if payment is already FAILED (idempotency)`() {
        val payment = createPayment(transactionKey = "txn-000", status = PaymentStatus.FAILED)

        every { mockPaymentService.getByTransactionKey("txn-000") } returns payment

        val result = callbackFacade.handleCallback(
            PaymentCallbackCommand(transactionKey = "txn-000", status = "FAILED", reason = "rejected"),
        )

        assertThat(result.status).isEqualTo(PaymentStatus.FAILED)
        verify(exactly = 0) { mockPaymentService.updatePaymentStatus(any()) }
    }

    // ─── Helpers ───

    private fun createPayment(
        orderId: Long = 1L,
        transactionKey: String? = null,
        status: PaymentStatus = PaymentStatus.PENDING,
    ): Payment {
        val payment = Payment(
            orderId = orderId,
            amount = 10000,
            cardType = CardType.SAMSUNG,
            cardNo = "1234-5678-9012-3456",
            transactionKey = transactionKey,
        )
        if (status == PaymentStatus.PAID) {
            payment.confirmPaid(java.time.ZonedDateTime.now())
        } else if (status == PaymentStatus.FAILED) {
            payment.confirmFailed("previous failure", java.time.ZonedDateTime.now())
        }
        return payment
    }

    private fun createOrder(): Order = Order.create(
        userId = 1L,
        items = listOf(
            OrderItem(
                orderId = 0L,
                productId = 1L,
                productName = "Test Product",
                brandId = 1L,
                brandName = "Test Brand",
                price = 10000,
                quantity = 1,
            ),
        ),
    )
}
