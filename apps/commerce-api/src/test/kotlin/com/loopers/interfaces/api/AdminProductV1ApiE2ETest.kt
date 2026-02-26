package com.loopers.interfaces.api

import com.loopers.interfaces.api.admin.brand.AdminBrandRegisterRequest
import com.loopers.interfaces.api.admin.brand.AdminBrandResponse
import com.loopers.interfaces.api.admin.product.AdminProductRegisterRequest
import com.loopers.interfaces.api.admin.product.AdminProductResponse
import com.loopers.interfaces.api.admin.product.AdminProductUpdateRequest
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
import com.loopers.support.constant.AuthHeaders
import com.loopers.support.error.CommonErrorCode
import com.loopers.support.error.ProductErrorCode
import com.loopers.testcontainers.MySqlTestContainersConfig
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
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestContainersConfig::class)
class AdminProductV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(AuthHeaders.Admin.LDAP, AuthHeaders.Admin.LDAP_VALUE)
        }
    }

    private fun registerBrand(name: String = "나이키"): AdminBrandResponse {
        val request = AdminBrandRegisterRequest(name = name)
        val responseType = object : ParameterizedTypeReference<ApiResponse<AdminBrandResponse>>() {}
        val response = testRestTemplate.exchange(
            ApiPaths.AdminBrands.BASE,
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        return requireNotNull(response.body?.data) { "브랜드 등록 응답이 비어 있습니다." }
    }

    private fun registerProduct(
        brandId: Long,
        name: String = "테스트 상품",
        price: Long = 10000,
        stock: Int = 100,
    ): AdminProductResponse {
        val request = AdminProductRegisterRequest(
            brandId = brandId,
            name = name,
            description = "상품 설명",
            price = price,
            stock = stock,
            imageUrl = "https://example.com/image.jpg",
        )
        val responseType = object : ParameterizedTypeReference<ApiResponse<AdminProductResponse>>() {}
        val response = testRestTemplate.exchange(
            ApiPaths.AdminProducts.BASE,
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        return requireNotNull(response.body?.data) { "상품 등록 응답이 비어 있습니다." }
    }

    @DisplayName("POST /api/admin/v1/products - 상품 등록")
    @Nested
    inner class Register {

        @DisplayName("정상적인 입력으로 등록하면, 201 CREATED와 상품 정보를 반환한다.")
        @Test
        fun success() {
            // arrange
            val brand = registerBrand("나이키")
            val request = AdminProductRegisterRequest(
                brandId = brand.id,
                name = "에어맥스",
                description = "나이키 에어맥스",
                price = 150000,
                stock = 50,
                imageUrl = "https://example.com/airmax.jpg",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminProductResponse>>() {}
            val response = testRestTemplate.exchange(
                ApiPaths.AdminProducts.BASE,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.id).isGreaterThan(0) },
                { assertThat(response.body?.data?.name).isEqualTo("에어맥스") },
                { assertThat(response.body?.data?.brandName).isEqualTo("나이키") },
                { assertThat(response.body?.data?.stock).isEqualTo(50) },
            )
        }

        @DisplayName("어드민 인증 헤더가 없으면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun failWhenAdminAuthMissing() {
            // arrange
            val brand = registerBrand()
            val request = AdminProductRegisterRequest(
                brandId = brand.id,
                name = "에어맥스",
                description = "설명",
                price = 10000,
                stock = 10,
                imageUrl = "https://example.com/image.jpg",
            )

            // act
            val response = testRestTemplate.postForEntity(
                ApiPaths.AdminProducts.BASE,
                request,
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(CommonErrorCode.ADMIN_AUTHENTICATION_FAILED.code) },
            )
        }

        @DisplayName("삭제된 브랜드로 등록하면, 400 BAD_REQUEST와 PRODUCT_004 에러를 반환한다.")
        @Test
        fun failWhenBrandDeleted() {
            // arrange
            val brand = registerBrand()
            testRestTemplate.exchange(
                "${ApiPaths.AdminBrands.BASE}/${brand.id}",
                HttpMethod.DELETE,
                HttpEntity<Void>(adminHeaders()),
                ApiResponse::class.java,
            )
            val request = AdminProductRegisterRequest(
                brandId = brand.id,
                name = "에어맥스",
                description = "설명",
                price = 10000,
                stock = 10,
                imageUrl = "https://example.com/image.jpg",
            )

            // act
            val response = testRestTemplate.exchange(
                ApiPaths.AdminProducts.BASE,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(ProductErrorCode.INVALID_BRAND.code) },
            )
        }

        @DisplayName("상품명이 빈 값이면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun failWhenNameBlank() {
            // arrange
            val brand = registerBrand()
            val request = AdminProductRegisterRequest(
                brandId = brand.id,
                name = "",
                description = "설명",
                price = 10000,
                stock = 10,
                imageUrl = "https://example.com/image.jpg",
            )

            // act
            val response = testRestTemplate.exchange(
                ApiPaths.AdminProducts.BASE,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE.code) },
            )
        }
    }

    @DisplayName("GET /api/admin/v1/products - 상품 목록 조회")
    @Nested
    inner class GetProducts {

        @DisplayName("등록된 상품 목록을 반환한다.")
        @Test
        fun success() {
            // arrange
            val brand = registerBrand()
            registerProduct(brand.id, name = "상품1")
            registerProduct(brand.id, name = "상품2")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResult<AdminProductResponse>>>() {}
            val response = testRestTemplate.exchange(
                ApiPaths.AdminProducts.BASE,
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(2) },
                { assertThat(response.body?.data?.totalElements).isEqualTo(2) },
            )
        }
    }

    @DisplayName("GET /api/admin/v1/products/{productId} - 상품 단건 조회")
    @Nested
    inner class GetProduct {

        @DisplayName("존재하는 상품을 조회하면, 200 OK와 상품 정보를 반환한다.")
        @Test
        fun success() {
            // arrange
            val brand = registerBrand("나이키")
            val product = registerProduct(brand.id, name = "에어맥스")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminProductResponse>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminProducts.BASE}/${product.id}",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("에어맥스") },
                { assertThat(response.body?.data?.brandName).isEqualTo("나이키") },
            )
        }

        @DisplayName("존재하지 않는 상품을 조회하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun failWhenNotFound() {
            // act
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminProducts.BASE}/999",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND.code) },
            )
        }
    }

    @DisplayName("PUT /api/admin/v1/products/{productId} - 상품 수정")
    @Nested
    inner class Update {

        @DisplayName("새 정보로 수정하면, 200 OK와 수정된 상품 정보를 반환한다.")
        @Test
        fun success() {
            // arrange
            val brand = registerBrand()
            val product = registerProduct(brand.id)
            val request = AdminProductUpdateRequest(
                name = "수정된 상품",
                description = "수정된 설명",
                price = 20000,
                stock = 50,
                imageUrl = "https://example.com/new.jpg",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminProductResponse>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminProducts.BASE}/${product.id}",
                HttpMethod.PUT,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("수정된 상품") },
                { assertThat(response.body?.data?.price).isEqualTo(20000) },
            )
        }
    }

    @DisplayName("DELETE /api/admin/v1/products/{productId} - 상품 삭제")
    @Nested
    inner class Delete {

        @DisplayName("삭제하면 204 NO_CONTENT를 반환하고, 이후 조회 시 404를 반환한다.")
        @Test
        fun successAndVerifyNotFound() {
            // arrange
            val brand = registerBrand()
            val product = registerProduct(brand.id)

            // act - 삭제
            val deleteResponse = testRestTemplate.exchange(
                "${ApiPaths.AdminProducts.BASE}/${product.id}",
                HttpMethod.DELETE,
                HttpEntity<Void>(adminHeaders()),
                ApiResponse::class.java,
            )

            // assert - 204
            assertThat(deleteResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            // verify - 삭제 후 조회 시 404
            val getResponse = testRestTemplate.exchange(
                "${ApiPaths.AdminProducts.BASE}/${product.id}",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                ApiResponse::class.java,
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("존재하지 않는 상품을 삭제하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun failWhenNotFound() {
            // act
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminProducts.BASE}/999",
                HttpMethod.DELETE,
                HttpEntity<Void>(adminHeaders()),
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND.code) },
            )
        }
    }
}
