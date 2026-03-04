package com.loopers.domain.product.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StockTest {

    @Nested
    inner class Create {
        @Test
        fun `0_이상_재고로_생성할_수_있다`() {
            val stock = Stock(10)
            assertThat(stock.value).isEqualTo(10)
        }

        @Test
        fun `0_재고로_생성할_수_있다`() {
            val stock = Stock(0)
            assertThat(stock.value).isEqualTo(0)
        }

        @Test
        fun `음수_재고면_예외가_발생한다`() {
            val result = assertThrows<CoreException> { Stock(-1) }
            assertThat(result.errorType).isEqualTo(ErrorType.INSUFFICIENT_STOCK)
        }
    }

    @Nested
    inner class Deduct {
        @Test
        fun `재고를_차감할_수_있다`() {
            val stock = Stock(10)
            val deducted = stock.deduct(3)
            assertThat(deducted.value).isEqualTo(7)
        }

        @Test
        fun `재고를_전부_차감할_수_있다`() {
            val stock = Stock(5)
            val deducted = stock.deduct(5)
            assertThat(deducted.value).isEqualTo(0)
        }

        @Test
        fun `재고가_부족하면_예외가_발생한다`() {
            val stock = Stock(3)
            val result = assertThrows<CoreException> { stock.deduct(5) }
            assertThat(result.errorType).isEqualTo(ErrorType.INSUFFICIENT_STOCK)
        }
    }

    @Nested
    inner class Restore {
        @Test
        fun `재고를_복원할_수_있다`() {
            val stock = Stock(5)
            val restored = stock.restore(3)
            assertThat(restored.value).isEqualTo(8)
        }

        @Test
        fun `0_재고에서_복원할_수_있다`() {
            val stock = Stock(0)
            val restored = stock.restore(10)
            assertThat(restored.value).isEqualTo(10)
        }
    }
}
