package com.loopers.domain.payment

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("ReceiptService Test")
class PaymentServiceTest {

    private val receiptRepository: ReceiptRepository = mockk()
    private val receiptService = ReceiptService(receiptRepository)

    @Test
    @DisplayName("주문 ID로 결제 조회")
    fun getPaymentByOrderId() {
        // given
        val receipt = Receipt.create(1L, "TXN001", BigDecimal("10000"), "", "")
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
        val receipt = Receipt.create(1L, "TXN001", BigDecimal("10000"), "", "")
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
        val receipt = Receipt.create(1L, "TXN001", BigDecimal("10000"), "", "")
        every { receiptRepository.save(any()) } returns receipt

        // when
        val result = receiptService.initiateReceipt(1L, "TXN001", BigDecimal("10000"), "", "")

        // then
        assert(result.orderId == 1L)
        assert(result.transactionId == "TXN001")
        verify { receiptRepository.save(any()) }
    }
}
