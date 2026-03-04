package com.loopers.domain.product.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProductPriceTest {

    @Nested
    inner class Create {
        @Test
        fun `0원으로_생성할_수_있다`() {
            val price = ProductPrice(0)
            assertThat(price.value).isEqualTo(0)
        }

        @Test
        fun `양수_가격으로_생성할_수_있다`() {
            val price = ProductPrice(10000)
            assertThat(price.value).isEqualTo(10000)
        }

        @Test
        fun `음수_가격이면_예외가_발생한다`() {
            val result = assertThrows<CoreException> { ProductPrice(-1) }
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_PRODUCT_PRICE)
        }
    }
}
