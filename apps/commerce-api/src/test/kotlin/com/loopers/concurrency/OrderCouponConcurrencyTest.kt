package com.loopers.concurrency

import com.loopers.application.user.order.OrderCreateCommand
import com.loopers.application.user.order.OrderCreateUseCase
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
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
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("주문 쿠폰 동시성 테스트")
@SpringBootTest
class OrderCouponConcurrencyTest
@Autowired
constructor(
    private val orderCreateUseCase: OrderCreateUseCase,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val brandRepository: BrandRepository,
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
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
    private var issuedCouponId: Long = 0

    @BeforeEach
    fun setUp() {
        val user = User.register(
            loginId = "couponconcurrency",
            rawPassword = "Password1!",
            name = "테스트",
            birthDate = LocalDate.of(1990, 1, 1),
            email = "coupon-concurrency@test.com",
            passwordHasher = passwordHasher,
        )
        userId = userRepository.save(user).id!!

        val brand = brandRepository.save(Brand.register(name = "테스트브랜드"), ADMIN)
        val activeBrand = brandRepository.save(brand.update("테스트브랜드", "ACTIVE"), ADMIN)

        val product = Product.register(
            name = "쿠폰 동시성 테스트 상품",
            regularPrice = Money(BigDecimal.valueOf(10000)),
            sellingPrice = Money(BigDecimal.valueOf(10000)),
            brandId = activeBrand.id!!,
        )
        val saved = productRepository.save(product, ADMIN)
        productId = productRepository.save(saved.activate(), ADMIN).id!!

        productStockRepository.save(
            ProductStock.create(productId = productId, initialQuantity = Quantity(10)),
            ADMIN,
        )

        val coupon = couponRepository.save(
            Coupon.register(
                name = "동시 주문 쿠폰",
                type = Coupon.Type.FIXED,
                discountValue = 1000,
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        )
        issuedCouponId = issuedCouponRepository.save(
            IssuedCoupon.issue(
                couponId = coupon.id!!,
                userId = userId,
                expiredAt = coupon.expiredAt,
            ),
        ).id!!
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    @DisplayName("동일 발급 쿠폰으로 동시 주문하면 1건만 성공한다")
    fun create_concurrent_sameIssuedCoupon_onlyOneSucceeds() {
        val threadCount = 20
        val executor = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val expectedFailCount = AtomicInteger(0)
        val unexpectedFailCount = AtomicInteger(0)

        try {
            repeat(threadCount) { idx ->
                executor.submit {
                    try {
                        readyLatch.countDown()
                        if (!startLatch.await(5, TimeUnit.SECONDS)) {
                            unexpectedFailCount.incrementAndGet()
                            return@submit
                        }

                        orderCreateUseCase.create(
                            OrderCreateCommand(
                                userId = userId,
                                idempotencyKey = "coupon-concurrency-$idx",
                                items = listOf(OrderCreateCommand.Item(productId = productId, quantity = 1)),
                                issuedCouponId = issuedCouponId,
                            ),
                        )
                        successCount.incrementAndGet()
                    } catch (e: CoreException) {
                        if (e.errorType == ErrorType.ISSUED_COUPON_CONFLICT ||
                            e.errorType == ErrorType.ISSUED_COUPON_ALREADY_USED
                        ) {
                            expectedFailCount.incrementAndGet()
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

            assertThat(readyLatch.await(10, TimeUnit.SECONDS))
                .withFailMessage("readyLatch timeout: 스레드 준비 실패")
                .isTrue()
            startLatch.countDown()
            assertThat(doneLatch.await(30, TimeUnit.SECONDS))
                .withFailMessage("doneLatch timeout: 스레드 완료 대기 실패")
                .isTrue()
        } finally {
            executor.shutdownNow()
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS))
                .withFailMessage("executor termination timeout: worker thread 종료 실패")
                .isTrue()
        }

        val finalStock = productStockRepository.findByProductId(productId)!!
        val issuedCoupon = issuedCouponRepository.findById(issuedCouponId)!!

        assertThat(successCount.get()).isEqualTo(1)
        assertThat(expectedFailCount.get()).isEqualTo(threadCount - 1)
        assertThat(unexpectedFailCount.get()).isEqualTo(0)
        assertThat(orderJpaRepository.count()).isEqualTo(1L)
        assertThat(finalStock.quantity.value).isEqualTo(9)
        assertThat(issuedCoupon.status).isEqualTo(IssuedCoupon.Status.USED)
    }
}
