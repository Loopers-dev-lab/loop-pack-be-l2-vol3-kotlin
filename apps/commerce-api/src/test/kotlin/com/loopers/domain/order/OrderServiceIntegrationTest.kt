package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.ZoneId

@SpringBootTest
class OrderServiceIntegrationTest @Autowired constructor(
    private val orderService: OrderService,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    inner class CreateOrder {

        @Test
        fun `유효한 userId와 totalAmount로 주문을 생성한다`() {
            val order = orderService.createOrder(userId = 1L, totalAmount = 50_000L)

            assertThat(order.id).isGreaterThan(0)
            assertThat(order.userId).isEqualTo(1L)
            assertThat(order.totalAmount.value).isEqualTo(50_000L)
            assertThat(order.status).isEqualTo(OrderStatus.PENDING_PAYMENT)
        }
    }

    @Nested
    inner class GetOrders {

        @Test
        fun `userId와 날짜 범위로 주문 목록을 조회한다`() {
            orderService.createOrder(userId = 1L, totalAmount = 10_000L)
            orderService.createOrder(userId = 1L, totalAmount = 20_000L)
            orderService.createOrder(userId = 2L, totalAmount = 30_000L)

            val startAt = LocalDate.now().minusDays(1).atStartOfDay(ZoneId.systemDefault())
            val endAt = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault())
            val pageable = PageRequest.of(0, 10)

            val result = orderService.getOrders(1L, startAt, endAt, pageable)

            assertThat(result.content).hasSize(2)
            assertThat(result.content.map { it.userId }).containsOnly(1L)
        }
    }

    @Nested
    inner class GetOrderById {

        @Test
        fun `존재하는 ID로 주문을 조회한다`() {
            val created = orderService.createOrder(userId = 1L, totalAmount = 50_000L)

            val result = orderService.getOrderById(created.id)

            assertThat(result.id).isEqualTo(created.id)
            assertThat(result.totalAmount.value).isEqualTo(50_000L)
        }

        @Test
        fun `존재하지 않는 ID로 조회 시 NOT_FOUND 예외가 발생한다`() {
            val exception = assertThrows<CoreException> {
                orderService.getOrderById(99999L)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    inner class GetAllOrders {

        @Test
        fun `전체 주문 페이징 조회를 반환한다`() {
            orderService.createOrder(userId = 1L, totalAmount = 10_000L)
            orderService.createOrder(userId = 2L, totalAmount = 20_000L)
            orderService.createOrder(userId = 3L, totalAmount = 30_000L)

            val pageable = PageRequest.of(0, 10)
            val result = orderService.getAllOrders(pageable)

            assertThat(result.content).hasSize(3)
            assertThat(result.totalElements).isEqualTo(3L)
        }
    }
}
