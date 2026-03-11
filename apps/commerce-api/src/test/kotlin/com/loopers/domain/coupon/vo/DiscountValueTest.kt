package com.loopers.domain.coupon.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DiscountValueTest {

    @Nested
    inner class Create {
        @Test
        fun `유효한_값으로_생성할_수_있다`() {
            val discountValue = DiscountValue(1000L)
            assertThat(discountValue.value).isEqualTo(1000L)
        }

        @Test
        fun `1로_생성할_수_있다`() {
            val discountValue = DiscountValue(1L)
            assertThat(discountValue.value).isEqualTo(1L)
        }

        @Test
        fun `100으로_생성할_수_있다`() {
            val discountValue = DiscountValue(100L)
            assertThat(discountValue.value).isEqualTo(100L)
        }

        @Test
        fun `0이면_예외가_발생한다`() {
            val result = assertThrows<CoreException> { DiscountValue(0L) }
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_COUPON_VALUE)
        }

        @Test
        fun `음수이면_예외가_발생한다`() {
            val result = assertThrows<CoreException> { DiscountValue(-1L) }
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_COUPON_VALUE)
        }
    }
}
