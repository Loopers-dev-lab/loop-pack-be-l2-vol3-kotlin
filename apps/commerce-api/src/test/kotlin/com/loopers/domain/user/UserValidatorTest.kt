package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class UserValidatorTest {

    private val validator = UserValidator()

    @Test
    fun `비밀번호에 생년월일이 포함되지 않으면 통과한다`() {
        val password = Password("test1234!@#$")
        val birthDate = BirthDate("2000-01-01")

        assertDoesNotThrow {
            validator.validatePasswordNotContainsBirthDate(password, birthDate)
        }
    }

    @Test
    fun `비밀번호에 생년월일(8자리)이 포함되면 실패한다`() {
        val password = Password("test20000101!@#$")
        val birthDate = BirthDate("2000-01-01")

        val exception = assertThrows<CoreException> {
            validator.validatePasswordNotContainsBirthDate(password, birthDate)
        }

        assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
    }

    @Test
    fun `비밀번호에 생년월일(6자리)이 포함되면 실패한다`() {
        val password = Password("test000101!@#$")
        val birthDate = BirthDate("2000-01-01")

        val exception = assertThrows<CoreException> {
            validator.validatePasswordNotContainsBirthDate(password, birthDate)
        }

        assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
    }
}
