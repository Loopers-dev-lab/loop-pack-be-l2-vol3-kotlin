package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class BrandModelTest {

    companion object {
        private const val DEFAULT_NAME = "나이키"
        private const val DEFAULT_DESCRIPTION = "스포츠 브랜드"
        private const val DEFAULT_LOGO_URL = "https://example.com/nike-logo.png"
    }

    private fun createBrandModel(
        name: String = DEFAULT_NAME,
        description: String? = DEFAULT_DESCRIPTION,
        logoUrl: String? = DEFAULT_LOGO_URL,
    ) = BrandModel(
        name = name,
        description = description,
        logoUrl = logoUrl,
    )

    @DisplayName("생성")
    @Nested
    inner class Create {

        @DisplayName("유효한 정보가 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsBrandModelWhenValidParametersAreProvided() {
            // act
            val brandModel = createBrandModel()

            // assert
            assertAll(
                { assertThat(brandModel.name).isEqualTo(DEFAULT_NAME) },
                { assertThat(brandModel.description).isEqualTo(DEFAULT_DESCRIPTION) },
                { assertThat(brandModel.logoUrl).isEqualTo(DEFAULT_LOGO_URL) },
            )
        }

        @DisplayName("이름이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestExceptionWhenNameIsBlank() {
            // act
            val result = assertThrows<CoreException> {
                createBrandModel(name = "   ")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
