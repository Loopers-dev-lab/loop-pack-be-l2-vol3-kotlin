package com.loopers.interfaces.api

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.interfaces.api.product.ProductV1Dto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    inner class GetProducts {
        @DisplayName("상품 목록을 페이지네이션하여 조회한다.")
        @Test
        fun returnsProductList() {
            // arrange
            val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            productJpaRepository.save(Product(brandId = brand.id, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100))
            productJpaRepository.save(Product(brandId = brand.id, name = "에어포스", description = "스니커즈", price = 119000, stockQuantity = 50))

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductV1Dto.ProductResponse>>>() {}
            val response = testRestTemplate.exchange("/api/v1/products?page=0&size=20", HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.content).hasSize(2) },
                { assertThat(response.body?.data?.totalElements).isEqualTo(2) },
            )
        }

        @DisplayName("브랜드 ID로 필터링하여 조회한다.")
        @Test
        fun returnsFilteredList_whenBrandIdIsProvided() {
            // arrange
            val brand1 = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val brand2 = brandJpaRepository.save(Brand(name = "아디다스", description = "독일 스포츠 브랜드"))
            productJpaRepository.save(Product(brandId = brand1.id, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100))
            productJpaRepository.save(Product(brandId = brand2.id, name = "울트라부스트", description = "운동화", price = 189000, stockQuantity = 30))

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductV1Dto.ProductResponse>>>() {}
            val response = testRestTemplate.exchange("/api/v1/products?brandId=${brand1.id}", HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.content).hasSize(1) },
                { assertThat(response.body?.data?.content?.get(0)?.brandId).isEqualTo(brand1.id) },
            )
        }
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    inner class GetProduct {
        @DisplayName("존재하는 상품 ID를 주면, 상품 정보를 반환한다.")
        @Test
        fun returnsProductInfo_whenProductExists() {
            // arrange
            val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = productJpaRepository.save(Product(brandId = brand.id, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100))

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>>() {}
            val response = testRestTemplate.exchange("/api/v1/products/${product.id}", HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isEqualTo(product.id) },
                { assertThat(response.body?.data?.name).isEqualTo("에어맥스") },
                { assertThat(response.body?.data?.brandName).isEqualTo("나이키") },
                { assertThat(response.body?.data?.price).isEqualTo(139000L) },
            )
        }

        @DisplayName("존재하지 않는 상품 ID를 주면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun throwsNotFound_whenProductNotExists() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>>() {}
            val response = testRestTemplate.exchange("/api/v1/products/999", HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
