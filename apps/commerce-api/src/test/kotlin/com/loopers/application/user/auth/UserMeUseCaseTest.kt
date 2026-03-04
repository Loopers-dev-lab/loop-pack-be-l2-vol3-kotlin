package com.loopers.application.user.auth

import com.loopers.domain.user.User
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.kotlin.mock
import java.time.LocalDate

@DisplayName("UserMeUseCase")
class UserMeUseCaseTest {
    private val userAuthenticateUseCase: UserAuthenticateUseCase = mock()
    private val service = UserMeUseCase(userAuthenticateUseCase)

    private val defaultBirthDate = LocalDate.of(1990, 1, 1)

    private fun existingUser(): User = User.retrieve(
        id = 1L,
        loginId = "testuser1",
        password = "encoded_Password1!",
        name = "홍길동",
        birthDate = defaultBirthDate,
        email = "test@example.com",
    )

    @Nested
    @DisplayName("유효한 인증으로 조회하면 사용자 정보를 반환한다")
    inner class WhenValidCredentials {
        @Test
        @DisplayName("마스킹된 UserResult.Me를 반환한다")
        fun getMe_success_returnsMaskedUserInfo() {
            // arrange
            given(userAuthenticateUseCase.authenticate("testuser1", "Password1!"))
                .willReturn(existingUser())

            // act
            val result = service.getMe("testuser1", "Password1!")

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
    @DisplayName("인증 실패 시 예외를 던진다")
    inner class WhenAuthenticationFails {
        @Test
        @DisplayName("존재하지 않는 loginId로 조회 시 CoreException(UNAUTHORIZED)")
        fun getMe_invalidLoginId_throwsException() {
            // arrange
            given(userAuthenticateUseCase.authenticate("nonexistent", "Password1!"))
                .willThrow(CoreException(ErrorType.UNAUTHORIZED))

            // act
            val exception = assertThrows<CoreException> {
                service.getMe("nonexistent", "Password1!")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @Test
        @DisplayName("비밀번호 불일치 시 CoreException(UNAUTHORIZED)")
        fun getMe_wrongPassword_throwsException() {
            // arrange
            given(userAuthenticateUseCase.authenticate("testuser1", "WrongPassword1!"))
                .willThrow(CoreException(ErrorType.UNAUTHORIZED))

            // act
            val exception = assertThrows<CoreException> {
                service.getMe("testuser1", "WrongPassword1!")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }
}
