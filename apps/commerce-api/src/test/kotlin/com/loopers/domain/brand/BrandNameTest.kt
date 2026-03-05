package com.loopers.domain.brand

import com.loopers.domain.brand.vo.BrandName
import com.loopers.domain.error.CoreException
import com.loopers.domain.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class BrandNameTest {
    @DisplayName("브랜드 이름을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("유효한 이름이면, 정상적으로 생성된다.")
        @Test
        fun createsBrandName_whenNameIsValid() {
            val brandName = assertDoesNotThrow { BrandName.of("루퍼스") }
            assertThat(brandName.value).isEqualTo("루퍼스")
        }

        @DisplayName("255자를 초과하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNameExceedsMaxLength() {
            val longName = "a".repeat(256)
            val result = assertThrows<CoreException> { BrandName.of(longName) }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("255자이면, 정상적으로 생성된다.")
        @Test
        fun createsBrandName_whenNameIsExactlyMaxLength() {
            val maxName = "a".repeat(255)
            val brandName = assertDoesNotThrow { BrandName.of(maxName) }
            assertThat(brandName.value).isEqualTo(maxName)
        }
    }
}
