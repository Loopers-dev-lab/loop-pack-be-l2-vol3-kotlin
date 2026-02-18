package com.loopers.application.brand

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
@Sql(statements = ["DELETE FROM brand"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class RegisterBrandUseCaseIntegrationTest {

    @Autowired
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @Test
    fun `정상 요청의 경우 브랜드가 등록되고 ID를 반환해야 한다`() {
        val command = createCommand()

        val result = registerBrandUseCase.register(command)

        assertThat(result).isPositive()
    }

    @Test
    fun `브랜드명이 중복인 경우 CoreException이 발생해야 한다`() {
        val command = createCommand()
        registerBrandUseCase.register(command)

        val exception = assertThrows<CoreException> { registerBrandUseCase.register(command) }
        assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
    }

    private fun createCommand() = RegisterBrandCommand(
        name = BRAND_NAME,
        description = DESCRIPTION,
        logoUrl = LOGO_URL,
    )

    companion object {
        private const val BRAND_NAME = "테스트브랜드"
        private const val DESCRIPTION = "테스트 설명"
        private const val LOGO_URL = "https://example.com/logo.png"
    }
}
