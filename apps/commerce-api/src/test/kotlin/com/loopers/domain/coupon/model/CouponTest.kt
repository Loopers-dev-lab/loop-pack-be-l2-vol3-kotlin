package com.loopers.domain.coupon.model

import com.loopers.domain.common.vo.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.ZonedDateTime

class CouponTest {

    private fun fixedCoupon(
        name: String = "테스트 쿠폰",
        value: Long = 1000L,
        maxDiscount: Money? = null,
        minOrderAmount: Money? = null,
        totalQuantity: Int? = 100,
        issuedCount: Int = 0,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(7),
        deletedAt: ZonedDateTime? = null,
    ) = Coupon(
        name = name,
        type = Coupon.CouponType.FIXED,
        value = value,
        maxDiscount = maxDiscount,
        minOrderAmount = minOrderAmount,
        totalQuantity = totalQuantity,
        issuedCount = issuedCount,
        expiredAt = expiredAt,
        deletedAt = deletedAt,
    )

    private fun rateCoupon(
        name: String = "비율 쿠폰",
        value: Long = 10L,
        maxDiscount: Money? = null,
        minOrderAmount: Money? = null,
        totalQuantity: Int? = 100,
        issuedCount: Int = 0,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(7),
        deletedAt: ZonedDateTime? = null,
    ) = Coupon(
        name = name,
        type = Coupon.CouponType.RATE,
        value = value,
        maxDiscount = maxDiscount,
        minOrderAmount = minOrderAmount,
        totalQuantity = totalQuantity,
        issuedCount = issuedCount,
        expiredAt = expiredAt,
        deletedAt = deletedAt,
    )

