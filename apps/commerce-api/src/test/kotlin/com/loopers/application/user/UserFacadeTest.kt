package com.loopers.application.user

import com.loopers.domain.user.User
import com.loopers.domain.user.UserCommand
import com.loopers.domain.user.UserService
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
import java.time.LocalDate

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
                loginId = "testuser1",
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com"
            )
            val user = User(
                loginId = command.loginId,
                password = command.password,
                name = command.name,
                birthDate = command.birthDate,
                email = command.email
            )
            every { userService.signUp(command) } returns user

            // act
            val result = userFacade.signUp(command)

            // assert
            assertThat(result.loginId).isEqualTo(command.loginId)
            assertThat(result.name).isEqualTo(command.name)
            assertThat(result.email).isEqualTo(command.email)
            verify(exactly = 1) { userService.signUp(command) }
        }
    }

    @Nested
    @DisplayName("getUserInfo 호출 시")
    inner class GetUserInfo {

        @Test
        @DisplayName("사용자가 존재하면 마스킹된 이름으로 UserInfo를 반환한다")
        fun getUserInfo_userExists_returnsUserInfoWithMaskedName() {
            // arrange
            val loginId = "testuser1"
            val user = User(
                loginId = loginId,
                password = "Password1!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com"
            )
            every { userService.getUserInfo(loginId) } returns user

            // act
            val result = userFacade.getUserInfo(loginId)

            // assert
            assertThat(result.loginId).isEqualTo(loginId)
            assertThat(result.name).isEqualTo("홍길*")
            verify(exactly = 1) { userService.getUserInfo(loginId) }
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
            val loginId = "testuser1"
            val command = UserCommand.ChangePassword(
                currentPassword = "Password1!",
                newPassword = "NewPass12!"
            )
            every { userService.changePassword(loginId, command) } returns Unit

            // act
            userFacade.changePassword(loginId, command)

            // assert
            verify(exactly = 1) { userService.changePassword(loginId, command) }
        }
    }
}
