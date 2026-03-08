package com.loopers.application.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponQuantity
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.Discount
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest
class CouponFacadeIntegrationTest @Autowired constructor(
    private val couponFacade: CouponFacade,
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createCoupon(
        name: String = "신규가입 할인",
        discount: Discount = Discount(DiscountType.FIXED_AMOUNT, 5000L),
        totalQuantity: Int = 100,
        expiresAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Coupon {
        return couponRepository.save(
            Coupon(
                name = name,
                discount = discount,
                quantity = CouponQuantity(totalQuantity, 0),
                expiresAt = expiresAt,
            ),
        )
    }

    @DisplayName("같은 사용자가 동시에 쿠폰 발급을 요청하면,")
    @Nested
    inner class ConcurrentIssueSameUser {

        @DisplayName("비관적 락으로 직렬화되어, 발급된 쿠폰은 정확히 1개이다.")
        @Test
        fun handlesRaceCondition_whenSameUserConcurrentlyIssues() {
            // arrange
            val coupon = createCoupon()
            val threadCount = 10
            val latch = CountDownLatch(threadCount)
            val executor = Executors.newFixedThreadPool(threadCount)

            // act
            repeat(threadCount) {
                executor.submit {
                    try {
                        couponFacade.issue(couponId = coupon.id, userId = 1L)
                    } catch (_: Exception) {
                        // CoreException(CONFLICT) 등은 허용
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            val issuedCoupons = issuedCouponRepository.findByUserId(1L)
            assertThat(issuedCoupons).hasSize(1)
        }
    }
}
