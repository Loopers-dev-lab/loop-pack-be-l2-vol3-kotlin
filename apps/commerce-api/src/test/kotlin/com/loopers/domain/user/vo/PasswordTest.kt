package com.loopers.domain.user.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PasswordTest {
    @DisplayName("패스워드를 검증할 때, ")
    @Nested
    inner class Validate {
        @DisplayName("유효한 패스워드와 생일로 검증하면 성공한다")
        @Test
        fun validatesPassword_whenValidPasswordAndBirthDateIsProvided() {
            // arrange
            val validPassword = "TestPass123!"
            val birthDate = "19900515"

            // act + assert - 예외 없음
            Password.validate(validPassword, birthDate)
        }

        @DisplayName("패스워드가 비어있으면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenPasswordIsBlank() {
            // arrange
            val blankPassword = ""
            val birthDate = "19900515"

            // act
            val result = assertThrows<CoreException> {
                Password.validate(blankPassword, birthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("패스워드가 8자 미만이면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenPasswordIsShorterThanEightCharacters() {
            // arrange
            val shortPassword = "Pass123"
            val birthDate = "19900515"

            // act
            val result = assertThrows<CoreException> {
                Password.validate(shortPassword, birthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("패스워드가 16자 초과이면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenPasswordIsLongerThanSixteenCharacters() {
            // arrange
            val longPassword = "Pass1234567890123"
            val birthDate = "19900515"

            // act
            val result = assertThrows<CoreException> {
                Password.validate(longPassword, birthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("패스워드에 한글이 포함되면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenPasswordContainsKorean() {
            // arrange
            val passwordWithKorean = "Pass한글123!"
            val birthDate = "19900515"

            // act
            val result = assertThrows<CoreException> {
                Password.validate(passwordWithKorean, birthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("패스워드에 생일이 포함되면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenPasswordContainsBirthDate() {
            // arrange
            val passwordWithBirthDate = "Pass19900515!"
            val birthDate = "19900515"

            // act
            val result = assertThrows<CoreException> {
                Password.validate(passwordWithBirthDate, birthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("패스워드에 영문 대소문자, 숫자, 특수문자만 포함되면 성공한다")
        @Test
        fun validatesPassword_whenOnlyContainsValidCharacters() {
            // arrange
            val validPassword = "AbcDefgh123!@#"
            val birthDate = "19900515"

            // act + assert - 예외 없음
            Password.validate(validPassword, birthDate)
        }

        @DisplayName("패스워드에 공백이 포함되면 예외를 던진다")
        @Test
        fun throwsBadRequestException_whenPasswordContainsSpace() {
            // arrange
            val passwordWithSpace = "Pass word123!"
            val birthDate = "19900515"

            // act
            val result = assertThrows<CoreException> {
                Password.validate(passwordWithSpace, birthDate)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("암호화된 패스워드를 생성할 때, ")
    @Nested
    inner class OfEncrypted {
        @DisplayName("암호화된 패스워드로 Password 객체를 생성한다")
        @Test
        fun createsEncryptedPassword_whenValidEncryptedPasswordIsProvided() {
            // arrange
            val encryptedPassword = "\$2a\$10\$N9qo8uLOickgx2ZMRZoSyeIjZAgcg7b3XeKeUxWdeS86E36aiAFZm"

            // act
            val password = Password.ofEncrypted(encryptedPassword)

            // assert
            assertThat(password.value).isEqualTo(encryptedPassword)
        }
    }
}
