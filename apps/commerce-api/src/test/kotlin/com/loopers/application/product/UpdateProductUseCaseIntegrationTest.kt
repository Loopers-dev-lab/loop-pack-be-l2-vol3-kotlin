package com.loopers.application.product

import com.loopers.application.brand.RegisterBrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.domain.product.ProductException
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
class UpdateProductUseCaseIntegrationTest {

    @Autowired
    private lateinit var updateProductUseCase: UpdateProductUseCase

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
    fun `정상적인 경우 상품이 수정되어야 한다`() {
        val productId = registerProductUseCase.register(createRegisterCommand())
        val updateCommand = UpdateProductCommand(
            name = "수정된상품",
            description = "수정된 설명",
            price = 20000L,
            stock = 50,
            thumbnailUrl = null,
            status = "ACTIVE",
            images = emptyList(),
        )

        val result = updateProductUseCase.update(productId, updateCommand)

        assertThat(result.name).isEqualTo("수정된상품")
        assertThat(result.price).isEqualTo(20000L)
        assertThat(result.stock).isEqualTo(50)
    }

    @Test
    fun `존재하지 않는 상품을 수정하면 NOT_FOUND 예외가 발생한다`() {
        val updateCommand = UpdateProductCommand(
            name = "수정된상품",
            description = null,
            price = 10000L,
            stock = 100,
            thumbnailUrl = null,
            status = "ACTIVE",
            images = emptyList(),
        )

        assertThatThrownBy { updateProductUseCase.update(9999L, updateCommand) }
            .isInstanceOfSatisfying(CoreException::class.java) { ex ->
                assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
            }
    }

    @Test
    fun `삭제된 상품을 수정하면 ProductException이 발생한다`() {
        val productId = registerProductUseCase.register(createRegisterCommand())
        deleteProductUseCase.delete(productId)
        val updateCommand = UpdateProductCommand(
            name = "수정된상품",
            description = null,
            price = 10000L,
            stock = 100,
            thumbnailUrl = null,
            status = "ACTIVE",
            images = emptyList(),
        )

        assertThatThrownBy { updateProductUseCase.update(productId, updateCommand) }
            .isInstanceOf(ProductException::class.java)
    }

    private fun createRegisterCommand() = RegisterProductCommand(
        brandId = brandId,
        name = "테스트상품",
        description = "상품 설명",
        price = 10000L,
        stock = 100,
        thumbnailUrl = null,
        images = emptyList(),
    )
}
