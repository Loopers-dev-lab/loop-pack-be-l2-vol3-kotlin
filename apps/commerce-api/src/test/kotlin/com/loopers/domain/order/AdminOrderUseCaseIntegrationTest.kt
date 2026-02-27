package com.loopers.domain.order

import com.loopers.application.order.AdminGetOrderUseCase
import com.loopers.application.order.AdminGetOrdersUseCase
import com.loopers.application.order.ListOrdersCriteria
import com.loopers.domain.user.Email
import com.loopers.domain.user.Password
import com.loopers.domain.user.UserModel
import com.loopers.domain.user.Username
import com.loopers.infrastructure.order.OrderItemJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import java.math.BigDecimal
import java.time.ZonedDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AdminOrderUseCaseIntegrationTest @Autowired constructor(
    private val adminGetOrdersUseCase: AdminGetOrdersUseCase,
    private val adminGetOrderUseCase: AdminGetOrderUseCase,
    private val orderJpaRepository: OrderJpaRepository,
    private val orderItemJpaRepository: OrderItemJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val DEFAULT_USERNAME = "testuser"
        private const val DEFAULT_PASSWORD = "password1234"
        private const val DEFAULT_NAME = "테스트유저"
        private const val DEFAULT_EMAIL = "test@example.com"
        private val DEFAULT_BIRTH_DATE: ZonedDateTime = ZonedDateTime.parse("1990-01-01T00:00:00+09:00")
        private val DEFAULT_TOTAL_PRICE = BigDecimal("258000")
        private const val DEFAULT_PRODUCT_NAME = "에어맥스 90"
        private const val DEFAULT_QUANTITY = 2
        private val DEFAULT_ITEM_PRICE = BigDecimal("129000")
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createUser(username: String = DEFAULT_USERNAME): UserModel {
        return userJpaRepository.save(
            UserModel(
                username = Username.of(username),
                password = Password.of(DEFAULT_PASSWORD, DEFAULT_BIRTH_DATE),
                name = DEFAULT_NAME,
                email = Email.of(DEFAULT_EMAIL),
                birthDate = DEFAULT_BIRTH_DATE,
            ),
        )
    }

    private fun createOrder(userId: Long, totalPrice: BigDecimal = DEFAULT_TOTAL_PRICE): OrderModel {
        return orderJpaRepository.save(
            OrderModel(userId = userId, totalPrice = totalPrice),
        )
    }

    private fun createOrderItem(
        orderId: Long,
        productId: Long = 1L,
        productName: String = DEFAULT_PRODUCT_NAME,
        quantity: Int = DEFAULT_QUANTITY,
        price: BigDecimal = DEFAULT_ITEM_PRICE,
    ): OrderItemModel {
        return orderItemJpaRepository.save(
            OrderItemModel(
                orderId = orderId,
                productId = productId,
                productName = productName,
                quantity = quantity,
                price = price,
            ),
        )
    }

    @DisplayName("주문 목록 조회")
    @Nested
    inner class ListOrders {

        @DisplayName("주문이 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyListWhenNoOrdersExist() {
            // arrange
            val criteria = ListOrdersCriteria(page = 0, size = 10)

            // act
            val result = adminGetOrdersUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).isEmpty() },
                { assertThat(result.hasNext).isFalse() },
            )
        }

        @DisplayName("주문이 존재하면, 목록을 반환한다.")
        @Test
        fun returnsOrdersWhenOrdersExist() {
            // arrange
            val user = createUser()
            createOrder(userId = user.id)
            createOrder(userId = user.id, totalPrice = BigDecimal("100000"))
            val criteria = ListOrdersCriteria(page = 0, size = 10)

            // act
            val result = adminGetOrdersUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.content[0].username).isEqualTo(DEFAULT_USERNAME) },
                { assertThat(result.hasNext).isFalse() },
            )
        }

        @DisplayName("페이지 크기보다 주문이 많으면, hasNext가 true이다.")
        @Test
        fun returnsHasNextTrueWhenMoreOrdersExist() {
            // arrange
            val user = createUser()
            createOrder(userId = user.id)
            createOrder(userId = user.id, totalPrice = BigDecimal("100000"))
            createOrder(userId = user.id, totalPrice = BigDecimal("200000"))
            val criteria = ListOrdersCriteria(page = 0, size = 2)

            // act
            val result = adminGetOrdersUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.hasNext).isTrue() },
            )
        }
    }

    @DisplayName("주문 상세 조회")
    @Nested
    inner class GetOrder {

        @DisplayName("유효한 주문이면, 주문 상세 정보를 반환한다.")
        @Test
        fun returnsOrderDetailWhenOrderExists() {
            // arrange
            val user = createUser()
            val order = createOrder(userId = user.id)
            createOrderItem(orderId = order.id, productId = 1L, productName = "에어맥스 90")
            createOrderItem(orderId = order.id, productId = 2L, productName = "에어포스 1")

            // act
            val result = adminGetOrderUseCase.execute(order.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(order.id) },
                { assertThat(result.userId).isEqualTo(user.id) },
                { assertThat(result.username).isEqualTo(DEFAULT_USERNAME) },
                { assertThat(result.totalPrice).isEqualByComparingTo(DEFAULT_TOTAL_PRICE) },
                { assertThat(result.items).hasSize(2) },
            )
        }

        @DisplayName("존재하지 않는 주문이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenOrderDoesNotExist() {
            // arrange
            val nonExistentId = 999L

            // act
            val result = assertThrows<CoreException> {
                adminGetOrderUseCase.execute(nonExistentId)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
