package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class BrandTest {

    private fun createBrand(
        name: String = "나이키",
        description: String? = "스포츠 브랜드",
        imageUrl: String? = "https://example.com/nike.png",
    ): Brand = Brand(
        name = name,
        description = description,
        imageUrl = imageUrl,
    )

    @Nested
    inner class CreateBrand {

        @Test
        @DisplayName("모든 정보가 올바르면 정상적으로 생성된다")
        fun success() {
            // arrange & act
            val brand = createBrand()

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo("나이키") },
                { assertThat(brand.description).isEqualTo("스포츠 브랜드") },
                { assertThat(brand.imageUrl).isEqualTo("https://example.com/nike.png") },
            )
        }

        @Test
        @DisplayName("브랜드명이 빈칸이면 BAD_REQUEST 예외가 발생한다")
        fun nameBlankThrowsBadRequest() {
            // arrange & act
            val result = assertThrows<CoreException> {
                createBrand(name = "   ")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("브랜드명이 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
        fun nameEmptyThrowsBadRequest() {
            // arrange & act
            val result = assertThrows<CoreException> {
                createBrand(name = "")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("설명과 이미지URL이 null이어도 정상적으로 생성된다")
        fun nullableFieldsSuccess() {
            // arrange & act
            val brand = createBrand(description = null, imageUrl = null)

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo("나이키") },
                { assertThat(brand.description).isNull() },
                { assertThat(brand.imageUrl).isNull() },
            )
        }
    }

    @Nested
    inner class UpdateBrand {

        @Test
        @DisplayName("올바른 정보로 수정하면 필드가 변경된다")
        fun success() {
            // arrange
            val brand = createBrand()

            // act
            brand.update(name = "아디다스", description = "독일 브랜드", imageUrl = "https://example.com/adidas.png")

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo("아디다스") },
                { assertThat(brand.description).isEqualTo("독일 브랜드") },
                { assertThat(brand.imageUrl).isEqualTo("https://example.com/adidas.png") },
            )
        }

        @Test
        @DisplayName("수정 시 브랜드명이 빈칸이면 BAD_REQUEST 예외가 발생한다")
        fun nameBlankThrowsBadRequest() {
            // arrange
            val brand = createBrand()

            // act
            val result = assertThrows<CoreException> {
                brand.update(name = "   ", description = null, imageUrl = null)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
