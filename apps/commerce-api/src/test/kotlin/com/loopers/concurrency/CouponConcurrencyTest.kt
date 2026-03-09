package com.loopers.concurrency

import com.loopers.application.order.OrderFacade
import com.loopers.application.order.OrderItemCriteria
import com.loopers.domain.brand.Brand
import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponStatus
import com.loopers.domain.product.Product
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("쿠폰 동시성 테스트")
@SpringBootTest
class CouponConcurrencyTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val orderJpaRepository: OrderJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("동일 발급 쿠폰으로 10개 스레드가 동시에 주문하면, 정확히 1개만 성공하고 쿠폰은 USED 상태가 된다.")
    @Test
    fun allowsOnlyOneUsage_whenConcurrentCouponUse() {
        // arrange
        val threadCount = 10
        val userId = 1L
        val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        val product = productJpaRepository.save(
            Product(
                brandId = brand.id,
                name = "에어맥스 90",
                price = BigDecimal("50000"),
                stock = 100,
                description = null,
                imageUrl = null,
            ),
        )
        val coupon = couponJpaRepository.save(
            Coupon(
                name = "5000원 할인 쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            ),
        )
        val issuedCoupon = issuedCouponJpaRepository.save(
            IssuedCoupon(couponId = coupon.id, userId = userId),
        )

        val latch = CountDownLatch(1)
        val executorService = Executors.newFixedThreadPool(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // act
        repeat(threadCount) {
            executorService.submit {
                try {
                    latch.await()
                    val criteria = listOf(OrderItemCriteria(productId = product.id, quantity = 1))
                    orderFacade.createOrder(userId = userId, criteria = criteria, couponId = issuedCoupon.id)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                }
            }
        }

        latch.countDown()
        executorService.shutdown()
        executorService.awaitTermination(10, TimeUnit.SECONDS)

        // assert
        val updatedCoupon = issuedCouponJpaRepository.findById(issuedCoupon.id).get()
        val orders = orderJpaRepository.findAll()
        assertAll(
            { assertThat(successCount.get()).isEqualTo(1) },
            { assertThat(failCount.get()).isEqualTo(threadCount - 1) },
            { assertThat(updatedCoupon.status).isEqualTo(IssuedCouponStatus.USED) },
            { assertThat(orders).hasSize(1) },
        )
    }
}
