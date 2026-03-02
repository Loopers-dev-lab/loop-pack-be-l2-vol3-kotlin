package com.loopers.application.user

import com.loopers.infrastructure.user.UserJpaRepository
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
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate

@SpringBootTest
class ChangePasswordUseCaseTest @Autowired constructor(
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("비밀번호 변경")
    @Nested
    inner class Execute {

        @DisplayName("현재 비밀번호가 일치하고 새 비밀번호가 규칙을 만족하면 성공한다")
        @Test
        fun success() {
            val currentPassword = "OldPass123!"
            val newPassword = "NewPass456!"
            val userId = registerAndGetId(currentPassword)

            changePasswordUseCase.execute(
                UserCommand.ChangePassword(
                    userId = userId,
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                ),
            )

            val updatedUser = userJpaRepository.findById(userId).get()
            assertThat(passwordEncoder.matches(newPassword, updatedUser.password.value)).isTrue()
        }

        @DisplayName("현재 비밀번호가 틀리면 INVALID_CURRENT_PASSWORD 예외가 발생한다")
        @Test
        fun failWhenCurrentPasswordIsWrong() {
            val userId = registerAndGetId("Correct123!")

            val exception = assertThrows<CoreException> {
                changePasswordUseCase.execute(
                    UserCommand.ChangePassword(
                        userId = userId,
                        currentPassword = "WrongPass123!",
                        newPassword = "NewPass456!",
                    ),
                )
            }
            assertThat(exception.errorCode).isEqualTo(UserErrorCode.INVALID_CURRENT_PASSWORD)
        }

        @DisplayName("새 비밀번호가 현재 비밀번호와 같으면 SAME_PASSWORD 예외가 발생한다")
        @Test
        fun failWhenNewPasswordIsSameAsCurrent() {
            val currentPassword = "SamePass123!"
            val userId = registerAndGetId(currentPassword)

            val exception = assertThrows<CoreException> {
                changePasswordUseCase.execute(
                    UserCommand.ChangePassword(
                        userId = userId,
                        currentPassword = currentPassword,
                        newPassword = currentPassword,
                    ),
                )
            }
            assertThat(exception.errorCode).isEqualTo(UserErrorCode.SAME_PASSWORD)
        }

        @DisplayName("새 비밀번호가 규칙을 위반하면 예외가 발생한다")
        @Test
        fun failWhenNewPasswordViolatesRule() {
            val currentPassword = "OldPass123!"
            val userId = registerAndGetId(currentPassword)

            val exception = assertThrows<CoreException> {
                changePasswordUseCase.execute(
                    UserCommand.ChangePassword(
                        userId = userId,
                        currentPassword = currentPassword,
                        newPassword = "Short1!",
                    ),
                )
            }
            assertThat(exception.errorCode).isEqualTo(UserErrorCode.INVALID_PASSWORD_LENGTH)
        }
    }

    private fun registerAndGetId(password: String): Long {
        registerUserUseCase.execute(
            UserCommand.Register(
                loginId = "testuser",
                rawPassword = password,
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            ),
        )
        return userJpaRepository.findByLoginId("testuser")!!.id!!
    }
}
