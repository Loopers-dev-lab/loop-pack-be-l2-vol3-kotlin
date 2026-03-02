package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class UserValidatorTest {

    private val userValidator = UserValidator()

    @Nested
    inner class ValidatePasswordNotContainsBirthDate {

        @Test
        fun `생년월일을 포함하지 않는 비밀번호는 검증을 통과한다`() {
            val password = Password("Test1234!@#$")
            val birthDate = BirthDate("2000-01-01")

            userValidator.validateNoBirthDate(password, birthDate)
        }

        @Test
        fun `비밀번호에 생년월일(8자리)이 포함되면 BAD_REQUEST 예외가 발생한다`() {
            val password = Password("Test20000101!@")
            val birthDate = BirthDate("2000-01-01")

            val exception = assertThrows<CoreException> {
                userValidator.validateNoBirthDate(password, birthDate)
            }

            assertEquals(ErrorType.BAD_REQUEST, exception.errorType)
        }

        @Test
        fun `비밀번호에 생년월일(6자리)이 포함되면 BAD_REQUEST 예외가 발생한다`() {
            val password = Password("Test000101!@#$")
            val birthDate = BirthDate("2000-01-01")

            val exception = assertThrows<CoreException> {
                userValidator.validateNoBirthDate(password, birthDate)
            }

            assertEquals(ErrorType.BAD_REQUEST, exception.errorType)
        }
    }
}
