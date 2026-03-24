package com.loopers.interfaces.api.payment

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderService
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.payment.Receipt
import com.loopers.domain.payment.ReceiptService
import com.loopers.domain.payment.ReceiptStatus
import com.loopers.infrastructure.payment.ReceiptJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.support.eventually
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Payment Callback E2E Test")
class PaymentCallbackE2ETest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var receiptService: ReceiptService

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var receiptRepository: ReceiptJpaRepository

    @Autowired
    private lateinit var orderRepository: OrderJpaRepository

    @BeforeEach
    fun setup() {
        receiptRepository.deleteAll()
        orderRepository.deleteAll()
    }

    @Test
    @DisplayName("결제 콜백 - 주문 상태가 PENDING에서 PAID로 변경됨")
    fun paymentCallback_changeOrderStatus() {
        // given
        val order = Order.create(id = 100L, userId = 1L)
        order.markAsPaymentRequested() // ✅ Order 상태: PENDING → PAYMENT_REQUESTED
        val savedOrder = orderRepository.save(order)

        val receipt = Receipt.create(
            orderId = savedOrder.id,
            transactionId = "TXN001",
            amount = BigDecimal("10000"),
            cardType = "",
            cardNo = "",
        )
        val savedReceipt = receiptRepository.save(receipt)

        val callbackRequest = PaymentCallbackDto.CallbackRequest(
            transactionKey = savedReceipt.transactionId,
            orderId = savedOrder.id.toString(),
            cardType = CardType.SAMSUNG,
            cardNo = "1234-5678-9814-1451",
            amount = savedReceipt.amount.multiply(BigDecimal("100")).longValueExact(),
            status = TransactionStatus.COMPLETED,
            reason = null,
        )

        // when
        mockMvc.post("/api/v1/payments/callback") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(callbackRequest)
        }.andExpect {
            status { isOk() }
        }

        // then
        eventually {
            val updatedOrder = orderRepository.findById(savedOrder.id).orElseThrow()
            val updatedReceipt = receiptRepository.findByTransactionId("TXN001")

            assertThat(updatedOrder.status).isEqualTo(OrderStatus.PAID)
            assertThat(updatedReceipt?.status).isEqualTo(ReceiptStatus.COMPLETED)
        }
    }

    @Test
    @DisplayName("결제 콜백 - 멱등성: 중복 콜백은 상태를 변경하지 않음")
    fun paymentCallback_idempotency() {
        // given
        val order = Order.create(id = 101L, userId = 1L)
        order.markAsPaymentRequested() // ✅ Order 상태: PENDING → PAYMENT_REQUESTED
        val savedOrder = orderRepository.save(order)

        val receipt = Receipt.create(
            orderId = savedOrder.id,
            transactionId = "TXN001",
            amount = BigDecimal("10000"),
            cardType = "",
            cardNo = "",
        )
        val savedReceipt = receiptRepository.save(receipt)

        val callbackRequest = PaymentCallbackDto.CallbackRequest(
            transactionKey = savedReceipt.transactionId,
            orderId = savedOrder.id.toString(),
            cardType = CardType.SAMSUNG,
            cardNo = "1234-5678-9814-1451",
            amount = savedReceipt.amount.multiply(BigDecimal("100")).longValueExact(),
            status = TransactionStatus.COMPLETED,
            reason = null,
        )

        // when - 첫 번째 콜백
        mockMvc.post("/api/v1/payments/callback") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(callbackRequest)
        }.andExpect {
            status { isOk() }
        }

        // then - 첫 번째 후 상태 확인
        eventually {
            val updatedReceipt = receiptRepository.findByTransactionId("TXN001")
            assertThat(updatedReceipt?.status).isEqualTo(ReceiptStatus.COMPLETED)
        }

        // when - 두 번째 콜백 (중복)
        mockMvc.post("/api/v1/payments/callback") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(callbackRequest)
        }.andExpect {
            status { isOk() }
        }

        // then - 두 번째 후에도 상태 변경 없음
        eventually {
            val updatedReceipt = receiptRepository.findByTransactionId("TXN001")
            val finalOrder = orderRepository.findById(savedOrder.id).orElseThrow()
            assertThat(updatedReceipt?.status).isEqualTo(ReceiptStatus.COMPLETED)
            assertThat(finalOrder.status).isEqualTo(OrderStatus.PAID)
        }
    }

    @Test
    @DisplayName("결제 콜백 - 실패 상태")
    fun paymentCallback_failedStatus() {
        // given
        val order = Order.create(id = 102L, userId = 1L)
        order.markAsPaymentRequested() // ✅ Order 상태: PENDING → PAYMENT_REQUESTED
        val savedOrder = orderRepository.save(order)

        val receipt = Receipt.create(
            orderId = savedOrder.id,
            transactionId = "TXN001",
            amount = BigDecimal("10000"),
            cardType = "",
            cardNo = "",
        )
        val savedReceipt = receiptRepository.save(receipt)

        val callbackRequest = PaymentCallbackDto.CallbackRequest(
            transactionKey = savedReceipt.transactionId,
            orderId = savedOrder.id.toString(),
            cardType = CardType.SAMSUNG,
            cardNo = "1234-5678-9814-1451",
            amount = savedReceipt.amount.multiply(BigDecimal("100")).longValueExact(),
            status = TransactionStatus.FAILED,
            reason = "Card declined",
        )

        // when & then
        mockMvc.post("/api/v1/payments/callback") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(callbackRequest)
        }.andExpect {
            status { isOk() }
        }

        eventually {
            val updatedOrder = orderRepository.findById(savedOrder.id).orElseThrow()
            val updatedReceipt = receiptRepository.findByTransactionId("TXN001")

            assertThat(updatedOrder.status).isEqualTo(OrderStatus.PENDING)
            assertThat(updatedReceipt?.status).isEqualTo(ReceiptStatus.FAILED)
        }
    }

    @Test
    @DisplayName("결제 콜백 - 금액 불일치")
    fun paymentCallback_amountMismatch() {
        // given
        val order = Order.create(id = 103L, userId = 1L)
        val savedOrder = orderRepository.save(order)

        val receipt = Receipt.create(
            orderId = savedOrder.id,
            transactionId = "TXN001",
            amount = BigDecimal("10000"),
            cardType = "",
            cardNo = "",
        )
        val savedReceipt = receiptRepository.save(receipt)

        // 금액 불일치
        val callbackRequest = PaymentCallbackDto.CallbackRequest(
            transactionKey = savedReceipt.transactionId,
            orderId = savedOrder.id.toString(),
            cardType = CardType.SAMSUNG,
            cardNo = "1234-5678-9814-1451",
            amount = 9999L,
            status = TransactionStatus.COMPLETED,
            reason = null,
        )

        // when & then
        mockMvc.post("/api/v1/payments/callback") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(callbackRequest)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    @DisplayName("결제 콜백 - orderId 불일치 시 BadRequest")
    fun paymentCallback_orderIdMismatch() {
        // given
        val order = Order.create(id = 104L, userId = 1L)
        val savedOrder = orderRepository.save(order)

        val receipt = Receipt.create(
            orderId = savedOrder.id,
            transactionId = "TXN001",
            amount = BigDecimal("10000"),
            cardType = "",
            cardNo = "",
        )
        val savedReceipt = receiptRepository.save(receipt)

        // 잘못된 orderId로 콜백 전송 (Receipt의 orderId=104, 콜백의 orderId=999)
        val callbackRequest = PaymentCallbackDto.CallbackRequest(
            transactionKey = savedReceipt.transactionId,
            // ❌ 다른 orderId
            orderId = "999",
            cardType = CardType.SAMSUNG,
            cardNo = "1234-5678-9814-1451",
            amount = savedReceipt.amount.multiply(BigDecimal("100")).longValueExact(),
            status = TransactionStatus.COMPLETED,
            reason = null,
        )

        // when & then
        mockMvc.post("/api/v1/payments/callback") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(callbackRequest)
        }.andExpect {
            status { isBadRequest() } // ✅ orderId 불일치로 BadRequest
        }
    }

    @Test
    @DisplayName("결제 콜백 - Order 상태가 PAYMENT_REQUESTED가 아니면 실패")
    fun paymentCallback_invalidOrderStatus() {
        // given - Order를 PENDING으로 놔둠 (PAYMENT_REQUESTED로 안 함)
        val order = Order.create(id = 105L, userId = 1L)
        val savedOrder = orderRepository.save(order)
        // Order 상태: PENDING (markAsPaymentRequested() 호출 안 함)

        val receipt = Receipt.create(
            orderId = savedOrder.id,
            transactionId = "TXN001",
            amount = BigDecimal("10000"),
            cardType = "",
            cardNo = "",
        )
        val savedReceipt = receiptRepository.save(receipt)

        val callbackRequest = PaymentCallbackDto.CallbackRequest(
            transactionKey = savedReceipt.transactionId,
            orderId = savedOrder.id.toString(),
            cardType = CardType.SAMSUNG,
            cardNo = "1234-5678-9814-1451",
            amount = savedReceipt.amount.multiply(BigDecimal("100")).longValueExact(),
            status = TransactionStatus.COMPLETED,
            reason = null,
        )

        // when & then
        mockMvc.post("/api/v1/payments/callback") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(callbackRequest)
        }.andExpect {
            status { isBadRequest() }
        }

        eventually {
            val updatedOrder = orderRepository.findById(savedOrder.id).orElseThrow()
            val updatedReceipt = receiptRepository.findByTransactionId("TXN001")

            assertThat(updatedOrder.status).isEqualTo(OrderStatus.PENDING)
            assertThat(updatedReceipt?.status).isEqualTo(ReceiptStatus.PENDING)
        }
    }
}
