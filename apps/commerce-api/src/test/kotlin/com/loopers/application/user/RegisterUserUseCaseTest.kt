package com.loopers.application.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.UserErrorCode
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class RegisterUserUseCaseTest @Autowired constructor(
    private val registerUserUseCase: RegisterUserUseCase,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("회원가입")
    @Nested
    inner class Execute {

        @DisplayName("정상적인 정보가 주어지면 회원가입에 성공하고 마스킹된 UserInfo를 반환한다")
        @Test
        fun success() {
            val result = registerUserUseCase.execute(
                UserCommand.Register(
                    loginId = "testuser",
                    rawPassword = "Test123!",
                    name = "홍길동",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "test@example.com",
                ),
            )

            assertAll(
                { assertThat(result.loginId).isEqualTo("testuser") },
                { assertThat(result.name).isEqualTo("홍길*") },
                { assertThat(result.birthDate).isEqualTo(LocalDate.of(1990, 1, 1)) },
                { assertThat(result.email).isEqualTo("test@example.com") },
            )
        }

        @DisplayName("이미 존재하는 로그인 ID로 가입하면 DUPLICATE_LOGIN_ID 예외가 발생한다")
        @Test
        fun failWhenDuplicateLoginId() {
            registerUserUseCase.execute(
                UserCommand.Register(
                    loginId = "existinguser",
                    rawPassword = "Test123!",
                    name = "기존회원",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "existing@example.com",
                ),
            )

            val exception = assertThrows<CoreException> {
                registerUserUseCase.execute(
                    UserCommand.Register(
                        loginId = "existinguser",
                        rawPassword = "Test456!",
                        name = "신규회원",
                        birthDate = LocalDate.of(1995, 5, 5),
                        email = "new@example.com",
                    ),
                )
            }
            assertThat(exception.errorCode).isEqualTo(UserErrorCode.DUPLICATE_LOGIN_ID)
        }
    }
}
