package com.loopers.application.user.auth

import com.loopers.domain.user.EncodedPassword
import com.loopers.domain.user.RawPassword
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.kotlin.mock
import java.time.LocalDate

@DisplayName("UserAuthenticateUseCase")
class UserAuthenticateUseCaseTest {
    private val userRepository: UserRepository = mock()
    private val passwordHasher: UserPasswordHasher = mock()
    private val sut = UserAuthenticateUseCase(userRepository, passwordHasher)

    private fun existingUser(): User = User.retrieve(
        id = 1L,
        loginId = "testuser1",
        password = "encoded_Password1!",
        name = "홍길동",
        birthDate = LocalDate.of(1990, 1, 1),
        email = "test@example.com",
    )

    @Nested
    @DisplayName("유효한 자격 증명이면 User를 반환한다")
    inner class WhenValidCredentials {
        @Test
        @DisplayName("loginId와 password가 일치하면 User 객체를 반환한다")
        fun authenticate_validCredentials_returnsUser() {
            // arrange
            val user = existingUser()
            given(userRepository.findByLoginId("testuser1")).willReturn(user)
            given(passwordHasher.matches(RawPassword("Password1!"), EncodedPassword("encoded_Password1!")))
                .willReturn(true)

            // act
            val result = sut.authenticate("testuser1", "Password1!")

            // assert
            assertThat(result.id).isEqualTo(1L)
            assertThat(result.loginId.value).isEqualTo("testuser1")
        }
    }

    @Nested
    @DisplayName("authenticateAndGetId는 userId를 반환한다")
    inner class WhenAuthenticateAndGetId {
        @Test
        @DisplayName("인증 성공 시 userId를 반환한다")
        fun authenticateAndGetId_validCredentials_returnsUserId() {
            // arrange
            given(userRepository.findByLoginId("testuser1")).willReturn(existingUser())
            given(passwordHasher.matches(RawPassword("Password1!"), EncodedPassword("encoded_Password1!")))
                .willReturn(true)

            // act
            val result = sut.authenticateAndGetId("testuser1", "Password1!")

            // assert
            assertThat(result).isEqualTo(1L)
        }
    }

    @Nested
    @DisplayName("자격 증명이 유효하지 않으면 예외를 던진다")
    inner class WhenInvalidCredentials {
        @Test
        @DisplayName("존재하지 않는 loginId → CoreException(UNAUTHORIZED)")
        fun authenticate_unknownLoginId_throwsUnauthorized() {
            // arrange
            given(userRepository.findByLoginId("nonexistent")).willReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                sut.authenticate("nonexistent", "Password1!")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @Test
        @DisplayName("비밀번호 불일치 → CoreException(UNAUTHORIZED)")
        fun authenticate_wrongPassword_throwsUnauthorized() {
            // arrange
            given(userRepository.findByLoginId("testuser1")).willReturn(existingUser())
            given(passwordHasher.matches(RawPassword("WrongPassword1!"), EncodedPassword("encoded_Password1!")))
                .willReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                sut.authenticate("testuser1", "WrongPassword1!")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }
}
