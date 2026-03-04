package com.loopers.concurrency

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.order.CreateOrderUseCase
import com.loopers.application.order.OrderCommand
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.application.user.UserCommand
import com.loopers.domain.product.ProductRepository
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
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
class StockConcurrencyTest @Autowired constructor(
    private val createOrderUseCase: CreateOrderUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val registerProductUseCase: RegisterProductUseCase,
    private val productRepository: ProductRepository,
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

    @DisplayName("재고보다 많은 주문이 동시에 들어오면 재고만큼만 성공하고 나머지는 실패해야 한다")
    @Test
    fun onlyStockAmountOrdersShouldSucceedUnderConcurrency() {
        // arrange
        val threadCount = 20
        val initialStock = 10
        val userIds = (1..threadCount).map { registerUser("user$it") }
        val brandId = registerBrand()
        val productId = registerProduct(brandId, stock = initialStock)

        val executorService = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)

        // act
        val futures = userIds.map { userId ->
            executorService.submit(Callable {
                try {
                    readyLatch.countDown()
                    startLatch.await()
                    createOrderUseCase.execute(
                        OrderCommand.Create(
                            userId = userId,
                            items = listOf(
                                OrderCommand.Create.OrderLineItem(productId = productId, quantity = 1),
                            ),
                        ),
                    )
                    true
                } catch (e: Exception) {
                    false
                }
            })
        }

        readyLatch.await()
        startLatch.countDown()

        val results = futures.map { it.get() }
        val successCount = results.count { it }
        val failCount = results.count { !it }
        executorService.shutdown()

        // assert
        val product = productRepository.findByIdOrNull(productId)!!

        assertAll(
            { assertThat(successCount).`as`("재고 수만큼만 성공해야 한다").isEqualTo(initialStock) },
            { assertThat(failCount).`as`("초과 주문은 실패해야 한다").isEqualTo(threadCount - initialStock) },
            { assertThat(product.stock.quantity).`as`("재고가 정확히 0이어야 한다").isEqualTo(0) },
        )
    }
}
