package com.loopers.domain.product

import com.loopers.domain.brand.BrandCommand
import com.loopers.domain.brand.BrandService
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ProductServiceIntegrationTest @Autowired constructor(
    private val productService: ProductService,
    private val brandService: BrandService,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private var brandId: Long = 0

    @BeforeEach
    fun setUp() {
        val brand = brandService.createBrand(
            BrandCommand.Create(name = "루퍼스", description = "테스트 브랜드", imageUrl = "https://example.com/brand.jpg"),
        )
        brandId = brand.id
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createCommand(
        name: String = "감성 티셔츠",
        description: String = "좋은 상품입니다.",
        price: Long = 39000,
        stockQuantity: Int = 100,
        imageUrl: String = "https://example.com/product.jpg",
    ) = ProductCommand.Create(
        brandId = brandId,
        name = name,
        description = description,
        price = price,
        stockQuantity = stockQuantity,
        imageUrl = imageUrl,
    )

    @DisplayName("상품을 등록할 때,")
    @Nested
    inner class CreateProduct {
        @DisplayName("유효한 정보가 주어지면, 정상적으로 등록된다.")
        @Test
        fun createsProduct_whenValidInfoIsProvided() {
            val product = productService.createProduct(createCommand())
            val saved = productJpaRepository.findById(product.id).orElse(null)
            assertAll(
                { assertThat(saved).isNotNull() },
                { assertThat(saved!!.name).isEqualTo("감성 티셔츠") },
                { assertThat(saved!!.price).isEqualTo(39000L) },
                { assertThat(saved!!.stockQuantity).isEqualTo(100) },
                { assertThat(saved!!.status).isEqualTo(ProductStatus.ACTIVE) },
            )
        }
    }

    @DisplayName("고객 상품을 조회할 때,")
    @Nested
    inner class GetProduct {
        @DisplayName("ACTIVE 상품을 조회하면, 상품을 반환한다.")
        @Test
        fun returnsProduct_whenActive() {
            val created = productService.createProduct(createCommand())
            val result = productService.getProduct(created.id)
            assertThat(result.name).isEqualTo("감성 티셔츠")
        }

        @DisplayName("삭제된 상품을 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenDeleted() {
            val created = productService.createProduct(createCommand())
            productService.deleteProduct(created.id)
            val result = assertThrows<CoreException> { productService.getProduct(created.id) }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("재고를 차감할 때,")
    @Nested
    inner class DeductStock {
        @DisplayName("충분한 재고가 있으면, 정상적으로 차감된다.")
        @Test
        fun deductsStock_whenSufficient() {
            val created = productService.createProduct(createCommand(stockQuantity = 10))
            productService.deductStock(created.id, 3)
            val updated = productJpaRepository.findById(created.id).orElse(null)
            assertThat(updated!!.stockQuantity).isEqualTo(7)
        }

        @DisplayName("재고가 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenInsufficient() {
            val created = productService.createProduct(createCommand(stockQuantity = 2))
            val result = assertThrows<CoreException> { productService.deductStock(created.id, 5) }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("상품을 삭제할 때,")
    @Nested
    inner class DeleteProduct {
        @DisplayName("존재하는 상품을 삭제하면, 상태가 DELETED로 변경된다.")
        @Test
        fun deletesProduct_whenExists() {
            val created = productService.createProduct(createCommand())
            productService.deleteProduct(created.id)
            val deleted = productJpaRepository.findById(created.id).orElse(null)
            assertAll(
                { assertThat(deleted!!.status).isEqualTo(ProductStatus.DELETED) },
                { assertThat(deleted!!.deletedAt).isNotNull() },
            )
        }
    }

    @DisplayName("브랜드별 상품을 일괄 삭제할 때,")
    @Nested
    inner class DeleteProductsByBrandId {
        @DisplayName("브랜드의 모든 상품이 삭제된다.")
        @Test
        fun deletesAllProductsOfBrand() {
            productService.createProduct(createCommand(name = "상품1"))
            productService.createProduct(createCommand(name = "상품2"))
            productService.deleteProductsByBrandId(brandId)
            val all = productJpaRepository.findAll()
            assertThat(all).allMatch { it.status == ProductStatus.DELETED }
        }
    }
}
