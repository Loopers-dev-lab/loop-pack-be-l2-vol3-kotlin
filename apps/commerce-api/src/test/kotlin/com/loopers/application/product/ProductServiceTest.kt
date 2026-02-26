package com.loopers.application.product

import com.loopers.domain.product.ProductCommand
import com.loopers.domain.product.ProductStatus
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class ProductServiceTest {
    private lateinit var productService: ProductService
    private lateinit var fakeRepository: FakeProductRepository

    @BeforeEach
    fun setUp() {
        fakeRepository = FakeProductRepository()
        productService = ProductService(fakeRepository)
    }

    private fun createCommand(
        brandId: Long = 1L,
        name: String = "감성 티셔츠",
        description: String = "좋은 상품",
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
            // act
            val product = productService.createProduct(createCommand())

            // assert
            assertAll(
                { assertThat(product.name).isEqualTo("감성 티셔츠") },
                { assertThat(product.price).isEqualTo(39000) },
                { assertThat(product.stockQuantity).isEqualTo(100) },
                { assertThat(product.status).isEqualTo(ProductStatus.ACTIVE) },
            )
        }
    }

    @DisplayName("어드민 상품을 조회할 때,")
    @Nested
    inner class GetProductForAdmin {
        @DisplayName("존재하는 ID로 조회하면, 상품을 반환한다.")
        @Test
        fun returnsProduct_whenIdExists() {
            // arrange
            val created = productService.createProduct(createCommand())

            // act
            val result = productService.getProductForAdmin(created.id)

            // assert
            assertThat(result.name).isEqualTo("감성 티셔츠")
        }

        @DisplayName("삭제된 상품도 조회된다.")
        @Test
        fun returnsDeletedProduct_whenIdExists() {
            // arrange
            val created = productService.createProduct(createCommand())
            productService.deleteProduct(created.id)

            // act
            val result = productService.getProductForAdmin(created.id)

            // assert
            assertThat(result.status).isEqualTo(ProductStatus.DELETED)
        }

        @DisplayName("존재하지 않는 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenIdDoesNotExist() {
            // act & assert
            val result = assertThrows<CoreException> {
                productService.getProductForAdmin(999L)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("고객 상품을 조회할 때,")
    @Nested
    inner class GetProduct {
        @DisplayName("ACTIVE 상품을 조회하면, 상품을 반환한다.")
        @Test
        fun returnsProduct_whenProductIsActive() {
            // arrange
            val created = productService.createProduct(createCommand())

            // act
            val result = productService.getProduct(created.id)

            // assert
            assertThat(result.name).isEqualTo("감성 티셔츠")
        }

        @DisplayName("삭제된 상품을 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductIsDeleted() {
            // arrange
            val created = productService.createProduct(createCommand())
            productService.deleteProduct(created.id)

            // act & assert
            val result = assertThrows<CoreException> {
                productService.getProduct(created.id)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품을 수정할 때,")
    @Nested
    inner class UpdateProduct {
        @DisplayName("유효한 정보가 주어지면, 정상적으로 수정된다.")
        @Test
        fun updatesProduct_whenValidInfoIsProvided() {
            // arrange
            val created = productService.createProduct(createCommand())
            val updateCommand = ProductCommand.Update(
                name = "새 상품",
                description = "새 설명",
                price = 45000,
                stockQuantity = 50,
                imageUrl = "https://example.com/new.jpg",
            )

            // act
            productService.updateProduct(created.id, updateCommand)

            // assert
            val updated = productService.getProductForAdmin(created.id)
            assertAll(
                { assertThat(updated.name).isEqualTo("새 상품") },
                { assertThat(updated.price).isEqualTo(45000) },
                { assertThat(updated.stockQuantity).isEqualTo(50) },
            )
        }
    }

    @DisplayName("상품을 삭제할 때,")
    @Nested
    inner class DeleteProduct {
        @DisplayName("존재하는 상품을 삭제하면, 상태가 DELETED로 변경된다.")
        @Test
        fun deletesProduct_whenProductExists() {
            // arrange
            val created = productService.createProduct(createCommand())

            // act
            productService.deleteProduct(created.id)

            // assert
            val deleted = productService.getProductForAdmin(created.id)
            assertAll(
                { assertThat(deleted.status).isEqualTo(ProductStatus.DELETED) },
                { assertThat(deleted.deletedAt).isNotNull() },
            )
        }
    }

    @DisplayName("브랜드별 상품을 일괄 삭제할 때,")
    @Nested
    inner class DeleteProductsByBrandId {
        @DisplayName("해당 브랜드의 ACTIVE 상품이 모두 삭제된다.")
        @Test
        fun deletesAllActiveProducts_whenBrandIdMatches() {
            // arrange
            productService.createProduct(createCommand(brandId = 1L, name = "상품1"))
            productService.createProduct(createCommand(brandId = 1L, name = "상품2"))
            productService.createProduct(createCommand(brandId = 2L, name = "다른 브랜드 상품"))

            // act
            productService.deleteProductsByBrandId(1L)

            // assert
            val brand1Products = fakeRepository.findAllByBrandIdAndStatus(1L, ProductStatus.ACTIVE)
            val brand2Products = fakeRepository.findAllByBrandIdAndStatus(2L, ProductStatus.ACTIVE)
            assertAll(
                { assertThat(brand1Products).isEmpty() },
                { assertThat(brand2Products).hasSize(1) },
            )
        }
    }

    @DisplayName("재고를 차감할 때,")
    @Nested
    inner class DeductStock {
        @DisplayName("유효한 수량이면, 재고가 차감된다.")
        @Test
        fun deductsStock_whenQuantityIsValid() {
            // arrange
            val created = productService.createProduct(createCommand(stockQuantity = 100))

            // act
            productService.deductStock(created.id, 10)

            // assert
            val product = productService.getProductForAdmin(created.id)
            assertThat(product.stockQuantity).isEqualTo(90)
        }

        @DisplayName("재고가 부족하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenStockInsufficient() {
            // arrange
            val created = productService.createProduct(createCommand(stockQuantity = 5))

            // act & assert
            val result = assertThrows<CoreException> {
                productService.deductStock(created.id, 10)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("존재하지 않는 상품이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductDoesNotExist() {
            // act & assert
            val result = assertThrows<CoreException> {
                productService.deductStock(999L, 1)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품 ID 목록으로 조회할 때,")
    @Nested
    inner class GetProductsByIds {
        @DisplayName("ACTIVE 상품만 반환한다.")
        @Test
        fun returnsOnlyActiveProducts() {
            // arrange
            val p1 = productService.createProduct(createCommand(name = "상품1"))
            val p2 = productService.createProduct(createCommand(name = "상품2"))
            productService.deleteProduct(p2.id)

            // act
            val result = productService.getProductsByIds(listOf(p1.id, p2.id))

            // assert
            assertAll(
                { assertThat(result).hasSize(1) },
                { assertThat(result[0].name).isEqualTo("상품1") },
            )
        }
    }
}
