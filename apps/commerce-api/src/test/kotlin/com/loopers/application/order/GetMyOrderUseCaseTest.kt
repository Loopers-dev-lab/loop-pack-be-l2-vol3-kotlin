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
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class GetMyOrderUseCaseTest @Autowired constructor(
    private val getMyOrderUseCase: GetMyOrderUseCase,
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

    @DisplayName("내 주문 상세 조회")
    @Nested
    inner class Execute {

        @DisplayName("정상 조회 시 스냅샷 기반 데이터를 반환한다")
        @Test
        fun success() {
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId, name = "에어맥스", price = 15000)

            val orderInfo = createOrderUseCase.execute(
                OrderCommand.Create(
                    userId = userId,
                    items = listOf(
                        OrderCommand.Create.OrderLineItem(productId = productId, quantity = 2),
                    ),
                ),
            )

            val result = getMyOrderUseCase.execute(userId, orderInfo.orderId)

            assertAll(
                { assertThat(result.totalAmount).isEqualTo(30000) },
                { assertThat(result.items).hasSize(1) },
                { assertThat(result.items[0].productName).isEqualTo("에어맥스") },
                { assertThat(result.items[0].quantity).isEqualTo(2) },
            )
        }

        @DisplayName("존재하지 않는 주문이면 ORDER_NOT_FOUND")
        @Test
        fun failWhenOrderNotFound() {
            val userId = registerUser()

            val exception = assertThrows<CoreException> {
                getMyOrderUseCase.execute(userId, 999L)
            }

            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.ORDER_NOT_FOUND)
        }

        @DisplayName("타 유저 주문이면 ORDER_ACCESS_DENIED")
        @Test
        fun failWhenAccessDenied() {
            val userId = registerUser("user1")
            val otherUserId = registerUser("user2")
            val brandId = registerBrand()
            val productId = registerProduct(brandId)

            val orderInfo = createOrderUseCase.execute(
                OrderCommand.Create(
                    userId = userId,
                    items = listOf(
                        OrderCommand.Create.OrderLineItem(productId = productId, quantity = 1),
                    ),
                ),
            )

            val exception = assertThrows<CoreException> {
                getMyOrderUseCase.execute(otherUserId, orderInfo.orderId)
            }

            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.ORDER_ACCESS_DENIED)
        }
    }
}
