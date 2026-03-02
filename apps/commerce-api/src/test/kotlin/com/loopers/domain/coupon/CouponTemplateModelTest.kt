package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class CouponTemplateModelTest {

    private fun createTemplate(
        type: CouponType = CouponType.FIXED,
        value: Long = 1_000L,
        minOrderAmount: Long? = null,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): CouponTemplateModel = CouponTemplateModel(
        name = CouponName("테스트 쿠폰"),
        type = type,
        value = CouponValue(value),
        minOrderAmount = minOrderAmount?.let { MinOrderAmount(it) },
        expiredAt = expiredAt,
    )

    @Nested
    inner class Create {

        @Test
        fun `FIXED 타입으로 생성한다`() {
            assertDoesNotThrow {
                createTemplate(type = CouponType.FIXED, value = 1_000L)
            }
        }

        @Test
        fun `RATE 타입 value=50으로 생성한다`() {
            assertDoesNotThrow {
                createTemplate(type = CouponType.RATE, value = 50L)
            }
        }
    }

    @Nested
    inner class Guard {

        @Test
        fun `RATE 타입 value=1이면 guard를 통과한다`() {
            // given
            val template = createTemplate(type = CouponType.RATE, value = 1L)

            // when / then
            assertDoesNotThrow {
                template.guard()
            }
        }

        @Test
        fun `RATE 타입 value=100이면 guard를 통과한다`() {
            // given
            val template = createTemplate(type = CouponType.RATE, value = 100L)

            // when / then
            assertDoesNotThrow {
                template.guard()
            }
        }

        @Test
        fun `RATE 타입 value=101이면 guard 시 에러를 반환한다`() {
            // given
            val template = createTemplate(type = CouponType.RATE, value = 101L)

            // when / then
            assertThrows<IllegalArgumentException> {
                template.guard()
            }
        }

        @Test
        fun `FIXED 타입은 value 범위 제한이 없다`() {
            // given
            val template = createTemplate(type = CouponType.FIXED, value = 999_999L)

            // when / then
            assertDoesNotThrow {
                template.guard()
            }
        }
    }

    @Nested
    inner class Calculate {

        @Test
        fun `FIXED 타입은 할인값을 반환한다`() {
            // given
            val template = createTemplate(type = CouponType.FIXED, value = 3_000L)

            // when
            val result = template.calculate(10_000L)

            // then
            assertThat(result).isEqualTo(3_000L)
        }

        @Test
        fun `FIXED 타입은 할인값이 주문금액을 초과하면 주문금액을 반환한다`() {
            // given
            val template = createTemplate(type = CouponType.FIXED, value = 3_000L)

            // when
            val result = template.calculate(2_000L)

            // then
            assertThat(result).isEqualTo(2_000L)
        }

        @Test
        fun `RATE 타입은 비율에 따른 할인값을 반환한다`() {
            // given
            val template = createTemplate(type = CouponType.RATE, value = 10L)

            // when
            val result = template.calculate(10_000L)

            // then
            assertThat(result).isEqualTo(1_000L)
        }

        @Test
        fun `RATE 타입은 할인값을 절사한다`() {
            // given
            val template = createTemplate(type = CouponType.RATE, value = 3L)

            // when
            val result = template.calculate(10_000L)

            // then
            assertThat(result).isEqualTo(300L)
        }

        @Test
        fun `최소 주문 금액 조건을 충족하면 정상적으로 계산한다`() {
            // given
            val template = createTemplate(type = CouponType.FIXED, value = 1_000L, minOrderAmount = 5_000L)

            // when
            val result = template.calculate(10_000L)

            // then
            assertThat(result).isEqualTo(1_000L)
        }

        @Test
        fun `최소 주문 금액 조건 미충족 시 에러를 반환한다`() {
            // given
            val template = createTemplate(type = CouponType.FIXED, value = 1_000L, minOrderAmount = 5_000L)

            // when / then
            val ex = assertThrows<CoreException> {
                template.calculate(3_000L)
            }
            assertThat(ex.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        fun `최소 주문 금액이 없으면 조건 없이 계산한다`() {
            // given
            val template = createTemplate(type = CouponType.FIXED, value = 50L, minOrderAmount = null)

            // when
            val result = template.calculate(100L)

            // then
            assertThat(result).isEqualTo(50L)
        }
    }
}
