package com.loopers.domain.order

import com.loopers.application.catalog.AdminRegisterProductUseCase
import com.loopers.application.catalog.RegisterProductCriteria
import com.loopers.application.coupon.IssueCouponCriteria
import com.loopers.application.coupon.UserIssueCouponUseCase
import com.loopers.application.order.CreateOrderCriteria
import com.loopers.application.order.CreateOrderItemCriteria
import com.loopers.application.order.UserCreateOrderUseCase
import com.loopers.domain.catalog.BrandService
import com.loopers.domain.catalog.RegisterBrandCommand
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.RegisterCouponCommand
import com.loopers.domain.user.RegisterCommand
import com.loopers.domain.user.UserService
import com.loopers.infrastructure.catalog.ProductJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class OrderConcurrencyTest @Autowired constructor(
    private val userCreateOrderUseCase: UserCreateOrderUseCase,
    private val adminRegisterProductUseCase: AdminRegisterProductUseCase,
    private val brandService: BrandService,
    private val userService: UserService,
    private val couponService: CouponService,
    private val userIssueCouponUseCase: UserIssueCouponUseCase,
    private val productJpaRepository: ProductJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val DEFAULT_USERNAME = "testuser"
        private const val DEFAULT_PASSWORD = "password1234!"
        private const val DEFAULT_NAME = "테스트유저"
        private const val DEFAULT_EMAIL = "test@loopers.com"
        private val DEFAULT_BIRTH_DATE = ZonedDateTime.of(1995, 5, 29, 0, 0, 0, 0, ZoneId.of("Asia/Seoul"))
        private const val AWAIT_TIMEOUT_SECONDS = 30L
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerUser(username: String = DEFAULT_USERNAME, email: String = DEFAULT_EMAIL) {
        userService.register(
            RegisterCommand(
                username = username,
                password = DEFAULT_PASSWORD,
                name = DEFAULT_NAME,
                email = email,
                birthDate = DEFAULT_BIRTH_DATE,
            ),
        )
    }

    @DisplayName("재고 동시성")
    @Nested
    inner class StockConcurrency {
        @DisplayName("재고 10개인 상품에 100명이 동시에 1개씩 주문하면, 10명만 성공해야 한다.")
        @Test
        fun onlyTenOrdersShouldSucceedWhenHundredConcurrentOrdersOnTenStock() {
            // arrange
            val initialStock = 10
            val threadCount = 100

            registerUser()
            val brandId = brandService.register(RegisterBrandCommand(name = "나이키")).id
            val product = adminRegisterProductUseCase.execute(
                RegisterProductCriteria(
                    brandId = brandId,
                    name = "에어맥스 90",
                    quantity = initialStock,
                    price = BigDecimal("129000"),
                ),
            )

            val executorService = Executors.newFixedThreadPool(threadCount)
            val readyLatch = CountDownLatch(threadCount)
            val startLatch = CountDownLatch(1)
            val doneLatch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)
            val unexpectedExceptions = ConcurrentLinkedQueue<Throwable>()

            // act
            repeat(threadCount) {
                executorService.submit {
                    try {
                        readyLatch.countDown()
                        startLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)

                        userCreateOrderUseCase.execute(
                            CreateOrderCriteria(
                                loginId = DEFAULT_USERNAME,
                                items = listOf(CreateOrderItemCriteria(productId = product.id, quantity = 1)),
                            ),
                        )
                        successCount.incrementAndGet()
                    } catch (e: CoreException) {
                        if (e.errorType == ErrorType.BAD_REQUEST) {
                            failCount.incrementAndGet()
                        } else {
                            unexpectedExceptions.add(e)
                        }
                    } catch (e: Exception) {
                        unexpectedExceptions.add(e)
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            readyLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            startLatch.countDown()
            doneLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            executorService.shutdown()

            // assert
            assertThat(unexpectedExceptions).isEmpty()
            val finalStock = productJpaRepository.findById(product.id).get().quantity
            assertThat(successCount.get()).isEqualTo(initialStock)
            assertThat(finalStock).isZero()
        }
    }

    @DisplayName("쿠폰 발급 동시성")
    @Nested
    inner class CouponIssueConcurrency {
        @DisplayName("수량 10개 쿠폰에 100명이 동시에 발급 요청하면, 10명만 성공해야 한다.")
        @Test
        fun onlyTenIssuesShouldSucceedWhenHundredConcurrentIssuesOnTenQuantity() {
            // arrange
            val totalQuantity = 10
            val threadCount = 100

            val couponInfo = couponService.register(
                RegisterCouponCommand(
                    name = "선착순 쿠폰",
                    discountType = DiscountType.FIXED,
                    discountValue = 5000,
                    totalQuantity = totalQuantity,
                    expiredAt = ZonedDateTime.now().plusDays(7),
                ),
            )

            repeat(threadCount) { i ->
                registerUser(username = "user$i", email = "user$i@loopers.com")
            }

            val executorService = Executors.newFixedThreadPool(threadCount)
            val readyLatch = CountDownLatch(threadCount)
            val startLatch = CountDownLatch(1)
            val doneLatch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)
            val unexpectedExceptions = ConcurrentLinkedQueue<Throwable>()

            // act
            repeat(threadCount) { i ->
                executorService.submit {
                    try {
                        readyLatch.countDown()
                        startLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)

                        userIssueCouponUseCase.execute(
                            IssueCouponCriteria(loginId = "user$i", couponId = couponInfo.id),
                        )
                        successCount.incrementAndGet()
                    } catch (e: CoreException) {
                        if (e.errorType in setOf(ErrorType.BAD_REQUEST, ErrorType.CONFLICT)) {
                            failCount.incrementAndGet()
                        } else {
                            unexpectedExceptions.add(e)
                        }
                    } catch (e: Exception) {
                        unexpectedExceptions.add(e)
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            readyLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            startLatch.countDown()
            doneLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            executorService.shutdown()

            // assert
            assertThat(unexpectedExceptions).isEmpty()
            val finalCoupon = couponJpaRepository.findById(couponInfo.id).get()
            val issuedCount = issuedCouponJpaRepository.findAll().size
            assertThat(successCount.get()).isEqualTo(totalQuantity)
            assertThat(finalCoupon.issuedQuantity).isEqualTo(totalQuantity)
            assertThat(issuedCount).isEqualTo(totalQuantity)
        }
    }

    @DisplayName("쿠폰 사용 동시성")
    @Nested
    inner class CouponUseConcurrency {
        @DisplayName("동일 쿠폰으로 여러 기기에서 동시에 주문하면, 1건만 성공해야 한다.")
        @Test
        fun onlyOneOrderShouldSucceedWhenConcurrentOrdersWithSameCoupon() {
            // arrange
            val threadCount = 10

            registerUser()
            val brandId = brandService.register(RegisterBrandCommand(name = "나이키")).id
            val product = adminRegisterProductUseCase.execute(
                RegisterProductCriteria(
                    brandId = brandId,
                    name = "에어맥스 90",
                    quantity = 100,
                    price = BigDecimal("129000"),
                ),
            )

            val couponInfo = couponService.register(
                RegisterCouponCommand(
                    name = "할인 쿠폰",
                    discountType = DiscountType.FIXED,
                    discountValue = 10000,
                    totalQuantity = 100,
                    expiredAt = ZonedDateTime.now().plusDays(7),
                ),
            )

            val issuedCoupon = userIssueCouponUseCase.execute(
                IssueCouponCriteria(loginId = DEFAULT_USERNAME, couponId = couponInfo.id),
            )

            val executorService = Executors.newFixedThreadPool(threadCount)
            val readyLatch = CountDownLatch(threadCount)
            val startLatch = CountDownLatch(1)
            val doneLatch = CountDownLatch(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)
            val unexpectedExceptions = ConcurrentLinkedQueue<Throwable>()

            // act
            repeat(threadCount) {
                executorService.submit {
                    try {
                        readyLatch.countDown()
                        startLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)

                        userCreateOrderUseCase.execute(
                            CreateOrderCriteria(
                                loginId = DEFAULT_USERNAME,
                                items = listOf(CreateOrderItemCriteria(productId = product.id, quantity = 1)),
                                couponId = issuedCoupon.id,
                            ),
                        )
                        successCount.incrementAndGet()
                    } catch (e: CoreException) {
                        if (e.errorType == ErrorType.BAD_REQUEST) {
                            failCount.incrementAndGet()
                        } else {
                            unexpectedExceptions.add(e)
                        }
                    } catch (e: ObjectOptimisticLockingFailureException) {
                        failCount.incrementAndGet()
                    } catch (e: Exception) {
                        unexpectedExceptions.add(e)
                    } finally {
                        doneLatch.countDown()
                    }
                }
            }

            readyLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            startLatch.countDown()
            doneLatch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            executorService.shutdown()

            // assert
            assertThat(unexpectedExceptions).isEmpty()
            assertThat(successCount.get()).isEqualTo(1)
            assertThat(failCount.get()).isEqualTo(threadCount - 1)
        }
    }
}
