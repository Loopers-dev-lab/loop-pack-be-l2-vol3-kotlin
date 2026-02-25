package com.loopers.domain.brand.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BrandNameTest {

    @Nested
    inner class Create {
        @Test
        fun `유효한_이름으로_생성할_수_있다`() {
            // arrange
            val value = "나이키"

            // act
            val brandName = BrandName(value)

            // assert
            assertThat(brandName.value).isEqualTo(value)
        }

        @Test
        fun `1자_이름으로_생성할_수_있다`() {
            // arrange
            val value = "A"

            // act
            val brandName = BrandName(value)

            // assert
            assertThat(brandName.value).isEqualTo(value)
        }

        @Test
        fun `50자_이름으로_생성할_수_있다`() {
            // arrange
            val value = "A".repeat(50)

            // act
            val brandName = BrandName(value)

            // assert
            assertThat(brandName.value).isEqualTo(value)
        }

        @Test
        fun `한글_이름으로_생성할_수_있다`() {
            // arrange
            val value = "나이키코리아"

            // act
            val brandName = BrandName(value)

            // assert
            assertThat(brandName.value).isEqualTo(value)
        }

        @Test
        fun `공백만으로는_생성할_수_없다`() {
            // arrange
            val value = "   "

            // act
            val result = assertThrows<CoreException> { BrandName(value) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_BRAND_NAME_FORMAT)
        }

        @Test
        fun `빈값으로는_생성할_수_없다`() {
            // arrange
            val value = ""

            // act
            val result = assertThrows<CoreException> { BrandName(value) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_BRAND_NAME_FORMAT)
        }

        @Test
        fun `51자_이상이면_예외가_발생한다`() {
            // arrange
            val value = "A".repeat(51)

            // act
            val result = assertThrows<CoreException> { BrandName(value) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_BRAND_NAME_FORMAT)
        }
    }
}
