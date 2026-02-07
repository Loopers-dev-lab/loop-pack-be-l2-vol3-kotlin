package com.loopers.domain.member.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NameTest {
    @Nested
    inner class Create {
        @Test
        fun `유효한_이름으로_생성할_수_있다`() {
            // arrange
            val value = "홍길동"

            // act
            val name = Name(value)

            // assert
            assertThat(name.value).isEqualTo(value)
        }

        @Test
        fun `빈값은_허용하지_않는다`() {
            // arrange
            val value = ""

            // act
            val result = assertThrows<CoreException> { Name(value) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_NAME_FORMAT)
        }

        @Test
        fun `공백만으로_이루어진_이름은_허용하지_않는다`() {
            // arrange
            val value = "   "

            // act
            val result = assertThrows<CoreException> { Name(value) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_NAME_FORMAT)
        }
    }

    @Nested
    inner class Masking {
        @Test
        fun `마지막_글자가_마스킹된_이름을_반환한다`() {
            // arrange
            val name = Name("홍길동")

            // act
            val masked = name.masked()

            // assert
            assertThat(masked).isEqualTo("홍길*")
        }

        @Test
        fun `한_글자_이름은_마스킹_처리한다`() {
            // arrange
            val name = Name("홍")

            // act
            val masked = name.masked()

            // assert
            assertThat(masked).isEqualTo("*")
        }

        @Test
        fun `두_글자_이름의_마지막_글자를_마스킹한다`() {
            // arrange
            val name = Name("홍길")

            // act
            val masked = name.masked()

            // assert
            assertThat(masked).isEqualTo("홍*")
        }
    }
}
