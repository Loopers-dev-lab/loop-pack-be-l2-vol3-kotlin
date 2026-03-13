package com.loopers.domain.coupon

import com.loopers.domain.common.Money
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.ZonedDateTime

@DisplayName("Coupon 도메인")
class CouponTest {
    private val futureDate = ZonedDateTime.now().plusDays(30)

    private fun registerCoupon(
        name: String = "테스트 쿠폰",
        type: Coupon.Type = Coupon.Type.FIXED,
        discountValue: Long = 1000L,
        minOrderAmount: Money? = null,
        expiredAt: ZonedDateTime = futureDate,
    ): Coupon = Coupon.register(
        name = name,
        type = type,
        discountValue = discountValue,
        minOrderAmount = minOrderAmount,
        expiredAt = expiredAt,
    )

    @Nested
    @DisplayName("유효한 정보로 쿠폰 템플릿을 등록하면 성공한다")
    inner class WhenValidRegistration {
        @Test
        @DisplayName("FIXED 타입, discountValue=1000, 만료일 미래 → 성공")
        fun register_fixedType() {
            val coupon = registerCoupon(type = Coupon.Type.FIXED, discountValue = 1000L)

            assertAll(
                { assertThat(coupon.name).isEqualTo("테스트 쿠폰") },
                { assertThat(coupon.type).isEqualTo(Coupon.Type.FIXED) },
                { assertThat(coupon.discountValue).isEqualTo(1000L) },
                { assertThat(coupon.id).isNull() },
            )
        }

        @Test
        @DisplayName("RATE 타입, discountValue=10, minOrderAmount=10000, 만료일 미래 → 성공")
        fun register_rateType() {
            val coupon = registerCoupon(
                type = Coupon.Type.RATE,
                discountValue = 10L,
                minOrderAmount = Money(BigDecimal.valueOf(10000)),
            )

            assertAll(
                { assertThat(coupon.type).isEqualTo(Coupon.Type.RATE) },
                { assertThat(coupon.discountValue).isEqualTo(10L) },
                { assertThat(coupon.minOrderAmount).isEqualTo(Money(BigDecimal.valueOf(10000))) },
            )
        }

        @Test
        @DisplayName("RATE 타입, discountValue=1 (경계값) → 성공")
        fun register_rateMinBoundary() {
            val coupon = registerCoupon(type = Coupon.Type.RATE, discountValue = 1L)
            assertThat(coupon.discountValue).isEqualTo(1L)
        }

        @Test
        @DisplayName("RATE 타입, discountValue=100 (경계값) → 성공")
        fun register_rateMaxBoundary() {
            val coupon = registerCoupon(type = Coupon.Type.RATE, discountValue = 100L)
            assertThat(coupon.discountValue).isEqualTo(100L)
        }
    }

