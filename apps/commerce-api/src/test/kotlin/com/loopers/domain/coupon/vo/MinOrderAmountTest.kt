package com.loopers.domain.coupon.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MinOrderAmountTest {

    @Nested
    inner class Create {
        @Test
        fun `null로_생성할_수_있다`() {
            val minOrderAmount = MinOrderAmount(null)
            assertThat(minOrderAmount.value).isNull()
        }

        @Test
        fun `0으로_생성할_수_있다`() {
            val minOrderAmount = MinOrderAmount(0L)
            assertThat(minOrderAmount.value).isEqualTo(0L)
        }

        @Test
        fun `양수로_생성할_수_있다`() {
            val minOrderAmount = MinOrderAmount(10000L)
            assertThat(minOrderAmount.value).isEqualTo(10000L)
        }

        @Test
        fun `음수이면_예외가_발생한다`() {
            val result = assertThrows<CoreException> { MinOrderAmount(-1L) }
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_MIN_ORDER_AMOUNT)
        }
    }
}
