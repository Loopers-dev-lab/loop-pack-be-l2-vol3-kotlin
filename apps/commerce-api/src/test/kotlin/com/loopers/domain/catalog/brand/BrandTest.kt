package com.loopers.domain.catalog.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BrandTest {

    @Nested
    @DisplayName("Brand 생성 시")
    inner class Create {

        @Test
        @DisplayName("유효한 이름으로 생성하면 성공한다")
        fun create_withValidName_success() {
            // arrange & act
            val brand = BrandTestFixture.createBrand()

            // assert
            assertThat(brand.name).isEqualTo(BrandTestFixture.DEFAULT_NAME)
        }

        @Test
        @DisplayName("빈 이름으로 생성하면 BAD_REQUEST 예외가 발생한다")
        fun create_withEmptyName_throwsException() {
            // arrange & act
            val exception = assertThrows<CoreException> { BrandTestFixture.createBrand(name = "") }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("브랜드명은 필수입니다.")
        }

        @Test
        @DisplayName("공백만 있는 이름으로 생성하면 BAD_REQUEST 예외가 발생한다")
        fun create_withBlankName_throwsException() {
            // arrange & act
            val exception = assertThrows<CoreException> { BrandTestFixture.createBrand(name = "   ") }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("update 시")
    inner class Update {

        @Test
        @DisplayName("유효한 이름으로 수정하면 성공한다")
        fun update_withValidName_success() {
            // arrange
            val brand = BrandTestFixture.createBrand()

            // act
            brand.update("아디다스")

            // assert
            assertThat(brand.name).isEqualTo("아디다스")
        }

        @Test
        @DisplayName("빈 이름으로 수정하면 BAD_REQUEST 예외가 발생한다")
        fun update_withBlankName_throwsException() {
            // arrange
            val brand = BrandTestFixture.createBrand()

            // act & assert
            val exception = assertThrows<CoreException> { brand.update("") }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("브랜드명은 필수입니다.")
        }
    }
}
