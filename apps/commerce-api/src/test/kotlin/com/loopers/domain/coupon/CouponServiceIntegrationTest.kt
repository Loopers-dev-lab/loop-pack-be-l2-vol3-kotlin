package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest
class CouponServiceIntegrationTest @Autowired constructor(
    private val couponService: CouponService,
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
        discountType: DiscountType = DiscountType.FIXED_AMOUNT,
        discountValue: Long = 5000L,
        totalQuantity: Int = 100,
        expiresAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Coupon {
        return couponRepository.save(
            Coupon(
                name = name,
                discountType = discountType,
                discountValue = discountValue,
                totalQuantity = totalQuantity,
                expiresAt = expiresAt,
            ),
        )
    }

    @DisplayName("쿠폰을 발급할 때,")
    @Nested
    inner class IssueCoupon {

        @DisplayName("유효한 쿠폰이면, 발급에 성공한다.")
        @Test
        fun issuesCoupon_whenCouponIsValid() {
            // arrange
            val coupon = createCoupon()
            val userId = 1L

            // act
            couponService.issue(couponId = coupon.id, userId = userId)

            // assert
            val issuedCoupons = issuedCouponRepository.findByUserId(userId)
            assertThat(issuedCoupons).hasSize(1)
            assertThat(issuedCoupons[0].couponId).isEqualTo(coupon.id)
        }

        @DisplayName("이미 발급받은 쿠폰이면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenAlreadyIssued() {
            // arrange
            val coupon = createCoupon()
            val userId = 1L
            couponService.issue(couponId = coupon.id, userId = userId)

            // act
            val exception = assertThrows<CoreException> {
                couponService.issue(couponId = coupon.id, userId = userId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("만료된 쿠폰이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenCouponIsExpired() {
            // arrange
            val coupon = createCoupon(expiresAt = ZonedDateTime.now().minusDays(1))
            val userId = 1L

            // act
            val exception = assertThrows<CoreException> {
                couponService.issue(couponId = coupon.id, userId = userId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("발급 수량이 소진된 쿠폰이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenCouponIsExhausted() {
            // arrange
            val coupon = createCoupon(totalQuantity = 1)
            couponService.issue(couponId = coupon.id, userId = 1L)

            // act
            val exception = assertThrows<CoreException> {
                couponService.issue(couponId = coupon.id, userId = 2L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("존재하지 않는 쿠폰이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenCouponNotExists() {
            // act
            val exception = assertThrows<CoreException> {
                couponService.issue(couponId = 999999L, userId = 1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("동시에 여러 사용자가 같은 쿠폰 발급을 요청하면,")
    @Nested
    inner class ConcurrentIssueCoupon {

        @DisplayName("정확한 수량만큼만 발급된다.")
        @Test
        fun issuesExactQuantity_whenConcurrentRequests() {
            // arrange
            val coupon = createCoupon(totalQuantity = 10)
            val threadCount = 20
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)

            // act
            (1..threadCount).forEach { userId ->
                executorService.submit {
                    try {
                        couponService.issue(couponId = coupon.id, userId = userId.toLong())
                    } catch (_: CoreException) {
                        // 수량 초과 시 예외 발생 예상
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executorService.shutdown()

            // assert
            val issuedCoupons = issuedCouponRepository.findByCouponId(coupon.id)
            assertThat(issuedCoupons).hasSize(10)
        }
    }
}
