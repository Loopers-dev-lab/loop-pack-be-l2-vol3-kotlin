package com.loopers.domain.order

import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderStatus
import com.loopers.domain.order.TotalAmount
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
class OrderServiceTest {

    private val orderRepository: OrderRepository = mockk()

    private lateinit var orderService: OrderService

    @BeforeEach
    fun setUp() {
        orderService = OrderService(orderRepository)
    }

    private fun createOrderModel(
        userId: Long = 1L,
        totalAmount: Long = 10_000L,
        status: OrderStatus = OrderStatus.PENDING_PAYMENT,
    ): OrderModel {
        val model = OrderModel(userId = userId, totalAmount = TotalAmount(totalAmount), status = status)
        val now = ZonedDateTime.now()
        listOf("createdAt", "updatedAt").forEach { fieldName ->
            val field = OrderModel::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(model, now)
        }
        return model
    }

    @Nested
    inner class CreateOrder {

        @Test
        fun `주문 생성 성공`() {
            // given
            val savedOrder = createOrderModel(userId = 1L, totalAmount = 20_000L)
            every { orderRepository.save(any()) } returns savedOrder

            // when
            val result = orderService.createOrder(userId = 1L, totalAmount = 20_000L)

            // then
            assertNotNull(result)
            assertEquals(20_000L, result.totalAmount.value)
            verify(exactly = 1) { orderRepository.save(any()) }
        }
    }

    @Nested
    inner class GetOrders {

        @Test
        fun `사용자 주문 목록을 Page로 반환`() {
            // given
            val pageable = PageRequest.of(0, 10)
            val orders = listOf(
                createOrderModel(userId = 1L, totalAmount = 10_000L),
                createOrderModel(userId = 1L, totalAmount = 20_000L),
            )
            val start = ZonedDateTime.now().minusDays(7)
            val end = ZonedDateTime.now()
            every { orderRepository.findAllByUserId(1L, start, end, pageable) } returns PageImpl(orders, pageable, 2L)

            // when
            val result = orderService.getOrders(1L, start, end, pageable)

            // then
            assertEquals(2, result.content.size)
            verify(exactly = 1) { orderRepository.findAllByUserId(1L, start, end, pageable) }
        }
    }

    @Nested
    inner class GetOrderById {

        @Test
        fun `주문 단건 조회 성공`() {
            // given
            val order = createOrderModel(userId = 1L, totalAmount = 10_000L)
            every { orderRepository.findById(1L) } returns order

            // when
            val result = orderService.getOrderById(1L)

            // then
            assertNotNull(result)
            assertEquals(1L, result.userId)
            verify(exactly = 1) { orderRepository.findById(1L) }
        }

        @Test
        fun `존재하지 않는 주문 조회 시 NOT_FOUND 예외`() {
            // given
            every { orderRepository.findById(99L) } returns null

            // when
            val exception = assertThrows<CoreException> {
                orderService.getOrderById(99L)
            }

            // then
            assertEquals(ErrorType.NOT_FOUND, exception.errorType)
        }
    }

    @Nested
    inner class GetAllOrders {

        @Test
        fun `전체 주문 목록을 Page로 반환`() {
            // given
            val pageable = PageRequest.of(0, 10)
            val orders = listOf(
                createOrderModel(userId = 1L, totalAmount = 10_000L),
                createOrderModel(userId = 2L, totalAmount = 30_000L),
            )
            every { orderRepository.findAll(pageable) } returns PageImpl(orders, pageable, 2L)

            // when
            val result = orderService.getAllOrders(pageable)

            // then
            assertEquals(2, result.content.size)
            verify(exactly = 1) { orderRepository.findAll(pageable) }
        }
    }
}
