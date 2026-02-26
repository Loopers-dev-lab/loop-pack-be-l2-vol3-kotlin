package com.loopers.interfaces.api

import com.loopers.application.catalog.AdminRegisterBrandUseCase
import com.loopers.application.catalog.AdminRegisterProductUseCase
import com.loopers.application.catalog.RegisterBrandCriteria
import com.loopers.application.catalog.RegisterBrandResult
import com.loopers.application.catalog.RegisterProductCriteria
import com.loopers.application.catalog.RegisterProductResult
import com.loopers.interfaces.api.catalog.ProductV1AdminDto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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
import kotlin.test.Test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductAdminV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val adminRegisterBrandUseCase: AdminRegisterBrandUseCase,
    private val adminRegisterProductUseCase: AdminRegisterProductUseCase,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val LDAP_HEADER = "loopers.admin"
        private const val ENDPOINT_BASE = "/api-admin/v1/products"
        private const val DEFAULT_BRAND_NAME = "나이키"
        private const val DEFAULT_PRODUCT_NAME = "에어맥스 90"
        private const val DEFAULT_QUANTITY = 100
        private val DEFAULT_PRICE = BigDecimal("129000")
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
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

    private fun createAuthAdminHeader(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-Ldap", LDAP_HEADER)
        }
    }

    @DisplayName("POST /api-admin/v1/products")
    @Nested
    inner class Register {
        @DisplayName("유효한 정보가 주어지면, 201 CREATED를 반환한다.")
        @Test
        fun returnsCreatedWhenValidInfoIsProvided() {
            // arrange
            val brand = registerBrand()
            val request = ProductV1AdminDto.RegisterRequest(
                brandId = brand.id,
                name = DEFAULT_PRODUCT_NAME,
                quantity = DEFAULT_QUANTITY,
                price = DEFAULT_PRICE,
            )
            val headers = createAuthAdminHeader()

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT_BASE,
                HttpMethod.POST,
                HttpEntity(request, headers),
                Void::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body).isNull() },
            )
        }

        @DisplayName("존재하지 않는 브랜드이면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFoundWhenBrandDoesNotExist() {
            // arrange
            val request = ProductV1AdminDto.RegisterRequest(
                brandId = 999L,
                name = DEFAULT_PRODUCT_NAME,
                quantity = DEFAULT_QUANTITY,
                price = DEFAULT_PRICE,
            )
            val headers = createAuthAdminHeader()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_BASE,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api-admin/v1/products/{productId}")
    @Nested
    inner class GetProduct {
        @DisplayName("유효한 정보가 주어지면, 200 OK와 상품 상세 정보를 반환한다.")
        @Test
        fun returnsProductDetailAndOkWhenValidInfoIsProvided() {
            // arrange
            val brand = registerBrand()
            val product = registerProduct(brandId = brand.id)
            val headers = createAuthAdminHeader()
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1AdminDto.ProductDetailResponse>>() {}

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
                { assertThat(response.body?.data?.quantity).isEqualTo(DEFAULT_QUANTITY) },
            )
        }

        @DisplayName("존재하지 않는 상품이면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFoundWhenProductDoesNotExist() {
            // arrange
            val headers = createAuthAdminHeader()
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1AdminDto.ProductDetailResponse>>() {}

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

    @DisplayName("GET /api-admin/v1/products")
    @Nested
    inner class GetProducts {
        @DisplayName("상품 목록을 조회하면, 200 OK와 Slice 페이지네이션 응답을 반환한다.")
        @Test
        fun returnsProductSliceAndOk() {
            // arrange
            val brand = registerBrand()
            registerProduct(brandId = brand.id, name = "상품1")
            registerProduct(brandId = brand.id, name = "상품2")
            val headers = createAuthAdminHeader()
            val url = "$ENDPOINT_BASE?page=0&size=10"

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1AdminDto.ProductSliceResponse>>() {}
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
            val brand1 = registerBrand(name = "나이키")
            val brand2 = registerBrand(name = "아디다스")
            registerProduct(brandId = brand1.id, name = "나이키 상품")
            registerProduct(brandId = brand2.id, name = "아디다스 상품")
            val headers = createAuthAdminHeader()
            val url = "$ENDPOINT_BASE?page=0&size=10&brandId=${brand1.id}"

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1AdminDto.ProductSliceResponse>>() {}
            val response = testRestTemplate.exchange(url, HttpMethod.GET, HttpEntity(null, headers), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(1) },
                { assertThat(response.body?.data?.content?.get(0)?.brandName).isEqualTo("나이키") },
            )
        }
    }

    @DisplayName("PUT /api-admin/v1/products/{productId}")
    @Nested
    inner class ModifyProduct {
        @DisplayName("유효한 정보가 주어지면, 204 NO_CONTENT를 반환한다.")
        @Test
        fun returnsNoContentWhenValidInfoIsProvided() {
            // arrange
            val brand = registerBrand()
            val product = registerProduct(brandId = brand.id)
            val request = ProductV1AdminDto.UpdateRequest(
                newName = "에어포스 1",
                newQuantity = 50,
                newPrice = BigDecimal("99000"),
            )
            val headers = createAuthAdminHeader()

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT_BASE/${product.id}",
                HttpMethod.PUT,
                HttpEntity(request, headers),
                Void::class.java,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @DisplayName("존재하지 않는 상품이면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFoundWhenProductDoesNotExist() {
            // arrange
            val request = ProductV1AdminDto.UpdateRequest(
                newName = "에어포스 1",
                newQuantity = 50,
                newPrice = BigDecimal("99000"),
            )
            val headers = createAuthAdminHeader()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT_BASE/999",
                HttpMethod.PUT,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("DELETE /api-admin/v1/products/{productId}")
    @Nested
    inner class DeleteProduct {
        @DisplayName("존재하는 상품을 삭제하면, 204 NO_CONTENT를 반환한다.")
        @Test
        fun returnsNoContentWhenProductIsDeleted() {
            // arrange
            val brand = registerBrand()
            val product = registerProduct(brandId = brand.id)
            val headers = createAuthAdminHeader()

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT_BASE/${product.id}",
                HttpMethod.DELETE,
                HttpEntity(null, headers),
                Void::class.java,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @DisplayName("존재하지 않는 상품을 삭제하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFoundWhenProductDoesNotExist() {
            // arrange
            val headers = createAuthAdminHeader()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT_BASE/999",
                HttpMethod.DELETE,
                HttpEntity(null, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
