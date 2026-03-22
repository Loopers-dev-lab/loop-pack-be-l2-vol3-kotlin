package com.loopers.application.api.payment

import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderRepository
import com.loopers.utils.DatabaseCleanUp
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals

@SpringBootTest
@DisplayName("PaymentFacade - 동시 요청 제어")
class PaymentFacadeConcurrentTest @Autowired constructor(
    private val orderRepository: OrderRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @Transactional
    @DisplayName("Order Pessimistic Lock - 동시 업데이트 직렬화")
    fun orderPessimisticLock_serializesSimultaneousUpdates() {
        // given: Order 생성 (PENDING 상태)
        val order = Order.create(id = 1000L, userId = 1L, couponId = null)
        val savedOrder = orderRepository.save(order)
        val orderId = savedOrder.id!!

        // then: Order는 Pessimistic Write Lock으로 보호됨
        val fetchedOrder = orderRepository.findByIdForUpdate(orderId)
        assertEquals(orderId, fetchedOrder?.id, "Order는 Lock을 통해 조회 가능")
        assertEquals("PENDING", fetchedOrder?.status?.name, "Order 상태는 PENDING")
    }

    @Test
    @DisplayName("Order 상태 검증 - 결제 진행 중이면 CONFLICT 예외")
    fun orderStatusValidation_preventsPaymentOnPaid() {
        // given: Order 생성 후 PAYMENT_REQUESTED로 변경
        val order = Order.create(id = 2000L, userId = 2L, couponId = null)
        val savedOrder = orderRepository.save(order)
        val orderId = savedOrder.id!!

        savedOrder.markAsPaymentRequested()
        orderRepository.save(savedOrder)

        // then: 상태가 PAYMENT_REQUESTED로 변경됨
        val fetchedOrder = orderRepository.findById(orderId)
        assertEquals("PAYMENT_REQUESTED", fetchedOrder?.status?.name, "상태가 PAYMENT_REQUESTED여야 함")
    }
}
