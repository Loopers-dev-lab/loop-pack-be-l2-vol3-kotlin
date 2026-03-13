package com.loopers.domain.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
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
import com.loopers.support.common.PageQuery
import com.loopers.support.common.SortOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ProductServiceIntegrationTest @Autowired constructor(
    private val productService: ProductService,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("상품 단건 조회할 때,")
    @Nested
    inner class GetProduct {

        @DisplayName("DB에 저장된 상품을 조회하면, 상품 정보를 반환한다.")
        @Test
        fun returnsProduct_whenProductExistsInDb() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val saved = productRepository.save(
                Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = LikeCount.of(10), stockQuantity = StockQuantity.of(100), brandId = brand.id),
            )

            // act
            val result = productService.getProduct(saved.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(saved.id) },
                { assertThat(result.name).isEqualTo("에어맥스") },
                { assertThat(result.price).isEqualTo(Money.of(159000L)) },
                { assertThat(result.brandId).isEqualTo(brand.id) },
            )
        }

        @DisplayName("존재하지 않는 productId로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotExistsInDb() {
            // act
            val exception = assertThrows<CoreException> {
                productService.getProduct(9999L)
            }

            // assert
            assertAll(
                { assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND) },
                { assertThat(exception.message).contains("상품을 찾을 수 없습니다") },
            )
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

            // act
            val exception = assertThrows<CoreException> {
                productService.getProduct(saved.id)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품 목록 조회할 때,")
    @Nested
    inner class GetProducts {

        @DisplayName("brandId를 지정하면, 해당 브랜드의 상품만 반환한다.")
        @Test
        fun returnsFilteredProducts_whenBrandIdProvided() {
            // arrange
            val nike = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val adidas = brandRepository.save(Brand(name = "아디다스", description = "스포츠 브랜드"))
            productRepository.save(Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = LikeCount.of(10), stockQuantity = StockQuantity.of(100), brandId = nike.id))
            productRepository.save(Product(name = "울트라부스트", description = "러닝화", price = Money.of(199000L), likes = LikeCount.of(30), stockQuantity = StockQuantity.of(80), brandId = adidas.id))

            val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)

            // act
            val result = productService.getProducts(nike.id, pageQuery)

            // assert
            assertAll(
                { assertThat(result.totalElements).isEqualTo(1) },
                { assertThat(result.content.first().brandId).isEqualTo(nike.id) },
            )
        }

        @DisplayName("brandId를 지정하지 않으면, 전체 상품을 반환한다.")
        @Test
        fun returnsAllProducts_whenBrandIdIsNull() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            productRepository.save(Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = LikeCount.of(10), stockQuantity = StockQuantity.of(100), brandId = brand.id))
            productRepository.save(Product(name = "에어포스", description = "캐주얼화", price = Money.of(139000L), likes = LikeCount.of(20), stockQuantity = StockQuantity.of(50), brandId = brand.id))

            val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)

            // act
            val result = productService.getProducts(null, pageQuery)

            // assert
            assertThat(result.totalElements).isEqualTo(2)
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

            val pageQuery = PageQuery(0, 20, SortOrder.UNSORTED)

            // act
            val result = productService.getProducts(null, pageQuery)

            // assert
            assertAll(
                { assertThat(result.totalElements).isEqualTo(1) },
                { assertThat(result.content.first().name).isEqualTo("에어맥스") },
            )
        }
    }

    @DisplayName("상위 브랜드 조회할 때,")
    @Nested
    inner class GetTopBrandIdsByProductCount {

        @DisplayName("여러 브랜드의 상품이 개수순으로 정렬되어 반환된다.")
        @Test
        fun returnsBrandIdsSortedByProductCount() {
            // arrange
            val brandA = brandRepository.save(Brand(name = "브랜드A", description = "설명"))
            val brandB = brandRepository.save(Brand(name = "브랜드B", description = "설명"))
            val brandC = brandRepository.save(Brand(name = "브랜드C", description = "설명"))

            // brandB: 3개, brandC: 2개, brandA: 1개
            repeat(1) { productRepository.save(createProduct(brandA.id)) }
            repeat(3) { productRepository.save(createProduct(brandB.id)) }
            repeat(2) { productRepository.save(createProduct(brandC.id)) }

            // act
            val result = productService.getTopBrandIdsByProductCount(3)

            // assert
            assertAll(
                { assertThat(result).hasSize(3) },
                { assertThat(result[0]).isEqualTo(brandB.id) },
                { assertThat(result[1]).isEqualTo(brandC.id) },
                { assertThat(result[2]).isEqualTo(brandA.id) },
            )
        }

        @DisplayName("삭제된 상품은 집계에서 제외된다.")
        @Test
        fun excludesDeletedProductsFromCount() {
            // arrange
            val brandA = brandRepository.save(Brand(name = "브랜드A", description = "설명"))
            val brandB = brandRepository.save(Brand(name = "브랜드B", description = "설명"))

            // brandA: 활성 2개, 삭제 3개 → 실제 2개
            repeat(2) { productRepository.save(createProduct(brandA.id)) }
            repeat(3) {
                val product = productRepository.save(createProduct(brandA.id))
                product.delete()
                productRepository.save(product)
            }

            // brandB: 활성 3개 → 실제 3개
            repeat(3) { productRepository.save(createProduct(brandB.id)) }

            // act
            val result = productService.getTopBrandIdsByProductCount(2)

            // assert
            assertAll(
                { assertThat(result).hasSize(2) },
                { assertThat(result[0]).isEqualTo(brandB.id) },
                { assertThat(result[1]).isEqualTo(brandA.id) },
            )
        }

        @DisplayName("limit보다 브랜드가 많으면, 상위 limit개만 반환된다.")
        @Test
        fun returnsExactlyLimitBrands() {
            // arrange
            val brands = (1..5).map {
                brandRepository.save(Brand(name = "브랜드$it", description = "설명"))
            }
            brands.forEach { brand ->
                productRepository.save(createProduct(brand.id))
            }

            // act
            val result = productService.getTopBrandIdsByProductCount(3)

            // assert
            assertThat(result).hasSize(3)
        }
    }

    private fun createProduct(brandId: Long): Product {
        return Product(
            name = "상품",
            description = "설명",
            price = Money.of(10000L),
            likes = LikeCount.of(0),
            stockQuantity = StockQuantity.of(100),
            brandId = brandId,
        )
    }

    @DisplayName("상품 생성할 때,")
    @Nested
    inner class CreateProduct {

        @DisplayName("유효한 상품 정보가 주어지면, DB에 저장되고 조회할 수 있다.")
        @Test
        fun savesProductToDb_whenValidInfo() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))

            // act
            val result = productService.createProduct(
                name = "에어맥스",
                description = "러닝화",
                price = Money.of(159000L),
                stockQuantity = StockQuantity.of(100),
                brandId = brand.id,
            )

            // assert
            val found = productService.getProduct(result.id)
            assertAll(
                { assertThat(found.name).isEqualTo("에어맥스") },
                { assertThat(found.description).isEqualTo("러닝화") },
                { assertThat(found.price).isEqualTo(Money.of(159000L)) },
                { assertThat(found.stockQuantity).isEqualTo(StockQuantity.of(100)) },
                { assertThat(found.brandId).isEqualTo(brand.id) },
                { assertThat(found.likes).isEqualTo(LikeCount.of(0)) },
            )
        }

        @DisplayName("설명이 null이면, 설명 없이 저장된다.")
        @Test
        fun savesProductWithNullDescription() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))

            // act
            val result = productService.createProduct(
                name = "에어맥스",
                description = null,
                price = Money.of(159000L),
                stockQuantity = StockQuantity.of(100),
                brandId = brand.id,
            )

            // assert
            val found = productService.getProduct(result.id)
            assertAll(
                { assertThat(found.name).isEqualTo("에어맥스") },
                { assertThat(found.description).isNull() },
            )
        }
    }
}
