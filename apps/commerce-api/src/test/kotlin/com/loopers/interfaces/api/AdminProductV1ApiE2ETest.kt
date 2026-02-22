package com.loopers.interfaces.api

import com.loopers.interfaces.api.admin.brand.AdminBrandV1Dto
import com.loopers.interfaces.api.admin.product.AdminProductV1Dto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminProductV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api-admin/v1/products"
        private const val BRAND_ENDPOINT = "/api-admin/v1/brands"
        private const val HEADER_LDAP = "X-Loopers-Ldap"
        private const val LDAP_VALUE = "loopers.admin"
    }

    private var brandId: Long = 0

    @BeforeEach
    fun setUp() {
        val brandRequest = AdminBrandV1Dto.CreateRequest(
            name = "루퍼스",
            description = "테스트 브랜드",
            imageUrl = "https://example.com/brand.jpg",
        )
        val response = testRestTemplate.exchange(
            BRAND_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(brandRequest, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Dto.BrandResponse>>() {},
        )
        brandId = response.body!!.data!!.id
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply { set(HEADER_LDAP, LDAP_VALUE) }
    }

    private fun createProductRequest(
        name: String = "감성 티셔츠",
        description: String = "좋은 상품입니다.",
        price: Long = 39000,
        stockQuantity: Int = 100,
        imageUrl: String = "https://example.com/product.jpg",
    ) = AdminProductV1Dto.CreateRequest(
        brandId = brandId,
        name = name,
        description = description,
        price = price,
        stockQuantity = stockQuantity,
        imageUrl = imageUrl,
    )

    private fun createProductViaApi(
        name: String = "감성 티셔츠",
    ): AdminProductV1Dto.ProductResponse {
        val response = testRestTemplate.exchange(
            ENDPOINT,
            HttpMethod.POST,
            HttpEntity(createProductRequest(name = name), adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminProductV1Dto.ProductResponse>>() {},
        )
        return response.body!!.data!!
    }

    @DisplayName("POST /api-admin/v1/products (상품 등록)")
    @Nested
    inner class CreateProduct {
        @DisplayName("유효한 정보로 등록하면, 201 CREATED 응답을 받는다.")
        @Test
        fun returns201_whenValidInfoIsProvided() {
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(createProductRequest(), adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<AdminProductV1Dto.ProductResponse>>() {},
            )
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.name).isEqualTo("감성 티셔츠") },
                { assertThat(response.body?.data?.brandName).isEqualTo("루퍼스") },
                { assertThat(response.body?.data?.price).isEqualTo(39000L) },
            )
        }

        @DisplayName("인증 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returns401_whenNoAdminHeader() {
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(createProductRequest()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("상품명이 빈 값이면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returns400_whenNameIsBlank() {
            // arrange
            val request = createProductRequest(name = "")

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("가격이 음수이면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returns400_whenPriceIsNegative() {
            // arrange
            val request = createProductRequest(price = -1)

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("GET /api-admin/v1/products/{productId} (상품 상세 조회)")
    @Nested
    inner class GetProduct {
        @DisplayName("존재하는 상품을 조회하면, 200 OK 응답을 받는다.")
        @Test
        fun returns200_whenProductExists() {
            val created = createProductViaApi()
            val response = testRestTemplate.exchange(
                "$ENDPOINT/${created.id}",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<AdminProductV1Dto.ProductResponse>>() {},
            )
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("감성 티셔츠") },
            )
        }
    }

    @DisplayName("PUT /api-admin/v1/products/{productId} (상품 수정)")
    @Nested
    inner class UpdateProduct {
        @DisplayName("유효한 정보로 수정하면, 200 OK 응답을 받는다.")
        @Test
        fun returns200_whenValidUpdate() {
            val created = createProductViaApi()
            val updateRequest = AdminProductV1Dto.UpdateRequest(
                name = "새 상품명",
                description = "새 설명",
                price = 50000,
                stockQuantity = 200,
                imageUrl = "https://example.com/new.jpg",
            )
            val response = testRestTemplate.exchange(
                "$ENDPOINT/${created.id}",
                HttpMethod.PUT,
                HttpEntity(updateRequest, adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<AdminProductV1Dto.ProductResponse>>() {},
            )
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("새 상품명") },
                { assertThat(response.body?.data?.price).isEqualTo(50000L) },
            )
        }
    }

    @DisplayName("DELETE /api-admin/v1/products/{productId} (상품 삭제)")
    @Nested
    inner class DeleteProduct {
        @DisplayName("존재하는 상품을 삭제하면, 200 OK 응답을 받는다.")
        @Test
        fun returns200_whenProductExists() {
            val created = createProductViaApi()
            val response = testRestTemplate.exchange(
                "$ENDPOINT/${created.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }
    }
}
