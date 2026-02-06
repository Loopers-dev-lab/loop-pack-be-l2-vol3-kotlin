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
        @DisplayName("로그인 ID가 16자를 초과하면 실패한다")
        fun createUser_tooLongLoginId_throwsException() {
            // arrange
            val longLoginId = "a".repeat(17)

            // act
            val exception = assertThrows<CoreException> {
                User(longLoginId, "Password1!", "홍길동",
                    LocalDate.of(1990, 1, 15), "test@example.com")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("로그인 ID는 16자를 초과할 수 없습니다.")
        }

        @Test
        @DisplayName("비밀번호에 동일 문자가 3회 이상 연속되면 실패한다")
        fun createUser_passwordWithConsecutiveChars_throwsException() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                User("testuser1", "Passsword1!", "홍길동",
                    LocalDate.of(1990, 1, 15), "test@example.com")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("동일 문자가 3회 이상 연속될 수 없습니다.")
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

        @Test
        @DisplayName("이름이 비어있거나 공백이면 실패한다")
        fun createUser_blankName_throwsException() {
            val exception = assertThrows<CoreException> {
                User("testuser1", "Password1!", " ",
                    LocalDate.of(1990, 1, 15), "test@example.com")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("이름은 필수입니다.")
        }

        @Test
        @DisplayName("이름이 10자를 초과하면 실패한다")
        fun createUser_tooLongName_throwsException() {
            val longName = "가".repeat(20)
            val exception = assertThrows<CoreException> {
                User("testuser1", "Password1!", longName,
                    LocalDate.of(1990, 1, 15), "test@example.com")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("이름은 10자 이내여야 합니다.")
        }

        @Test
        @DisplayName("이름에 숫자나 특수문자가 포함되면 실패한다")
        fun createUser_invalidNameCharacters_throwsException() {
            // 숫자가 포함된 경우
            val exceptionWithNumber = assertThrows<CoreException> {
                User("testuser1", "Password1!", "홍길동1",
                    LocalDate.of(1990, 1, 15), "test@example.com")
            }
            assertThat(exceptionWithNumber.message).isEqualTo("이름은 한글 또는 영문만 허용됩니다.")

            // 특수문자가 포함된 경우
            val exceptionWithSpecialChar = assertThrows<CoreException> {
                User("testuser1", "Password1!", "John!",
                    LocalDate.of(1990, 1, 15), "test@example.com")
            }
            assertThat(exceptionWithSpecialChar.message).isEqualTo("이름은 한글 또는 영문만 허용됩니다.")
        }
    }

    @Nested
    @DisplayName("이름 마스킹 시")
    inner class MaskName {
        @Test
        @DisplayName("마지막 글자가 *로 마스킹된다")
        fun getMaskedName_masksLastChar() {
            val user = User("testuser1", "Password1!", "홍길동",
                LocalDate.of(1990, 1, 15), "test@example.com")

            assertThat(user.getMaskedName()).isEqualTo("홍길*")
        }
    }

    @Nested
    @DisplayName("비밀번호 변경 시")
    inner class ChangePassword {
        @Test
        @DisplayName("현재 비밀번호와 같으면 실패한다")
        fun changePassword_sameAsCurrent_throwsException() {
            val user = User("testuser1", "Password1!", "홍길동",
                LocalDate.of(1990, 1, 15), "test@example.com")

            val exception = assertThrows<CoreException> {
                user.changePassword("Password1!")
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("유효한 새 비밀번호로 변경하면 성공한다")
        fun changePassword_withValidPassword_success() {
            val user = User("testuser1", "Password1!", "홍길동",
                LocalDate.of(1990, 1, 15), "test@example.com")

            user.changePassword("NewPass12!")

            assertThat(user.verifyPassword("NewPass12!")).isTrue()
        }
    }
}
