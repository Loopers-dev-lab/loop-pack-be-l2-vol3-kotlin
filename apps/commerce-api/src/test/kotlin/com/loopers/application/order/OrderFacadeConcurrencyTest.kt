package com.loopers.application.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.coupon.CouponTemplate
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.product.Product
import com.loopers.domain.user.User
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.coupon.CouponTemplateJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class OrderFacadeConcurrencyTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val userJpaRepository: UserJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val couponTemplateJpaRepository: CouponTemplateJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val orderJpaRepository: OrderJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private lateinit var user: User
    private lateinit var product: Product

    companion object {
        private const val PASSWORD = "abcd1234"
        private const val THREAD_COUNT = 10
    }

    @BeforeEach
    fun setUp() {
        user = userJpaRepository.save(User(loginId = "testuser1", password = PASSWORD, name = "테스트유저", birth = "2000-01-01", email = "test@test.com"))
        val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        product = productJpaRepository.save(Product(brandId = brand.id, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100))
    }

    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("주문 동시성 - 재고 차감")
    @Nested
    inner class StockDecrease {
        @DisplayName("동일 상품에 10명이 동시에 1개씩 주문하면, 재고가 정확히 10 감소한다.")
        @Test
        fun decreasesStockExactly_whenConcurrentOrders() {
            // arrange
            val users = (1..THREAD_COUNT).map { i ->
                userJpaRepository.save(User(loginId = "tester0$i", password = PASSWORD, name = "유저$i", birth = "2000-01-01", email = "user$i@test.com"))
            }
            val executor = Executors.newFixedThreadPool(THREAD_COUNT)
            val latch = CountDownLatch(THREAD_COUNT)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act
            users.forEach { u ->
                executor.submit {
                    try {
                        orderFacade.createOrder(
                            loginId = u.loginId,
                            password = PASSWORD,
                            itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
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

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertAll(
                { assertThat(successCount.get()).isEqualTo(THREAD_COUNT) },
                { assertThat(updatedProduct.stockQuantity).isEqualTo(100 - THREAD_COUNT) },
            )
        }

        @DisplayName("재고가 5개인 상품에 10명이 동시에 1개씩 주문하면, 5명만 성공한다.")
        @Test
        fun onlyPartialOrdersSucceed_whenStockInsufficient() {
            // arrange
            product.update(name = product.name, description = product.description, price = product.price, stockQuantity = 5)
            productJpaRepository.saveAndFlush(product)

            val users = (1..THREAD_COUNT).map { i ->
                userJpaRepository.save(User(loginId = "tester0$i", password = PASSWORD, name = "유저$i", birth = "2000-01-01", email = "user$i@test.com"))
            }
            val executor = Executors.newFixedThreadPool(THREAD_COUNT)
            val latch = CountDownLatch(THREAD_COUNT)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act
            users.forEach { u ->
                executor.submit {
                    try {
                        orderFacade.createOrder(
                            loginId = u.loginId,
                            password = PASSWORD,
                            itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
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

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertAll(
                { assertThat(successCount.get()).isEqualTo(5) },
                { assertThat(failCount.get()).isEqualTo(5) },
                { assertThat(updatedProduct.stockQuantity).isEqualTo(0) },
            )
        }
    }

    @DisplayName("주문 동시성 - 쿠폰 이중 사용 방지")
    @Nested
    inner class CouponDoubleUse {
        @DisplayName("동일 쿠폰으로 동시에 2건 주문하면, 1건만 성공하고 실패한 주문의 재고와 쿠폰은 롤백된다.")
        @Test
        fun onlyOneOrderSucceeds_andFailedOrderIsRolledBack() {
            // arrange
            val template = couponTemplateJpaRepository.save(
                CouponTemplate(name = "1000원 할인", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30)),
            )
            val coupon = issuedCouponJpaRepository.save(IssuedCoupon(userId = user.id, couponTemplateId = template.id))
            val initialStock = product.stockQuantity

            val executor = Executors.newFixedThreadPool(2)
            val latch = CountDownLatch(2)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act
            repeat(2) {
                executor.submit {
                    try {
                        orderFacade.createOrder(
                            loginId = user.loginId,
                            password = PASSWORD,
                            itemRequests = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                            couponId = coupon.id,
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

            // assert
            val updatedCoupon = issuedCouponJpaRepository.findById(coupon.id).get()
            val updatedProduct = productJpaRepository.findById(product.id).get()
            val orders = orderJpaRepository.findAll()
            assertAll(
                { assertThat(successCount.get()).isEqualTo(1) },
                { assertThat(failCount.get()).isEqualTo(1) },
                { assertThat(updatedCoupon.used).isTrue() },
                { assertThat(orders).hasSize(1) },
                { assertThat(updatedProduct.stockQuantity).isEqualTo(initialStock - 1) },
            )
        }
    }
}
