package com.loopers.domain.member

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class PasswordPolicyTest {

    private val passwordPolicy = PasswordPolicy(NoOpPasswordEncoder())

    @Nested
    inner class CreatePassword {
        @Test
        fun `유효한_비밀번호로_생성할_수_있다`() {
            // arrange
            val value = "Password1!"
            val birthDate = LocalDate.of(1990, 1, 15)

            // act
            val password = passwordPolicy.createPassword(value, birthDate)

            // assert
            assertThat(password.value).isEqualTo(value)
        }

        @Test
        fun `8자_미만이면_예외가_발생한다`() {
            // arrange
            val value = "Pass1!"
            val birthDate = LocalDate.of(1990, 1, 15)

            // act
            val result = assertThrows<CoreException> { passwordPolicy.createPassword(value, birthDate) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_PASSWORD_FORMAT)
        }

        @Test
        fun `16자_초과면_예외가_발생한다`() {
            // arrange
            val value = "Password12345678!"
            val birthDate = LocalDate.of(1990, 1, 15)

            // act
            val result = assertThrows<CoreException> { passwordPolicy.createPassword(value, birthDate) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_PASSWORD_FORMAT)
        }

        @Test
        fun `허용되지_않는_문자가_포함되면_예외가_발생한다`() {
            // arrange
            val value = "Password1한글"
            val birthDate = LocalDate.of(1990, 1, 15)

            // act
            val result = assertThrows<CoreException> { passwordPolicy.createPassword(value, birthDate) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_PASSWORD_FORMAT)
        }

        @Test
        fun `생년월일이_포함되면_예외가_발생한다`() {
            // arrange
            val birthDate = LocalDate.of(1990, 1, 15)
            val invalidPasswords = listOf("Pass19900115!", "19900115Ab!", "Ab19900115!")

            // act & assert
            invalidPasswords.forEach { value ->
                val result = assertThrows<CoreException> { passwordPolicy.createPassword(value, birthDate) }
                assertThat(result.errorType).isEqualTo(ErrorType.PASSWORD_CONTAINS_BIRTHDATE)
            }
        }

        @Test
        fun `구분자_포함_생년월일이_포함되면_예외가_발생한다`() {
            // arrange
            val birthDate = LocalDate.of(1990, 1, 15)
            val invalidPasswords = listOf("1990-01-15Ab", "Ab1990/01/15")

            // act & assert
            invalidPasswords.forEach { value ->
                val result = assertThrows<CoreException> { passwordPolicy.createPassword(value, birthDate) }
                assertThat(result.errorType).isEqualTo(ErrorType.PASSWORD_CONTAINS_BIRTHDATE)
            }
        }
    }

    @Nested
    inner class Matches {
        @Test
        fun `동일한_평문_비밀번호인지_검증할_수_있다`() {
            // arrange
            val value = "Password1!"
            val birthDate = LocalDate.of(1990, 1, 15)
            val password = passwordPolicy.createPassword(value, birthDate)

            // act & assert
            assertThat(passwordPolicy.matches(value, password)).isTrue()
            assertThat(passwordPolicy.matches("WrongPassword1!", password)).isFalse()
        }
    }
}
