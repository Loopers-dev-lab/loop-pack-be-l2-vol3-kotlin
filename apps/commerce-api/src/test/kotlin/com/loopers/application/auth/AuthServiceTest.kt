package com.loopers.application.auth

import com.loopers.domain.user.User
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
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

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
        @DisplayName("올바른 로그인 정보로 인증하면 성공한다")
        fun authenticate_withValidCredentials_success() {
            // arrange
            val loginId = "testuser1"
            val password = "Password1!"
            val user = User(
                loginId = loginId,
                password = password,
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com"
            )
            every { userService.getUserInfo(loginId) } returns user

            // act & assert
            assertDoesNotThrow {
                authService.authenticate(loginId, password)
            }
            verify(exactly = 1) { userService.getUserInfo(loginId) }
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 인증하면 NOT_FOUND 예외가 발생한다")
        fun authenticate_userNotFound_throwsException() {
            // arrange
            val loginId = "nonexistent"
            val password = "Password1!"
            every { userService.getUserInfo(loginId) } returns null

            // act
            val exception = assertThrows<CoreException> {
                authService.authenticate(loginId, password)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
            assertThat(exception.message).isEqualTo("사용자를 찾을 수 없습니다.")
        }

        @Test
        @DisplayName("잘못된 비밀번호로 인증하면 UNAUTHORIZED 예외가 발생한다")
        fun authenticate_wrongPassword_throwsException() {
            // arrange
            val loginId = "testuser1"
            val correctPassword = "Password1!"
            val wrongPassword = "WrongPass1!"
            val user = User(
                loginId = loginId,
                password = correctPassword,
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com"
            )
            every { userService.getUserInfo(loginId) } returns user

            // act
            val exception = assertThrows<CoreException> {
                authService.authenticate(loginId, wrongPassword)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
            assertThat(exception.message).isEqualTo("비밀번호가 일치하지 않습니다.")
        }
    }
}
