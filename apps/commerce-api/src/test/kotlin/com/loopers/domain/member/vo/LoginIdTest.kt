package com.loopers.domain.member.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LoginIdTest {
    @Nested
    inner class Create {
        @Test
        fun `유효한_로그인ID로_생성할_수_있다`() {
            // arrange
            val value = "testuser123"

            // act
            val loginId = LoginId(value)

            // assert
            assertThat(loginId.value).isEqualTo(value)
        }

        @Test
        fun `영문과_숫자만_허용한다`() {
            // arrange
            val validValues = listOf("user", "USER", "User123", "abc", "123abc")

            // act & assert
            validValues.forEach { value ->
                val loginId = LoginId(value)
                assertThat(loginId.value).isEqualTo(value)
            }
        }

        @Test
        fun `빈값은_허용하지_않는다`() {
            // arrange
            val value = ""

            // act
            val result = assertThrows<CoreException> { LoginId(value) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_LOGIN_ID_FORMAT)
        }

        @Test
        fun `특수문자가_포함되면_예외가_발생한다`() {
            // arrange
            val invalidValues = listOf("user@name", "user#123", "user!", "user-name", "user_name")

            // act & assert
            invalidValues.forEach { value ->
                val result = assertThrows<CoreException> { LoginId(value) }
                assertThat(result.errorType).isEqualTo(ErrorType.INVALID_LOGIN_ID_FORMAT)
            }
        }

        @Test
        fun `공백이_포함되면_예외가_발생한다`() {
            // arrange
            val value = "user name"

            // act
            val result = assertThrows<CoreException> { LoginId(value) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_LOGIN_ID_FORMAT)
        }
    }
}
