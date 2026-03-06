package com.loopers.interfaces.api.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.product.dto.ProductInfo
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.interfaces.api.ApiResponse
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
import com.loopers.interfaces.api.PageResponse
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val PRODUCTS_ENDPOINT = "/api/v1/products"
        private val GET_PRODUCT_INFO: (Long) -> String = { id: Long -> "$PRODUCTS_ENDPOINT/$id" }
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("GET /api/v1/products/{id}")
    @Nested
    inner class GetProductInfo {
        @DisplayName("존재하는 상품 ID를 주면, 해당 상품 정보를 반환한다.")
        @Test
        fun returnsProductInfo_whenValidIdIsProvided() {
            // arrange
            val brand = brandJpaRepository.save(Brand.create(name = "테스트 브랜드", description = "설명"))
            val product = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "테스트 상품",
                    price = BigDecimal("10000.00"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                ),
            )
            val requestUrl = GET_PRODUCT_INFO(product.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductInfo>>() {}
            val response = testRestTemplate.exchange(requestUrl, HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(product.id) },
                { assertThat(response.body?.data?.name).isEqualTo("테스트 상품") },
                { assertThat(response.body?.data?.price).isEqualTo(BigDecimal("10000.00")) },
                { assertThat(response.body?.data?.stock).isEqualTo(100) },
                { assertThat(response.body?.data?.brandName).isEqualTo("테스트 브랜드") },
            )
        }

        @DisplayName("존재하지 않는 상품 ID를 주면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun throwsNotFound_whenProductDoesNotExist() {
            // arrange
            val invalidId = 999999L
            val requestUrl = GET_PRODUCT_INFO(invalidId)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductInfo>>() {}
            val response = testRestTemplate.exchange(requestUrl, HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("삭제된 상품 ID를 주면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun throwsNotFound_whenProductIsDeleted() {
            // arrange
            val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
            val product = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "삭제될 상품",
                    price = BigDecimal("10000.00"),
                    stock = 100,
                ),
            )
            product.delete()
            productJpaRepository.save(product)
            val requestUrl = GET_PRODUCT_INFO(product.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductInfo>>() {}
            val response = testRestTemplate.exchange(requestUrl, HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("비활성 상품 ID를 주면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun throwsNotFound_whenProductIsInactive() {
            // arrange
            val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
            val product = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "비활성 상품",
                    price = BigDecimal("10000.00"),
                    stock = 100,
                    status = ProductStatus.INACTIVE,
                ),
            )
            val requestUrl = GET_PRODUCT_INFO(product.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductInfo>>() {}
            val response = testRestTemplate.exchange(requestUrl, HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    inner class GetProducts {
        @DisplayName("기본 요청으로 첫 번째 페이지 상품 목록을 반환한다.")
        @Test
        fun returnsFirstPage_withDefaultParameters() {
            // arrange
            val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
            productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "상품1",
                    price = BigDecimal("10000.00"),
                    stock = 100,
                ),
            )
            productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "상품2",
                    price = BigDecimal("20000.00"),
                    stock = 50,
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
            val response = testRestTemplate.exchange(
                PRODUCTS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            // assert
            assertThat(response.statusCode).`as`("statusCode").isEqualTo(HttpStatus.OK)
            assertThat(response.body?.data?.totalElements).`as`("totalElements").isEqualTo(2L)
            assertThat(response.body?.data?.number).`as`("number").isEqualTo(0)
            assertThat(response.body?.data?.size).`as`("size").isEqualTo(20)
        }

        @DisplayName("brandId 필터로 해당 브랜드의 상품만 반환한다.")
        @Test
        fun returnsProductsByBrandId() {
            // arrange
            val brand1 = brandJpaRepository.save(Brand.create(name = "브랜드1", description = "설명"))
            val brand2 = brandJpaRepository.save(Brand.create(name = "브랜드2", description = "설명"))
            productJpaRepository.save(
                Product.create(brand = brand1, name = "상품1", price = BigDecimal("10000.00"), stock = 100),
            )
            productJpaRepository.save(
                Product.create(brand = brand2, name = "상품2", price = BigDecimal("20000.00"), stock = 50),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCTS_ENDPOINT?brandId=${brand1.id}",
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.totalElements).isEqualTo(1L) },
                { assertThat(response.body?.data?.content?.first()?.brandName).isEqualTo("브랜드1") },
            )
        }

        @DisplayName("페이징으로 지정된 페이지 상품을 반환한다.")
        @Test
        fun returnsPaginatedResults() {
            // arrange
            val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
            repeat(30) { i ->
                productJpaRepository.save(
                    Product.create(
                        brand = brand,
                        name = "상품$i",
                        price = BigDecimal((i + 1) * 1000),
                        stock = 100,
                    ),
                )
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCTS_ENDPOINT?page=1&size=20",
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.totalElements).isEqualTo(30L) },
                { assertThat(response.body?.data?.number).isEqualTo(1) },
                { assertThat(response.body?.data?.content).hasSize(10) },
            )
        }

        @DisplayName("size 파라미터가 유효하지 않으면 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun throwsBadRequest_whenSizeIsInvalid() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCTS_ENDPOINT?size=30",
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("page가 음수이면 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun throwsBadRequest_whenPageIsNegative() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCTS_ENDPOINT?page=-1",
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("비활성 상품은 조회 결과에 포함되지 않는다.")
        @Test
        fun excludesInactiveProducts() {
            // arrange
            val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
            productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "활성 상품",
                    price = BigDecimal("10000.00"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                ),
            )
            productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "비활성 상품",
                    price = BigDecimal("20000.00"),
                    stock = 50,
                    status = ProductStatus.INACTIVE,
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
            val response = testRestTemplate.exchange(
                PRODUCTS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.totalElements).isEqualTo(1L) },
                { assertThat(response.body?.data?.content?.first()?.name).isEqualTo("활성 상품") },
            )
        }

        @DisplayName("삭제된 상품은 조회 결과에 포함되지 않는다.")
        @Test
        fun excludesDeletedProducts() {
            // arrange
            val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
            productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "활성 상품",
                    price = BigDecimal("10000.00"),
                    stock = 100,
                ),
            )
            val deletedProduct = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "삭제될 상품",
                    price = BigDecimal("20000.00"),
                    stock = 50,
                ),
            )
            deletedProduct.delete()
            productJpaRepository.save(deletedProduct)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
            val response = testRestTemplate.exchange(
                PRODUCTS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.totalElements).isEqualTo(1L) },
                { assertThat(response.body?.data?.content?.first()?.name).isEqualTo("활성 상품") },
            )
        }

        @DisplayName("sort 파라미터로 정렬 순서를 지정할 수 있다.")
        @Test
        fun appliesSortingByCreatedAt() {
            // arrange
            val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
            val product1 = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "상품1",
                    price = BigDecimal("10000.00"),
                    stock = 100,
                ),
            )
            Thread.sleep(10) // 생성 순서 보장
            val product2 = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "상품2",
                    price = BigDecimal("20000.00"),
                    stock = 50,
                ),
            )

            // act - LATEST (기본값, createdAt 역순)
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCTS_ENDPOINT?sort=latest",
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content?.first()?.id).isEqualTo(product2.id) },
                { assertThat(response.body?.data?.content?.last()?.id).isEqualTo(product1.id) },
            )
        }

        @DisplayName("sort=price_asc로 가격 오름차순 정렬이 가능하다.")
        @Test
        fun appliesSortingByPriceAsc() {
            // arrange
            val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
            productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "저가 상품",
                    price = BigDecimal("10000.00"),
                    stock = 100,
                ),
            )
            productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "고가 상품",
                    price = BigDecimal("30000.00"),
                    stock = 50,
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCTS_ENDPOINT?sort=price_asc",
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content?.first()?.price).isEqualTo(BigDecimal("10000.00")) },
                { assertThat(response.body?.data?.content?.last()?.price).isEqualTo(BigDecimal("30000.00")) },
            )
        }
    }
}
