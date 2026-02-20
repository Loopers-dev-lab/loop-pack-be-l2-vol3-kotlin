package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class UserServiceTest {

    private lateinit var userRepository: FakeUserRepository
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userRepository = FakeUserRepository()
        userService = UserService(userRepository)
    }

    private fun signUpDefaultUser(): com.loopers.domain.user.entity.User {
        val command = UserCommand.SignUp(
            loginId = UserTestFixture.DEFAULT_LOGIN_ID,
            password = UserTestFixture.DEFAULT_PASSWORD,
            name = UserTestFixture.DEFAULT_NAME,
            birthDate = UserTestFixture.DEFAULT_BIRTH_DATE,
            email = UserTestFixture.DEFAULT_EMAIL,
        )
        return userService.signUp(command)
    }

    @Nested
    @DisplayName("회원가입 시")
    inner class SignUp {

        @Test
        @DisplayName("유효한 정보로 가입하면 User가 저장되고 반환된다")
        fun signUp_withValidData_savesAndReturnsUser() {
            // act
            val result = signUpDefaultUser()

            // assert
            assertThat(result.id).isNotEqualTo(0L)
            assertThat(result.loginId).isEqualTo("testuser1")
            assertThat(result.name).isEqualTo("홍길동")
            assertThat(result.email).isEqualTo("test@example.com")
        }

        @Test
        @DisplayName("이미 존재하는 로그인 ID로 가입하면 CONFLICT 예외가 발생한다")
        fun signUp_duplicateLoginId_throwsConflict() {
            // arrange
            signUpDefaultUser()

            val command = UserCommand.SignUp(
                loginId = UserTestFixture.DEFAULT_LOGIN_ID,
                password = "Password2!",
                name = "김철수",
                birthDate = LocalDate.of(1995, 5, 20),
                email = "other@example.com",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.signUp(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
            assertThat(exception.message).isEqualTo("이미 존재하는 로그인 ID입니다.")
        }
    }

    @Nested
    @DisplayName("내 정보 조회 시")
    inner class GetUserInfo {

        @Test
        @DisplayName("존재하는 사용자를 조회하면 User를 반환한다")
        fun getUserInfo_userExists_returnsUser() {
            // arrange
            signUpDefaultUser()

            // act
            val result = userService.getUserInfo("testuser1")

            // assert
            assertThat(result).isNotNull()
            assertThat(result?.loginId).isEqualTo("testuser1")
        }

        @Test
        @DisplayName("존재하지 않는 사용자를 조회하면 null을 반환한다")
        fun getUserInfo_userNotExists_returnsNull() {
            // act
            val result = userService.getUserInfo("nonexistent")

            // assert
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("getUser 호출 시")
    inner class GetUser {

        @Test
        @DisplayName("존재하는 사용자를 조회하면 User를 반환한다")
        fun getUser_userExists_returnsUser() {
            // arrange
            val saved = signUpDefaultUser()

            // act
            val result = userService.getUser(saved.id)

            // assert
            assertThat(result.loginId).isEqualTo("testuser1")
        }

        @Test
        @DisplayName("존재하지 않는 사용자이면 NOT_FOUND 예외가 발생한다")
        fun getUser_userNotFound_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                userService.getUser(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
            assertThat(exception.message).isEqualTo("사용자를 찾을 수 없습니다.")
        }
    }

    @Nested
    @DisplayName("비밀번호 변경 시")
    inner class ChangePassword {

        @Test
        @DisplayName("유효한 요청이면 비밀번호가 변경된다")
        fun changePassword_withValidRequest_success() {
            // arrange
            val saved = signUpDefaultUser()
            val command = UserCommand.ChangePassword(
                currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                newPassword = "NewPass12!",
            )

            // act
            userService.changePassword(saved.id, command)

            // assert
            val user = userService.getUser(saved.id)
            assertThat(user.verifyPassword("NewPass12!")).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 사용자이면 NOT_FOUND 예외가 발생한다")
        fun changePassword_userNotFound_throwsNotFound() {
            // arrange
            val command = UserCommand.ChangePassword(
                currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                newPassword = "NewPass12!",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(999L, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
            assertThat(exception.message).isEqualTo("사용자를 찾을 수 없습니다.")
        }

        @Test
        @DisplayName("현재 비밀번호가 일치하지 않으면 BAD_REQUEST 예외가 발생한다")
        fun changePassword_wrongCurrentPassword_throwsBadRequest() {
            // arrange
            val saved = signUpDefaultUser()
            val command = UserCommand.ChangePassword(
                currentPassword = "WrongPass1!",
                newPassword = "NewPass12!",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(saved.id, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("현재 비밀번호가 일치하지 않습니다.")
        }

        @Test
        @DisplayName("현재 비밀번호와 동일한 비밀번호로 변경하면 BAD_REQUEST 예외가 발생한다")
        fun changePassword_sameAsCurrent_throwsBadRequest() {
            // arrange
            val saved = signUpDefaultUser()
            val command = UserCommand.ChangePassword(
                currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                newPassword = UserTestFixture.DEFAULT_PASSWORD,
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(saved.id, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.")
        }

        @Test
        @DisplayName("새 비밀번호가 8자 미만이면 BAD_REQUEST 예외가 발생한다")
        fun changePassword_tooShort_throwsBadRequest() {
            // arrange
            val saved = signUpDefaultUser()
            val command = UserCommand.ChangePassword(
                currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                newPassword = "Short1!",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(saved.id, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("새 비밀번호가 16자 초과하면 BAD_REQUEST 예외가 발생한다")
        fun changePassword_tooLong_throwsBadRequest() {
            // arrange
            val saved = signUpDefaultUser()
            val command = UserCommand.ChangePassword(
                currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                newPassword = "Password12345678!",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(saved.id, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @Test
        @DisplayName("새 비밀번호에 생년월일이 포함되면 BAD_REQUEST 예외가 발생한다")
        fun changePassword_containsBirthDate_throwsBadRequest() {
            // arrange
            val saved = signUpDefaultUser()
            val command = UserCommand.ChangePassword(
                currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                newPassword = "Pass19900115!",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(saved.id, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("비밀번호에 생년월일을 포함할 수 없습니다.")
        }

        @Test
        @DisplayName("새 비밀번호에 동일 문자가 3회 이상 연속되면 BAD_REQUEST 예외가 발생한다")
        fun changePassword_consecutiveChars_throwsBadRequest() {
            // arrange
            val saved = signUpDefaultUser()
            val command = UserCommand.ChangePassword(
                currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                newPassword = "Passsword1!",
            )

            // act
            val exception = assertThrows<CoreException> {
                userService.changePassword(saved.id, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("동일 문자가 3회 이상 연속될 수 없습니다.")
        }
    }
}
