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

    @DisplayName("브랜드를 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("정상적인 정보가 주어지면, 브랜드가 생성된다.")
        @Test
        fun createsBrand_whenValidInfoProvided() {
            // arrange
            val name = "나이키"
            val description = "스포츠 브랜드"

            // act
            val brand = Brand(name = name, description = description)

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo(name) },
                { assertThat(brand.description).isEqualTo(description) },
            )
        }

        @DisplayName("description이 null이어도, 브랜드가 생성된다.")
        @Test
        fun createsBrand_whenDescriptionIsNull() {
            // arrange
            val name = "나이키"

            // act
            val brand = Brand(name = name, description = null)

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo(name) },
                { assertThat(brand.description).isNull() },
            )
        }

        @DisplayName("name이 빈 값이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenNameIsBlank() {
            // arrange
            val blankName = "  "

            // act
            val exception = assertThrows<CoreException> {
                Brand(name = blankName, description = "설명")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("name이 빈 문자열이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenNameIsEmpty() {
            // arrange
            val emptyName = ""

            // act
            val exception = assertThrows<CoreException> {
                Brand(name = emptyName, description = "설명")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("name이 100자를 초과하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenNameExceedsMaxLength() {
            // arrange
            val longName = "a".repeat(101)

            // act
            val exception = assertThrows<CoreException> {
                Brand(name = longName, description = "설명")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("브랜드를 수정할 때,")
    @Nested
    inner class Update {

        @DisplayName("정상적인 정보가 주어지면, 브랜드가 수정된다.")
        @Test
        fun updatesBrand_whenValidInfoProvided() {
            // arrange
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")

            // act
            brand.update(name = "아디다스", description = "독일 스포츠 브랜드")

            // assert
            assertAll(
                { assertThat(brand.name).isEqualTo("아디다스") },
                { assertThat(brand.description).isEqualTo("독일 스포츠 브랜드") },
            )
        }

        @DisplayName("description을 null로 변경할 수 있다.")
        @Test
        fun updatesBrand_whenDescriptionIsNull() {
            // arrange
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")

            // act
            brand.update(name = "나이키", description = null)

            // assert
            assertThat(brand.description).isNull()
        }

        @DisplayName("name이 빈 값이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsException_whenNameIsBlank() {
            // arrange
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")

            // act
            val exception = assertThrows<CoreException> {
                brand.update(name = "  ", description = "설명")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("브랜드를 삭제할 때,")
    @Nested
    inner class Delete {

        @DisplayName("삭제하면, deletedAt이 설정된다.")
        @Test
        fun setDeletedAt_whenDeleteCalled() {
            // arrange
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")

            // act
            brand.delete()

            // assert
            assertThat(brand.isDeleted()).isTrue()
        }

        @DisplayName("이미 삭제된 브랜드를 다시 삭제해도, 정상 동작한다.")
        @Test
        fun remainsDeleted_whenAlreadyDeleted() {
            // arrange
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")
            brand.delete()

            // act
            brand.delete()

            // assert
            assertThat(brand.isDeleted()).isTrue()
        }

        @DisplayName("삭제되지 않은 브랜드는 isDeleted가 false이다.")
        @Test
        fun returnsFalse_whenNotDeleted() {
            // arrange
            val brand = Brand(name = "나이키", description = "스포츠 브랜드")

            // act & assert
            assertThat(brand.isDeleted()).isFalse()
        }
    }
}
