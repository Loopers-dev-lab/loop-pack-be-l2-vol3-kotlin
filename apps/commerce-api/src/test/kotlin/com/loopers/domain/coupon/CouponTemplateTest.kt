package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class CouponTemplateTest {
    @DisplayName("쿠폰 템플릿을 생성할 때, ")
    @Nested
    inner class Create {
        @DisplayName("정액 쿠폰을 정상적으로 생성한다.")
        @Test
        fun createsFixedCoupon() {
            // arrange & act
            val template = CouponTemplate(
                name = "1000원 할인",
                type = CouponType.FIXED,
                value = 1000,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // assert
            assertThat(template.name).isEqualTo("1000원 할인")
            assertThat(template.type).isEqualTo(CouponType.FIXED)
            assertThat(template.value).isEqualTo(1000)
        }

        @DisplayName("정률 쿠폰을 정상적으로 생성한다.")
        @Test
        fun createsRateCoupon() {
            // arrange & act
            val template = CouponTemplate(
                name = "10% 할인",
                type = CouponType.RATE,
                value = 10,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // assert
            assertThat(template.type).isEqualTo(CouponType.RATE)
            assertThat(template.value).isEqualTo(10)
        }

        @DisplayName("할인 값이 0 이하이면 예외가 발생한다.")
        @Test
        fun throwsException_whenValueIsZeroOrNegative() {
            // act
            val exception = assertThrows<CoreException> {
                CouponTemplate(name = "잘못된 쿠폰", type = CouponType.FIXED, value = 0, expiredAt = ZonedDateTime.now().plusDays(30))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("정률 쿠폰의 할인 값이 100을 초과하면 예외가 발생한다.")
        @Test
        fun throwsException_whenRateExceeds100() {
            // act
            val exception = assertThrows<CoreException> {
                CouponTemplate(name = "잘못된 쿠폰", type = CouponType.RATE, value = 101, expiredAt = ZonedDateTime.now().plusDays(30))
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("할인 금액을 계산할 때, ")
    @Nested
    inner class CalculateDiscount {
        @DisplayName("정액 쿠폰은 고정 금액을 할인한다.")
        @Test
        fun calculatesFixedDiscount() {
            // arrange
            val template = CouponTemplate(name = "3000원 할인", type = CouponType.FIXED, value = 3000, expiredAt = ZonedDateTime.now().plusDays(30))

            // act
            val discount = template.calculateDiscount(10000)

            // assert
            assertThat(discount).isEqualTo(3000)
        }

        @DisplayName("정률 쿠폰은 비율에 따라 할인한다.")
        @Test
        fun calculatesRateDiscount() {
            // arrange
            val template = CouponTemplate(name = "10% 할인", type = CouponType.RATE, value = 10, expiredAt = ZonedDateTime.now().plusDays(30))

            // act
            val discount = template.calculateDiscount(10000)

            // assert
            assertThat(discount).isEqualTo(1000)
        }

        @DisplayName("할인 금액이 총 금액을 초과하면 총 금액으로 제한한다.")
        @Test
        fun capsDiscountAtTotalPrice() {
            // arrange
            val template = CouponTemplate(name = "5000원 할인", type = CouponType.FIXED, value = 5000, expiredAt = ZonedDateTime.now().plusDays(30))

            // act
            val discount = template.calculateDiscount(3000)

            // assert
            assertThat(discount).isEqualTo(3000)
        }
    }

    @DisplayName("만료 여부를 확인할 때, ")
    @Nested
    inner class IsExpired {
        @DisplayName("만료 시간이 지나면 true를 반환한다.")
        @Test
        fun returnsTrue_whenExpired() {
            // arrange
            val template = CouponTemplate(name = "만료 쿠폰", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().minusDays(1))

            // act & assert
            assertThat(template.isExpired()).isTrue()
        }

        @DisplayName("만료 시간이 지나지 않으면 false를 반환한다.")
        @Test
        fun returnsFalse_whenNotExpired() {
            // arrange
            val template = CouponTemplate(name = "유효 쿠폰", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30))

            // act & assert
            assertThat(template.isExpired()).isFalse()
        }
    }

    @DisplayName("쿠폰 템플릿을 수정할 때, ")
    @Nested
    inner class Update {
        @DisplayName("이름, 값, 최소 주문 금액, 만료일을 수정한다.")
        @Test
        fun updatesFields() {
            // arrange
            val template = CouponTemplate(name = "기존 쿠폰", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30))
            val newExpiredAt = ZonedDateTime.now().plusDays(60)

            // act
            template.update(name = "수정된 쿠폰", value = 2000, minOrderAmount = 10000, expiredAt = newExpiredAt)

            // assert
            assertThat(template.name).isEqualTo("수정된 쿠폰")
            assertThat(template.value).isEqualTo(2000)
            assertThat(template.minOrderAmount).isEqualTo(10000)
            assertThat(template.expiredAt).isEqualTo(newExpiredAt)
        }
    }
}