    @Nested
    @DisplayName("discountValue가 타입별 유효 범위를 벗어나면 실패한다")
    inner class WhenInvalidDiscountValue {
        @Test
        @DisplayName("FIXED, discountValue=0 → 실패")
        fun register_fixedZero() {
            val exception = assertThrows<CoreException> {
                registerCoupon(type = Coupon.Type.FIXED, discountValue = 0L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.COUPON_INVALID_DISCOUNT_VALUE)
        }

        @Test
        @DisplayName("FIXED, discountValue=-1 → 실패")
        fun register_fixedNegative() {
            val exception = assertThrows<CoreException> {
                registerCoupon(type = Coupon.Type.FIXED, discountValue = -1L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.COUPON_INVALID_DISCOUNT_VALUE)
        }

        @Test
        @DisplayName("RATE, discountValue=0 → 실패")
        fun register_rateZero() {
            val exception = assertThrows<CoreException> {
                registerCoupon(type = Coupon.Type.RATE, discountValue = 0L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.COUPON_INVALID_DISCOUNT_VALUE)
        }

        @Test
        @DisplayName("RATE, discountValue=101 → 실패")
        fun register_rateOverMax() {
            val exception = assertThrows<CoreException> {
                registerCoupon(type = Coupon.Type.RATE, discountValue = 101L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.COUPON_INVALID_DISCOUNT_VALUE)
        }
    }

    @Nested
    @DisplayName("만료일이 현재 이전이면 등록이 실패한다")
    inner class WhenExpiredExpiration {
        @Test
        @DisplayName("만료일 = 어제 → 실패")
        fun register_expiredDate() {
            val exception = assertThrows<CoreException> {
                registerCoupon(expiredAt = ZonedDateTime.now().minusDays(1))
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.COUPON_INVALID_EXPIRATION)
        }
    }

    @Nested
    @DisplayName("정액(FIXED) 할인이 정확히 적용된다")
    inner class WhenFixedDiscount {
        @Test
        @DisplayName("50000원, FIXED 3000원 → 할인 3000원, 최종 47000원")
        fun calculateDiscount_normalCase() {
            val coupon = registerCoupon(type = Coupon.Type.FIXED, discountValue = 3000L)
            val orderAmount = Money(BigDecimal.valueOf(50000))

            val discount = coupon.calculateDiscount(orderAmount)

            assertAll(
                { assertThat(discount).isEqualTo(Money(BigDecimal.valueOf(3000))) },
                { assertThat(orderAmount - discount).isEqualTo(Money(BigDecimal.valueOf(47000))) },
            )
        }

        @Test
        @DisplayName("2000원, FIXED 3000원 → 할인 2000원, 최종 0원 (음수 방지)")
        fun calculateDiscount_capAtOrderAmount() {
            val coupon = registerCoupon(type = Coupon.Type.FIXED, discountValue = 3000L)
            val orderAmount = Money(BigDecimal.valueOf(2000))

            val discount = coupon.calculateDiscount(orderAmount)

            assertAll(
                { assertThat(discount).isEqualTo(Money(BigDecimal.valueOf(2000))) },
                { assertThat(orderAmount - discount).isEqualTo(Money(BigDecimal.ZERO)) },
            )
        }
    }

    @Nested
    @DisplayName("정률(RATE) 할인이 정확히 적용된다 (올림 처리)")
    inner class WhenRateDiscount {
        @Test
        @DisplayName("50000원, RATE 10% → 할인 5000원, 최종 45000원")
        fun calculateDiscount_normalCase() {
            val coupon = registerCoupon(type = Coupon.Type.RATE, discountValue = 10L)
            val orderAmount = Money(BigDecimal.valueOf(50000))

            val discount = coupon.calculateDiscount(orderAmount)

            assertAll(
                { assertThat(discount).isEqualTo(Money(BigDecimal.valueOf(5000))) },
                { assertThat(orderAmount - discount).isEqualTo(Money(BigDecimal.valueOf(45000))) },
            )
        }

        @Test
        @DisplayName("33322원, RATE 1% → 올림 334원 할인, 최종 32988원")
        fun calculateDiscount_ceilingRounding() {
            val coupon = registerCoupon(type = Coupon.Type.RATE, discountValue = 1L)
            val orderAmount = Money(BigDecimal.valueOf(33322))

            val discount = coupon.calculateDiscount(orderAmount)

            assertAll(
                { assertThat(discount).isEqualTo(Money(BigDecimal.valueOf(334))) },
                { assertThat(orderAmount - discount).isEqualTo(Money(BigDecimal.valueOf(32988))) },
            )
        }

        @Test
        @DisplayName("RATE 100% → 할인 = 주문 총액, 최종 0원")
        fun calculateDiscount_fullDiscount() {
            val coupon = registerCoupon(type = Coupon.Type.RATE, discountValue = 100L)
            val orderAmount = Money(BigDecimal.valueOf(50000))

            val discount = coupon.calculateDiscount(orderAmount)

            assertAll(
                { assertThat(discount).isEqualTo(orderAmount) },
                { assertThat(orderAmount - discount).isEqualTo(Money(BigDecimal.ZERO)) },
            )
        }
    }
}
