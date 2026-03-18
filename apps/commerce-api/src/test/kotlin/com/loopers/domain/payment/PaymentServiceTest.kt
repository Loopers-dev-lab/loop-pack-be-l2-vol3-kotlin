package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@DisplayName("ReceiptService Test")
class PaymentServiceTest {

    private val receiptRepository: ReceiptRepository = mockk()
    private val receiptService = ReceiptService(receiptRepository)

    @Test
    @DisplayName("결제를 거래 ID로 조회")
    fun getPaymentByTransactionId_success() {
        // given
        val receipt = Receipt.create(1L, "TXN001", BigDecimal("10000"))
        every { receiptRepository.findByTransactionId("TXN001") } returns receipt

        // when
        val result = receiptService.getReceiptByTransactionId("TXN001")

        // then
        assert(result.id == receipt.id)
        verify { receiptRepository.findByTransactionId("TXN001") }
    }

    @Test
    @DisplayName("결제를 거래 ID로 조회 - 존재하지 않으면 예외 발생")
    fun getPaymentByTransactionId_notFound() {
        // given
        every { receiptRepository.findByTransactionId("INVALID") } returns null

        // when & then
        assertThrows<CoreException> {
            receiptService.getReceiptByTransactionId("INVALID")
        }
    }

    @Test
    @DisplayName("주문 ID로 결제 조회")
    fun getPaymentByOrderId() {
        // given
        val receipt = Receipt.create(1L, "TXN001", BigDecimal("10000"))
        every { receiptRepository.findByOrderId(1L) } returns receipt

        // when
        val result = receiptService.getReceiptByOrderId(1L)

        // then
        assert(result?.id == receipt.id)
    }

    @Test
    @DisplayName("결제 저장")
    fun save() {
        // given
        val receipt = Receipt.create(1L, "TXN001", BigDecimal("10000"))
        every { receiptRepository.save(receipt) } returns receipt

        // when
        val result = receiptService.save(receipt)

        // then
        assert(result.id == receipt.id)
        verify { receiptRepository.save(receipt) }
    }

    @Test
    @DisplayName("결제 시작")
    fun initiateReceipt() {
        // given
        val receipt = Receipt.create(1L, "TXN001", BigDecimal("10000"))
        every { receiptRepository.save(any()) } returns receipt

        // when
        val result = receiptService.initiateReceipt(1L, "TXN001", BigDecimal("10000"))

        // then
        assert(result.orderId == 1L)
        assert(result.transactionId == "TXN001")
        verify { receiptRepository.save(any()) }
    }
}
