package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class UserTest {

    @Nested
    @DisplayName("User 생성 시")
    inner class Create {

        @Test
        @DisplayName("유효한 정보로 생성하면 성공한다")
        fun createUser_withValidData_success() {
            // arrange
            val loginId = "testuser1"
            val password = "Password1!"
            val name = "홍길동"
            val birthDate = LocalDate.of(1990, 1, 15)
            val email = "test@example.com"

            // act
            val user = User(loginId, password, name, birthDate, email)

            // assert
            assertThat(user.loginId).isEqualTo(loginId)
            assertThat(user.name).isEqualTo(name)
            assertThat(user.email).isEqualTo(email)
        }

        @Test
        @DisplayName("비밀번호는 암호화되어 저장된다")
        fun createUser_passwordIsEncoded() {
            // arrange & act
            val user = User("testuser1", "Password1!", "홍길동",
                LocalDate.of(1990, 1, 15), "test@example.com")

            // assert
            assertThat(user.password).isNotEqualTo("Password1!")
        }

        @Test
        @DisplayName("비밀번호가 8자 미만이면 실패한다")
        fun createUser_shortPassword_throwsException() {
            val exception = assertThrows<CoreException> {
                User("testuser1", "Pass1!", "홍길동",
                    LocalDate.of(1990, 1, 15), "test@example.com")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("비밀번호가 16자 초과하면 실패한다")
        fun createUser_longPassword_throwsException() {
            val exception = assertThrows<CoreException> {
                User("testuser1", "Password12345678!", "홍길동",
                    LocalDate.of(1990, 1, 15), "test@example.com")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("비밀번호에 생년월일이 포함되면 실패한다")
        fun createUser_passwordContainsBirthDate_throwsException() {
            val birthDate = LocalDate.of(1990, 1, 15)
            val exception = assertThrows<CoreException> {
                User("testuser1", "Pass19900115!", "홍길동", birthDate, "test@example.com")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("로그인 ID가 영문/숫자가 아니면 실패한다")
        fun createUser_invalidLoginId_throwsException() {
            val exception = assertThrows<CoreException> {
                User("invalid_id!", "Password1!", "홍길동",
                    LocalDate.of(1990, 1, 15), "test@example.com")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("이메일 형식이 올바르지 않으면 실패한다")
        fun createUser_invalidEmail_throwsException() {
            val exception = assertThrows<CoreException> {
                User("testuser1", "Password1!", "홍길동",
                    LocalDate.of(1990, 1, 15), "invalid-email")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
