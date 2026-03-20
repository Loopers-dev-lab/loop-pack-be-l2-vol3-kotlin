package com.loopers.domain.payment

import com.loopers.domain.order.OrderService
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
import java.math.BigDecimal

@DisplayName("PaymentRecoveryService Unit Test")
class PaymentRecoveryServiceTest {

    private val receiptService: ReceiptService = mockk()
    private val orderService: OrderService = mockk()
    private val paymentClient: PaymentClient = mockk()

    private val service = PaymentRecoveryService(receiptService, orderService, paymentClient)

    @Nested
    @DisplayName("recoverFailedPayments")
    inner class RecoverFailedPayments {

        @Test
        @DisplayName("PENDING Receipt 조회 후 모두 복구 성공")
        fun recoversAllPendingReceipts() {
            // given
            val receipt1 = Receipt.create(100L, "TXN_001", BigDecimal("10000"), "SAMSUNG", "1234")
            val receipt2 = Receipt.create(200L, "TXN_002", BigDecimal("20000"), "HYUNDAI", "5678")

            every { receiptService.getReceiptsForRecovery(any()) } returns listOf(receipt1, receipt2)
            every { paymentClient.checkPaymentStatus(100L) } returns
                PaymentStatusCheckResult("TXN_001", "COMPLETED", BigDecimal("10000"), null)
            every { paymentClient.checkPaymentStatus(200L) } returns
                PaymentStatusCheckResult("TXN_002", "COMPLETED", BigDecimal("20000"), null)
            every { receiptService.markAsCompleted(any()) } just runs
            every { orderService.markOrderAsPaid(any()) } just runs

            // when
            val result = service.recoverFailedPayments()

            // then
            assert(result == 2)
            verify(exactly = 2) { receiptService.markAsCompleted(any()) }
            verify(exactly = 2) { orderService.markOrderAsPaid(any()) }
        }

        @Test
        @DisplayName("PENDING 상태 없으면 0 반환")
        fun noRecoveryWhenNoPendingReceipts() {
            // given
            every { receiptService.getReceiptsForRecovery(any()) } returns emptyList()

            // when
            val result = service.recoverFailedPayments()

            // then
            assert(result == 0)
            verify(exactly = 0) { paymentClient.checkPaymentStatus(any()) }
        }

        @Test
        @DisplayName("일부 복구 성공, 일부 실패")
        fun partialRecovery() {
            // given
            val receipt1 = Receipt.create(100L, "TXN_001", BigDecimal("10000"), "SAMSUNG", "1234")
            val receipt2 = Receipt.create(200L, "TXN_002", BigDecimal("20000"), "HYUNDAI", "5678")

            every { receiptService.getReceiptsForRecovery(any()) } returns listOf(receipt1, receipt2)
            every { paymentClient.checkPaymentStatus(100L) } returns
                PaymentStatusCheckResult("TXN_001", "COMPLETED", BigDecimal("10000"), null)
            every { paymentClient.checkPaymentStatus(200L) } throws
                CoreException(ErrorType.INTERNAL_ERROR, "PG connection failed")
            every { receiptService.markAsCompleted(any()) } just runs
            every { orderService.markOrderAsPaid(any()) } just runs

            // when
            val result = service.recoverFailedPayments()

            // then
            assert(result == 1)
            verify(exactly = 1) { receiptService.markAsCompleted(any()) }
            verify(exactly = 1) { orderService.markOrderAsPaid(any()) }
        }
    }

    @Nested
    @DisplayName("attemptRecovery - COMPLETED")
    inner class AttemptRecoveryCompleted {

        @Test
        @DisplayName("PG 상태 COMPLETED → Receipt/Order 업데이트")
        fun pgCompletedStatus_updatesReceiptAndOrder() {
            // given
            val receipt = Receipt.create(100L, "TXN_001", BigDecimal("10000"), "SAMSUNG", "1234")

            every { receiptService.getReceiptsForRecovery(any()) } returns listOf(receipt)
            every { paymentClient.checkPaymentStatus(100L) } returns
                PaymentStatusCheckResult("TXN_001", "COMPLETED", BigDecimal("10000"), null)
            every { receiptService.markAsCompleted(any()) } just runs
            every { orderService.markOrderAsPaid(any()) } just runs

            // when
            val result = service.recoverFailedPayments()

            // then
            assert(result == 1)
            verify(exactly = 1) { paymentClient.checkPaymentStatus(100L) }
            verify(exactly = 1) { receiptService.markAsCompleted(any()) }
            verify(exactly = 1) { orderService.markOrderAsPaid(100L) }
        }
    }

