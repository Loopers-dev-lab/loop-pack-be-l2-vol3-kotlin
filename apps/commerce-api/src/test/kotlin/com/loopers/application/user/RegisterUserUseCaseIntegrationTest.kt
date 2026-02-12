package com.loopers.application.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.testcontainers.MySqlTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
@Sql(statements = ["DELETE FROM users"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class RegisterUserUseCaseIntegrationTest {

    @Autowired
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @Test
    fun `정상 요청의 경우 회원가입이 성공하고 ID를 반환해야 한다`() {
        val command = createCommand()

        val result = registerUserUseCase.register(command)

        assertThat(result).isPositive()
    }

    @Test
    fun `로그인 ID가 중복인 경우 CoreException이 발생해야 한다`() {
        val command = createCommand()
        registerUserUseCase.register(command)

        val exception = assertThrows<CoreException> { registerUserUseCase.register(command) }
        assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
    }

    private fun createCommand() = RegisterUserCommand(
        loginId = "testuser",
        password = "Password1!",
        name = "테스트",
        birthDate = "1993-04-01",
        email = "test@example.com",
        gender = "MALE",
    )
}
