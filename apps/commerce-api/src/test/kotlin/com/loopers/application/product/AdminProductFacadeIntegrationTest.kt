package com.loopers.application.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.SortOrder
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
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
}
