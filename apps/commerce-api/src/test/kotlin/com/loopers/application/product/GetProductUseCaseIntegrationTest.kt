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
class GetProductUseCaseIntegrationTest {

    @Autowired
    private lateinit var getProductUseCase: GetProductUseCase

    @Autowired
    private lateinit var registerProductUseCase: RegisterProductUseCase

    @Autowired
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @Autowired
    private lateinit var deleteProductUseCase: DeleteProductUseCase

    private var brandId: Long = 0

    @BeforeEach
    fun setUp() {
        brandId = registerBrandUseCase.register(
            RegisterBrandCommand(name = "테스트브랜드", description = null, logoUrl = null),
        )
    }

    @Test
    fun `존재하는 상품을 정상 조회할 수 있다`() {
        val productId = registerProductUseCase.register(createCommand())

        val result = getProductUseCase.getById(productId)

        assertThat(result.id).isEqualTo(productId)
        assertThat(result.name).isEqualTo(PRODUCT_NAME)
    }

    @Test
    fun `존재하지 않는 상품을 조회하면 NOT_FOUND 예외가 발생한다`() {
        assertThatThrownBy { getProductUseCase.getById(9999L) }
            .isInstanceOfSatisfying(CoreException::class.java) { ex ->
                assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
            }
    }

    @Test
    fun `삭제된 상품을 getActiveById로 조회하면 NOT_FOUND 예외가 발생한다`() {
        val productId = registerProductUseCase.register(createCommand())
        deleteProductUseCase.delete(productId)

        assertThatThrownBy { getProductUseCase.getActiveById(productId) }
            .isInstanceOfSatisfying(CoreException::class.java) { ex ->
                assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
            }
    }

    @Test
    fun `삭제된 상품을 getById로 조회하면 정상적으로 반환된다`() {
        val productId = registerProductUseCase.register(createCommand())
        deleteProductUseCase.delete(productId)

        val result = getProductUseCase.getById(productId)

        assertThat(result.id).isEqualTo(productId)
        assertThat(result.deletedAt).isNotNull()
    }

    private fun createCommand() = RegisterProductCommand(
        brandId = brandId,
        name = PRODUCT_NAME,
        description = "상품 설명",
        price = 10000L,
        stock = 100,
        thumbnailUrl = null,
        images = emptyList(),
    )

    companion object {
        private const val PRODUCT_NAME = "테스트상품"
    }
}
