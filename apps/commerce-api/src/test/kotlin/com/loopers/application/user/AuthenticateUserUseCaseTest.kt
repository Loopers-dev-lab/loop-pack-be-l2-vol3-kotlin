package com.loopers.application.user

import com.loopers.domain.user.FakeUserRepository
import com.loopers.domain.user.UserTestFixture
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AuthenticateUserUseCaseTest {

    private lateinit var userRepository: FakeUserRepository
    private lateinit var authenticateUserUseCase: AuthenticateUserUseCase

    @BeforeEach
    fun setUp() {
        userRepository = FakeUserRepository()
        authenticateUserUseCase = AuthenticateUserUseCase(userRepository)
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("존재하는 사용자의 올바른 비밀번호로 인증하면 userId를 반환한다")
        fun execute_validCredentials_returnsUserId() {
            // arrange
            val user = userRepository.save(UserTestFixture.createUser())

            // act
            val result = authenticateUserUseCase.execute(
                loginId = UserTestFixture.DEFAULT_LOGIN_ID,
                password = UserTestFixture.DEFAULT_PASSWORD,
            )

            // assert
            assertThat(result).isEqualTo(user.id)
        }

        @Test
        @DisplayName("존재하지 않는 로그인 ID로 인증하면 null을 반환한다")
        fun execute_userNotExists_returnsNull() {
            // act
            val result = authenticateUserUseCase.execute(
                loginId = "nonexistent",
                password = UserTestFixture.DEFAULT_PASSWORD,
            )

            // assert
            assertThat(result).isNull()
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 null을 반환한다")
        fun execute_wrongPassword_returnsNull() {
            // arrange
            userRepository.save(UserTestFixture.createUser())

            // act
            val result = authenticateUserUseCase.execute(
                loginId = UserTestFixture.DEFAULT_LOGIN_ID,
                password = "WrongPass1!",
            )

            // assert
            assertThat(result).isNull()
        }
    }
}
