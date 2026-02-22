package com.loopers.domain.product

import com.loopers.domain.product.vo.ProductDescription
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ProductDescriptionTest {
    @DisplayName("상품 설명을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("유효한 설명이면, 정상적으로 생성된다.")
        @Test
        fun createsDescription_whenValid() {
            val desc = assertDoesNotThrow { ProductDescription.of("좋은 상품입니다.") }
            assertThat(desc.value).isEqualTo("좋은 상품입니다.")
        }

        @DisplayName("1000자를 초과하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenExceedsMaxLength() {
            val result = assertThrows<CoreException> { ProductDescription.of("a".repeat(1001)) }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
