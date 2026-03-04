package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class PasswordValidatorTest {

    private val validator = PasswordValidator()

    @Nested
    inner class Validate {
        @Test
        fun `유효한_비밀번호는_검증을_통과한다`() {
            // arrange
            val rawPassword = "Password1!"
            val birthDate = LocalDate.of(1990, 1, 15)

            // act & assert
            assertDoesNotThrow { validator.validate(rawPassword, birthDate) }
        }

        @Test
        fun `8자_미만이면_예외가_발생한다`() {
            // arrange
            val rawPassword = "Pass1!"
            val birthDate = LocalDate.of(1990, 1, 15)

            // act
            val result = assertThrows<CoreException> { validator.validate(rawPassword, birthDate) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_PASSWORD_FORMAT)
        }

        @Test
        fun `16자_초과면_예외가_발생한다`() {
            // arrange
            val rawPassword = "Password12345678!"
            val birthDate = LocalDate.of(1990, 1, 15)

            // act
            val result = assertThrows<CoreException> { validator.validate(rawPassword, birthDate) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_PASSWORD_FORMAT)
        }

        @Test
        fun `허용되지_않는_문자가_포함되면_예외가_발생한다`() {
            // arrange
            val rawPassword = "Password1한글"
            val birthDate = LocalDate.of(1990, 1, 15)

            // act
            val result = assertThrows<CoreException> { validator.validate(rawPassword, birthDate) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_PASSWORD_FORMAT)
        }

        @Test
        fun `생년월일이_포함되면_예외가_발생한다`() {
            // arrange
            val birthDate = LocalDate.of(1990, 1, 15)
            val invalidPasswords = listOf("Pass19900115!", "19900115Ab!", "Ab19900115!")

            // act & assert
            invalidPasswords.forEach { rawPassword ->
                val result = assertThrows<CoreException> { validator.validate(rawPassword, birthDate) }
                assertThat(result.errorType).isEqualTo(ErrorType.PASSWORD_CONTAINS_BIRTHDATE)
            }
        }

        @Test
        fun `구분자_포함_생년월일이_포함되면_예외가_발생한다`() {
            // arrange
            val birthDate = LocalDate.of(1990, 1, 15)
            val invalidPasswords = listOf("1990-01-15Ab", "Ab1990/01/15")

            // act & assert
            invalidPasswords.forEach { rawPassword ->
                val result = assertThrows<CoreException> { validator.validate(rawPassword, birthDate) }
                assertThat(result.errorType).isEqualTo(ErrorType.PASSWORD_CONTAINS_BIRTHDATE)
            }
        }
    }
}
