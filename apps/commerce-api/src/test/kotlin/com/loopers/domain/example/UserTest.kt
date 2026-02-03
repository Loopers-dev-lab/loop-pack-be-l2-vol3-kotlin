package com.loopers.domain.example

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class UserTest {

    @DisplayName("회원 생성할 때, 정상적으로 생성된다.")
    @Nested
    inner class Create {
        var loginId = "abcde12345"
        var password = "Abcd1234abcd!@#$"
        var name = "홍길동"
        var email = "abcde@gmail.com"
        var birthday = LocalDate.of(1980, 1, 1)

        @DisplayName("회원 데이터가 모두 주어지면, 정상적으로 생성된다.")
        @Test
        fun createUser() {
            val user = User(loginId = loginId, password = password, name = name, email = email, birthday = birthday)

            assertAll(
                { assertThat(user.loginId).isEqualTo(loginId) },
                { assertThat(user.password).isEqualTo(password) },
                { assertThat(user.name).isEqualTo(name) },
                { assertThat(user.email).isEqualTo(email) },
                { assertThat(user.birthday).isEqualTo(birthday) },
            )
        }

        @DisplayName("loginId가 빈칸으로만 이루어져 있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenLoginIdIsBlank() {
            var blankUserId = "   "

            val result = assertThrows<CoreException> {
                User(loginId = blankUserId, password = password, name = name, email = email, birthday = birthday)
            }

            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("loginId에 특수문자가 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenLoginIdContainsSpecialCharacter() {
            val invalidLoginId = "abc@123"

            val result = assertThrows<CoreException> {
                User(loginId = invalidLoginId, password = password, name = name, email = email, birthday = birthday)
            }

            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("loginId에 한글이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenLoginIdContainsKorean() {
            val invalidLoginId = "abc한글123"

            val result = assertThrows<CoreException> {
                User(loginId = invalidLoginId, password = password, name = name, email = email, birthday = birthday)
            }

            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("password 8자 미만, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPassword1() {
            var failPassword = "ab123#$"

            val result = assertThrows<CoreException> {
                User(loginId = loginId, password = failPassword, name = name, email = email, birthday = birthday)
            }

            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("password 16자 초과, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPassword2() {
            var failPassword = "abcdd1234abcd!@#$"

            val result = assertThrows<CoreException> {
                User(loginId = loginId, password = failPassword, name = name, email = email, birthday = birthday)
            }

            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("password 영문 대소문자 미포함, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPassword3() {
            var failPassword = "1234!@#$"

            val result = assertThrows<CoreException> {
                User(loginId = loginId, password = failPassword, name = name, email = email, birthday = birthday)
            }

            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("password 숫자 미포함, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPassword4() {
            var failPassword = "abcdabcd!@#$"

            val result = assertThrows<CoreException> {
                User(loginId = loginId, password = failPassword, name = name, email = email, birthday = birthday)
            }

            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("password 특수문자 미포함, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPassword5() {
            var failPassword = "abcdd1234abcd"

            val result = assertThrows<CoreException> {
                User(loginId = loginId, password = failPassword, name = name, email = email, birthday = birthday)
            }

            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("password에 생년월일(yyyyMMdd)이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenPasswordContainsBirthday() {
            val failPassword = "Abcd19800101!@"

            val result = assertThrows<CoreException> {
                User(loginId = loginId, password = failPassword, name = name, email = email, birthday = birthday)
            }

            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("name 빈칸으로만 이루어져 있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenNameIsBlank() {
            val blankName = "  "

            val result = assertThrows<CoreException> {
                User(loginId = loginId, password = password, name = blankName, email = email, birthday = birthday)
            }

            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("email 빈칸으로만 이루어져 있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenEmailIsBlank() {
            var blankEmail = "   "

            val result = assertThrows<CoreException> {
                User(loginId = loginId, password = password, name = name, email = blankEmail, birthday = birthday)
            }

            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("email 형식 검증, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenEmail() {
            var failEmail = "abcde@.com"

            val result = assertThrows<CoreException> {
                User(loginId = loginId, password = password, name = name, email = failEmail, birthday = birthday)
            }

            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
