package com.loopers.domain.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
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
import org.springframework.data.domain.Sort

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
                Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = 10, stockQuantity = 100, brandId = brand.id),
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
                Product(name = "단종상품", description = "단종", price = Money.of(99000L), likes = 5, stockQuantity = 0, brandId = brand.id),
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
            productRepository.save(Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = 10, stockQuantity = 100, brandId = nike.id))
            productRepository.save(Product(name = "울트라부스트", description = "러닝화", price = Money.of(199000L), likes = 30, stockQuantity = 80, brandId = adidas.id))

            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

            // act
            val result = productService.getProducts(nike.id, pageable)

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
            productRepository.save(Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = 10, stockQuantity = 100, brandId = brand.id))
            productRepository.save(Product(name = "에어포스", description = "캐주얼화", price = Money.of(139000L), likes = 20, stockQuantity = 50, brandId = brand.id))

            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

            // act
            val result = productService.getProducts(null, pageable)

            // assert
            assertThat(result.totalElements).isEqualTo(2)
        }

        @DisplayName("삭제된 상품은 목록에서 제외된다.")
        @Test
        fun excludesDeletedProducts() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            productRepository.save(Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = 10, stockQuantity = 100, brandId = brand.id))
            val deleted = productRepository.save(Product(name = "단종상품", description = "단종", price = Money.of(99000L), likes = 5, stockQuantity = 0, brandId = brand.id))
            deleted.delete()
            productRepository.save(deleted)

            val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))

            // act
            val result = productService.getProducts(null, pageable)

            // assert
            assertAll(
                { assertThat(result.totalElements).isEqualTo(1) },
                { assertThat(result.content.first().name).isEqualTo("에어맥스") },
            )
        }
    }
}
