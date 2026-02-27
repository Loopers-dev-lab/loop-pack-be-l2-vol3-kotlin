package com.loopers.application.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.support.common.PageQuery
import com.loopers.support.common.SortOrder
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
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

@SpringBootTest
class AdminProductFacadeIntegrationTest @Autowired constructor(
    private val adminProductFacade: AdminProductFacade,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("어드민 상품 목록 조회할 때,")
    @Nested
    inner class GetProducts {

        private val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)

        @DisplayName("상품 목록을 조회하면, 브랜드명이 포함된 결과를 반환한다.")
        @Test
        fun returnsProductsWithBrandName() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            productRepository.save(
                Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = LikeCount.of(10), stockQuantity = StockQuantity.of(100), brandId = brand.id),
            )

            // act
            val result = adminProductFacade.getProducts(null, pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content.first().name).isEqualTo("에어맥스") },
                { assertThat(result.content.first().brandName).isEqualTo("나이키") },
                { assertThat(result.content.first().brandId).isEqualTo(brand.id) },
                { assertThat(result.content.first().createdAt).isNotNull() },
                { assertThat(result.content.first().updatedAt).isNotNull() },
            )
        }

        @DisplayName("brandId를 지정하면, 해당 브랜드의 상품만 반환한다.")
        @Test
        fun returnsFilteredProducts_whenBrandIdProvided() {
            // arrange
            val nike = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val adidas = brandRepository.save(Brand(name = "아디다스", description = "스포츠 브랜드"))
            productRepository.save(Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = LikeCount.of(10), stockQuantity = StockQuantity.of(100), brandId = nike.id))
            productRepository.save(Product(name = "울트라부스트", description = "러닝화", price = Money.of(199000L), likes = LikeCount.of(30), stockQuantity = StockQuantity.of(80), brandId = adidas.id))

            // act
            val result = adminProductFacade.getProducts(nike.id, pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content.first().brandName).isEqualTo("나이키") },
                { assertThat(result.totalElements).isEqualTo(1) },
            )
        }

        @DisplayName("삭제된 상품은 목록에서 제외된다.")
        @Test
        fun excludesDeletedProducts() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            productRepository.save(Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = LikeCount.of(10), stockQuantity = StockQuantity.of(100), brandId = brand.id))
            val deleted = productRepository.save(Product(name = "단종상품", description = "단종", price = Money.of(99000L), likes = LikeCount.of(5), stockQuantity = StockQuantity.of(0), brandId = brand.id))
            deleted.delete()
            productRepository.save(deleted)

            // act
            val result = adminProductFacade.getProducts(null, pageQuery)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content.first().name).isEqualTo("에어맥스") },
                { assertThat(result.totalElements).isEqualTo(1) },
            )
        }

        @DisplayName("페이징이 정상적으로 동작한다.")
        @Test
        fun returnsPaginatedProducts() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            repeat(3) { i ->
                productRepository.save(Product(name = "상품$i", description = "설명", price = Money.of(10000L), likes = LikeCount.of(0), stockQuantity = StockQuantity.of(10), brandId = brand.id))
            }

            val smallPageQuery = PageQuery(0, 2, SortOrder.UNSORTED)

            // act
            val result = adminProductFacade.getProducts(null, smallPageQuery)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.totalElements).isEqualTo(3) },
                { assertThat(result.totalPages).isEqualTo(2) },
            )
        }
    }

    @DisplayName("어드민 상품 수정할 때,")
    @Nested
    inner class UpdateProduct {

        @DisplayName("DB에 저장된 상품을 수정하면, 수정된 정보를 반환한다.")
        @Test
        fun returnsUpdatedInfo_whenProductExistsInDb() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val saved = productRepository.save(
                Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = LikeCount.of(10), stockQuantity = StockQuantity.of(100), brandId = brand.id),
            )

            // act
            val result = adminProductFacade.updateProduct(
                productId = saved.id,
                name = "수정된 상품",
                description = "수정된 설명",
                price = 200000L,
                stockQuantity = 50,
                brandId = brand.id,
            )

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(saved.id) },
                { assertThat(result.name).isEqualTo("수정된 상품") },
                { assertThat(result.description).isEqualTo("수정된 설명") },
                { assertThat(result.price).isEqualTo(200000L) },
                { assertThat(result.stockQuantity).isEqualTo(50) },
                { assertThat(result.brandName).isEqualTo("나이키") },
            )
        }

        @DisplayName("존재하지 않는 productId로 수정하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotExistsInDb() {
            // act & assert
            val exception = assertThrows<CoreException> {
                adminProductFacade.updateProduct(
                    productId = 9999L,
                    name = "수정",
                    description = null,
                    price = 100000L,
                    stockQuantity = 10,
                    brandId = 1L,
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("삭제된 상품을 수정하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductIsDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val saved = productRepository.save(
                Product(name = "단종상품", description = "단종", price = Money.of(99000L), likes = LikeCount.of(5), stockQuantity = StockQuantity.of(0), brandId = brand.id),
            )
            saved.delete()
            productRepository.save(saved)

            // act & assert
            val exception = assertThrows<CoreException> {
                adminProductFacade.updateProduct(
                    productId = saved.id,
                    name = "수정",
                    description = null,
                    price = 100000L,
                    stockQuantity = 10,
                    brandId = brand.id,
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("brandId가 변경된 요청이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenBrandIdChanged() {
            // arrange
            val nike = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val adidas = brandRepository.save(Brand(name = "아디다스", description = "스포츠 브랜드"))
            val saved = productRepository.save(
                Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = LikeCount.of(10), stockQuantity = StockQuantity.of(100), brandId = nike.id),
            )

            // act & assert
            val exception = assertThrows<CoreException> {
                adminProductFacade.updateProduct(
                    productId = saved.id,
                    name = "에어맥스",
                    description = "러닝화",
                    price = 159000L,
                    stockQuantity = 100,
                    brandId = adidas.id,
                )
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("어드민 상품 삭제할 때,")
    @Nested
    inner class DeleteProduct {

        @DisplayName("DB에 저장된 상품을 삭제하면, 정상적으로 삭제된다.")
        @Test
        fun deletesProduct_whenProductExistsInDb() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val saved = productRepository.save(
                Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = LikeCount.of(10), stockQuantity = StockQuantity.of(100), brandId = brand.id),
            )

            // act
            adminProductFacade.deleteProduct(saved.id)

            // assert
            val exception = assertThrows<CoreException> {
                adminProductFacade.getProductDetail(saved.id)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("존재하지 않는 productId로 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotExistsInDb() {
            // act & assert
            val exception = assertThrows<CoreException> {
                adminProductFacade.deleteProduct(9999L)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("이미 삭제된 상품을 삭제하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductAlreadyDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val saved = productRepository.save(
                Product(name = "단종상품", description = "단종", price = Money.of(99000L), likes = LikeCount.of(5), stockQuantity = StockQuantity.of(0), brandId = brand.id),
            )
            saved.delete()
            productRepository.save(saved)

            // act & assert
            val exception = assertThrows<CoreException> {
                adminProductFacade.deleteProduct(saved.id)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("어드민 상품 상세 조회할 때,")
    @Nested
    inner class GetProductDetail {

        @DisplayName("DB에 저장된 상품을 조회하면, 브랜드명이 포함된 AdminProductInfo를 반환한다.")
        @Test
        fun returnsAdminProductInfo_whenProductExistsInDb() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val saved = productRepository.save(
                Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = LikeCount.of(10), stockQuantity = StockQuantity.of(100), brandId = brand.id),
            )

            // act
            val result = adminProductFacade.getProductDetail(saved.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(saved.id) },
                { assertThat(result.name).isEqualTo("에어맥스") },
                { assertThat(result.description).isEqualTo("러닝화") },
                { assertThat(result.price).isEqualTo(159000L) },
                { assertThat(result.brandId).isEqualTo(brand.id) },
                { assertThat(result.brandName).isEqualTo("나이키") },
                { assertThat(result.stockQuantity).isEqualTo(100) },
                { assertThat(result.likeCount).isEqualTo(10) },
                { assertThat(result.createdAt).isNotNull() },
                { assertThat(result.updatedAt).isNotNull() },
            )
        }

        @DisplayName("존재하지 않는 productId로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotExistsInDb() {
            // act & assert
            val exception = assertThrows<CoreException> {
                adminProductFacade.getProductDetail(9999L)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("삭제된 상품을 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductIsDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val saved = productRepository.save(
                Product(name = "단종상품", description = "단종", price = Money.of(99000L), likes = LikeCount.of(5), stockQuantity = StockQuantity.of(0), brandId = brand.id),
            )
            saved.delete()
            productRepository.save(saved)

            // act & assert
            val exception = assertThrows<CoreException> {
                adminProductFacade.getProductDetail(saved.id)
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
