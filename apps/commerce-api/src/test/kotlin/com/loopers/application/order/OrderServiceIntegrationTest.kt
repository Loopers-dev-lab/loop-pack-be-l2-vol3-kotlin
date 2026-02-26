package com.loopers.application.order

import com.loopers.domain.order.Order
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.ZonedDateTime

/**
 * OrderService 통합 테스트
 * - 실제 DB(TestContainers)와 연동하여 Service → Repository 레이어 통합 테스트
 */
@SpringBootTest
class OrderServiceIntegrationTest @Autowired constructor(
    private val orderService: OrderService,
    private val orderJpaRepository: OrderJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createOrder(userId: Long = 1L): Order {
        val order = Order(userId = userId)
        order.addItem(
            productId = 1L,
            productName = "에어맥스 90",
            brandName = "나이키",
            quantity = 2,
            unitPrice = BigDecimal("129000"),
        )
        return orderJpaRepository.save(order)
    }

    @DisplayName("주문을 조회할 때,")
    @Nested
    inner class GetOrder {

        @DisplayName("존재하는 주문 ID로 조회하면, 주문 정보가 반환된다.")
        @Test
        fun returnsOrder_whenOrderExists() {
            // arrange
            val saved = createOrder()

            // act
            val result = orderService.getOrder(saved.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(saved.id) },
                { assertThat(result.userId).isEqualTo(1L) },
                { assertThat(result.totalAmount).isEqualByComparingTo(BigDecimal("258000")) },
            )
        }

        @DisplayName("존재하지 않는 주문 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenOrderNotExists() {
            // act & assert
            val exception = assertThrows<CoreException> {
                orderService.getOrder(999L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("유저의 주문 목록을 기간별로 조회할 때,")
    @Nested
    inner class GetUserOrders {

        @DisplayName("기간 내 주문이 있으면, 주문 목록이 반환된다.")
        @Test
        fun returnsOrders_whenInPeriod() {
            // arrange
            createOrder(userId = 1L)
            createOrder(userId = 1L)
            val startAt = ZonedDateTime.now().minusDays(1)
            val endAt = ZonedDateTime.now().plusDays(1)

            // act
            val result = orderService.getUserOrders(1L, startAt, endAt)

            // assert
            assertThat(result).hasSize(2)
        }

        @DisplayName("기간 내 주문이 없으면, 빈 목록이 반환된다.")
        @Test
        fun returnsEmptyList_whenNoOrdersInPeriod() {
            // arrange
            createOrder(userId = 1L)
            val startAt = ZonedDateTime.now().minusDays(10)
            val endAt = ZonedDateTime.now().minusDays(5)

            // act
            val result = orderService.getUserOrders(1L, startAt, endAt)

            // assert
            assertThat(result).isEmpty()
        }
    }
}
