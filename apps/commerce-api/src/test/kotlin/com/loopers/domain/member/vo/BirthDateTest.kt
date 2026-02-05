package com.loopers.domain.member.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class BirthDateTest {
    @Nested
    inner class Create {
        @Test
        fun `유효한_생년월일로_생성할_수_있다`() {
            // arrange
            val value = LocalDate.of(1990, 1, 15)

            // act
            val birthDate = BirthDate(value)

            // assert
            assertThat(birthDate.value).isEqualTo(value)
        }

        @Test
        fun `미래_날짜는_허용하지_않는다`() {
            // arrange
            val value = LocalDate.now().plusDays(1)

            // act
            val result = assertThrows<CoreException> { BirthDate(value) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_BIRTHDATE_FORMAT)
        }

        @Test
        fun `오늘_날짜는_허용한다`() {
            // arrange
            val value = LocalDate.now()

            // act
            val birthDate = BirthDate(value)

            // assert
            assertThat(birthDate.value).isEqualTo(value)
        }
    }

    @Nested
    inner class Format {
        @Test
        fun `YYYYMMDD_형식_문자열을_반환한다`() {
            // arrange
            val birthDate = BirthDate(LocalDate.of(1990, 1, 15))

            // act
            val formatted = birthDate.toYYYYMMDD()

            // assert
            assertThat(formatted).isEqualTo("19900115")
        }
    }
}
