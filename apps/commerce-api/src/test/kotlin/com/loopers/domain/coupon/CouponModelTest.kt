package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

@DisplayName("CouponModel")
class CouponModelTest {

    companion object {
        private const val VALID_NAME = "신규가입 10% 할인"
        private const val VALID_VALUE = 10L
        private const val VALID_MIN_ORDER_AMOUNT = 10000L
        private val VALID_EXPIRED_AT: ZonedDateTime = ZonedDateTime.now().plusDays(30)
    }

    private fun createCoupon(
        name: String = VALID_NAME,
        type: CouponType = CouponType.RATE,
        value: Long = VALID_VALUE,
        minOrderAmount: Long? = VALID_MIN_ORDER_AMOUNT,
        expiredAt: ZonedDateTime = VALID_EXPIRED_AT,
    ): CouponModel = CouponModel(
        name = name,
        type = type,
        value = value,
        minOrderAmount = minOrderAmount,
        expiredAt = expiredAt,
    )

    @DisplayName("정상 생성")
    @Nested
    inner class Create {
        @DisplayName("모든 필드가 유효하면 CouponModel이 생성된다")
        @Test
        fun createsCoupon_whenAllFieldsAreValid() {
            // arrange & act
            val coupon = createCoupon()

            // assert
            assertThat(coupon.name).isEqualTo(VALID_NAME)
            assertThat(coupon.type).isEqualTo(CouponType.RATE)
            assertThat(coupon.value).isEqualTo(VALID_VALUE)
            assertThat(coupon.minOrderAmount).isEqualTo(VALID_MIN_ORDER_AMOUNT)
        }

        @DisplayName("최소 주문 금액이 null이어도 정상 생성된다")
        @Test
        fun createsCoupon_whenMinOrderAmountIsNull() {
            // arrange & act
            val coupon = createCoupon(minOrderAmount = null)

            // assert
            assertThat(coupon.minOrderAmount).isNull()
        }
    }

    @DisplayName("이름 검증")
    @Nested
    inner class NameValidation {
        @DisplayName("이름이 빈 문자열이면 예외가 발생한다")
        @Test
        fun throwsException_whenNameIsBlank() {
            assertThatThrownBy { createCoupon(name = "") }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 100자를 초과하면 예외가 발생한다")
        @Test
        fun throwsException_whenNameExceeds100Characters() {
            assertThatThrownBy { createCoupon(name = "가".repeat(101)) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("값 검증")
    @Nested
    inner class ValueValidation {
        @DisplayName("값이 0 이하이면 예외가 발생한다")
        @Test
        fun throwsException_whenValueIsZeroOrNegative() {
            assertThatThrownBy { createCoupon(value = 0L) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("정률 쿠폰의 값이 100을 초과하면 예외가 발생한다")
        @Test
        fun throwsException_whenRateValueExceeds100() {
            assertThatThrownBy { createCoupon(type = CouponType.RATE, value = 101L) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("할인 금액 계산")
    @Nested
    inner class CalculateDiscount {
        @DisplayName("정액 쿠폰: 3000원 할인, 주문 10000원이면 할인 금액은 3000원이다")
        @Test
        fun calculatesFixedDiscount() {
            // arrange
            val coupon = createCoupon(
                type = CouponType.FIXED,
                value = 3000L,
                minOrderAmount = null,
            )

            // act
            val discount = coupon.calculateDiscount(10000L)

            // assert
            assertThat(discount).isEqualTo(3000L)
        }

        @DisplayName("정률 쿠폰: 10% 할인, 주문 50000원이면 할인 금액은 5000원이다")
        @Test
        fun calculatesRateDiscount() {
            // arrange
            val coupon = createCoupon(
                type = CouponType.RATE,
                value = 10L,
                minOrderAmount = null,
            )

            // act
            val discount = coupon.calculateDiscount(50000L)

            // assert
            assertThat(discount).isEqualTo(5000L)
        }

        @DisplayName("정액 쿠폰이 주문 금액보다 크면 주문 금액만큼만 할인된다")
        @Test
        fun capsFixedDiscount_whenExceedsOrderAmount() {
            // arrange
            val coupon = createCoupon(
                type = CouponType.FIXED,
                value = 15000L,
                minOrderAmount = null,
            )

            // act
            val discount = coupon.calculateDiscount(10000L)

            // assert
            assertThat(discount).isEqualTo(10000L)
        }

        @DisplayName("최소 주문 금액 미달 시 예외가 발생한다")
        @Test
        fun throwsException_whenBelowMinOrderAmount() {
            // arrange
            val coupon = createCoupon(minOrderAmount = 20000L)

            // act & assert
            assertThatThrownBy { coupon.calculateDiscount(15000L) }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
                .hasMessageContaining("최소 주문 금액")
        }
    }

    @DisplayName("만료 여부 판정")
    @Nested
    inner class Expiration {
        @DisplayName("만료일이 지났으면 isExpired()는 true를 반환한다")
        @Test
        fun returnsTrue_whenExpired() {
            // arrange
            val coupon = createCoupon(expiredAt = ZonedDateTime.now().minusDays(1))

            // act & assert
            assertThat(coupon.isExpired()).isTrue()
        }

        @DisplayName("만료일이 지나지 않았으면 isExpired()는 false를 반환한다")
        @Test
        fun returnsFalse_whenNotExpired() {
            // arrange
            val coupon = createCoupon(expiredAt = ZonedDateTime.now().plusDays(1))

            // act & assert
            assertThat(coupon.isExpired()).isFalse()
        }
    }
}
