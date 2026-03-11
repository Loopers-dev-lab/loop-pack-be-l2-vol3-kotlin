package com.loopers.domain.coupon.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CouponNameTest {

    @Nested
    inner class Create {
        @Test
        fun `유효한_이름으로_생성할_수_있다`() {
            val couponName = CouponName("신규가입 할인 쿠폰")
            assertThat(couponName.value).isEqualTo("신규가입 할인 쿠폰")
        }

        @Test
        fun `1자_이름으로_생성할_수_있다`() {
            val couponName = CouponName("A")
            assertThat(couponName.value).isEqualTo("A")
        }

        @Test
        fun `50자_이름으로_생성할_수_있다`() {
            val value = "A".repeat(50)
            val couponName = CouponName(value)
            assertThat(couponName.value).isEqualTo(value)
        }

        @Test
        fun `공백만으로는_생성할_수_없다`() {
            val result = assertThrows<CoreException> { CouponName("   ") }
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_COUPON_NAME)
        }

        @Test
        fun `빈값으로는_생성할_수_없다`() {
            val result = assertThrows<CoreException> { CouponName("") }
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_COUPON_NAME)
        }

        @Test
        fun `51자_이상이면_예외가_발생한다`() {
            val result = assertThrows<CoreException> { CouponName("A".repeat(51)) }
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_COUPON_NAME)
        }
    }
}
