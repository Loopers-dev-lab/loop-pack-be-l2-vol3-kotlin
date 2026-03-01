package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

@DisplayName("RawPassword VO")
class RawPasswordTest {
    @Nested
    @DisplayName("패턴 검증")
    inner class PatternValidation {
        @Test
        @DisplayName("영문+숫자+허용 특수문자 조합 → 성공")
        fun create_validPattern() {
            assertDoesNotThrow { RawPassword("Password1!") }
        }

        @Test
        @DisplayName("공백 포함 → 실패")
        fun create_containsSpace_throwsException() {
            val exception = assertThrows<CoreException> {
                RawPassword("Pass word1!")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
        }

        @Test
        @DisplayName("한글 포함 → 실패")
        fun create_containsKorean_throwsException() {
            val exception = assertThrows<CoreException> {
                RawPassword("비밀번호1!")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
        }
    }

    @Nested
    @DisplayName("생년월일 포함 검증")
    inner class BirthDateValidation {
        private val birthDate = LocalDate.of(1990, 1, 1)

        @Test
        @DisplayName("생년월일 미포함 → 성공")
        fun withBirthDateValidation_notContainsBirthDate() {
            assertDoesNotThrow {
                RawPassword.withBirthDateValidation("Password1!", birthDate)
            }
        }

        @Test
        @DisplayName("yyyyMMdd 형식 포함 → 실패")
        fun withBirthDateValidation_containsCompactDate_throwsException() {
            val exception = assertThrows<CoreException> {
                RawPassword.withBirthDateValidation("Pass19900101!", birthDate)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
        }

        @Test
        @DisplayName("yyyy-MM-dd 형식 포함 → 실패")
        fun withBirthDateValidation_containsDashedDate_throwsException() {
            val exception = assertThrows<CoreException> {
                RawPassword.withBirthDateValidation("P1990-01-01!", birthDate)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.USER_INVALID_PASSWORD)
        }

        @Test
        @DisplayName("기본 생성자는 생년월일 검증을 하지 않는다")
        fun create_withoutBirthDate_noValidation() {
            assertDoesNotThrow {
                RawPassword("Pass19900101!")
            }
        }
    }
}
