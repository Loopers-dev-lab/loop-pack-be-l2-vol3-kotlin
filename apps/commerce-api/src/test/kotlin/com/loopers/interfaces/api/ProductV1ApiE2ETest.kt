package com.loopers.interfaces.api

import com.loopers.application.catalog.AdminRegisterBrandUseCase
import com.loopers.application.catalog.AdminRegisterProductUseCase
import com.loopers.application.catalog.RegisterBrandCriteria
import com.loopers.application.catalog.RegisterBrandResult
import com.loopers.application.catalog.RegisterProductCriteria
import com.loopers.application.catalog.RegisterProductResult
import com.loopers.domain.user.RegisterCommand
import com.loopers.domain.user.UserService
import com.loopers.interfaces.api.catalog.ProductV1Dto
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
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val adminRegisterBrandUseCase: AdminRegisterBrandUseCase,
    private val adminRegisterProductUseCase: AdminRegisterProductUseCase,
    private val userService: UserService,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_BASE = "/api/v1/products"

        private const val DEFAULT_BRAND_NAME = "나이키"
        private const val DEFAULT_PRODUCT_NAME = "에어맥스 90"
        private const val DEFAULT_QUANTITY = 100
        private val DEFAULT_PRICE = BigDecimal("129000")

        private const val DEFAULT_USERNAME = "testuser"
        private const val DEFAULT_PASSWORD = "password1234!"
        private const val DEFAULT_NAME = "테스트유저"
        private const val DEFAULT_EMAIL = "test@loopers.com"
        private val DEFAULT_BIRTH_DATE = ZonedDateTime.of(1995, 5, 29, 0, 0, 0, 0, ZoneId.of("Asia/Seoul"))
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerUser() {
        userService.register(
            RegisterCommand(
                username = DEFAULT_USERNAME,
                password = DEFAULT_PASSWORD,
                name = DEFAULT_NAME,
                email = DEFAULT_EMAIL,
                birthDate = DEFAULT_BIRTH_DATE,
            ),
        )
    }

    private fun registerBrand(name: String = DEFAULT_BRAND_NAME): RegisterBrandResult {
        return adminRegisterBrandUseCase.execute(RegisterBrandCriteria(name = name))
    }

    private fun registerProduct(
        brandId: Long,
        name: String = DEFAULT_PRODUCT_NAME,
        quantity: Int = DEFAULT_QUANTITY,
        price: BigDecimal = DEFAULT_PRICE,
    ): RegisterProductResult {
        return adminRegisterProductUseCase.execute(
            RegisterProductCriteria(
                brandId = brandId,
                name = name,
                quantity = quantity,
                price = price,
            ),
        )
    }

    private fun createAuthHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", DEFAULT_USERNAME)
            set("X-Loopers-LoginPw", DEFAULT_PASSWORD)
        }
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    inner class GetProduct {
        @DisplayName("유효한 정보가 주어지면, 200 OK와 상품 상세 정보를 반환한다. (quantity 미포함)")
        @Test
        fun returnsProductDetailWithoutQuantityWhenValidInfoIsProvided() {
            // arrange
            registerUser()
            val brand = registerBrand()
            val product = registerProduct(brandId = brand.id)
            val headers = createAuthHeaders()
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>>() {}

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT_BASE/${product.id}",
                HttpMethod.GET,
                HttpEntity(null, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(product.id) },
                { assertThat(response.body?.data?.name).isEqualTo(DEFAULT_PRODUCT_NAME) },
                { assertThat(response.body?.data?.brandName).isEqualTo(DEFAULT_BRAND_NAME) },
                { assertThat(response.body?.data?.price).isEqualByComparingTo(DEFAULT_PRICE) },
            )
        }

        @DisplayName("존재하지 않는 상품이면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFoundWhenProductDoesNotExist() {
            // arrange
            registerUser()
            val headers = createAuthHeaders()
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductDetailResponse>>() {}

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT_BASE/999",
                HttpMethod.GET,
                HttpEntity(null, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    inner class GetProducts {
        @DisplayName("상품 목록을 조회하면, 200 OK와 Slice 페이지네이션 응답을 반환한다.")
        @Test
        fun returnsProductSliceAndOk() {
            // arrange
            registerUser()
            val brand = registerBrand()
            registerProduct(brandId = brand.id, name = "상품1")
            registerProduct(brandId = brand.id, name = "상품2")
            val headers = createAuthHeaders()
            val url = "$ENDPOINT_BASE?page=0&size=10"

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductSliceResponse>>() {}
            val response = testRestTemplate.exchange(url, HttpMethod.GET, HttpEntity(null, headers), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(2) },
                { assertThat(response.body?.data?.hasNext).isFalse() },
            )
        }

        @DisplayName("brandId 필터로 조회하면, 해당 브랜드 상품만 반환한다.")
        @Test
        fun returnsFilteredProductsWhenBrandIdIsProvided() {
            // arrange
            registerUser()
            val brand1 = registerBrand(name = "나이키")
            val brand2 = registerBrand(name = "아디다스")
            registerProduct(brandId = brand1.id, name = "나이키 상품")
            registerProduct(brandId = brand2.id, name = "아디다스 상품")
            val headers = createAuthHeaders()
            val url = "$ENDPOINT_BASE?page=0&size=10&brandId=${brand1.id}"

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductSliceResponse>>() {}
            val response = testRestTemplate.exchange(url, HttpMethod.GET, HttpEntity(null, headers), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(1) },
                { assertThat(response.body?.data?.content?.get(0)?.brandName).isEqualTo("나이키") },
            )
        }

        @DisplayName("sort=price_asc 로 조회하면, 가격 오름차순으로 반환한다.")
        @Test
        fun returnsSortedByPriceAscWhenSortParamIsProvided() {
            // arrange
            registerUser()
            val brand = registerBrand()
            registerProduct(brandId = brand.id, name = "비싼 상품", price = BigDecimal("200000"))
            registerProduct(brandId = brand.id, name = "싼 상품", price = BigDecimal("50000"))
            val headers = createAuthHeaders()
            val url = "$ENDPOINT_BASE?page=0&size=10&sort=price_asc"

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductSliceResponse>>() {}
            val response = testRestTemplate.exchange(url, HttpMethod.GET, HttpEntity(null, headers), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content?.get(0)?.name).isEqualTo("싼 상품") },
                { assertThat(response.body?.data?.content?.get(1)?.name).isEqualTo("비싼 상품") },
            )
        }
    }
}
