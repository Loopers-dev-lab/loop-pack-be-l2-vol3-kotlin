package com.loopers.domain.coupon

import com.loopers.domain.coupon.model.Coupon
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class CouponTest {

    @Nested
    @DisplayName("issue 시")
    inner class Issue {

        private fun createCoupon(
            totalQuantity: Int? = 100,
            expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
        ): Coupon {
            return Coupon(
                name = "테스트 쿠폰",
                type = Coupon.CouponType.FIXED,
                value = 1000L,
                totalQuantity = totalQuantity,
                expiredAt = expiredAt,
            )
        }

        @Test
        @DisplayName("정상 쿠폰에 issue() 호출 시 issuedCount가 증가한다")
        fun issue_normal_incrementsIssuedCount() {
            // arrange
            val coupon = createCoupon()

            // act
            coupon.issue()

            // assert
            assertThat(coupon.issuedCount).isEqualTo(1)
        }

        @Test
        @DisplayName("삭제된 쿠폰에 issue() 호출 시 BAD_REQUEST 예외가 발생한다")
        fun issue_deleted_throwsException() {
            // arrange
            val coupon = createCoupon()
            coupon.delete()

            // act
            val exception = assertThrows<CoreException> {
                coupon.issue()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.customMessage).contains("삭제된")
        }

        @Test
        @DisplayName("만료된 쿠폰에 issue() 호출 시 BAD_REQUEST 예외가 발생한다")
        fun issue_expired_throwsException() {
            // arrange
            val coupon = createCoupon(expiredAt = ZonedDateTime.now().minusDays(1))

            // act
            val exception = assertThrows<CoreException> {
                coupon.issue()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.customMessage).contains("만료된")
        }

        @Test
        @DisplayName("발급 수량이 초과되면 BAD_REQUEST 예외가 발생한다")
        fun issue_quantityExceeded_throwsException() {
            // arrange
            val coupon = createCoupon(totalQuantity = 1)
            coupon.issue() // 1번째 발급

            // act
            val exception = assertThrows<CoreException> {
                coupon.issue() // 2번째 발급 시도
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.customMessage).contains("수량")
        }
    }
}
