package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class CouponTest {

    @DisplayName("쿠폰을 생성할 때,")
    @Nested
    inner class Create {
        private val name = "신규가입 할인"
        private val discountType = DiscountType.FIXED_AMOUNT
        private val discountValue = 5000L
        private val totalQuantity = 100
        private val expiresAt: ZonedDateTime = ZonedDateTime.now().plusDays(30)

        @DisplayName("유효한 값이 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsCoupon_whenValidValuesProvided() {
            // arrange & act
            val coupon = Coupon(
                name = name,
                discountType = discountType,
                discountValue = discountValue,
                totalQuantity = totalQuantity,
                expiresAt = expiresAt,
            )

            // assert
            assertAll(
                { assertThat(coupon.name).isEqualTo(name) },
                { assertThat(coupon.discountType).isEqualTo(discountType) },
                { assertThat(coupon.discountValue).isEqualTo(discountValue) },
                { assertThat(coupon.totalQuantity).isEqualTo(totalQuantity) },
                { assertThat(coupon.issuedQuantity).isEqualTo(0) },
                { assertThat(coupon.expiresAt).isEqualTo(expiresAt) },
            )
        }

        @DisplayName("쿠폰명이 빈 문자열이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNameIsBlank() {
            // act
            val exception = assertThrows<CoreException> {
                Coupon(
                    name = "",
                    discountType = discountType,
                    discountValue = discountValue,
                    totalQuantity = totalQuantity,
                    expiresAt = expiresAt,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("할인값이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenDiscountValueIsNotPositive() {
            // act
            val exception = assertThrows<CoreException> {
                Coupon(
                    name = name,
                    discountType = discountType,
                    discountValue = 0L,
                    totalQuantity = totalQuantity,
                    expiresAt = expiresAt,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("총 발급 수량이 0 이하이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenTotalQuantityIsNotPositive() {
            // act
            val exception = assertThrows<CoreException> {
                Coupon(
                    name = name,
                    discountType = discountType,
                    discountValue = discountValue,
                    totalQuantity = 0,
                    expiresAt = expiresAt,
                )
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("쿠폰을 발급할 때,")
    @Nested
    inner class Issue {

        @DisplayName("유효한 쿠폰이면, 발급 수량이 1 증가한다.")
        @Test
        fun increasesIssuedQuantity_whenCouponIsValid() {
            // arrange
            val coupon = Coupon(
                name = "신규가입 할인",
                discountType = DiscountType.FIXED_AMOUNT,
                discountValue = 5000L,
                totalQuantity = 100,
                expiresAt = ZonedDateTime.now().plusDays(30),
            )

            // act
            coupon.issue()

            // assert
            assertThat(coupon.issuedQuantity).isEqualTo(1)
        }

        @DisplayName("만료된 쿠폰이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenCouponIsExpired() {
            // arrange
            val coupon = Coupon(
                name = "만료된 쿠폰",
                discountType = DiscountType.FIXED_AMOUNT,
                discountValue = 5000L,
                totalQuantity = 100,
                expiresAt = ZonedDateTime.now().minusDays(1),
            )

            // act
            val exception = assertThrows<CoreException> {
                coupon.issue()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("발급 수량이 소진되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenCouponIsExhausted() {
            // arrange
            val coupon = Coupon(
                name = "한정 쿠폰",
                discountType = DiscountType.FIXED_AMOUNT,
                discountValue = 5000L,
                totalQuantity = 1,
                expiresAt = ZonedDateTime.now().plusDays(30),
            )
            coupon.issue()

            // act
            val exception = assertThrows<CoreException> {
                coupon.issue()
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
