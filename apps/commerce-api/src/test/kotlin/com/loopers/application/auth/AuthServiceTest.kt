package com.loopers.application.auth

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

class AuthServiceTest {

    private lateinit var userService: UserService
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        userService = mockk()
        authService = AuthService(userService)
    }

    @Nested
    @DisplayName("authenticate 호출 시")
    inner class Authenticate {

        @Test
        @DisplayName("올바른 로그인 정보로 인증하면 User를 반환한다")
        fun authenticate_withValidCredentials_returnsUser() {
            // arrange
            val user = UserTestFixture.createUser()
            every { userService.getUserInfo(UserTestFixture.DEFAULT_LOGIN_ID) } returns user

            // act
            val result = authService.authenticate(UserTestFixture.DEFAULT_LOGIN_ID, UserTestFixture.DEFAULT_PASSWORD)

            // assert
            assertThat(result).isSameAs(user)
            verify(exactly = 1) { userService.getUserInfo(UserTestFixture.DEFAULT_LOGIN_ID) }
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 인증하면 UNAUTHORIZED 예외가 발생한다")
        fun authenticate_userNotFound_throwsException() {
            // arrange
            val loginId = "nonexistent"
            every { userService.getUserInfo(loginId) } returns null

            // act
            val exception = assertThrows<CoreException> {
                authService.authenticate(loginId, UserTestFixture.DEFAULT_PASSWORD)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
            assertThat(exception.message).isEqualTo("사용자를 찾을 수 없습니다.")
        }

        @Test
        @DisplayName("잘못된 비밀번호로 인증하면 UNAUTHORIZED 예외가 발생한다")
        fun authenticate_wrongPassword_throwsException() {
            // arrange
            val user = UserTestFixture.createUser()
            every { userService.getUserInfo(UserTestFixture.DEFAULT_LOGIN_ID) } returns user

            // act
            val exception = assertThrows<CoreException> {
                authService.authenticate(UserTestFixture.DEFAULT_LOGIN_ID, "WrongPass1!")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
            assertThat(exception.message).isEqualTo("로그인 정보가 일치하지 않습니다.")
        }
    }
}
