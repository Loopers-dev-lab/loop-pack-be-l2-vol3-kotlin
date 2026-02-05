package com.loopers.domain.member.vo

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EmailTest {
    @Nested
    inner class Create {
        @Test
        fun `유효한_이메일로_생성할_수_있다`() {
            // arrange
            val value = "test@example.com"

            // act
            val email = Email(value)

            // assert
            assertThat(email.value).isEqualTo(value)
        }

        @Test
        fun `다양한_유효한_이메일_형식을_허용한다`() {
            // arrange
            val validEmails = listOf(
                "user@domain.com",
                "user.name@domain.com",
                "user+tag@domain.com",
                "user@subdomain.domain.com",
            )

            // act & assert
            validEmails.forEach { value ->
                val email = Email(value)
                assertThat(email.value).isEqualTo(value)
            }
        }

        @Test
        fun `이메일_형식이_아니면_예외가_발생한다`() {
            // arrange
            val invalidEmails = listOf(
                "notanemail",
                "@domain.com",
                "user@",
                "user@.com",
                "user domain@test.com",
            )

            // act & assert
            invalidEmails.forEach { value ->
                val result = assertThrows<CoreException> { Email(value) }
                assertThat(result.errorType).isEqualTo(ErrorType.INVALID_EMAIL_FORMAT)
            }
        }

        @Test
        fun `빈값은_허용하지_않는다`() {
            // arrange
            val value = ""

            // act
            val result = assertThrows<CoreException> { Email(value) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.INVALID_EMAIL_FORMAT)
        }
    }
}
