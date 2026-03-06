package com.loopers.domain.coupon.service

import com.loopers.domain.common.vo.CouponId
import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.coupon.model.Coupon
import com.loopers.domain.coupon.model.IssuedCoupon
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.ZonedDateTime

class CouponValidatorTest {

    private val validator = CouponValidator()

    private fun coupon(
        id: CouponId = CouponId(1L),
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(7),
        minOrderAmount: Money? = null,
    ) = Coupon(
        id = id,
        name = "테스트쿠폰",
        type = Coupon.CouponType.FIXED,
        value = 1000L,
        minOrderAmount = minOrderAmount,
        expiredAt = expiredAt,
    )

    private fun issuedCoupon(
        refCouponId: CouponId = CouponId(1L),
        refUserId: UserId = UserId(1L),
        status: IssuedCoupon.CouponStatus = IssuedCoupon.CouponStatus.AVAILABLE,
    ) = IssuedCoupon(
        refCouponId = refCouponId,
        refUserId = refUserId,
        status = status,
        usedAt = null,
        createdAt = ZonedDateTime.now(),
    )

    @Nested
    @DisplayName("validateForOrder 호출 시")
    inner class ValidateForOrder {

        @Test
        @DisplayName("발급 쿠폰이 참조하는 쿠폰 ID와 전달된 쿠폰의 ID가 불일치하면 BAD_REQUEST 예외가 발생한다")
        fun validateForOrder_couponIdMismatch_throwsBadRequest() {
            // arrange
            val coupon = coupon(id = CouponId(1L))
            val issuedCoupon = issuedCoupon(refCouponId = CouponId(999L))
            val userId = UserId(1L)
            val orderAmount = Money(BigDecimal("10000"))

            // act
            val exception = assertThrows<CoreException> {
                validator.validateForOrder(issuedCoupon, coupon, userId, orderAmount)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.customMessage).isEqualTo("발급 쿠폰과 쿠폰 정보가 일치하지 않습니다.")
        }

        @Test
        @DisplayName("발급 쿠폰의 소유자가 요청 사용자와 다르면 BAD_REQUEST 예외가 발생한다")
        fun validateForOrder_userIdMismatch_throwsBadRequest() {
            // arrange
            val couponId = CouponId(1L)
            val coupon = coupon(id = couponId)
            val issuedCoupon = issuedCoupon(refCouponId = couponId, refUserId = UserId(999L))
            val userId = UserId(1L)
            val orderAmount = Money(BigDecimal("10000"))

            // act
            val exception = assertThrows<CoreException> {
                validator.validateForOrder(issuedCoupon, coupon, userId, orderAmount)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.customMessage).isEqualTo("본인 소유의 쿠폰이 아닙니다.")
        }

        @Test
        @DisplayName("발급 쿠폰의 상태가 USED이면 BAD_REQUEST 예외가 발생한다")
        fun validateForOrder_statusUsed_throwsBadRequest() {
            // arrange
            val couponId = CouponId(1L)
            val userId = UserId(1L)
            val coupon = coupon(id = couponId)
            val issuedCoupon = IssuedCoupon(
                refCouponId = couponId,
                refUserId = userId,
                status = IssuedCoupon.CouponStatus.USED,
                usedAt = ZonedDateTime.now().minusDays(1),
                createdAt = ZonedDateTime.now().minusDays(2),
            )
            val orderAmount = Money(BigDecimal("10000"))

            // act
            val exception = assertThrows<CoreException> {
                validator.validateForOrder(issuedCoupon, coupon, userId, orderAmount)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.customMessage).isEqualTo("사용할 수 없는 쿠폰입니다.")
        }

        @Test
        @DisplayName("쿠폰의 만료일이 지났으면 BAD_REQUEST 예외가 발생한다")
        fun validateForOrder_couponExpired_throwsBadRequest() {
            // arrange
            val couponId = CouponId(1L)
            val userId = UserId(1L)
            val coupon = coupon(id = couponId, expiredAt = ZonedDateTime.now().minusDays(1))
            val issuedCoupon = issuedCoupon(refCouponId = couponId, refUserId = userId)
            val orderAmount = Money(BigDecimal("10000"))

            // act
            val exception = assertThrows<CoreException> {
                validator.validateForOrder(issuedCoupon, coupon, userId, orderAmount)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.customMessage).isEqualTo("만료된 쿠폰입니다.")
        }

        @Test
        @DisplayName("주문 금액이 쿠폰의 최소 주문 금액에 미달하면 BAD_REQUEST 예외가 발생한다")
        fun validateForOrder_orderAmountBelowMinimum_throwsBadRequest() {
            // arrange
            val couponId = CouponId(1L)
            val userId = UserId(1L)
            val coupon = coupon(id = couponId, minOrderAmount = Money(BigDecimal("20000")))
            val issuedCoupon = issuedCoupon(refCouponId = couponId, refUserId = userId)
            val orderAmount = Money(BigDecimal("10000"))

            // act
            val exception = assertThrows<CoreException> {
                validator.validateForOrder(issuedCoupon, coupon, userId, orderAmount)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.customMessage).isEqualTo("최소 주문 금액을 충족하지 않습니다.")
        }

        @Test
        @DisplayName("쿠폰 ID 일치, 본인 소유, 유효한 상태, 충분한 주문금액이면 예외가 발생하지 않는다")
        fun validateForOrder_validConditions_doesNotThrow() {
            // arrange
            val couponId = CouponId(1L)
            val userId = UserId(1L)
            val coupon = coupon(id = couponId, minOrderAmount = Money(BigDecimal("5000")))
            val issuedCoupon = issuedCoupon(refCouponId = couponId, refUserId = userId)
            val orderAmount = Money(BigDecimal("10000"))

            // act & assert
            assertDoesNotThrow {
                validator.validateForOrder(issuedCoupon, coupon, userId, orderAmount)
            }
        }
    }
}
