package com.loopers.concurrency

import com.loopers.application.user.order.OrderCreateCommand
import com.loopers.application.user.order.OrderCreateUseCase
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("주문 멱등성 키 동시성 테스트")
@SpringBootTest
class OrderIdempotencyConcurrencyTest
@Autowired
constructor(
    private val orderCreateUseCase: OrderCreateUseCase,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val brandRepository: BrandRepository,
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
    private val orderJpaRepository: OrderJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ADMIN = "loopers.admin"
    }

    private var userId: Long = 0
    private var productId: Long = 0

    @BeforeEach
    fun setUp() {
        val user = User.register(
            loginId = "idempotencytest",
            rawPassword = "Password1!",
            name = "테스트",
            birthDate = LocalDate.of(1990, 1, 1),
            email = "idempotency@test.com",
            passwordHasher = passwordHasher,
        )
        userId = userRepository.save(user).id!!

        val brand = brandRepository.save(Brand.register(name = "테스트브랜드"), ADMIN)
        val activeBrand = brandRepository.save(brand.update("테스트브랜드", "ACTIVE"), ADMIN)

        val product = Product.register(
            name = "멱등성 테스트 상품",
            regularPrice = Money(BigDecimal.valueOf(10000)),
            sellingPrice = Money(BigDecimal.valueOf(10000)),
            brandId = activeBrand.id!!,
        )
        val saved = productRepository.save(product, ADMIN)
        productId = productRepository.save(saved.activate(), ADMIN).id!!

        productStockRepository.save(
            ProductStock.create(productId = productId, initialQuantity = Quantity(100)),
            ADMIN,
        )
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("동일한 멱등성 키로 10스레드 동시 주문 → 1건만 성공, 나머지는 멱등성 키 중복 실패")
    fun create_sameIdempotencyKey_onlyOneSucceeds() {
        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val idempotencyFailCount = AtomicInteger(0)
        val unexpectedFailCount = AtomicInteger(0)

        repeat(threadCount) {
            executor.submit {
                try {
                    readyLatch.countDown()
                    startLatch.await()

                    orderCreateUseCase.create(
                        OrderCreateCommand(
                            userId = userId,
                            idempotencyKey = "same-key",
                            items = listOf(OrderCreateCommand.Item(productId, 1)),
                        ),
                    )
                    successCount.incrementAndGet()
                } catch (e: CoreException) {
                    if (e.errorType == ErrorType.ORDER_IDEMPOTENCY_KEY_DUPLICATE) {
                        idempotencyFailCount.incrementAndGet()
                    } else {
                        unexpectedFailCount.incrementAndGet()
                    }
                } catch (e: Exception) {
                    unexpectedFailCount.incrementAndGet()
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        readyLatch.await()
        startLatch.countDown()
        doneLatch.await()
        executor.shutdown()

        val finalStock = productStockRepository.findByProductId(productId)!!

        assertThat(successCount.get()).isEqualTo(1)
        assertThat(idempotencyFailCount.get() + successCount.get()).isEqualTo(threadCount)
        assertThat(unexpectedFailCount.get()).isEqualTo(0)
        assertThat(orderJpaRepository.count()).isEqualTo(1L)
        assertThat(finalStock.quantity.value).isEqualTo(99)
    }
}
