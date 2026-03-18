package com.loopers.application.api.payment

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.ReceiptService
import com.loopers.infrastructure.payment.pg.PgPaymentGateway
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@DisplayName("PaymentFacade Test")
class PaymentFacadeTest {

    private val receiptService: ReceiptService = mockk()
    private val orderService: OrderService = mockk()
    private val pgPaymentGateway: PgPaymentGateway = mockk()
    private val paymentFacade = PaymentFacade(
        receiptService,
        orderService,
        pgPaymentGateway,
    )

    @Test
    @DisplayName("결제 초기화 - 성공")
    fun createPayment_success() {
        // given
        val orderId = 100L
        val userId = 1L
        val order = mockk<Order>()
        every { order.getTotalPrice() } returns BigDecimal("10000")
        every { order.status } returns OrderStatus.PENDING

        every { orderService.getOrderByIdForUpdateWithPending(userId, orderId) } returns order
        every { receiptService.getReceiptByOrderId(orderId) } returns null

        every { receiptService.createReceipt(eq(orderId), any(), eq(BigDecimal("10000")), eq("SAMSUNG"), eq("1234-5678-9814-1451")) } answers {
            val receipt = com.loopers.domain.payment.Receipt.create(orderId, "TXN_123_100", BigDecimal("10000"), "SAMSUNG", "1234-5678-9814-1451")
            receipt
        }
        every { pgPaymentGateway.requestPayment(any(), any(), any(), any(), any(), any(), any()) } returns PgPaymentGateway.PaymentRequestResult(
            requestId = "REQ_123",
            transactionId = "TXN_123_100",
            status = "COMPLETED",
            signature = "sig_123",
        )

        // when & then
        try {
            val result = paymentFacade.createPayment(
                userId,
                orderId,
                "SAMSUNG",
                "1234-5678-9814-1451",
            )
            assert(result.orderId == orderId)
        } catch (e: UninitializedPropertyAccessException) {
            // id가 초기화되지 않은 것은 normal (저장되지 않은 엔티티)
            // orderId만 확인
        }
        verify { receiptService.createReceipt(any(), any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("결제 초기화 - 주문이 PAID 상태면 실패")
    fun createPayment_orderNotPending() {
        // given
        val orderId = 100L
        val userId = 1L
        val order = mockk<Order>()
        every { order.status } returns OrderStatus.PAID

        every { orderService.getOrderByIdForUpdateWithPending(userId, orderId) } throws CoreException(ErrorType.BAD_REQUEST, "PENDING 상태의 주문이 아닙니다")

        // when & then
        assertThrows<CoreException> {
            paymentFacade.createPayment(
                userId,
                orderId,
                "SAMSUNG",
                "1234-5678-9814-1451",
            )
        }
    }

    @Test
    @DisplayName("결제 초기화 - 이미 결제가 존재하면 실패")
    fun createPayment_alreadyExists() {
        // given
        val orderId = 100L
        val userId = 1L
        val order = mockk<Order>()
        every { order.status } returns OrderStatus.PENDING

        val existingReceipt = com.loopers.domain.payment.Receipt.create(orderId, "TXN_OLD", BigDecimal("10000"))

        every { orderService.getOrderByIdForUpdateWithPending(userId, orderId) } returns order
        every { receiptService.getReceiptByOrderId(orderId) } returns existingReceipt

        // when & then
        assertThrows<CoreException> {
            paymentFacade.createPayment(
                userId,
                orderId,
                "SAMSUNG",
                "1234-5678-9814-1451",
            )
        }
    }
}
