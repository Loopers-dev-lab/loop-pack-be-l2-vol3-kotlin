package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

@DisplayName("Brand 등록")
class BrandRegisterTest {

    @Nested
    @DisplayName("브랜드명이 1~50자이면 등록에 성공한다")
    inner class WhenValidName {
        @Test
        @DisplayName("유효한 이름으로 등록하면 status=INACTIVE, id=null로 생성된다")
        fun register_validName() {
            val brand = Brand.register(name = "나이키")

            assertAll(
                { assertThat(brand.id).isNull() },
                { assertThat(brand.name.value).isEqualTo("나이키") },
                { assertThat(brand.status).isEqualTo(Brand.Status.INACTIVE) },
            )
        }

        @Test
        @DisplayName("50자 이름으로 등록한다 (경계값)")
        fun register_maxLengthName() {
            val name = "가".repeat(50)
            val brand = Brand.register(name = name)
            assertThat(brand.name.value).isEqualTo(name)
        }
    }

    @Nested
    @DisplayName("브랜드명이 유효하지 않으면 등록에 실패한다")
    inner class WhenInvalidName {
        @Test
        @DisplayName("빈 문자열이면 예외를 던진다")
        fun register_emptyName() {
            val exception = assertThrows<CoreException> { Brand.register(name = "") }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_INVALID_NAME)
        }

        @Test
        @DisplayName("공백만 있으면 예외를 던진다")
        fun register_blankName() {
            val exception = assertThrows<CoreException> { Brand.register(name = "   ") }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_INVALID_NAME)
        }

        @Test
        @DisplayName("51자 이상이면 예외를 던진다")
        fun register_tooLongName() {
            val name = "가".repeat(51)
            val exception = assertThrows<CoreException> { Brand.register(name = name) }
            assertThat(exception.errorType).isEqualTo(ErrorType.BRAND_INVALID_NAME)
        }
    }
}
