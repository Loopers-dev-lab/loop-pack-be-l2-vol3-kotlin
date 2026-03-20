package com.loopers.application.api.payment

import com.loopers.domain.order.OrderService
import com.loopers.domain.order.dto.OrderInfo
import com.loopers.domain.payment.PaymentClient
import com.loopers.domain.payment.PaymentRequestResult
import com.loopers.domain.payment.Receipt
import com.loopers.domain.payment.ReceiptService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@DisplayName("PaymentFacade.requestPayment")
class PaymentFacadeTest {

    private val receiptService: ReceiptService = mockk()
    private val orderService: OrderService = mockk()
    private val paymentClient: PaymentClient = mockk()
    private val facade = PaymentFacade(receiptService, orderService, paymentClient)

    @Nested
    @DisplayName("PG 결제 완료 응답 (COMPLETED)")
    inner class PgCompletedResponse {

        @Test
        @DisplayName("Receipt COMPLETED, Order PAYMENT_REQUESTED로 변경")
        fun success() {
            // given
            val userId = 1L
            val orderId = 100L
            val receipt = mockk<Receipt>(relaxed = true)

            every { orderService.getOrderInfoForPayment(userId, orderId) } returns
                OrderInfo(orderId = orderId, amount = BigDecimal("10000"))
            every { receiptService.getReceiptByOrderId(orderId) } returns null
            every { receiptService.initiateReceipt(any(), any(), any(), any(), any()) } returns receipt
            every { paymentClient.requestPayment(any(), any(), any(), any(), any(), any()) } returns
                PaymentRequestResult("TXN_001", "100", "SAMSUNG", "1234", 10000L, "COMPLETED", null)
            every { receiptService.markAsCompleted(any()) } just runs
            every { orderService.markOrderAsPaymentRequested(any(), any()) } just runs

            // when
            facade.requestPayment(userId, orderId, "SAMSUNG", "1234")

            // then
            verify(exactly = 1) { receiptService.markAsCompleted(any()) }
            verify(exactly = 1) { orderService.markOrderAsPaymentRequested(userId, orderId) }
        }
    }

    @Nested
    @DisplayName("PG 결제 대기 응답 (PENDING)")
    inner class PgPendingResponse {

        @Test
        @DisplayName("Receipt PENDING 유지, Order 상태 변경 안 함")
        fun pending() {
            // given
            val userId = 1L
            val orderId = 100L
            val receipt = mockk<Receipt>(relaxed = true)

            every { orderService.getOrderInfoForPayment(userId, orderId) } returns
                OrderInfo(orderId = orderId, amount = BigDecimal("10000"))
            every { receiptService.getReceiptByOrderId(orderId) } returns null
            every { receiptService.initiateReceipt(any(), any(), any(), any(), any()) } returns receipt
            every { paymentClient.requestPayment(any(), any(), any(), any(), any(), any()) } returns
                PaymentRequestResult("TXN_002", "100", "SAMSUNG", "1234", 10000L, "PENDING", null)

            // when
            facade.requestPayment(userId, orderId, "SAMSUNG", "1234")

            // then
            verify(exactly = 0) { receiptService.markAsCompleted(any()) }
            verify(exactly = 0) { orderService.markOrderAsPaymentRequested(any(), any()) }
            verify(exactly = 0) { receiptService.markAsFailed(any(), any()) }
        }
    }

    @Nested
    @DisplayName("PG 결제 실패 응답 (FAILED)")
    inner class PgFailedResponse {

        @Test
        @DisplayName("Receipt FAILED, Exception 발생")
        fun failed() {
            // given
            val userId = 1L
            val orderId = 100L
            val receipt = mockk<Receipt>()

            every { orderService.getOrderInfoForPayment(userId, orderId) } returns
                OrderInfo(orderId = orderId, amount = BigDecimal("10000"))
            every { receiptService.getReceiptByOrderId(orderId) } returns null
            every { receiptService.initiateReceipt(any(), any(), any(), any(), any()) } returns receipt
            every { paymentClient.requestPayment(any(), any(), any(), any(), any(), any()) } returns
                PaymentRequestResult("TXN_003", "100", "SAMSUNG", "1234", 10000L, "FAILED", "Card declined")
            every { receiptService.markAsFailed(any(), any()) } just runs

            // when & then
            assertThrows<CoreException> {
                facade.requestPayment(userId, orderId, "SAMSUNG", "1234")
            }
            verify(exactly = 1) { receiptService.markAsFailed(any(), "Card declined") }
        }
    }

    @Nested
    @DisplayName("PG 취소 응답 (CANCELLED)")
    inner class PgCancelledResponse {

        @Test
        @DisplayName("Receipt FAILED, Exception 발생")
        fun cancelled() {
            // given
            val userId = 1L
            val orderId = 100L
            val receipt = mockk<Receipt>()

            every { orderService.getOrderInfoForPayment(userId, orderId) } returns
                OrderInfo(orderId = orderId, amount = BigDecimal("10000"))
            every { receiptService.getReceiptByOrderId(orderId) } returns null
            every { receiptService.initiateReceipt(any(), any(), any(), any(), any()) } returns receipt
            every { paymentClient.requestPayment(any(), any(), any(), any(), any(), any()) } returns
                PaymentRequestResult("TXN_004", "100", "SAMSUNG", "1234", 10000L, "CANCELLED", "User cancelled")
            every { receiptService.markAsFailed(any(), any()) } just runs

            // when & then
            assertThrows<CoreException> {
                facade.requestPayment(userId, orderId, "SAMSUNG", "1234")
            }
            verify(exactly = 1) { receiptService.markAsFailed(any(), "User cancelled") }
        }
    }

    @Nested
    @DisplayName("네트워크 타임아웃")
    inner class NetworkTimeout {

        @Test
        @DisplayName("Receipt TIMEOUT, Exception 발생")
        fun timeout() {
            // given
            val userId = 1L
            val orderId = 100L
            val receipt = mockk<Receipt>()

            every { orderService.getOrderInfoForPayment(userId, orderId) } returns
                OrderInfo(orderId = orderId, amount = BigDecimal("10000"))
            every { receiptService.getReceiptByOrderId(orderId) } returns null
            every { receiptService.initiateReceipt(any(), any(), any(), any(), any()) } returns receipt
            every { paymentClient.requestPayment(any(), any(), any(), any(), any(), any()) } throws
                RuntimeException("Connection timeout")
            every { receiptService.markAsTimeout(any()) } just runs

            // when & then
            assertThrows<CoreException> {
                facade.requestPayment(userId, orderId, "SAMSUNG", "1234")
            }
            verify(exactly = 1) { receiptService.markAsTimeout(any()) }
        }
    }

    @Nested
    @DisplayName("주문 검증")
    inner class OrderValidation {

        @Test
        @DisplayName("이미 결제가 존재하면 Exception 발생")
        fun alreadyExists() {
            // given
            val userId = 1L
            val orderId = 100L
            val existingReceipt = mockk<Receipt>()

            every { orderService.getOrderInfoForPayment(userId, orderId) } returns
                OrderInfo(orderId = orderId, amount = BigDecimal("10000"))
            every { receiptService.getReceiptByOrderId(orderId) } returns existingReceipt
            every { receiptService.validateReceiptForNewPayment(existingReceipt) } throws
                CoreException(ErrorType.CONFLICT, "이미 이 주문에 대한 결제가 진행 중입니다")

            // when & then
            assertThrows<CoreException> {
                facade.requestPayment(userId, orderId, "SAMSUNG", "1234")
            }
        }
    }
}
