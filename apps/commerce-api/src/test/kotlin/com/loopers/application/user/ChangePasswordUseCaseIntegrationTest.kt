package com.loopers.application.user

import com.loopers.domain.user.LoginId
import com.loopers.domain.user.PasswordEncoder
import com.loopers.domain.user.UserRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.testcontainers.MySqlTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
@Sql(statements = ["DELETE FROM users"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class ChangePasswordUseCaseIntegrationTest {

    @Autowired
    private lateinit var changePasswordUseCase: ChangePasswordUseCase

    @Autowired
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = registerUserUseCase.register(
            RegisterUserCommand(
                loginId = LOGIN_ID,
                password = OLD_PASSWORD,
                name = NAME,
                birthDate = BIRTH_DATE,
                email = EMAIL,
                gender = GENDER,
            ),
        )
    }

    @Test
    fun `정상적인 경우 비밀번호가 변경되어야 한다`() {
        val command = ChangePasswordCommand(
            oldPassword = OLD_PASSWORD,
            newPassword = NEW_PASSWORD,
        )

        changePasswordUseCase.execute(userId, command)

        val user = userRepository.findByLoginId(LoginId(LOGIN_ID))!!
        assertThat(user.password.matches(NEW_PASSWORD, passwordEncoder)).isTrue()
    }

    @Test
    fun `기존 비밀번호가 틀리면 실패해야 한다`() {
        val command = ChangePasswordCommand(
            oldPassword = "WrongPass1!",
            newPassword = NEW_PASSWORD,
        )

        assertThatThrownBy { changePasswordUseCase.execute(userId, command) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("기존 비밀번호가 일치하지 않습니다")
    }

    @Test
    fun `새 비밀번호가 기존과 동일하면 실패해야 한다`() {
        val command = ChangePasswordCommand(
            oldPassword = OLD_PASSWORD,
            newPassword = OLD_PASSWORD,
        )

        assertThatThrownBy { changePasswordUseCase.execute(userId, command) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("새 비밀번호는 기존과 달라야 합니다")
    }

    @Test
    fun `새 비밀번호에 생년월일이 포함되면 실패해야 한다`() {
        val command = ChangePasswordCommand(
            oldPassword = OLD_PASSWORD,
            newPassword = "New19930401!",
        )

        assertThatThrownBy { changePasswordUseCase.execute(userId, command) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("비밀번호에 생년월일을 포함할 수 없습니다")
    }

    @Test
    fun `존재하지 않는 userId면 NOT_FOUND 예외가 발생해야 한다`() {
        val command = ChangePasswordCommand(
            oldPassword = OLD_PASSWORD,
            newPassword = NEW_PASSWORD,
        )

        assertThatThrownBy { changePasswordUseCase.execute(9999L, command) }
            .isInstanceOfSatisfying(CoreException::class.java) { ex ->
                assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
            }
    }

    @Test
    fun `새 비밀번호가 8자 미만이면 실패해야 한다`() {
        val command = ChangePasswordCommand(
            oldPassword = OLD_PASSWORD,
            newPassword = "Short1!",
        )

        assertThatThrownBy { changePasswordUseCase.execute(userId, command) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    companion object {
        private const val LOGIN_ID = "testuser"
        private const val OLD_PASSWORD = "Password1!"
        private const val NEW_PASSWORD = "NewPass2@"
        private const val NAME = "테스트"
        private const val BIRTH_DATE = "1993-04-01"
        private const val EMAIL = "test@example.com"
        private const val GENDER = "MALE"
    }
}
