package com.loopers.domain.catalog.brand

import com.loopers.domain.catalog.brand.vo.BrandName

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BrandNameTest {

    @Nested
    @DisplayName("BrandName 생성 시")
    inner class Create {

        @Test
        @DisplayName("유효한 이름이면 정상 생성된다")
        fun create_withValidName_success() {
            // arrange & act
            val brandName = BrandName("나이키")

            // assert
            assertThat(brandName.value).isEqualTo("나이키")
        }

        @Test
        @DisplayName("빈 값이면 BAD_REQUEST 예외가 발생한다")
        fun create_withEmptyName_throwsException() {
            // arrange & act
            val exception = assertThrows<CoreException> { BrandName("") }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("브랜드명은 필수입니다.")
        }

        @Test
        @DisplayName("공백만 있으면 BAD_REQUEST 예외가 발생한다")
        fun create_withBlankName_throwsException() {
            // arrange & act
            val exception = assertThrows<CoreException> { BrandName("   ") }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("브랜드명은 필수입니다.")
        }
    }

    @Nested
    @DisplayName("BrandName 동등성 비교 시")
    inner class Equality {

        @Test
        @DisplayName("같은 값이면 동등하다")
        fun equality_sameValue_areEqual() {
            // arrange
            val name1 = BrandName("나이키")
            val name2 = BrandName("나이키")

            // assert
            assertThat(name1).isEqualTo(name2)
            assertThat(name1.hashCode()).isEqualTo(name2.hashCode())
        }

        @Test
        @DisplayName("Set에서 동일 값은 중복 제거된다")
        fun equality_inSet_deduplicates() {
            // arrange
            val name1 = BrandName("나이키")
            val name2 = BrandName("나이키")

            // act
            val set = setOf(name1, name2)

            // assert
            assertThat(set).hasSize(1)
        }
    }
}
