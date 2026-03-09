package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Email VO")
class EmailTest {

    @DisplayName("유효한 이메일이 주어지면, 정상적으로 생성된다.")
    @Test
    fun createsEmail_whenValidValueIsProvided() {
        // act
        val email = Email.of("email@loopers.com")

        // assert
        assertThat(email.value).isEqualTo("email@loopers.com")
    }

    @DisplayName("검증")
    @Nested
    inner class Validation {

        @DisplayName("비어있을 때, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenEmailIsBlank() {
            // act
            val result = assertThrows<CoreException> {
                Email.of("   ")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이메일 형식이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenEmailFormatIsInvalid() {
            // act
            val result = assertThrows<CoreException> {
                Email.of("invalid-email")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
