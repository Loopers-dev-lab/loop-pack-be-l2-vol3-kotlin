package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime

@DisplayName("CouponTemplate")
class CouponTemplateTest {

    @DisplayName("쿠폰 템플릿")
    @Nested
    inner class CouponTemplateEntity {

        @DisplayName("쿠폰 템플릿을 생성한다")
        @Test
        fun createCouponTemplate_success() {
            // arrange
            val name = "신규 가입 쿠폰"
            val type = CouponType.FIXED
            val value = BigDecimal("5000")
            val minOrderAmount = BigDecimal("10000")
            val expiredAt = ZonedDateTime.now().plusDays(30)

            // act
            val template = CouponTemplate.create(
                name = name,
                type = type,
                value = value,
                minOrderAmount = minOrderAmount,
                expiredAt = expiredAt,
            )

            // assert
            assertThat(template.name).isEqualTo(name)
            assertThat(template.type).isEqualTo(type)
            assertThat(template.value).isEqualTo(value)
            assertThat(template.minOrderAmount).isEqualTo(minOrderAmount)
            assertThat(template.expiredAt).isEqualTo(expiredAt)
        }

        @DisplayName("쿠폰 이름이 비어있으면 예외가 발생한다")
        @Test
        fun createCouponTemplate_throwsException_whenNameIsBlank() {
            // act & assert
            assertThatThrownBy {
                CouponTemplate.create(
                    name = "",
                    type = CouponType.FIXED,
                    value = BigDecimal("5000"),
                    minOrderAmount = BigDecimal("10000"),
                    expiredAt = ZonedDateTime.now().plusDays(30),
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("할인 금액이 음수이면 예외가 발생한다")
        @Test
        fun createCouponTemplate_throwsException_whenValueIsNegative() {
            // act & assert
            assertThatThrownBy {
                CouponTemplate.create(
                    name = "쿠폰",
                    type = CouponType.FIXED,
                    value = BigDecimal("-1000"),
                    minOrderAmount = BigDecimal("10000"),
                    expiredAt = ZonedDateTime.now().plusDays(30),
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("최소 주문액이 음수이면 예외가 발생한다")
        @Test
        fun createCouponTemplate_throwsException_whenMinOrderAmountIsNegative() {
            // act & assert
            assertThatThrownBy {
                CouponTemplate.create(
                    name = "쿠폰",
                    type = CouponType.FIXED,
                    value = BigDecimal("5000"),
                    minOrderAmount = BigDecimal("-1000"),
                    expiredAt = ZonedDateTime.now().plusDays(30),
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("유효기간이 현재보다 과거이면 예외가 발생한다")
        @Test
        fun createCouponTemplate_throwsException_whenExpiredAtIsInPast() {
            // act & assert
            assertThatThrownBy {
                CouponTemplate.create(
                    name = "쿠폰",
                    type = CouponType.FIXED,
                    value = BigDecimal("5000"),
                    minOrderAmount = BigDecimal("10000"),
                    expiredAt = ZonedDateTime.now().minusDays(1),
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)
        }

        @DisplayName("주문금액이 최소 주문액 이상인지 확인한다")
        @Test
        fun isApplicable_returnsTrue_whenOrderAmountMeetsMinimum() {
            // arrange
            val template = CouponTemplate.create(
                name = "쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )
            val orderAmount = BigDecimal("15000")

            // act
            val isApplicable = template.isApplicable(orderAmount)

            // assert
            assertThat(isApplicable).isTrue()
        }

        @DisplayName("주문금액이 최소 주문액 미만이면 적용 불가능하다")
        @Test
        fun isApplicable_returnsFalse_whenOrderAmountBelowMinimum() {
            // arrange
            val template = CouponTemplate.create(
                name = "쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )
            val orderAmount = BigDecimal("5000")

            // act
            val isApplicable = template.isApplicable(orderAmount)

            // assert
            assertThat(isApplicable).isFalse()
        }
    }
}
