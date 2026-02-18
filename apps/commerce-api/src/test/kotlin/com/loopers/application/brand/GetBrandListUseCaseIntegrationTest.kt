package com.loopers.application.brand

import com.loopers.testcontainers.MySqlTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
@Sql(statements = ["DELETE FROM brand"], executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class GetBrandListUseCaseIntegrationTest {

    @Autowired
    private lateinit var getBrandListUseCase: GetBrandListUseCase

    @Autowired
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @Autowired
    private lateinit var deleteBrandUseCase: DeleteBrandUseCase

    @Test
    fun `getAll은 삭제된 브랜드를 포함하여 전체 목록을 반환한다`() {
        val brandId1 = registerBrandUseCase.register(createCommand("브랜드A"))
        registerBrandUseCase.register(createCommand("브랜드B"))
        deleteBrandUseCase.delete(brandId1)

        val result = getBrandListUseCase.getAll()

        assertThat(result).hasSize(2)
    }

    @Test
    fun `getAllActive는 삭제된 브랜드를 제외한 목록을 반환한다`() {
        val brandId1 = registerBrandUseCase.register(createCommand("브랜드A"))
        registerBrandUseCase.register(createCommand("브랜드B"))
        deleteBrandUseCase.delete(brandId1)

        val result = getBrandListUseCase.getAllActive()

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("브랜드B")
    }

    private fun createCommand(name: String) = RegisterBrandCommand(
        name = name,
        description = "테스트 설명",
        logoUrl = null,
    )
}
