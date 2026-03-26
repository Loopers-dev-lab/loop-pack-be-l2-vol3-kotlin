package com.loopers.concurrency

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.order.CreateOrderUseCase
import com.loopers.application.order.OrderCommand
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.application.user.UserCommand
import com.loopers.domain.product.ProductStockRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.LocalDate

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
class StockConcurrencyTest @Autowired constructor(
    private val createOrderUseCase: CreateOrderUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val registerProductUseCase: RegisterProductUseCase,
    private val productStockRepository: ProductStockRepository,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerUser(loginId: String): Long {
        registerUserUseCase.execute(
            UserCommand.Register(
                loginId = loginId,
                rawPassword = "Test123!",
                name = "테스트",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "$loginId@example.com",
            ),
        )
        return userJpaRepository.findByLoginId(loginId)!!.id
    }

    private fun registerBrand(): Long {
        return registerBrandUseCase.execute(BrandCommand.Register(name = "테스트브랜드")).id
    }

    private fun registerProduct(brandId: Long, stock: Int): Long {
        return registerProductUseCase.execute(
            ProductCommand.Register(
                brandId = brandId,
                name = "테스트상품",
                description = "설명",
                price = 1000,
                stock = stock,
                imageUrl = "https://example.com/image.jpg",
            ),
        ).id
    }

    @DisplayName("동시 주문이 들어와도 모두 성공하고 재고는 차감되지 않는다 (결제 성공 시 차감)")
    @Test
    fun allOrdersSucceedWithoutStockDeduction() {
        // arrange
        val threadCount = 20
        val initialStock = 10
        val userIds = (1..threadCount).map { registerUser("user$it") }
        val brandId = registerBrand()
        val productId = registerProduct(brandId, stock = initialStock)

        // act
        val actions = userIds.map { userId ->
            {
                createOrderUseCase.execute(
                    OrderCommand.Create(
                        userId = userId,
                        items = listOf(
                            OrderCommand.Create.OrderLineItem(productId = productId, quantity = 1),
                        ),
                    ),
                )
            }
        }
        val results = ConcurrencyTestHelper.executeConcurrently(actions)

        val successes = results.filter { it.isSuccess }

        // assert
        val productStock = productStockRepository.findByProductId(productId)!!

        assertAll(
            { assertThat(successes).`as`("주문은 모두 성공해야 한다").hasSize(threadCount) },
            { assertThat(productStock.stock.quantity).`as`("재고는 변하지 않아야 한다").isEqualTo(initialStock) },
        )
    }
}
