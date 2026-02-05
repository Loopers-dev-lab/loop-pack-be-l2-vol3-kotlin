package com.loopers.domain.user

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

    @DisplayName("비밀번호 변경할 때,")
    @Nested
    inner class ChangePassword {
        private val loginId = "testuser1"
        private val encodedPassword = "encoded_password"
        private val name = "홍길동"
        private val email = "test@example.com"
        private val birthday = LocalDate.of(1990, 5, 15)

        private val passwordEncoder = object : PasswordEncoder {
            override fun encode(rawPassword: String): String = "encoded_$rawPassword"
            override fun matches(rawPassword: String, encodedPassword: String): Boolean =
                "encoded_$rawPassword" == encodedPassword
        }

        @DisplayName("현재 비밀번호가 일치하지 않으면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorized_whenCurrentPasswordNotMatches() {
            // arrange
            val user = User(
                loginId = loginId,
                password = encodedPassword,
                name = name,
                email = email,
                birthday = birthday,
            )

            // act
            val exception = assertThrows<CoreException> {
                user.changePassword("wrongPassword", "NewPass1234!@", passwordEncoder)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @DisplayName("현재 비밀번호와 새 비밀번호가 동일하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNewPasswordSameAsCurrent() {
            // arrange
            val currentPassword = "password"
            val user = User(
                loginId = loginId,
                password = "encoded_$currentPassword",
                name = name,
                email = email,
                birthday = birthday,
            )

            // act
            val exception = assertThrows<CoreException> {
                user.changePassword(currentPassword, currentPassword, passwordEncoder)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("새 비밀번호 형식이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNewPasswordFormatInvalid() {
            // arrange
            val currentPassword = "password"
            val user = User(
                loginId = loginId,
                password = "encoded_$currentPassword",
                name = name,
                email = email,
                birthday = birthday,
            )

            // act
            val exception = assertThrows<CoreException> {
                user.changePassword(currentPassword, "weak", passwordEncoder)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("새 비밀번호에 생년월일이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNewPasswordContainsBirthday() {
            // arrange
            val currentPassword = "password"
            val user = User(
                loginId = loginId,
                password = "encoded_$currentPassword",
                name = name,
                email = email,
                birthday = birthday,
            )

            // act
            val exception = assertThrows<CoreException> {
                user.changePassword(currentPassword, "Pass19900515!@", passwordEncoder)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("유효한 비밀번호로 변경하면, 비밀번호가 변경된다.")
        @Test
        fun changesPassword_whenValidPasswordsProvided() {
            // arrange
            val currentPassword = "password"
            val newPassword = "NewPass1234!@"
            val user = User(
                loginId = loginId,
                password = "encoded_$currentPassword",
                name = name,
                email = email,
                birthday = birthday,
            )

            // act
            user.changePassword(currentPassword, newPassword, passwordEncoder)

            // assert
            assertThat(user.password).isEqualTo("encoded_$newPassword")
        }
    }

    @DisplayName("회원 생성할 때, 정상적으로 생성된다.")
    @Nested
    inner class Create {
        private val loginId = "abcde12345"
        private val password = "Abcd1234abcd!@#$"
        private val name = "홍길동"
        private val email = "abcde@gmail.com"
        private val birthday = LocalDate.of(1980, 1, 1)

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

        @DisplayName("name 빈칸으로만 이루어져 있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequestException_whenNameIsBlank() {
            val blankName = "  "

            val result = assertThrows<CoreException> {
                User(loginId = loginId, password = password, name = blankName, email = email, birthday = birthday)
            }

            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
