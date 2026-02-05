package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserTest {
    @DisplayName("유저를 생성할 때, ")
    @Nested
    inner class Create {
        @DisplayName("로그인 ID가 6~16자의 영문,숫자 형식에 맞지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenLoginIdIsInvalid() {
            // arrange
            val id = "abc"

            // act
            val result = assertThrows<CoreException> {
                User(loginId = id, password = "abcd1234", name = "testName", birth = "2026-01-31", email = "test@test.com")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호가 8~16자의 영문,숫자,특수문자 형식에 맞지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPasswordIsInvalid() {
            // arrange
            val password = "abcd"

            // act
            val result = assertThrows<CoreException> {
                User(loginId = "testId", password = password, name = "testName", birth = "2026-01-31", email = "test@test.com")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호가 생년월일을 포함하고 있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPasswordContainsBirth() {
            // arrange
            val password = "abcd20260131"
            val birth = "2026-01-31"

            // act
            val result = assertThrows<CoreException> {
                User(loginId = "testId", password = password, name = "testName", birth = birth, email = "test@test.com")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 1~16자의 영문,한글,숫자 형식에 맞지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenNameIsInvalid() {
            // arrange
            val name = " "

            // act
            val result = assertThrows<CoreException> {
                User(loginId = "testId", password = "abcd1234", name = name, birth = "2026-01-31", email = "test@test.com")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("생년월일이 yyyy-MM-dd 형식에 맞지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenBirthIsInvalid() {
            // arrange
            val birth = "31-01-2025"

            // act
            val result = assertThrows<CoreException> {
                User(loginId = "testId", password = "abcd1234", name = "testName", birth = birth, email = "test@test.com")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이메일이 xx@yy.zz 형식에 맞지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenEmailIsInvalid() {
            // arrange
            val email = "invalid-email"

            // act
            val result = assertThrows<CoreException> {
                User(loginId = "testId", password = "abcd1234", name = "testName", birth = "2026-01-31", email = email)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("유저 비밀번호를 검증할 때, ")
    @Nested
    inner class MatchPassword {
        @DisplayName("비밀번호가 다르면, false를 리턴한다.")
        @Test
        fun returnFalse_whenPasswordNotMatched() {
            // arrange
            val password = "abcd1234"
            val wrongPassword = "abcd1235"
            val user = User(loginId = "testId", password = password, name = "testName", birth = "2026-01-31", email = "test@test.com")

            // act
            val result = user.matchPassword(wrongPassword)

            // assert
            assertThat(result).isEqualTo(false)
        }
    }
}
