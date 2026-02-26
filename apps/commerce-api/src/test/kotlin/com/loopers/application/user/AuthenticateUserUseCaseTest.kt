package com.loopers.application.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.UserErrorCode
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class AuthenticateUserUseCaseTest @Autowired constructor(
    private val authenticateUserUseCase: AuthenticateUserUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("사용자 인증")
    @Nested
    inner class Execute {

        @DisplayName("올바른 로그인ID와 비밀번호가 주어지면 사용자 ID를 반환한다")
        @Test
        fun success() {
            registerUser()

            val userId = authenticateUserUseCase.execute("testuser", "Test123!")

            assertThat(userId).isGreaterThan(0)
        }

        @DisplayName("존재하지 않는 로그인ID이면 AUTHENTICATION_FAILED 예외가 발생한다")
        @Test
        fun failWhenUserNotFound() {
            val exception = assertThrows<CoreException> {
                authenticateUserUseCase.execute("nonexistent", "Test123!")
            }
            assertThat(exception.errorCode).isEqualTo(UserErrorCode.AUTHENTICATION_FAILED)
        }

        @DisplayName("비밀번호가 일치하지 않으면 AUTHENTICATION_FAILED 예외가 발생한다")
        @Test
        fun failWhenPasswordNotMatched() {
            registerUser()

            val exception = assertThrows<CoreException> {
                authenticateUserUseCase.execute("testuser", "WrongPassword!")
            }
            assertThat(exception.errorCode).isEqualTo(UserErrorCode.AUTHENTICATION_FAILED)
        }
    }

    private fun registerUser() {
        registerUserUseCase.execute(
            UserCommand.Register(
                loginId = "testuser",
                rawPassword = "Test123!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            ),
        )
    }
}
