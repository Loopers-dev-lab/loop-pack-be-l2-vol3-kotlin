package com.loopers.domain.product.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProductNameTest {

    @Nested
    inner class Create {
        @Test
        fun `유효한_이름으로_생성할_수_있다`() {
            val productName = ProductName("상품명")
            assertThat(productName.value).isEqualTo("상품명")
        }

        @Test
        fun `1자_이름으로_생성할_수_있다`() {
            val productName = ProductName("A")
            assertThat(productName.value).isEqualTo("A")
        }

        @Test
        fun `100자_이름으로_생성할_수_있다`() {
            val value = "A".repeat(100)
            val productName = ProductName(value)
            assertThat(productName.value).isEqualTo(value)
        }

        @Test
        fun `공백만으로는_생성할_수_없다`() {
            val result = assertThrows<CoreException> { ProductName("   ") }
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_PRODUCT_NAME_FORMAT)
        }

        @Test
        fun `빈값으로는_생성할_수_없다`() {
            val result = assertThrows<CoreException> { ProductName("") }
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_PRODUCT_NAME_FORMAT)
        }

        @Test
        fun `101자_이상이면_예외가_발생한다`() {
            val result = assertThrows<CoreException> { ProductName("A".repeat(101)) }
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_PRODUCT_NAME_FORMAT)
        }
    }
}