    @Nested
    @DisplayName("Coupon 생성 시")
    inner class Create {

        @Test
        @DisplayName("유효한 정보로 FIXED 타입 쿠폰을 생성하면 성공한다")
        fun create_withValidFixed_success() {
            // arrange & act
            val coupon = fixedCoupon(name = "고정 쿠폰", value = 5000L)

            // assert
            assertThat(coupon.name).isEqualTo("고정 쿠폰")
            assertThat(coupon.type).isEqualTo(Coupon.CouponType.FIXED)
            assertThat(coupon.value).isEqualTo(5000L)
        }

        @Test
        @DisplayName("유효한 정보로 RATE 타입 쿠폰을 생성하면 성공한다")
        fun create_withValidRate_success() {
            // arrange & act
            val coupon = rateCoupon(name = "비율 쿠폰", value = 20L)

            // assert
            assertThat(coupon.name).isEqualTo("비율 쿠폰")
            assertThat(coupon.type).isEqualTo(Coupon.CouponType.RATE)
            assertThat(coupon.value).isEqualTo(20L)
        }

        @Test
        @DisplayName("name이 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
        fun create_withBlankName_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                fixedCoupon(name = "")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("쿠폰 이름은 비어있을 수 없습니다.")
        }

        @Test
        @DisplayName("value가 0 이하이면 BAD_REQUEST 예외가 발생한다")
        fun create_withNonPositiveValue_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                fixedCoupon(value = 0L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("쿠폰 값은 0보다 커야 합니다.")
        }

        @Test
        @DisplayName("RATE 타입에서 value가 0이면 BAD_REQUEST 예외가 발생한다")
        fun create_rateWithZeroValue_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                rateCoupon(value = 0L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("RATE 타입에서 value가 101이면 BAD_REQUEST 예외가 발생한다")
        fun create_rateWithValueOver100_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                rateCoupon(value = 101L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("할인율은 1~100 사이여야 합니다.")
        }

        @Test
        @DisplayName("totalQuantity가 0 이하이면 BAD_REQUEST 예외가 발생한다")
        fun create_withNonPositiveTotalQuantity_throwsException() {
            // act
            val exception = assertThrows<CoreException> {
                fixedCoupon(totalQuantity = 0)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("총 수량은 0보다 커야 합니다.")
        }
    }

    @Nested
    @DisplayName("canIssue 확인 시")
    inner class CanIssue {

        @Test
        @DisplayName("수량 잔여, 미만료, 미삭제 쿠폰은 true를 반환한다")
        fun canIssue_available_returnsTrue() {
            // arrange
            val coupon = fixedCoupon(totalQuantity = 10, issuedCount = 5)

            // assert
            assertThat(coupon.canIssue()).isTrue()
        }

        @Test
        @DisplayName("만료된 쿠폰은 false를 반환한다")
        fun canIssue_expired_returnsFalse() {
            // arrange
            val coupon = fixedCoupon(expiredAt = ZonedDateTime.now().minusDays(1))

            // assert
            assertThat(coupon.canIssue()).isFalse()
        }

        @Test
        @DisplayName("삭제된 쿠폰은 false를 반환한다")
        fun canIssue_deleted_returnsFalse() {
            // arrange
            val coupon = fixedCoupon(deletedAt = ZonedDateTime.now().minusDays(1))

            // assert
            assertThat(coupon.canIssue()).isFalse()
        }

        @Test
        @DisplayName("수량이 소진된 쿠폰은 false를 반환한다")
        fun canIssue_quantityExhausted_returnsFalse() {
            // arrange
            val coupon = fixedCoupon(totalQuantity = 5, issuedCount = 5)

            // assert
            assertThat(coupon.canIssue()).isFalse()
        }

        @Test
        @DisplayName("totalQuantity가 null(무제한)이면 수량에 관계없이 true를 반환한다")
        fun canIssue_unlimitedQuantity_returnsTrue() {
            // arrange
            val coupon = fixedCoupon(totalQuantity = null, issuedCount = 9999)

            // assert
            assertThat(coupon.canIssue()).isTrue()
        }
    }

    @Nested
    @DisplayName("issue 호출 시")
    inner class Issue {

        @Test
        @DisplayName("정상 발급 시 issuedCount가 1 증가한다")
        fun issue_valid_incrementsIssuedCount() {
            // arrange
            val coupon = fixedCoupon(totalQuantity = 10, issuedCount = 3)

            // act
            coupon.issue()

            // assert
            assertThat(coupon.issuedCount).isEqualTo(4)
        }

        @Test
        @DisplayName("canIssue()가 false인 경우 BAD_REQUEST 예외가 발생한다")
        fun issue_whenCannotIssue_throwsException() {
            // arrange
            val coupon = fixedCoupon(totalQuantity = 5, issuedCount = 5)

            // act
            val exception = assertThrows<CoreException> {
                coupon.issue()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("쿠폰을 발급할 수 없습니다.")
        }
    }

    @Nested
    @DisplayName("calculateDiscount 호출 시")
    inner class CalculateDiscount {

        @Test
        @DisplayName("FIXED 타입에서 정상적으로 할인 금액을 계산한다")
        fun calculateDiscount_fixed_returnsDiscountAmount() {
            // arrange
            val coupon = fixedCoupon(value = 3000L)
            val orderAmount = Money(BigDecimal("10000"))

            // act
            val discount = coupon.calculateDiscount(orderAmount)

            // assert
            assertThat(discount.value).isEqualByComparingTo(BigDecimal("3000"))
        }

        @Test
        @DisplayName("FIXED 타입에서 할인액이 주문금액을 초과하면 주문금액을 반환한다")
        fun calculateDiscount_fixed_cappedByOrderAmount() {
            // arrange
            val coupon = fixedCoupon(value = 15000L)
            val orderAmount = Money(BigDecimal("10000"))

            // act
            val discount = coupon.calculateDiscount(orderAmount)

            // assert
            assertThat(discount.value).isEqualByComparingTo(BigDecimal("10000"))
        }

        @Test
        @DisplayName("RATE 타입에서 정상적으로 할인 금액을 계산한다")
        fun calculateDiscount_rate_returnsDiscountAmount() {
            // arrange
            val coupon = rateCoupon(value = 10L)
            val orderAmount = Money(BigDecimal("20000"))

            // act
            val discount = coupon.calculateDiscount(orderAmount)

            // assert
            assertThat(discount.value).isEqualByComparingTo(BigDecimal("2000"))
        }

        @Test
        @DisplayName("RATE 타입에서 maxDiscount 한도가 적용된다")
        fun calculateDiscount_rate_cappedByMaxDiscount() {
            // arrange
            val maxDiscount = Money(BigDecimal("1000"))
            val coupon = rateCoupon(value = 10L, maxDiscount = maxDiscount)
            val orderAmount = Money(BigDecimal("20000"))

            // act
            val discount = coupon.calculateDiscount(orderAmount)

            // assert
            assertThat(discount.value).isEqualByComparingTo(BigDecimal("1000"))
        }

        @Test
        @DisplayName("RATE 타입에서 maxDiscount가 null이면 한도 없이 계산된다")
        fun calculateDiscount_rate_withNoMaxDiscount_returnsFullDiscount() {
            // arrange
            val coupon = rateCoupon(value = 10L, maxDiscount = null)
            val orderAmount = Money(BigDecimal("20000"))

            // act
            val discount = coupon.calculateDiscount(orderAmount)

            // assert
            assertThat(discount.value).isEqualByComparingTo(BigDecimal("2000"))
        }
    }

    @Nested
    @DisplayName("isExpired 확인 시")
    inner class IsExpired {

        @Test
        @DisplayName("만료일이 미래이면 false를 반환한다")
        fun isExpired_futureDatetime_returnsFalse() {
            // arrange
            val coupon = fixedCoupon(expiredAt = ZonedDateTime.now().plusDays(1))

            // assert
            assertThat(coupon.isExpired()).isFalse()
        }

        @Test
        @DisplayName("만료일이 과거이면 true를 반환한다")
        fun isExpired_pastDatetime_returnsTrue() {
            // arrange
            val coupon = fixedCoupon(expiredAt = ZonedDateTime.now().minusSeconds(1))

            // assert
            assertThat(coupon.isExpired()).isTrue()
        }
    }
}
