package com.loopers.domain.order

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
import org.springframework.data.domain.PageRequest
import java.time.ZoneId
import java.time.ZonedDateTime

@SpringBootTest
class OrderServiceIntegrationTest @Autowired constructor(
    private val orderService: OrderService,
    private val orderJpaRepository: OrderJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("주문을 생성할 때, ")
    @Nested
    inner class CreateOrder {
        @DisplayName("유효한 주문 정보가 주어지면, 주문이 생성된다.")
        @Test
        fun createsOrder_whenValidOrderIsProvided() {
            // arrange
            val orderItems = listOf(
                OrderItem(productId = 1L, productName = "에어맥스", productPrice = 139000, quantity = 2),
                OrderItem(productId = 2L, productName = "에어포스", productPrice = 119000, quantity = 1),
            )
            val order = Order(userId = 1L, items = orderItems)

            // act
            val result = orderService.createOrder(order)

            // assert
            val savedOrder = orderJpaRepository.findById(result.id).get()
            assertAll(
                { assertThat(savedOrder.userId).isEqualTo(1L) },
                { assertThat(savedOrder.items).hasSize(2) },
                { assertThat(savedOrder.totalPrice).isEqualTo(139000 * 2 + 119000) },
            )
        }
    }

    @DisplayName("주문을 조회할 때, ")
    @Nested
    inner class GetOrder {
        @DisplayName("존재하는 주문 ID를 주면, 주문 정보를 반환한다.")
        @Test
        fun returnsOrder_whenOrderExists() {
            // arrange
            val order = orderJpaRepository.save(
                Order(
                    userId = 1L,
                    items = listOf(
                        OrderItem(productId = 1L, productName = "에어맥스", productPrice = 139000, quantity = 1),
                    ),
                ),
            )

            // act
            val result = orderService.getOrder(order.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(order.id) },
                { assertThat(result.userId).isEqualTo(1L) },
                { assertThat(result.items).hasSize(1) },
            )
        }

        @DisplayName("존재하지 않는 주문 ID를 주면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenOrderNotExists() {
            // act
            val exception = assertThrows<CoreException> {
                orderService.getOrder(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("삭제된 주문 ID를 주면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenOrderIsDeleted() {
            // arrange
            val order = orderJpaRepository.save(
                Order(
                    userId = 1L,
                    items = listOf(
                        OrderItem(productId = 1L, productName = "에어맥스", productPrice = 139000, quantity = 1),
                    ),
                ),
            )
            order.delete()
            orderJpaRepository.save(order)

            // act
            val exception = assertThrows<CoreException> {
                orderService.getOrder(order.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("유저의 주문 목록을 날짜 범위로 조회할 때, ")
    @Nested
    inner class GetUserOrders {
        @DisplayName("해당 기간 내 주문이 있으면, 목록을 반환한다.")
        @Test
        fun returnsOrders_whenOrdersExistInDateRange() {
            // arrange
            orderJpaRepository.save(
                Order(
                    userId = 1L,
                    items = listOf(
                        OrderItem(productId = 1L, productName = "에어맥스", productPrice = 139000, quantity = 1),
                    ),
                ),
            )
            orderJpaRepository.save(
                Order(
                    userId = 2L,
                    items = listOf(
                        OrderItem(productId = 2L, productName = "에어포스", productPrice = 119000, quantity = 1),
                    ),
                ),
            )

            val startAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(1)
            val endAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).plusDays(1)

            // act
            val result = orderService.getUserOrders(1L, startAt, endAt)

            // assert
            assertThat(result).hasSize(1)
        }

        @DisplayName("해당 기간 내 주문이 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyList_whenNoOrdersExistInDateRange() {
            // arrange
            orderJpaRepository.save(
                Order(
                    userId = 1L,
                    items = listOf(
                        OrderItem(productId = 1L, productName = "에어맥스", productPrice = 139000, quantity = 1),
                    ),
                ),
            )

            val startAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).plusDays(10)
            val endAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).plusDays(20)

            // act
            val result = orderService.getUserOrders(1L, startAt, endAt)

            // assert
            assertThat(result).isEmpty()
        }
    }

    @DisplayName("전체 주문 목록을 조회할 때, ")
    @Nested
    inner class GetOrders {
        @DisplayName("주문이 있으면, 페이지네이션된 목록을 반환한다.")
        @Test
        fun returnsOrderList_whenOrdersExist() {
            // arrange
            orderJpaRepository.save(
                Order(
                    userId = 1L,
                    items = listOf(
                        OrderItem(productId = 1L, productName = "에어맥스", productPrice = 139000, quantity = 1),
                    ),
                ),
            )
            orderJpaRepository.save(
                Order(
                    userId = 2L,
                    items = listOf(
                        OrderItem(productId = 2L, productName = "에어포스", productPrice = 119000, quantity = 1),
                    ),
                ),
            )

            // act
            val result = orderService.getOrders(PageRequest.of(0, 20))

            // assert
            assertThat(result.content).hasSize(2)
        }

        @DisplayName("주문이 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyList_whenNoOrdersExist() {
            // act
            val result = orderService.getOrders(PageRequest.of(0, 20))

            // assert
            assertThat(result.content).isEmpty()
        }
    }
}