    @Nested
    @DisplayName("attemptRecovery - FAILED")
    inner class AttemptRecoveryFailed {

        @Test
        @DisplayName("PG 상태 FAILED → Receipt FAILED로 변경, Order PENDING으로 복원")
        fun pgFailedStatus_marksReceiptAsFailedAndRestoresOrder() {
            // given
            val receipt = Receipt.create(100L, "TXN_001", BigDecimal("10000"), "SAMSUNG", "1234")

            every { receiptService.getReceiptsForRecovery(any()) } returns listOf(receipt)
            every { paymentClient.checkPaymentStatus(100L) } returns
                PaymentStatusCheckResult("TXN_001", "FAILED", BigDecimal("10000"), "Card declined")
            every { receiptService.markAsFailed(any(), any()) } just runs
            every { orderService.restoreOrderToPending(any()) } just runs

            // when
            val result = service.recoverFailedPayments()

            // then
            assert(result == 1)
            verify(exactly = 1) { receiptService.markAsFailed(any(), "Card declined") }
            verify(exactly = 1) { orderService.restoreOrderToPending(100L) }
        }
    }

    @Nested
    @DisplayName("attemptRecovery - CANCELLED")
    inner class AttemptRecoveryCancelled {

        @Test
        @DisplayName("PG 상태 CANCELLED → Receipt CANCELLED로 변경, Order PENDING으로 복원")
        fun pgCancelledStatus_marksReceiptAsCancelledAndRestoresOrder() {
            // given
            val receipt = Receipt.create(100L, "TXN_001", BigDecimal("10000"), "SAMSUNG", "1234")

            every { receiptService.getReceiptsForRecovery(any()) } returns listOf(receipt)
            every { paymentClient.checkPaymentStatus(100L) } returns
                PaymentStatusCheckResult("TXN_001", "CANCELLED", BigDecimal("10000"), "User cancelled")
            every { receiptService.markAsCancelled(any(), any()) } just runs
            every { orderService.restoreOrderToPending(any()) } just runs

            // when
            val result = service.recoverFailedPayments()

            // then
            assert(result == 1)
            verify(exactly = 1) { receiptService.markAsCancelled(any(), "User cancelled") }
            verify(exactly = 1) { orderService.restoreOrderToPending(100L) }
        }
    }

    @Nested
    @DisplayName("attemptRecovery - PENDING/TIMEOUT")
    inner class AttemptRecoveryPendingTimeout {

        @Test
        @DisplayName("PG 상태 PENDING → 복구 불가, Order PENDING으로 복원")
        fun pgPendingStatus_restoresOrderAndReturnsFailure() {
            // given
            val receipt = Receipt.create(100L, "TXN_001", BigDecimal("10000"), "SAMSUNG", "1234")

            every { receiptService.getReceiptsForRecovery(any()) } returns listOf(receipt)
            every { paymentClient.checkPaymentStatus(100L) } returns
                PaymentStatusCheckResult("TXN_001", "PENDING", BigDecimal("10000"), null)
            every { orderService.restoreOrderToPending(any()) } just runs

            // when
            val result = service.recoverFailedPayments()

            // then
            assert(result == 0)
            verify(exactly = 0) { receiptService.markAsCompleted(any()) }
            verify(exactly = 0) { receiptService.markAsFailed(any(), any()) }
            verify(exactly = 1) { orderService.restoreOrderToPending(100L) }
        }

        @Test
        @DisplayName("PG 상태 TIMEOUT → 복구 불가, Order PENDING으로 복원")
        fun pgTimeoutStatus_restoresOrderAndReturnsFailure() {
            // given
            val receipt = Receipt.create(100L, "TXN_001", BigDecimal("10000"), "SAMSUNG", "1234")

            every { receiptService.getReceiptsForRecovery(any()) } returns listOf(receipt)
            every { paymentClient.checkPaymentStatus(100L) } returns
                PaymentStatusCheckResult("TXN_001", "TIMEOUT", BigDecimal("10000"), null)
            every { orderService.restoreOrderToPending(any()) } just runs

            // when
            val result = service.recoverFailedPayments()

            // then
            assert(result == 0)
            verify(exactly = 1) { orderService.restoreOrderToPending(100L) }
        }
    }

    @Nested
    @DisplayName("attemptRecovery - Exception Handling")
    inner class AttemptRecoveryException {

        @Test
        @DisplayName("checkPaymentStatus 실패 → Exception 처리, 다음 주기 재시도")
        fun pgCheckFailed_catchesException() {
            // given
            val receipt = Receipt.create(100L, "TXN_001", BigDecimal("10000"), "SAMSUNG", "1234")

            every { receiptService.getReceiptsForRecovery(any()) } returns listOf(receipt)
            every { paymentClient.checkPaymentStatus(100L) } throws
                CoreException(ErrorType.INTERNAL_ERROR, "PG service error")

            // when
            val result = service.recoverFailedPayments()

            // then
            assert(result == 0)
            verify(exactly = 0) { receiptService.markAsCompleted(any()) }
        }

        @Test
        @DisplayName("Unknown PG 상태 → 복구 불가")
        fun unknownPgStatus_returnsFailure() {
            // given
            val receipt = Receipt.create(100L, "TXN_001", BigDecimal("10000"), "SAMSUNG", "1234")

            every { receiptService.getReceiptsForRecovery(any()) } returns listOf(receipt)
            every { paymentClient.checkPaymentStatus(100L) } returns
                PaymentStatusCheckResult("TXN_001", "UNKNOWN_STATUS", BigDecimal("10000"), null)

            // when
            val result = service.recoverFailedPayments()

            // then
            assert(result == 0)
        }
    }
}
