package com.loopers.application.brand

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
class GetBrandUseCaseIntegrationTest {

    @Autowired
    private lateinit var getBrandUseCase: GetBrandUseCase

    @Autowired
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @Autowired
    private lateinit var deleteBrandUseCase: DeleteBrandUseCase

    @Test
    fun `존재하는 브랜드를 정상 조회할 수 있다`() {
        val brandId = registerBrandUseCase.register(createCommand())

        val result = getBrandUseCase.getById(brandId)

        assertThat(result.id).isEqualTo(brandId)
        assertThat(result.name).isEqualTo(BRAND_NAME)
    }

    @Test
    fun `존재하지 않는 브랜드를 조회하면 NOT_FOUND 예외가 발생한다`() {
        assertThatThrownBy { getBrandUseCase.getById(9999L) }
            .isInstanceOfSatisfying(CoreException::class.java) { ex ->
                assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
            }
    }

    @Test
    fun `삭제된 브랜드를 getActiveById로 조회하면 NOT_FOUND 예외가 발생한다`() {
        val brandId = registerBrandUseCase.register(createCommand())
        deleteBrandUseCase.delete(brandId)

        assertThatThrownBy { getBrandUseCase.getActiveById(brandId) }
            .isInstanceOfSatisfying(CoreException::class.java) { ex ->
                assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
            }
    }

    @Test
    fun `삭제된 브랜드를 getById로 조회하면 정상적으로 반환된다`() {
        val brandId = registerBrandUseCase.register(createCommand())
        deleteBrandUseCase.delete(brandId)

        val result = getBrandUseCase.getById(brandId)

        assertThat(result.id).isEqualTo(brandId)
        assertThat(result.deletedAt).isNotNull()
    }

    private fun createCommand() = RegisterBrandCommand(
        name = BRAND_NAME,
        description = "테스트 설명",
        logoUrl = "https://example.com/logo.png",
    )

    companion object {
        private const val BRAND_NAME = "테스트브랜드"
    }
}
