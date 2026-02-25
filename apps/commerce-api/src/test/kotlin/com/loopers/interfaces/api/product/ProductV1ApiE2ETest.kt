package com.loopers.interfaces.api.product

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.brand.BrandV1Dto
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        private const val ADMIN_LDAP = "loopers.admin"
        private const val TEST_PRODUCT_NAME = "에어맥스 90"
        private val TEST_PRODUCT_PRICE = BigDecimal("129000")
        private const val TEST_PRODUCT_STOCK = 100
        private const val TEST_PRODUCT_DESCRIPTION = "나이키 에어맥스 90"
        private const val TEST_PRODUCT_IMAGE_URL = "https://example.com/airmax90.jpg"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-Ldap", ADMIN_LDAP)
        }
    }

    private fun createTestBrand(name: String = "나이키"): BrandV1Dto.BrandAdminResponse? {
        val request = BrandV1Dto.CreateRequest(name = name, description = "스포츠 브랜드")
        val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandAdminResponse>>() {}
        val response = testRestTemplate.exchange(
            "/api-admin/v1/brands",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        return response.body?.data
    }

    private fun createTestProduct(
        brandId: Long,
        name: String = TEST_PRODUCT_NAME,
        price: BigDecimal = TEST_PRODUCT_PRICE,
        stock: Int = TEST_PRODUCT_STOCK,
        description: String? = TEST_PRODUCT_DESCRIPTION,
        imageUrl: String? = TEST_PRODUCT_IMAGE_URL,
    ): ProductAdminV1Dto.ProductAdminResponse? {
        val request = ProductAdminV1Dto.CreateRequest(
            brandId = brandId,
            name = name,
            price = price,
            stock = stock,
            description = description,
            imageUrl = imageUrl,
        )
        val responseType = object : ParameterizedTypeReference<ApiResponse<ProductAdminV1Dto.ProductAdminResponse>>() {}
        val response = testRestTemplate.exchange(
            "/api-admin/v1/products",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        return response.body?.data
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    inner class GetAllProducts {

        @DisplayName("상품이 존재하면, 200 OK와 목록을 반환한다.")
        @Test
        fun returnsOk_whenProductsExist() {
            // arrange
            val brand = createTestBrand()!!
            createTestProduct(brandId = brand.id, name = "에어맥스 90")
            createTestProduct(brandId = brand.id, name = "에어포스 1")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products?page=0&size=20",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull() },
            )
        }

        @DisplayName("brandId로 필터링하면, 해당 브랜드 상품만 반환한다.")
        @Test
        fun returnsFilteredProducts_whenBrandIdProvided() {
            // arrange
            val brand1 = createTestBrand(name = "나이키")!!
            val brand2 = createTestBrand(name = "아디다스")!!
            createTestProduct(brandId = brand1.id, name = "에어맥스 90")
            createTestProduct(brandId = brand2.id, name = "울트라부스트")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products?brandId=${brand1.id}&page=0&size=20",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull() },
            )
        }
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    inner class GetProduct {

        @DisplayName("존재하는 상품을 조회하면, 200 OK와 상품 정보를 반환한다.")
        @Test
        fun returnsOk_whenProductExists() {
            // arrange
            val brand = createTestBrand()!!
            val created = createTestProduct(brandId = brand.id)!!

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products/${created.id}",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(created.id) },
                { assertThat(response.body?.data?.name).isEqualTo(TEST_PRODUCT_NAME) },
                { assertThat(response.body?.data?.brandId).isEqualTo(brand.id) },
            )
        }

        @DisplayName("존재하지 않는 상품을 조회하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenProductNotExists() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products/999",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("어드민 API 인증")
    @Nested
    inner class AdminAuth {

        @DisplayName("X-Loopers-Ldap 헤더 없이 요청하면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderMissing() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/products?page=0&size=20",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("잘못된 LDAP 값으로 요청하면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenLdapInvalid() {
            // arrange
            val headers = HttpHeaders().apply {
                set("X-Loopers-Ldap", "invalid.ldap")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/products?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("POST /api-admin/v1/products")
    @Nested
    inner class CreateProduct {

        @DisplayName("정상적인 요청이면, 200 OK와 생성된 상품을 반환한다.")
        @Test
        fun returnsOk_whenCreateSucceeds() {
            // arrange
            val brand = createTestBrand()!!
            val request = ProductAdminV1Dto.CreateRequest(
                brandId = brand.id,
                name = TEST_PRODUCT_NAME,
                price = TEST_PRODUCT_PRICE,
                stock = TEST_PRODUCT_STOCK,
                description = TEST_PRODUCT_DESCRIPTION,
                imageUrl = TEST_PRODUCT_IMAGE_URL,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductAdminV1Dto.ProductAdminResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/products",
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo(TEST_PRODUCT_NAME) },
                { assertThat(response.body?.data?.brandId).isEqualTo(brand.id) },
                { assertThat(response.body?.data?.id).isNotNull() },
                { assertThat(response.body?.data?.createdAt).isNotNull() },
                { assertThat(response.body?.data?.updatedAt).isNotNull() },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID로 등록하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenBrandNotExists() {
            // arrange
            val request = ProductAdminV1Dto.CreateRequest(
                brandId = 999L,
                name = TEST_PRODUCT_NAME,
                price = TEST_PRODUCT_PRICE,
                stock = TEST_PRODUCT_STOCK,
                description = null,
                imageUrl = null,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/products",
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("상품명이 빈 값이면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenNameIsBlank() {
            // arrange
            val brand = createTestBrand()!!
            val request = ProductAdminV1Dto.CreateRequest(
                brandId = brand.id,
                name = "  ",
                price = TEST_PRODUCT_PRICE,
                stock = TEST_PRODUCT_STOCK,
                description = null,
                imageUrl = null,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/products",
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("PUT /api-admin/v1/products/{productId}")
    @Nested
    inner class UpdateProduct {

        @DisplayName("정상적인 요청이면, 200 OK와 수정된 상품을 반환한다.")
        @Test
        fun returnsOk_whenUpdateSucceeds() {
            // arrange
            val brand = createTestBrand()!!
            val created = createTestProduct(brandId = brand.id)!!
            val request = ProductAdminV1Dto.UpdateRequest(
                name = "에어포스 1",
                price = BigDecimal("139000"),
                stock = 50,
                description = "나이키 에어포스 1",
                imageUrl = "https://example.com/airforce1.jpg",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductAdminV1Dto.ProductAdminResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/products/${created.id}",
                HttpMethod.PUT,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("에어포스 1") },
                { assertThat(response.body?.data?.brandId).isEqualTo(brand.id) },
                { assertThat(response.body?.data?.createdAt).isNotNull() },
                { assertThat(response.body?.data?.updatedAt).isNotNull() },
            )
        }

        @DisplayName("존재하지 않는 상품을 수정하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenProductNotExists() {
            // arrange
            val request = ProductAdminV1Dto.UpdateRequest(
                name = "에어포스 1",
                price = BigDecimal("139000"),
                stock = 50,
                description = null,
                imageUrl = null,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/products/999",
                HttpMethod.PUT,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("DELETE /api-admin/v1/products/{productId}")
    @Nested
    inner class DeleteProduct {

        @DisplayName("존재하는 상품을 삭제하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenDeleteSucceeds() {
            // arrange
            val brand = createTestBrand()!!
            val created = createTestProduct(brandId = brand.id)!!

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/products/${created.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @DisplayName("삭제된 상품을 조회하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenQueryDeletedProduct() {
            // arrange
            val brand = createTestBrand()!!
            val created = createTestProduct(brandId = brand.id)!!
            val deleteType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            testRestTemplate.exchange(
                "/api-admin/v1/products/${created.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                deleteType,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products/${created.id}",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("존재하지 않는 상품을 삭제하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenProductNotExists() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/products/999",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
