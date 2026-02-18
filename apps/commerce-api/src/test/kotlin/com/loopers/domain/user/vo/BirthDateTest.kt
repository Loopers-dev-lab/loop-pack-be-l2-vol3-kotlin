package com.loopers.domain.user.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BirthDateTest {
    @DisplayName("생년월일을 생성할 때, ")
    @Nested
    inner class Create {
        @DisplayName("유효한 생년월일 '20260101'로 생성하면 BirthDate 객체를 반환한다")
        @Test
        fun createsBirthDate_whenValidFormatYYYYMMDD() {
            // arrange
            val validBirthDate = "20260101"

            // act
            val birthDate = BirthDate.of(validBirthDate)

            // assert
            assertThat(birthDate.value).isEqualTo(validBirthDate)
        }

        @DisplayName("유효한 생년월일 '19900515'로 생성하면 BirthDate 객체를 반환한다")
        @Test
        fun createsBirthDate_whenValidPastDate() {
            // arrange
            val validBirthDate = "19900515"

            // act
            val birthDate = BirthDate.of(validBirthDate)

            // assert
            assertThat(birthDate.value).isEqualTo(validBirthDate)
        }

        @DisplayName("생년월일이 빈 문자열이면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenBirthDateIsEmpty() {
            // arrange
            val emptyBirthDate = ""

            // act
            val result = assertThrows<CoreException> {
                BirthDate.of(emptyBirthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("생년월일이 yyyyMMdd 형식이 아니면 예외를 던진다 (하이픈 포함)")
        @Test
        fun throwsBadRequestException_whenBirthDateContainsHyphen() {
            // arrange
            val invalidFormatBirthDate = "2026-01-01"

            // act
            val result = assertThrows<CoreException> {
                BirthDate.of(invalidFormatBirthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("생년월일이 존재하지 않는 날짜이면 예외를 던진다 (2월 30일)")
        @Test
        fun throwsBadRequestException_whenBirthDateIsInvalidDate() {
            // arrange
            val invalidDateBirthDate = "20260230"

            // act
            val result = assertThrows<CoreException> {
                BirthDate.of(invalidDateBirthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("생년월일이 7자리이면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenBirthDateIsSevenDigits() {
            // arrange
            val shortBirthDate = "2026010"

            // act
            val result = assertThrows<CoreException> {
                BirthDate.of(shortBirthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("생년월일이 9자리이면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenBirthDateIsNineDigits() {
            // arrange
            val longBirthDate = "202601010"

            // act
            val result = assertThrows<CoreException> {
                BirthDate.of(longBirthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("생년월일에 공백이 포함되면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenBirthDateContainsSpace() {
            // arrange
            val birthDateWithSpace = "2026 0101"

            // act
            val result = assertThrows<CoreException> {
                BirthDate.of(birthDateWithSpace)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("생년월일에 알파벳이 포함되면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenBirthDateContainsAlpha() {
            // arrange
            val birthDateWithAlpha = "2026010A"

            // act
            val result = assertThrows<CoreException> {
                BirthDate.of(birthDateWithAlpha)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("생년월일이 공백만 있으면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenBirthDateIsBlank() {
            // arrange
            val blankBirthDate = "   "

            // act
            val result = assertThrows<CoreException> {
                BirthDate.of(blankBirthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
