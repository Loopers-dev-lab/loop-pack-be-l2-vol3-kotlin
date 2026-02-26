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

    @DisplayName("브랜드 생성할 때,")
    @Nested
    inner class Create {
        private val name = "나이키"
        private val description = "스포츠 브랜드"

        @DisplayName("유효한 이름과 설명이 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsBrand_whenValidNameAndDescriptionProvided() {
            // arrange & act
            val brand = Brand(name = name, description = description)

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo(name) },
                { assertThat(brand.description).isEqualTo(description) },
            )
        }

        @DisplayName("설명이 null이면, 정상적으로 생성된다.")
        @Test
        fun createsBrand_whenDescriptionIsNull() {
            // arrange & act
            val brand = Brand(name = name, description = null)

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo(name) },
                { assertThat(brand.description).isNull() },
            )
        }

        @DisplayName("이름이 빈칸이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNameIsBlank() {
            // arrange
            val blankName = "  "

            // act
            val result = assertThrows<CoreException> {
                Brand(name = blankName, description = description)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("브랜드 수정할 때,")
    @Nested
    inner class Update {
        private val brand = Brand(name = "나이키", description = "스포츠 브랜드")

        @DisplayName("유효한 이름과 설명이 주어지면, 정상적으로 수정된다.")
        @Test
        fun updatesBrand_whenValidNameAndDescriptionProvided() {
            // arrange
            val newName = "아디다스"
            val newDescription = "독일 스포츠 브랜드"

            // act
            brand.update(name = newName, description = newDescription)

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo(newName) },
                { assertThat(brand.description).isEqualTo(newDescription) },
            )
        }

        @DisplayName("설명을 null로 변경하면, 정상적으로 수정된다.")
        @Test
        fun updatesBrand_whenDescriptionIsNull() {
            // act
            brand.update(name = "아디다스", description = null)

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo("아디다스") },
                { assertThat(brand.description).isNull() },
            )
        }

        @DisplayName("이름이 빈칸이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNameIsBlank() {
            // act
            val result = assertThrows<CoreException> {
                brand.update(name = "  ", description = "설명")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
