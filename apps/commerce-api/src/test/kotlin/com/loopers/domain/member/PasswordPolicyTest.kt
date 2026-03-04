package com.loopers.domain.member

import com.loopers.support.error.CoreException
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
        fun `검증_실패_시_예외가_전파된다`() {
            // arrange
            val value = "short"
            val birthDate = LocalDate.of(1990, 1, 15)

            // act & assert
            assertThrows<CoreException> { passwordPolicy.createPassword(value, birthDate) }
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
