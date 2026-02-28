package com.loopers.application.product

import com.loopers.application.brand.RegisterBrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.domain.product.ProductSortType
import com.loopers.testcontainers.MySqlTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
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
class GetProductListUseCaseIntegrationTest {

    @Autowired
    private lateinit var getProductListUseCase: GetProductListUseCase

    @Autowired
    private lateinit var registerProductUseCase: RegisterProductUseCase

    @Autowired
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @Autowired
    private lateinit var deleteProductUseCase: DeleteProductUseCase

    private var brandId1: Long = 0
    private var brandId2: Long = 0

    @BeforeEach
    fun setUp() {
        brandId1 = registerBrandUseCase.register(
            RegisterBrandCommand(name = "브랜드A", description = null, logoUrl = null),
        )
        brandId2 = registerBrandUseCase.register(
            RegisterBrandCommand(name = "브랜드B", description = null, logoUrl = null),
        )
    }

    @Test
    fun `getAllActive는 삭제된 상품을 제외한 목록을 반환한다`() {
        val productId1 = registerProductUseCase.register(createCommand(brandId1, "상품A"))
        registerProductUseCase.register(createCommand(brandId1, "상품B"))
        deleteProductUseCase.delete(productId1)

        val result = getProductListUseCase.getAllActive(null, ProductSortType.CREATED_AT)

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("상품B")
    }

    @Test
    fun `getAll은 삭제된 상품을 포함하여 전체 목록을 반환한다`() {
        val productId1 = registerProductUseCase.register(createCommand(brandId1, "상품A"))
        registerProductUseCase.register(createCommand(brandId1, "상품B"))
        deleteProductUseCase.delete(productId1)

        val result = getProductListUseCase.getAll(null)

        assertThat(result).hasSize(2)
    }

    @Test
    fun `brandId로 필터링하여 목록을 조회할 수 있다`() {
        registerProductUseCase.register(createCommand(brandId1, "상품A"))
        registerProductUseCase.register(createCommand(brandId2, "상품B"))

        val result = getProductListUseCase.getAllActive(brandId1, ProductSortType.CREATED_AT)

        assertThat(result).hasSize(1)
        assertThat(result[0].brandId).isEqualTo(brandId1)
    }

    private fun createCommand(brandId: Long, name: String) = RegisterProductCommand(
        brandId = brandId,
        name = name,
        description = "설명",
        price = 10000L,
        stock = 100,
        thumbnailUrl = null,
        images = emptyList(),
    )
}
