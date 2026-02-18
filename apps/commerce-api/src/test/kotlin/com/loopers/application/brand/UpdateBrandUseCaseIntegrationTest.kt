package com.loopers.application.brand

import com.loopers.domain.brand.BrandException
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.testcontainers.MySqlTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
@Sql(statements = ["DELETE FROM brand"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class UpdateBrandUseCaseIntegrationTest {

    @Autowired
    private lateinit var updateBrandUseCase: UpdateBrandUseCase

    @Autowired
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @Autowired
    private lateinit var deleteBrandUseCase: DeleteBrandUseCase

    @Test
    fun `정상적인 경우 브랜드가 수정되어야 한다`() {
        val brandId = registerBrandUseCase.register(createRegisterCommand())
        val updateCommand = UpdateBrandCommand(
            name = "수정된브랜드",
            description = "수정된 설명",
            logoUrl = "https://example.com/new-logo.png",
        )

        val result = updateBrandUseCase.update(brandId, updateCommand)

        assertThat(result.name).isEqualTo("수정된브랜드")
        assertThat(result.description).isEqualTo("수정된 설명")
    }

    @Test
    fun `존재하지 않는 브랜드를 수정하면 NOT_FOUND 예외가 발생한다`() {
        val updateCommand = UpdateBrandCommand(
            name = "수정된브랜드",
            description = null,
            logoUrl = null,
        )

        assertThatThrownBy { updateBrandUseCase.update(9999L, updateCommand) }
            .isInstanceOfSatisfying(CoreException::class.java) { ex ->
                assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
            }
    }

    @Test
    fun `삭제된 브랜드를 수정하면 BrandException이 발생한다`() {
        val brandId = registerBrandUseCase.register(createRegisterCommand())
        deleteBrandUseCase.delete(brandId)
        val updateCommand = UpdateBrandCommand(
            name = "수정된브랜드",
            description = null,
            logoUrl = null,
        )

        assertThatThrownBy { updateBrandUseCase.update(brandId, updateCommand) }
            .isInstanceOf(BrandException::class.java)
    }

    private fun createRegisterCommand() = RegisterBrandCommand(
        name = "테스트브랜드",
        description = "테스트 설명",
        logoUrl = "https://example.com/logo.png",
    )
}
