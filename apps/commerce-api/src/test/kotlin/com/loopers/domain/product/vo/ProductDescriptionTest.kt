package com.loopers.domain.product.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProductDescriptionTest {

    @Nested
    inner class Create {
        @Test
        fun `빈_설명으로_생성할_수_있다`() {
            val description = ProductDescription("")
            assertThat(description.value).isEqualTo("")
        }

        @Test
        fun `1000자_설명으로_생성할_수_있다`() {
            val value = "A".repeat(1000)
            val description = ProductDescription(value)
            assertThat(description.value).isEqualTo(value)
        }

        @Test
        fun `1001자_이상이면_예외가_발생한다`() {
            val result = assertThrows<CoreException> { ProductDescription("A".repeat(1001)) }
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_PRODUCT_DESCRIPTION_LENGTH)
        }
    }
}
