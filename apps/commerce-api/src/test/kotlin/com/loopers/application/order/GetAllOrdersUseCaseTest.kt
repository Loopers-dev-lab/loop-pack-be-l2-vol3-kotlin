package com.loopers.application.order

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.application.user.UserCommand
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class GetAllOrdersUseCaseTest @Autowired constructor(
    private val getAllOrdersUseCase: GetAllOrdersUseCase,
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

    private fun registerProduct(brandId: Long, name: String = "테스트 상품", stock: Int = 100): Long {
        return registerProductUseCase.execute(
            ProductCommand.Register(
                brandId = brandId,
                name = name,
                description = "상품 설명",
                price = 10000,
                stock = stock,
                imageUrl = "https://example.com/image.jpg",
            ),
        ).id
    }

    @DisplayName("전체 주문 목록 조회")
    @Nested
    inner class Execute {

        @DisplayName("전체 주문을 페이지네이션으로 반환한다")
        @Test
        fun success() {
            val userId = registerUser()
            val brandId = registerBrand()
            val product1 = registerProduct(brandId, name = "상품1")
            val product2 = registerProduct(brandId, name = "상품2")

            createOrderUseCase.execute(
                OrderCommand.Create(userId = userId, items = listOf(OrderCommand.Create.OrderLineItem(productId = product1, quantity = 1))),
            )
            createOrderUseCase.execute(
                OrderCommand.Create(userId = userId, items = listOf(OrderCommand.Create.OrderLineItem(productId = product2, quantity = 1))),
            )

            val result = getAllOrdersUseCase.execute(0, 20)

            assertThat(result.content).hasSize(2)
        }

        @DisplayName("주문이 없으면 빈 목록을 반환한다")
        @Test
        fun returnsEmpty() {
            val result = getAllOrdersUseCase.execute(0, 20)

            assertThat(result.content).isEmpty()
        }
    }
}
