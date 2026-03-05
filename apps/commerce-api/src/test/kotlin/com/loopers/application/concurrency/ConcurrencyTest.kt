package com.loopers.application.concurrency

import com.loopers.application.brand.RegisterBrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.coupon.IssueCouponUseCase
import com.loopers.application.coupon.RegisterCouponCommand
import com.loopers.application.coupon.RegisterCouponUseCase
import com.loopers.application.like.AddLikeUseCase
import com.loopers.application.order.CreateOrderCommand
import com.loopers.application.order.CreateOrderUseCase
import com.loopers.application.order.OrderItemCommand
import com.loopers.application.product.RegisterProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserCommand
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.domain.product.ProductRepository
import com.loopers.testcontainers.MySqlTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
@Sql(
    statements = [
        "DELETE FROM order_item",
        "DELETE FROM orders",
        "DELETE FROM user_coupon",
        "DELETE FROM coupon",
        "DELETE FROM likes",
        "DELETE FROM product_image",
        "DELETE FROM product",
        "DELETE FROM brand",
        "DELETE FROM users",
    ],
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class ConcurrencyTest {

    @Autowired
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @Autowired
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @Autowired
    private lateinit var registerProductUseCase: RegisterProductUseCase

    @Autowired
    private lateinit var createOrderUseCase: CreateOrderUseCase

    @Autowired
    private lateinit var registerCouponUseCase: RegisterCouponUseCase

    @Autowired
    private lateinit var issueCouponUseCase: IssueCouponUseCase

    @Autowired
    private lateinit var addLikeUseCase: AddLikeUseCase

    @Autowired
    private lateinit var productRepository: ProductRepository

    private var brandId: Long = 0

    @BeforeEach
    fun setUp() {
        brandId = registerBrandUseCase.register(
            RegisterBrandCommand(name = "테스트브랜드", description = null, logoUrl = null),
        )
    }

    @Nested
    inner class CouponConcurrency {
        @Test
        fun `동일 쿠폰으로 동시에 2건 주문 시 1건만 성공해야 한다`() {
            val userId = registerUser("cpnuser1")
            val productId = registerProduct(stock = 100)
            val couponId = registerCouponUseCase.register(createCouponCommand())
            val userCouponId = issueCouponUseCase.issue(userId, couponId)

            val threadCount = 2
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            repeat(threadCount) {
                executor.submit {
                    try {
                        createOrderUseCase.create(
                            userId,
                            CreateOrderCommand(
                                items = listOf(OrderItemCommand(productId = productId, quantity = 1)),
                                couponId = userCouponId,
                            ),
                        )
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

            assertThat(successCount.get()).isEqualTo(1)
            assertThat(failCount.get()).isEqualTo(1)
        }
    }

    @Nested
    inner class StockConcurrency {
        @Test
        fun `재고 100개 상품에 10개 스레드가 수량 10씩 주문하면 모두 성공하고 재고 0이 된다`() {
            val productId = registerProduct(stock = 100)
            val threadCount = 10

            val userIds = (1..threadCount).map { registerUser("stku$it") }
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            userIds.forEach { userId ->
                executor.submit {
                    try {
                        createOrderUseCase.create(
                            userId,
                            CreateOrderCommand(
                                items = listOf(OrderItemCommand(productId = productId, quantity = 10)),
                            ),
                        )
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

            assertThat(successCount.get()).isEqualTo(10)
            assertThat(failCount.get()).isEqualTo(0)

            val product = productRepository.findById(productId)!!
            assertThat(product.stock.quantity).isEqualTo(0)
        }

        @Test
        fun `재고 5개 상품에 10개 스레드가 수량 1씩 주문하면 5건만 성공한다`() {
            val productId = registerProduct(stock = 5)
            val threadCount = 10

            val userIds = (1..threadCount).map { registerUser("limu$it") }
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            userIds.forEach { userId ->
                executor.submit {
                    try {
                        createOrderUseCase.create(
                            userId,
                            CreateOrderCommand(
                                items = listOf(OrderItemCommand(productId = productId, quantity = 1)),
                            ),
                        )
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

            assertThat(successCount.get()).isEqualTo(5)
            assertThat(failCount.get()).isEqualTo(5)

            val product = productRepository.findById(productId)!!
            assertThat(product.stock.quantity).isEqualTo(0)
        }
    }

    @Nested
    inner class LikeConcurrency {
        @Test
        fun `같은 사용자가 10번 동시에 좋아요하면 likeCount는 1이어야 한다`() {
            val userId = registerUser("lku1")
            val productId = registerProduct(stock = 100)

            val threadCount = 10
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)

            repeat(threadCount) {
                executor.submit {
                    try {
                        addLikeUseCase.add(userId, productId)
                    } catch (_: Exception) {
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await()
            executor.shutdown()

            val product = productRepository.findById(productId)!!
            assertThat(product.likeCount).isEqualTo(1)
        }

        @Test
        fun `10명이 동시에 좋아요하면 likeCount는 10이어야 한다`() {
            val productId = registerProduct(stock = 100)
            val threadCount = 10

            val userIds = (1..threadCount).map { registerUser("mlik$it") }
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)

            userIds.forEach { userId ->
                executor.submit {
                    try {
                        addLikeUseCase.add(userId, productId)
                    } catch (_: Exception) {
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await()
            executor.shutdown()

            val product = productRepository.findById(productId)!!
            assertThat(product.likeCount).isEqualTo(10)
        }
    }

    private fun registerUser(loginId: String): Long {
        return registerUserUseCase.register(
            RegisterUserCommand(
                loginId = loginId,
                password = "Password1!",
                name = "테스트",
                birthDate = "1993-04-01",
                email = "$loginId@example.com",
                gender = "MALE",
            ),
        )
    }

    private fun registerProduct(stock: Int): Long {
        return registerProductUseCase.register(
            RegisterProductCommand(
                brandId = brandId,
                name = "테스트상품",
                description = "설명",
                price = 10000L,
                stock = stock,
                thumbnailUrl = null,
                images = emptyList(),
            ),
        )
    }

    private fun createCouponCommand() = RegisterCouponCommand(
        name = "테스트쿠폰",
        discountType = "FIXED",
        discountValue = 3000L,
        minOrderAmount = 0L,
        maxIssueCount = 100,
        expiredAt = ZonedDateTime.now().plusDays(30),
    )
}
