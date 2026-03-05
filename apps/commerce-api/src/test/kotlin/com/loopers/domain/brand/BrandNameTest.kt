package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("BrandName VO")
class BrandNameTest {

    @Nested
    @DisplayName("유효한 브랜드명이면 생성에 성공한다")
    inner class WhenValidName {
        @Test
        @DisplayName("1자 이상 50자 이하 브랜드명으로 생성한다")
        fun create_validName() {
            val brandName = BrandName("나이키")
            assertThat(brandName.value).isEqualTo("나이키")
        }

        @Test
        @DisplayName("50자 브랜드명으로 생성한다 (경계값)")
        fun create_maxLengthName() {
            val name = "가".repeat(50)
            val brandName = BrandName(name)
            assertThat(brandName.value).isEqualTo(name)
        }

        @Test
        @DisplayName("1자 브랜드명으로 생성한다 (경계값)")
        fun create_minLengthName() {
            val brandName = BrandName("A")
            assertThat(brandName.value).isEqualTo("A")
        }
    }

    @Nested
    @DisplayName("유효하지 않은 브랜드명이면 생성에 실패한다")
    inner class WhenInvalidName {
        @Test
        @DisplayName("빈 문자열이면 예외를 던진다")
        fun create_emptyName() {
            val exception = assertThrows<CoreException> { BrandName("") }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_INVALID_NAME)
        }

        @Test
        @DisplayName("공백만 있으면 예외를 던진다")
        fun create_blankName() {
            val exception = assertThrows<CoreException> { BrandName("   ") }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_INVALID_NAME)
        }

        @Test
        @DisplayName("51자 이상이면 예외를 던진다")
        fun create_tooLongName() {
            val name = "가".repeat(51)
            val exception = assertThrows<CoreException> { BrandName(name) }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_INVALID_NAME)
        }
    }
}
