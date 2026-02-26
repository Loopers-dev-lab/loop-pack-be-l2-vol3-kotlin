package com.loopers.domain.product

import com.loopers.domain.product.vo.ProductName
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ProductNameTest {
    @DisplayName("상품 이름을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("유효한 이름이면, 정상적으로 생성된다.")
        @Test
        fun createsProductName_whenNameIsValid() {
            val name = assertDoesNotThrow { ProductName.of("감성 티셔츠") }
            assertThat(name.value).isEqualTo("감성 티셔츠")
        }

        @DisplayName("255자를 초과하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNameExceedsMaxLength() {
            val result = assertThrows<CoreException> { ProductName.of("a".repeat(256)) }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
