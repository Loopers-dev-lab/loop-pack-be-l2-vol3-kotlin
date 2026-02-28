package com.loopers.application.brand

import com.loopers.application.product.ProductImageCommand
import com.loopers.application.product.RegisterProductCommand
import com.loopers.application.product.RegisterProductUseCase
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
@Sql(
    statements = ["DELETE FROM product_image", "DELETE FROM product", "DELETE FROM brand"],
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class DeleteBrandUseCaseIntegrationTest {

    @Autowired
    private lateinit var deleteBrandUseCase: DeleteBrandUseCase

    @Autowired
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @Autowired
    private lateinit var registerProductUseCase: RegisterProductUseCase

    @Autowired
    private lateinit var getBrandUseCase: GetBrandUseCase

    @Test
    fun `정상적인 경우 브랜드가 삭제되어야 한다`() {
        val brandId = registerBrandUseCase.register(createBrandCommand())

        deleteBrandUseCase.delete(brandId)

        val result = getBrandUseCase.getById(brandId)
        assertThat(result.deletedAt).isNotNull()
    }

    @Test
    fun `브랜드 삭제 시 해당 브랜드의 상품도 논리삭제 되어야 한다`() {
        val brandId = registerBrandUseCase.register(createBrandCommand())
        registerProductUseCase.register(createProductCommand(brandId))

        deleteBrandUseCase.delete(brandId)

        val brand = getBrandUseCase.getById(brandId)
        assertThat(brand.deletedAt).isNotNull()
    }

    @Test
    fun `존재하지 않는 브랜드를 삭제하면 NOT_FOUND 예외가 발생한다`() {
        assertThatThrownBy { deleteBrandUseCase.delete(9999L) }
            .isInstanceOfSatisfying(CoreException::class.java) { ex ->
                assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
            }
    }

    @Test
    fun `이미 삭제된 브랜드를 다시 삭제해도 멱등하게 성공한다`() {
        val brandId = registerBrandUseCase.register(createBrandCommand())
        deleteBrandUseCase.delete(brandId)

        deleteBrandUseCase.delete(brandId)

        val result = getBrandUseCase.getById(brandId)
        assertThat(result.deletedAt).isNotNull()
    }

    private fun createBrandCommand() = RegisterBrandCommand(
        name = "테스트브랜드",
        description = "테스트 설명",
        logoUrl = null,
    )

    private fun createProductCommand(brandId: Long) = RegisterProductCommand(
        brandId = brandId,
        name = "테스트상품",
        description = "상품 설명",
        price = 10000L,
        stock = 100,
        thumbnailUrl = null,
        images = listOf(ProductImageCommand(imageUrl = "https://example.com/img.png", displayOrder = 1)),
    )
}
