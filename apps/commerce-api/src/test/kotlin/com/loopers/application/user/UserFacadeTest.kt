package com.loopers.application.user

import com.loopers.application.user.model.UserChangePasswordCommand
import com.loopers.application.user.model.UserSignUpCommand
import com.loopers.domain.user.User
import com.loopers.domain.user.UserService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import java.time.LocalDate

@DisplayName("UserFacade")
class UserFacadeTest {
    private val userService: UserService = mock()
    private val userFacade = UserFacade(userService)

    private val defaultBirthDate = LocalDate.of(1990, 1, 1)

    private fun savedUser(loginId: String = "testuser1"): User = User.retrieve(
        id = 1L,
        loginId = loginId,
        password = "encoded_Password1!",
        name = "홍길동",
        birthDate = defaultBirthDate,
        email = "test@example.com",
    )

    @Nested
    @DisplayName("회원가입")
    inner class SignUp {
        @Test
        @DisplayName("signUp 성공 - UserInfo(loginId)를 반환한다")
        fun signUp_success_returnsUserInfo() {
            // arrange
            given(userService.register(any())).willReturn(savedUser())

            // act
            val result = userFacade.signUp(
                UserSignUpCommand("testuser1", "Password1!", "홍길동", defaultBirthDate, "test@example.com"),
            )

            // assert
            assertThat(result.loginId).isEqualTo("testuser1")
        }

        @Test
        @DisplayName("signUp 호출 시 UserService.register()에 위임한다")
        fun signUp_delegatesToUserServiceRegister() {
            // arrange
            given(userService.register(any())).willReturn(savedUser())

            // act
            userFacade.signUp(
                UserSignUpCommand("testuser1", "Password1!", "홍길동", defaultBirthDate, "test@example.com"),
            )

            // assert
            then(userService).should().register(any())
        }
    }

    @Nested
    @DisplayName("내 정보 조회")
    inner class GetMe {
        @Test
        @DisplayName("getMe 성공 - 마스킹된 UserInfo를 반환한다")
        fun getMe_success_returnsMaskedUserInfo() {
            // arrange
            given(userService.findByCredentials("testuser1", "Password1!")).willReturn(savedUser())

            // act
            val result = userFacade.getMe("testuser1", "Password1!")

            // assert
            assertAll(
                { assertThat(result.loginId).isEqualTo("testuser1") },
                { assertThat(result.name).isEqualTo("홍길*") },
                { assertThat(result.birthDate).isEqualTo(defaultBirthDate) },
                { assertThat(result.email).isEqualTo("test@example.com") },
            )
        }
    }

    @Nested
    @DisplayName("비밀번호 수정")
    inner class ChangePassword {
        @Test
        @DisplayName("changePassword 호출 시 UserService.changePassword()에 위임한다")
        fun changePassword_delegatesToUserServiceChangePassword() {
            // arrange
            val command = UserChangePasswordCommand("Password1!", "NewPassword1!")

            // act
            userFacade.changePassword("testuser1", "Password1!", command)

            // assert
            then(userService).should().changePassword(eq("testuser1"), eq("Password1!"), eq(command))
        }
    }
}
