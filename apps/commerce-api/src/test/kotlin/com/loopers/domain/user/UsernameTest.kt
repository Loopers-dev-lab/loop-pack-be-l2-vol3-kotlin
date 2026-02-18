package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("Username VO")
class UsernameTest {

    @DisplayName("유효한 아이디가 주어지면, 정상적으로 생성된다.")
    @Test
    fun createsUsername_whenValidValueIsProvided() {
        // act
        val username = Username.of("username123")

        // assert
        assertThat(username.value).isEqualTo("username123")
    }

    @DisplayName("검증")
    @Nested
    inner class Validation {

        @DisplayName("비어있을 때, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenUsernameIsBlank() {
            // act
            val result = assertThrows<CoreException> {
                Username.of("   ")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("영문과 숫자로 구성되지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenUsernameContainsInvalidCharacters() {
            // act
            val result = assertThrows<CoreException> {
                Username.of("유저이름!@#")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
