package com.loopers.application.concurrency

import com.loopers.application.brand.RegisterBrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.coupon.IssueCouponUseCase
import com.loopers.application.coupon.RegisterCouponCommand
import com.loopers.application.coupon.RegisterCouponUseCase
import com.loopers.application.like.AddLikeUseCase
import com.loopers.application.like.RemoveLikeUseCase
import com.loopers.application.order.CreateOrderCommand
import com.loopers.application.order.CreateOrderUseCase
import com.loopers.application.order.OrderItemCommand
import com.loopers.application.product.RegisterProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserCommand
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.domain.coupon.CouponException
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.UserCouponRepository
import com.loopers.domain.product.ProductException
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
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
    private lateinit var removeLikeUseCase: RemoveLikeUseCase

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var userCouponRepository: UserCouponRepository

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
        fun `동일 쿠폰으로 동시에 100건 주문 시 1건만 성공해야 한다`() {
            val userId = registerUser("cpnuser1")
            val productId = registerProduct(stock = 1000)
            val couponId = registerCouponUseCase.register(createCouponCommand())
            val userCouponId = issueCouponUseCase.issue(userId, couponId)

            val result = executeConcurrently(100) {
                createOrderUseCase.create(
                    userId,
                    CreateOrderCommand(
                        items = listOf(OrderItemCommand(productId = productId, quantity = 1)),
                        couponId = userCouponId,
                    ),
                )
            }

            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.failures).hasSize(99)
            assertThat(result.failures).allSatisfy { e ->
                assertThat(e).isInstanceOf(CouponException::class.java)
            }

            val userCoupon = userCouponRepository.findById(userCouponId)!!
            assertThat(userCoupon.status).isEqualTo(CouponStatus.USED)
        }
    }

    @Nested
    inner class StockConcurrency {
        @Test
        fun `재고 1000개 상품에 100개 스레드가 수량 10씩 주문하면 모두 성공하고 재고 0이 된다`() {
            val productId = registerProduct(stock = 1000)
            val userIds = (1..100).map { registerUser("stku$it") }

            val result = executeConcurrently(userIds) { userId ->
                createOrderUseCase.create(
                    userId,
                    CreateOrderCommand(
                        items = listOf(OrderItemCommand(productId = productId, quantity = 10)),
                    ),
                )
            }

            assertThat(result.successCount).isEqualTo(100)
            assertThat(result.failures).isEmpty()

            val product = productRepository.findById(productId)!!
            assertThat(product.stock.quantity).isEqualTo(0)
        }

        @Test
        fun `재고 50개 상품에 100개 스레드가 수량 1씩 주문하면 50건만 성공한다`() {
            val productId = registerProduct(stock = 50)
            val userIds = (1..100).map { registerUser("limu$it") }

            val result = executeConcurrently(userIds) { userId ->
                createOrderUseCase.create(
                    userId,
                    CreateOrderCommand(
                        items = listOf(OrderItemCommand(productId = productId, quantity = 1)),
                    ),
                )
            }

            assertThat(result.successCount).isEqualTo(50)
            assertThat(result.failures).hasSize(50)
            assertThat(result.failures).allSatisfy { e ->
                assertThat(e).isInstanceOf(ProductException::class.java)
            }

            val product = productRepository.findById(productId)!!
            assertThat(product.stock.quantity).isEqualTo(0)
        }
    }

    @Nested
    inner class LikeConcurrency {
        @Test
        fun `같은 사용자가 100번 동시에 좋아요하면 likeCount는 1이어야 한다`() {
            val userId = registerUser("lku1")
            val productId = registerProduct(stock = 100)

            val result = executeConcurrently(100) {
                addLikeUseCase.add(userId, productId)
            }

            assertThat(result.successCount + result.failures.size).isEqualTo(100)

            val product = productRepository.findById(productId)!!
            assertThat(product.likeCount).isEqualTo(1)
        }

        @Test
        fun `100명이 동시에 좋아요하면 likeCount는 100이어야 한다`() {
            val productId = registerProduct(stock = 100)
            val userIds = (1..100).map { registerUser("mlik$it") }

            val result = executeConcurrently(userIds) { userId ->
                addLikeUseCase.add(userId, productId)
            }

            assertThat(result.successCount).isEqualTo(100)
            assertThat(result.failures).isEmpty()

            val product = productRepository.findById(productId)!!
            assertThat(product.likeCount).isEqualTo(100)
        }

        @Test
        fun `같은 사용자가 100번 동시에 좋아요 취소하면 likeCount는 0이어야 한다`() {
            val userId = registerUser("rlk1")
            val productId = registerProduct(stock = 100)
            addLikeUseCase.add(userId, productId)

            val result = executeConcurrently(100) {
                removeLikeUseCase.remove(userId, productId)
            }

            assertThat(result.successCount + result.failures.size).isEqualTo(100)

            val product = productRepository.findById(productId)!!
            assertThat(product.likeCount).isEqualTo(0)
        }

        @Test
        fun `100명이 좋아요 후 동시에 취소하면 likeCount는 0이어야 한다`() {
            val productId = registerProduct(stock = 100)
            val userIds = (1..100).map { registerUser("rmlk$it") }

            userIds.forEach { userId -> addLikeUseCase.add(userId, productId) }

            val product = productRepository.findById(productId)!!
            assertThat(product.likeCount).isEqualTo(100)

            val result = executeConcurrently(userIds) { userId ->
                removeLikeUseCase.remove(userId, productId)
            }

            assertThat(result.successCount).isEqualTo(100)
            assertThat(result.failures).isEmpty()

            val updated = productRepository.findById(productId)!!
            assertThat(updated.likeCount).isEqualTo(0)
        }
    }

    private data class ConcurrentResult(
        val successCount: Int,
        val failures: List<Exception>,
    )

    private fun executeConcurrently(threadCount: Int, action: () -> Unit): ConcurrentResult {
        return executeConcurrently((1..threadCount).toList()) { action() }
    }

    private fun <T> executeConcurrently(items: List<T>, action: (T) -> Unit): ConcurrentResult {
        val threadCount = items.size
        val executor = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failures = CopyOnWriteArrayList<Exception>()

        try {
            items.forEach { item ->
                executor.submit {
                    try {
                        readyLatch.countDown()
                        startLatch.await()
                        action(item)
                        successCount.incrementAndGet()
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    } catch (e: Exception) {
                        failures.add(e)
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            readyLatch.await()
            startLatch.countDown()

            val completed = doneLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            assertThat(completed)
                .withFailMessage("동시성 테스트가 ${TIMEOUT_SECONDS}초 내에 완료되지 않았습니다.")
                .isTrue()
        } finally {
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)
        }

        return ConcurrentResult(successCount.get(), failures.toList())
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

    companion object {
        private const val TIMEOUT_SECONDS = 60L
    }
}
