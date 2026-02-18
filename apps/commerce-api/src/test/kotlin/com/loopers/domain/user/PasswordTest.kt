package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZoneId
import java.time.ZonedDateTime

@DisplayName("Password VO")
class PasswordTest {

    private val defaultBirthDate = ZonedDateTime.of(1995, 5, 29, 21, 40, 0, 0, ZoneId.of("Asia/Seoul"))

    @DisplayName("유효한 비밀번호가 주어지면, 정상적으로 생성된다.")
    @Test
    fun createsPassword_whenValidValueIsProvided() {
        // act
        val password = Password.of("password1234!", defaultBirthDate)

        // assert
        assertThat(password.value).isEqualTo("password1234!")
    }

    @DisplayName("검증")
    @Nested
    inner class Validation {

        @DisplayName("비어있을 때, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPasswordIsBlank() {
            // act
            val result = assertThrows<CoreException> {
                Password.of("   ", defaultBirthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("8자 미만 또는 16자 초과일 때, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPasswordLengthIsInvalid() {
            // act
            val result = assertThrows<CoreException> {
                Password.of("pass1!", defaultBirthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("영문자 또는 숫자 또는 특수문자로 이루어져있지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPasswordContainsInvalidCharacters() {
            // act
            val result = assertThrows<CoreException> {
                Password.of("패스워드12345678", defaultBirthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("생년월일이 포함되어 있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPasswordContainsBirthDate() {
            // act
            val result = assertThrows<CoreException> {
                Password.of("pass19950529!", defaultBirthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
