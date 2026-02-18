package com.loopers.application.product

import com.loopers.application.brand.DeleteBrandUseCase
import com.loopers.application.brand.RegisterBrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.domain.brand.BrandException
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
class RegisterProductUseCaseIntegrationTest {

    @Autowired
    private lateinit var registerProductUseCase: RegisterProductUseCase

    @Autowired
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @Autowired
    private lateinit var deleteBrandUseCase: DeleteBrandUseCase

    private var brandId: Long = 0

    @BeforeEach
    fun setUp() {
        brandId = registerBrandUseCase.register(
            RegisterBrandCommand(name = BRAND_NAME, description = null, logoUrl = null),
        )
    }

    @Test
    fun `정상 요청의 경우 상품이 등록되고 ID를 반환해야 한다`() {
        val command = createCommand(brandId)

        val result = registerProductUseCase.register(command)

        assertThat(result).isPositive()
    }

    @Test
    fun `존재하지 않는 브랜드로 상품을 등록하면 NOT_FOUND 예외가 발생한다`() {
        val command = createCommand(9999L)

        assertThatThrownBy { registerProductUseCase.register(command) }
            .isInstanceOfSatisfying(CoreException::class.java) { ex ->
                assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
            }
    }

    @Test
    fun `삭제된 브랜드로 상품을 등록하면 BrandException이 발생한다`() {
        deleteBrandUseCase.delete(brandId)
        val command = createCommand(brandId)

        assertThatThrownBy { registerProductUseCase.register(command) }
            .isInstanceOf(BrandException::class.java)
    }

    private fun createCommand(brandId: Long) = RegisterProductCommand(
        brandId = brandId,
        name = PRODUCT_NAME,
        description = "상품 설명",
        price = 10000L,
        stock = 100,
        thumbnailUrl = null,
        images = listOf(ProductImageCommand(imageUrl = "https://example.com/img.png", displayOrder = 1)),
    )

    companion object {
        private const val BRAND_NAME = "테스트브랜드"
        private const val PRODUCT_NAME = "테스트상품"
    }
}
