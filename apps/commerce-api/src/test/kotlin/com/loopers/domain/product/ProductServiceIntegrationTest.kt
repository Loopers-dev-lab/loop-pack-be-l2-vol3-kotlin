package com.loopers.domain.product

import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest

@SpringBootTest
class ProductServiceIntegrationTest @Autowired constructor(
    private val productService: ProductService,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("상품을 조회할 때, ")
    @Nested
    inner class GetProduct {
        @DisplayName("존재하는 상품 ID를 주면, 상품 정보를 반환한다.")
        @Test
        fun returnsProduct_whenProductExists() {
            // arrange
            val product = productJpaRepository.save(
                Product(brandId = 1L, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100),
            )

            // act
            val result = productService.getProduct(product.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(product.id) },
                { assertThat(result.name).isEqualTo("에어맥스") },
                { assertThat(result.price).isEqualTo(139000L) },
            )
        }

        @DisplayName("존재하지 않는 상품 ID를 주면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotExists() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                productService.getProduct(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("삭제된 상품 ID를 주면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductIsDeleted() {
            // arrange
            val product = productJpaRepository.save(
                Product(brandId = 1L, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100),
            )
            product.delete()
            productJpaRepository.save(product)

            // act
            val exception = assertThrows<CoreException> {
                productService.getProduct(product.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("비관적 락으로 상품을 조회할 때, ")
    @Nested
    inner class GetProductWithLock {
        @DisplayName("존재하는 상품 ID를 주면, 상품 정보를 반환한다.")
        @Test
        fun returnsProduct_whenProductExists() {
            // arrange
            val product = productJpaRepository.save(
                Product(brandId = 1L, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100),
            )

            // act
            val result = productService.getProductWithLock(product.id)

            // assert
            assertThat(result.id).isEqualTo(product.id)
        }

        @DisplayName("존재하지 않는 상품 ID를 주면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotExists() {
            // arrange & act
            val exception = assertThrows<CoreException> {
                productService.getProductWithLock(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품 목록을 조회할 때, ")
    @Nested
    inner class GetProducts {
        @DisplayName("등록된 상품이 있으면, 페이지네이션된 목록을 반환한다.")
        @Test
        fun returnsProductList_whenProductsExist() {
            // arrange
            productJpaRepository.save(
                Product(brandId = 1L, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100),
            )
            productJpaRepository.save(
                Product(brandId = 1L, name = "에어포스", description = "스니커즈", price = 119000, stockQuantity = 50),
            )

            // act
            val result = productService.getProducts(null, ProductSort.LATEST, PageRequest.of(0, 20))

            // assert
            assertThat(result.content).hasSize(2)
        }

        @DisplayName("브랜드 ID로 필터링하면, 해당 브랜드의 상품만 반환한다.")
        @Test
        fun returnsFilteredList_whenBrandIdIsProvided() {
            // arrange
            productJpaRepository.save(
                Product(brandId = 1L, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100),
            )
            productJpaRepository.save(
                Product(brandId = 2L, name = "울트라부스트", description = "운동화", price = 189000, stockQuantity = 30),
            )

            // act
            val result = productService.getProducts(1L, ProductSort.LATEST, PageRequest.of(0, 20))

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content[0].brandId).isEqualTo(1L) },
            )
        }

        @DisplayName("가격 오름차순 정렬을 적용하면, 가격순으로 반환한다.")
        @Test
        fun returnsSortedByPriceAsc_whenSortIsPriceAsc() {
            // arrange
            productJpaRepository.save(
                Product(brandId = 1L, name = "비싼상품", description = "설명", price = 200000, stockQuantity = 10),
            )
            productJpaRepository.save(
                Product(brandId = 1L, name = "싼상품", description = "설명", price = 50000, stockQuantity = 10),
            )

            // act
            val result = productService.getProducts(null, ProductSort.PRICE_ASC, PageRequest.of(0, 20))

            // assert
            assertAll(
                { assertThat(result.content[0].price).isEqualTo(50000L) },
                { assertThat(result.content[1].price).isEqualTo(200000L) },
            )
        }

        @DisplayName("등록된 상품이 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyList_whenNoProductsExist() {
            // act
            val result = productService.getProducts(null, ProductSort.LATEST, PageRequest.of(0, 20))

            // assert
            assertThat(result.content).isEmpty()
        }
    }

    @DisplayName("상품을 생성할 때, ")
    @Nested
    inner class CreateProduct {
        @DisplayName("유효한 정보가 주어지면, 상품이 생성된다.")
        @Test
        fun createsProduct_whenValidInfoIsProvided() {
            // arrange & act
            val result = productService.createProduct(
                brandId = 1L,
                name = "에어맥스",
                description = "운동화",
                price = 139000,
                stockQuantity = 100,
            )

            // assert
            val savedProduct = productJpaRepository.findById(result.id).get()
            assertAll(
                { assertThat(savedProduct.brandId).isEqualTo(1L) },
                { assertThat(savedProduct.name).isEqualTo("에어맥스") },
                { assertThat(savedProduct.description).isEqualTo("운동화") },
                { assertThat(savedProduct.price).isEqualTo(139000L) },
                { assertThat(savedProduct.stockQuantity).isEqualTo(100) },
                { assertThat(savedProduct.likeCount).isEqualTo(0) },
            )
        }
    }

    @DisplayName("상품을 수정할 때, ")
    @Nested
    inner class UpdateProduct {
        @DisplayName("존재하는 상품 ID와 새 정보가 주어지면, 수정된다.")
        @Test
        fun updatesProduct_whenProductExists() {
            // arrange
            val product = productJpaRepository.save(
                Product(brandId = 1L, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100),
            )

            // act
            val result = productService.updateProduct(product.id, "에어맥스 90", "클래식 운동화", 149000, 50)

            // assert
            assertAll(
                { assertThat(result.name).isEqualTo("에어맥스 90") },
                { assertThat(result.description).isEqualTo("클래식 운동화") },
                { assertThat(result.price).isEqualTo(149000L) },
                { assertThat(result.stockQuantity).isEqualTo(50) },
            )
        }

        @DisplayName("존재하지 않는 상품 ID를 주면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotExists() {
            // act
            val exception = assertThrows<CoreException> {
                productService.updateProduct(999L, "에어맥스", "운동화", 139000, 100)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품을 삭제할 때, ")
    @Nested
    inner class DeleteProduct {
        @DisplayName("존재하는 상품 ID를 주면, soft delete 된다.")
        @Test
        fun softDeletesProduct_whenProductExists() {
            // arrange
            val product = productJpaRepository.save(
                Product(brandId = 1L, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100),
            )

            // act
            productService.deleteProduct(product.id)

            // assert
            val deletedProduct = productJpaRepository.findById(product.id).get()
            assertThat(deletedProduct.deletedAt).isNotNull()
        }

        @DisplayName("존재하지 않는 상품 ID를 주면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotExists() {
            // act
            val exception = assertThrows<CoreException> {
                productService.deleteProduct(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("좋아요 수를 증가시킬 때, ")
    @Nested
    inner class IncreaseLikeCount {
        @DisplayName("상품의 좋아요 수가 1 증가한다.")
        @Test
        fun increasesLikeCount() {
            // arrange
            val product = productJpaRepository.save(
                Product(brandId = 1L, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100),
            )

            // act
            productService.increaseLikeCount(product.id)

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertThat(updatedProduct.likeCount).isEqualTo(1)
        }
    }

    @DisplayName("좋아요 수를 감소시킬 때, ")
    @Nested
    inner class DecreaseLikeCount {
        @DisplayName("좋아요 수가 1 이상이면, 1 감소한다.")
        @Test
        fun decreasesLikeCount_whenLikeCountIsPositive() {
            // arrange
            val product = Product(brandId = 1L, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100)
            product.increaseLikeCount()
            productJpaRepository.save(product)

            // act
            productService.decreaseLikeCount(product.id)

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertThat(updatedProduct.likeCount).isEqualTo(0)
        }

        @DisplayName("좋아요 수가 0이면, 0을 유지한다.")
        @Test
        fun keepsZero_whenLikeCountIsZero() {
            // arrange
            val product = productJpaRepository.save(
                Product(brandId = 1L, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100),
            )

            // act
            productService.decreaseLikeCount(product.id)

            // assert
            val updatedProduct = productJpaRepository.findById(product.id).get()
            assertThat(updatedProduct.likeCount).isEqualTo(0)
        }
    }

    @DisplayName("브랜드 ID로 상품을 일괄 삭제할 때, ")
    @Nested
    inner class SoftDeleteByBrandId {
        @DisplayName("해당 브랜드의 모든 상품이 soft delete 된다.")
        @Test
        fun softDeletesAllProductsByBrandId() {
            // arrange
            val product1 = productJpaRepository.save(
                Product(brandId = 1L, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100),
            )
            val product2 = productJpaRepository.save(
                Product(brandId = 1L, name = "에어포스", description = "스니커즈", price = 119000, stockQuantity = 50),
            )
            val otherBrandProduct = productJpaRepository.save(
                Product(brandId = 2L, name = "울트라부스트", description = "운동화", price = 189000, stockQuantity = 30),
            )

            // act
            productService.softDeleteByBrandId(1L)

            // assert
            assertAll(
                { assertThat(productJpaRepository.findById(product1.id).get().deletedAt).isNotNull() },
                { assertThat(productJpaRepository.findById(product2.id).get().deletedAt).isNotNull() },
                { assertThat(productJpaRepository.findById(otherBrandProduct.id).get().deletedAt).isNull() },
            )
        }
    }
}
