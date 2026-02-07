package com.loopers.application.user

import com.loopers.domain.user.UserCommand
import com.loopers.domain.user.UserService
import com.loopers.domain.user.UserTestFixture
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UserFacadeTest {

    private lateinit var userService: UserService
    private lateinit var userFacade: UserFacade

    @BeforeEach
    fun setUp() {
        userService = mockk()
        userFacade = UserFacade(userService)
    }

    @Nested
    @DisplayName("signUp 호출 시")
    inner class SignUp {

        @Test
        @DisplayName("회원가입이 성공하면 UserInfo를 반환한다")
        fun signUp_success_returnsUserInfo() {
            // arrange
            val command = UserCommand.SignUp(
                loginId = UserTestFixture.DEFAULT_LOGIN_ID,
                password = UserTestFixture.DEFAULT_PASSWORD,
                name = UserTestFixture.DEFAULT_NAME,
                birthDate = UserTestFixture.DEFAULT_BIRTH_DATE,
                email = UserTestFixture.DEFAULT_EMAIL,
            )
            val user = UserTestFixture.createUser()
            every { userService.signUp(command) } returns user

            // act
            val result = userFacade.signUp(command)

            // assert
            assertThat(result.loginId).isEqualTo(UserTestFixture.DEFAULT_LOGIN_ID)
            assertThat(result.name).isEqualTo(UserTestFixture.DEFAULT_NAME)
            assertThat(result.email).isEqualTo(UserTestFixture.DEFAULT_EMAIL)
            verify(exactly = 1) { userService.signUp(command) }
        }

        @Test
        @DisplayName("중복 로그인 ID로 가입하면 CONFLICT 예외가 전파된다")
        fun signUp_duplicateLoginId_propagatesConflict() {
            // arrange
            val command = UserCommand.SignUp(
                loginId = UserTestFixture.DEFAULT_LOGIN_ID,
                password = UserTestFixture.DEFAULT_PASSWORD,
                name = UserTestFixture.DEFAULT_NAME,
                birthDate = UserTestFixture.DEFAULT_BIRTH_DATE,
                email = UserTestFixture.DEFAULT_EMAIL,
            )
            every { userService.signUp(command) } throws CoreException(ErrorType.CONFLICT, "이미 존재하는 로그인 ID입니다.")

            // act
            val exception = assertThrows<CoreException> {
                userFacade.signUp(command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
            assertThat(exception.message).isEqualTo("이미 존재하는 로그인 ID입니다.")
        }
    }

    @Nested
    @DisplayName("getUserInfo 호출 시")
    inner class GetUserInfo {

        @Test
        @DisplayName("사용자가 존재하면 마스킹된 이름으로 UserInfo를 반환한다")
        fun getUserInfo_userExists_returnsUserInfoWithMaskedName() {
            // arrange
            val user = UserTestFixture.createUser()
            every { userService.getUserInfo(UserTestFixture.DEFAULT_LOGIN_ID) } returns user

            // act
            val result = userFacade.getUserInfo(UserTestFixture.DEFAULT_LOGIN_ID)

            // assert
            assertThat(result.loginId).isEqualTo(UserTestFixture.DEFAULT_LOGIN_ID)
            assertThat(result.name).isEqualTo("홍길*")
            verify(exactly = 1) { userService.getUserInfo(UserTestFixture.DEFAULT_LOGIN_ID) }
        }

        @Test
        @DisplayName("사용자가 존재하지 않으면 NOT_FOUND 예외가 발생한다")
        fun getUserInfo_userNotFound_throwsException() {
            // arrange
            val loginId = "nonexistent"
            every { userService.getUserInfo(loginId) } returns null

            // act
            val exception = assertThrows<CoreException> {
                userFacade.getUserInfo(loginId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
            assertThat(exception.message).isEqualTo("사용자를 찾을 수 없습니다.")
        }
    }

    @Nested
    @DisplayName("changePassword 호출 시")
    inner class ChangePassword {

        @Test
        @DisplayName("UserService의 changePassword를 호출한다")
        fun changePassword_callsUserService() {
            // arrange
            val command = UserCommand.ChangePassword(
                currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                newPassword = "NewPass12!",
            )
            every { userService.changePassword(UserTestFixture.DEFAULT_LOGIN_ID, command) } returns Unit

            // act
            userFacade.changePassword(UserTestFixture.DEFAULT_LOGIN_ID, command)

            // assert
            verify(exactly = 1) { userService.changePassword(UserTestFixture.DEFAULT_LOGIN_ID, command) }
        }

        @Test
        @DisplayName("존재하지 않는 사용자이면 NOT_FOUND 예외가 전파된다")
        fun changePassword_userNotFound_propagatesNotFound() {
            // arrange
            val loginId = "nonexistent"
            val command = UserCommand.ChangePassword(
                currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                newPassword = "NewPass12!",
            )
            every { userService.changePassword(loginId, command) } throws CoreException(ErrorType.NOT_FOUND, "사용자를 찾을 수 없습니다.")

            // act
            val exception = assertThrows<CoreException> {
                userFacade.changePassword(loginId, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
            assertThat(exception.message).isEqualTo("사용자를 찾을 수 없습니다.")
        }

        @Test
        @DisplayName("현재 비밀번호가 일치하지 않으면 BAD_REQUEST 예외가 전파된다")
        fun changePassword_wrongCurrentPassword_propagatesBadRequest() {
            // arrange
            val command = UserCommand.ChangePassword(
                currentPassword = "WrongPass1!",
                newPassword = "NewPass12!",
            )
            every { userService.changePassword(UserTestFixture.DEFAULT_LOGIN_ID, command) } throws CoreException(ErrorType.BAD_REQUEST, "현재 비밀번호가 일치하지 않습니다.")

            // act
            val exception = assertThrows<CoreException> {
                userFacade.changePassword(UserTestFixture.DEFAULT_LOGIN_ID, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
            assertThat(exception.message).isEqualTo("현재 비밀번호가 일치하지 않습니다.")
        }

        @Test
        @DisplayName("새 비밀번호가 규칙을 위반하면 BAD_REQUEST 예외가 전파된다")
        fun changePassword_invalidNewPassword_propagatesBadRequest() {
            // arrange
            val command = UserCommand.ChangePassword(
                currentPassword = UserTestFixture.DEFAULT_PASSWORD,
                newPassword = "short",
            )
            every { userService.changePassword(UserTestFixture.DEFAULT_LOGIN_ID, command) } throws CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8 ~ 16 자의 영문 대소문자, 숫자, 특수문자만 가능합니다.")

            // act
            val exception = assertThrows<CoreException> {
                userFacade.changePassword(UserTestFixture.DEFAULT_LOGIN_ID, command)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
