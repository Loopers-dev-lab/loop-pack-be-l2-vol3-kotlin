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
class GetOrderDetailUseCaseTest @Autowired constructor(
    private val getOrderDetailUseCase: GetOrderDetailUseCase,
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

    private fun registerProduct(brandId: Long, name: String = "테스트 상품", price: Long = 10000): Long {
        return registerProductUseCase.execute(
            ProductCommand.Register(
                brandId = brandId,
                name = name,
                description = "상품 설명",
                price = price,
                stock = 100,
                imageUrl = "https://example.com/image.jpg",
            ),
        ).id
    }

    @DisplayName("어드민 주문 상세 조회")
    @Nested
    inner class Execute {

        @DisplayName("정상 조회 시 주문자 정보를 포함하여 반환한다")
        @Test
        fun success() {
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId, name = "에어맥스", price = 15000)

            val orderInfo = createOrderUseCase.execute(
                OrderCommand.Create(
                    userId = userId,
                    items = listOf(OrderCommand.Create.OrderLineItem(productId = productId, quantity = 2)),
                ),
            )

            val result = getOrderDetailUseCase.execute(orderInfo.orderId)

            assertAll(
                { assertThat(result.userId).isEqualTo(userId) },
                { assertThat(result.totalAmount).isEqualTo(30000) },
                { assertThat(result.items).hasSize(1) },
            )
        }

        @DisplayName("존재하지 않는 주문이면 ORDER_NOT_FOUND")
        @Test
        fun failWhenOrderNotFound() {
            val exception = assertThrows<CoreException> {
                getOrderDetailUseCase.execute(999L)
            }

            assertThat(exception.errorCode).isEqualTo(OrderErrorCode.ORDER_NOT_FOUND)
        }
    }
}
