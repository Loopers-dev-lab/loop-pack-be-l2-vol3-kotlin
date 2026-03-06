package com.loopers.concurrency

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.order.IdempotencyKey
import com.loopers.domain.order.OrderDomainService
import com.loopers.domain.order.OrderRepository
import com.loopers.domain.order.OrderSnapshot
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("ProductStock 동시성 테스트")
@SpringBootTest
class ProductStockConcurrencyTest
@Autowired
constructor(
    private val productStockRepository: ProductStockRepository,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
    private val databaseCleanUp: DatabaseCleanUp,
    private val txManager: PlatformTransactionManager,
) {
    companion object {
        private const val ADMIN = "loopers.admin"
    }

    private var productId: Long = 0
    private var brandId: Long = 0
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        val user = User.register(
            loginId = "stocktest",
            rawPassword = "Password1!",
            name = "테스트",
            birthDate = LocalDate.of(1990, 1, 1),
            email = "stock@test.com",
            passwordHasher = passwordHasher,
        )
        userId = userRepository.save(user).id!!

        val brand = brandRepository.save(Brand.register(name = "테스트브랜드"), ADMIN)
        val activeBrand = brandRepository.save(brand.update("테스트브랜드", "ACTIVE"), ADMIN)
        brandId = activeBrand.id!!

        val product = Product.register(
            name = "동시성 테스트 상품",
            regularPrice = Money(BigDecimal.valueOf(10000)),
            sellingPrice = Money(BigDecimal.valueOf(10000)),
            brandId = brandId,
        )
        val saved = productRepository.save(product, ADMIN)
        productId = productRepository.save(saved.activate(), ADMIN).id!!

        productStockRepository.save(
            ProductStock.create(productId = productId, initialQuantity = Quantity(10)),
            ADMIN,
        )
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("재고 10개, 10스레드 동시 주문 → 재고 정확히 0으로 차감")
    fun decrease_concurrent_exactlyZero() {
        val threadCount = 10
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val txTemplate = TransactionTemplate(txManager)

        repeat(threadCount) { idx ->
            executor.submit {
                try {
                    txTemplate.execute {
                        val stocks = productStockRepository.findAllByProductIdInWithLock(
                            listOf(productId),
                        )
                        val stock = stocks.first()
                        val decreased = stock.decrease(Quantity(1))
                        productStockRepository.saveAll(listOf(decreased))

                        val snapshot = OrderSnapshot(
                            productId = productId,
                            productName = "동시성 테스트 상품",
                            brandId = brandId,
                            brandName = "테스트브랜드",
                            regularPrice = Money(BigDecimal.valueOf(10000)),
                            sellingPrice = Money(BigDecimal.valueOf(10000)),
                            thumbnailUrl = null,
                        )
                        val result = OrderDomainService.createOrder(
                            userId = userId,
                            idempotencyKey = IdempotencyKey("stock-test-$idx"),
                            orderItemRequests = listOf(
                                OrderDomainService.OrderItemRequest(
                                    productStock = stock,
                                    snapshot = snapshot,
                                    quantity = Quantity(1),
                                ),
                            ),
                        )
                        orderRepository.save(result.order)
                    }
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        val finalStock = productStockRepository.findByProductId(productId)!!
        assertThat(successCount.get()).isEqualTo(10)
        assertThat(finalStock.quantity.value).isEqualTo(0)
    }
}
