package com.loopers.application.product

import com.loopers.application.brand.RegisterBrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
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
@Sql(
    statements = ["DELETE FROM product_image", "DELETE FROM product", "DELETE FROM brand"],
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class DeleteProductUseCaseIntegrationTest {

    @Autowired
    private lateinit var deleteProductUseCase: DeleteProductUseCase

    @Autowired
    private lateinit var registerProductUseCase: RegisterProductUseCase

    @Autowired
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @Autowired
    private lateinit var getProductUseCase: GetProductUseCase

    private var brandId: Long = 0

    @BeforeEach
    fun setUp() {
        brandId = registerBrandUseCase.register(
            RegisterBrandCommand(name = "테스트브랜드", description = null, logoUrl = null),
        )
    }

    @Test
    fun `정상적인 경우 상품이 삭제되어야 한다`() {
        val productId = registerProductUseCase.register(createCommand())

        deleteProductUseCase.delete(productId)

        val result = getProductUseCase.getById(productId)
        assertThat(result.deletedAt).isNotNull()
    }

    @Test
    fun `존재하지 않는 상품을 삭제하면 NOT_FOUND 예외가 발생한다`() {
        assertThatThrownBy { deleteProductUseCase.delete(9999L) }
            .isInstanceOfSatisfying(CoreException::class.java) { ex ->
                assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
            }
    }

    @Test
    fun `이미 삭제된 상품을 다시 삭제해도 멱등하게 성공한다`() {
        val productId = registerProductUseCase.register(createCommand())
        deleteProductUseCase.delete(productId)

        deleteProductUseCase.delete(productId)

        val result = getProductUseCase.getById(productId)
        assertThat(result.deletedAt).isNotNull()
    }

    private fun createCommand() = RegisterProductCommand(
        brandId = brandId,
        name = "테스트상품",
        description = "상품 설명",
        price = 10000L,
        stock = 100,
        thumbnailUrl = null,
        images = emptyList(),
    )
}
