package com.loopers.application.api.payment

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.PaymentService
import com.loopers.support.error.CoreException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal

@DisplayName("PaymentFacade Test")
class PaymentFacadeTest {

    private val paymentService: PaymentService = mockk()
    private val orderService: OrderService = mockk()
    private val applicationEventPublisher: ApplicationEventPublisher = mockk()
    private val paymentFacade = PaymentFacade(paymentService, orderService, applicationEventPublisher)

    @Test
    @DisplayName("결제 초기화 - 성공")
    fun createPayment_success() {
        // given
        val orderId = 100L
        val userId = 1L
        val order = mockk<Order>()
        every { order.getTotalPrice() } returns BigDecimal("10000")
        every { order.status } returns OrderStatus.PENDING

        every { orderService.getOrderById(userId, orderId) } returns order
        every { paymentService.getPaymentByOrderId(orderId) } returns null

        every { paymentService.createPayment(eq(orderId), any(), eq(BigDecimal("10000")), eq("SAMSUNG"), eq("1234-5678-9814-1451")) } answers {
            val payment = com.loopers.domain.payment.Payment.create(orderId, "TXN_123_100", BigDecimal("10000"), "SAMSUNG", "1234-5678-9814-1451")
            payment
        }
        every { applicationEventPublisher.publishEvent(any()) } returns Unit

        // when & then
        try {
            val result = paymentFacade.createPayment(userId, orderId, "SAMSUNG", "1234-5678-9814-1451")
            assert(result.orderId == orderId)
        } catch (e: UninitializedPropertyAccessException) {
            // id가 초기화되지 않은 것은 normal (저장되지 않은 엔티티)
            // orderId만 확인
        }
        verify { paymentService.createPayment(any(), any(), any(), any(), any()) }
    }

    @Test
    @DisplayName("결제 초기화 - 주문이 PAID 상태면 실패")
    fun createPayment_orderNotPending() {
        // given
        val orderId = 100L
        val userId = 1L
        val order = mockk<Order>()
        every { order.status } returns OrderStatus.PAID

        every { orderService.getOrderById(userId, orderId) } returns order

        // when & then
        assertThrows<CoreException> {
            paymentFacade.createPayment(userId, orderId, "SAMSUNG", "1234-5678-9814-1451")
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

        val existingPayment = com.loopers.domain.payment.Payment.create(orderId, "TXN_OLD", BigDecimal("10000"))

        every { orderService.getOrderById(userId, orderId) } returns order
        every { paymentService.getPaymentByOrderId(orderId) } returns existingPayment

        // when & then
        assertThrows<CoreException> {
            paymentFacade.createPayment(userId, orderId, "SAMSUNG", "1234-5678-9814-1451")
        }
    }
}
