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

        @DisplayName("만료된 쿠폰은 적용 불가능하다")
        @Test
        fun isApplicable_returnsFalse_whenCouponIsExpired() {
            // arrange
            val template = CouponTemplate.createForTest(
                name = "쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().minusSeconds(1),
            )
            val orderAmount = BigDecimal("15000")

            // act
            val isApplicable = template.isApplicable(orderAmount)

            // assert
            assertThat(isApplicable).isFalse()
        }

        @DisplayName("유효 기간 직전은 적용 가능하다")
        @Test
        fun isApplicable_returnsTrue_justBeforeExpiration() {
            // arrange
            val now = ZonedDateTime.now()
            val expiredAt = now.plusSeconds(10)
            val template = CouponTemplate.create(
                name = "쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = expiredAt,
            )
            val orderAmount = BigDecimal("15000")

            // act
            val isApplicable = template.isApplicable(orderAmount)

            // assert
            assertThat(isApplicable).isTrue()
        }

        @DisplayName("쿠폰이 만료되었는지 확인한다")
        @Test
        fun isExpired_returnsTrue_whenCouponIsExpired() {
            // arrange
            val template = CouponTemplate.createForTest(
                name = "쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().minusSeconds(1),
            )

            // act
            val isExpired = template.isExpired()

            // assert
            assertThat(isExpired).isTrue()
        }

        @DisplayName("쿠폰이 만료되지 않았는지 확인한다")
        @Test
        fun isExpired_returnsFalse_whenCouponIsNotExpired() {
            // arrange
            val template = CouponTemplate.create(
                name = "쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // act
            val isExpired = template.isExpired()

            // assert
            assertThat(isExpired).isFalse()
        }
    }

    @DisplayName("쿠폰 템플릿 정보 업데이트")
    @Nested
    inner class UpdateInfo {

        @DisplayName("유효한 정보로 업데이트한다")
        @Test
        fun updateInfo_success() {
            // arrange
            val originalTemplate = CouponTemplate.create(
                name = "기존 쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )
            val newName = "업데이트된 쿠폰"
            val newValue = BigDecimal("10000")
            val newMinOrderAmount = BigDecimal("20000")
            val newExpiredAt = ZonedDateTime.now().plusDays(60)

            // act
            originalTemplate.updateInfo(
                newName = newName,
                newValue = newValue,
                newMinOrderAmount = newMinOrderAmount,
                newExpiredAt = newExpiredAt,
            )

            // assert
            assertThat(originalTemplate.name).isEqualTo(newName)
            assertThat(originalTemplate.value).isEqualTo(newValue)
            assertThat(originalTemplate.minOrderAmount).isEqualTo(newMinOrderAmount)
            assertThat(originalTemplate.expiredAt).isEqualTo(newExpiredAt)
        }

        @DisplayName("이름이 비어있으면 예외가 발생하고 원본 상태가 유지된다")
        @Test
        fun updateInfo_throwsException_whenNameIsBlank() {
            // arrange
            val originalTemplate = CouponTemplate.create(
                name = "기존 쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )
            val originalName = originalTemplate.name
            val originalValue = originalTemplate.value
            val originalMinOrderAmount = originalTemplate.minOrderAmount
            val originalExpiredAt = originalTemplate.expiredAt

            // act & assert
            assertThatThrownBy {
                originalTemplate.updateInfo(
                    newName = "",
                    newValue = BigDecimal("10000"),
                    newMinOrderAmount = BigDecimal("20000"),
                    newExpiredAt = ZonedDateTime.now().plusDays(60),
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)

            // assert original state is unchanged
            assertThat(originalTemplate.name).isEqualTo(originalName)
            assertThat(originalTemplate.value).isEqualTo(originalValue)
            assertThat(originalTemplate.minOrderAmount).isEqualTo(originalMinOrderAmount)
            assertThat(originalTemplate.expiredAt).isEqualTo(originalExpiredAt)
        }

        @DisplayName("할인 금액이 음수이면 예외가 발생하고 원본 상태가 유지된다")
        @Test
        fun updateInfo_throwsException_whenValueIsNegative() {
            // arrange
            val originalTemplate = CouponTemplate.create(
                name = "기존 쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )
            val originalName = originalTemplate.name
            val originalValue = originalTemplate.value
            val originalMinOrderAmount = originalTemplate.minOrderAmount
            val originalExpiredAt = originalTemplate.expiredAt

            // act & assert
            assertThatThrownBy {
                originalTemplate.updateInfo(
                    newName = "업데이트된 쿠폰",
                    newValue = BigDecimal("-1000"),
                    newMinOrderAmount = BigDecimal("20000"),
                    newExpiredAt = ZonedDateTime.now().plusDays(60),
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)

            // assert original state is unchanged
            assertThat(originalTemplate.name).isEqualTo(originalName)
            assertThat(originalTemplate.value).isEqualTo(originalValue)
            assertThat(originalTemplate.minOrderAmount).isEqualTo(originalMinOrderAmount)
            assertThat(originalTemplate.expiredAt).isEqualTo(originalExpiredAt)
        }

        @DisplayName("최소 주문액이 음수이면 예외가 발생하고 원본 상태가 유지된다")
        @Test
        fun updateInfo_throwsException_whenMinOrderAmountIsNegative() {
            // arrange
            val originalTemplate = CouponTemplate.create(
                name = "기존 쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )
            val originalName = originalTemplate.name
            val originalValue = originalTemplate.value
            val originalMinOrderAmount = originalTemplate.minOrderAmount
            val originalExpiredAt = originalTemplate.expiredAt

            // act & assert
            assertThatThrownBy {
                originalTemplate.updateInfo(
                    newName = "업데이트된 쿠폰",
                    newValue = BigDecimal("10000"),
                    newMinOrderAmount = BigDecimal("-1000"),
                    newExpiredAt = ZonedDateTime.now().plusDays(60),
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)

            // assert original state is unchanged
            assertThat(originalTemplate.name).isEqualTo(originalName)
            assertThat(originalTemplate.value).isEqualTo(originalValue)
            assertThat(originalTemplate.minOrderAmount).isEqualTo(originalMinOrderAmount)
            assertThat(originalTemplate.expiredAt).isEqualTo(originalExpiredAt)
        }

        @DisplayName("유효기간이 과거이면 예외가 발생하고 원본 상태가 유지된다")
        @Test
        fun updateInfo_throwsException_whenExpiredAtIsInPast() {
            // arrange
            val originalTemplate = CouponTemplate.create(
                name = "기존 쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )
            val originalName = originalTemplate.name
            val originalValue = originalTemplate.value
            val originalMinOrderAmount = originalTemplate.minOrderAmount
            val originalExpiredAt = originalTemplate.expiredAt

            // act & assert
            assertThatThrownBy {
                originalTemplate.updateInfo(
                    newName = "업데이트된 쿠폰",
                    newValue = BigDecimal("10000"),
                    newMinOrderAmount = BigDecimal("20000"),
                    newExpiredAt = ZonedDateTime.now().minusDays(1),
                )
            }
                .isInstanceOf(CoreException::class.java)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.BAD_REQUEST)

            // assert original state is unchanged
            assertThat(originalTemplate.name).isEqualTo(originalName)
            assertThat(originalTemplate.value).isEqualTo(originalValue)
            assertThat(originalTemplate.minOrderAmount).isEqualTo(originalMinOrderAmount)
            assertThat(originalTemplate.expiredAt).isEqualTo(originalExpiredAt)
        }
    }
}
