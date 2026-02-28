package com.loopers.application.brand

import com.loopers.domain.brand.fixture.FakeBrandRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RegisterBrandUseCaseTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        registerBrandUseCase = RegisterBrandUseCase(brandRepository)
    }

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

        assertThatThrownBy { registerBrandUseCase.register(command) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.CONFLICT)
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
