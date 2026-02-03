package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class PasswordValidatorTest {

    private val birthday = LocalDate.of(1999, 1, 1)

    @Nested
    inner class `비밀번호 검증 시` {

        @Test
        fun `올바른 비밀번호면 예외가 발생하지 않는다`() {
            // arrange & act & assert
            assertDoesNotThrow {
                PasswordValidator.validate("Abcd1234!", birthday)
            }
        }

        @Test
        fun `8자 미만이면 BAD_REQUEST 예외가 발생한다`() {
            // arrange & act
            val result = assertThrows<CoreException> {
                PasswordValidator.validate("Abc123!", birthday)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        fun `16자 초과면 BAD_REQUEST 예외가 발생한다`() {
            // arrange & act
            val result = assertThrows<CoreException> {
                PasswordValidator.validate("Abcdefgh12345678!", birthday)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        fun `한글이 포함되면 BAD_REQUEST 예외가 발생한다`() {
            // arrange & act
            val result = assertThrows<CoreException> {
                PasswordValidator.validate("Abcd1234한", birthday)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        fun `공백이 포함되면 BAD_REQUEST 예외가 발생한다`() {
            // arrange & act
            val result = assertThrows<CoreException> {
                PasswordValidator.validate("Abcd 1234", birthday)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        fun `생년월일이 포함되면 BAD_REQUEST 예외가 발생한다`() {
            // arrange & act
            val result = assertThrows<CoreException> {
                PasswordValidator.validate("A19990101!", birthday)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
