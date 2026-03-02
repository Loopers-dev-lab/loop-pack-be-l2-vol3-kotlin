package com.loopers.application.order

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.application.user.UserCommand
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.OrderErrorCode
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class GetMyOrdersUseCaseTest @Autowired constructor(
    private val getMyOrdersUseCase: GetMyOrdersUseCase,
    private val createOrderUseCase: CreateOrderUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val registerProductUseCase: RegisterProductUseCase,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerUser(loginId: String = "testuser"): Long {
        registerUserUseCase.execute(
            UserCommand.Register(
                loginId = loginId,
                rawPassword = "Test123!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            ),
        )
        return userJpaRepository.findByLoginId(loginId)!!.id
    }

    private fun registerBrand(name: String = "나이키"): Long {
        return registerBrandUseCase.execute(BrandCommand.Register(name = name)).id
    }

    private fun registerProduct(brandId: Long, name: String = "테스트 상품", price: Long = 10000, stock: Int = 100): Long {
        return registerProductUseCase.execute(
            ProductCommand.Register(
                brandId = brandId,
                name = name,
                description = "상품 설명",
                price = price,
                stock = stock,
                imageUrl = "https://example.com/image.jpg",
            ),
        ).id
    }

    private fun createOrder(userId: Long, productId: Long, quantity: Int = 1) {
        createOrderUseCase.execute(
            OrderCommand.Create(
                userId = userId,
                items = listOf(OrderCommand.Create.OrderLineItem(productId = productId, quantity = quantity)),
            ),
        )
    }

    @DisplayName("내 주문 목록 조회")
    @Nested
    inner class Execute {

        @DisplayName("기간 내 주문 목록을 반환한다")
        @Test
        fun success() {
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId)
            createOrder(userId, productId, quantity = 2)

            val today = LocalDate.now()
            val result = getMyOrdersUseCase.execute(userId, today.minusDays(1), today, 0, 20)

            assertThat(result.content).hasSize(1)
        }

        @DisplayName("주문의 항목 수를 정확히 반환한다")
        @Test
        fun returnsItemCount() {
            val userId = registerUser()
            val brandId = registerBrand()
            val product1 = registerProduct(brandId, name = "상품1")
            val product2 = registerProduct(brandId, name = "상품2")

            createOrderUseCase.execute(
                OrderCommand.Create(
                    userId = userId,
                    items = listOf(
                        OrderCommand.Create.OrderLineItem(productId = product1, quantity = 1),
                        OrderCommand.Create.OrderLineItem(productId = product2, quantity = 2),
                    ),
                ),
            )

            val today = LocalDate.now()
            val result = getMyOrdersUseCase.execute(userId, today.minusDays(1), today, 0, 20)

            assertThat(result.content[0].itemCount).isEqualTo(2)
        }

        @DisplayName("주문이 없으면 빈 목록을 반환한다")
        @Test
        fun returnsEmpty() {
            val userId = registerUser()

            val today = LocalDate.now()
            val result = getMyOrdersUseCase.execute(userId, today.minusDays(1), today, 0, 20)

            assertThat(result.content).isEmpty()
        }

        @DisplayName("startDate가 endDate보다 뒤면 INVALID_ORDER_PERIOD")
        @Test
        fun failWhenStartDateAfterEndDate() {
            val userId = registerUser()

            val exception = assertThrows<CoreException> {
                getMyOrdersUseCase.execute(userId, LocalDate.now(), LocalDate.now().minusDays(1), 0, 20)
            }

            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.INVALID_ORDER_PERIOD)
        }

        @DisplayName("365일 초과 기간이면 INVALID_ORDER_PERIOD")
        @Test
        fun failWhenPeriodExceeds365Days() {
            val userId = registerUser()

            val exception = assertThrows<CoreException> {
                getMyOrdersUseCase.execute(userId, LocalDate.now().minusDays(366), LocalDate.now(), 0, 20)
            }

            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.INVALID_ORDER_PERIOD)
        }
    }
}
