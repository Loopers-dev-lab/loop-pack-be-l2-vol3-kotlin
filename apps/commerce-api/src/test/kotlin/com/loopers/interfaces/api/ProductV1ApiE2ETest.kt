package com.loopers.interfaces.api

import com.loopers.interfaces.api.admin.brand.AdminBrandV1Dto
import com.loopers.interfaces.api.admin.product.AdminProductV1Dto
import com.loopers.interfaces.api.product.ProductV1Dto
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
class ProductV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api/v1/products"
        private const val ADMIN_BRAND_ENDPOINT = "/api-admin/v1/brands"
        private const val ADMIN_PRODUCT_ENDPOINT = "/api-admin/v1/products"
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
            ADMIN_BRAND_ENDPOINT,
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

    private fun createProductViaAdmin(
        name: String = "감성 티셔츠",
        price: Long = 39000,
        stockQuantity: Int = 100,
    ): AdminProductV1Dto.ProductResponse {
        val request = AdminProductV1Dto.CreateRequest(
            brandId = brandId,
            name = name,
            description = "좋은 상품입니다.",
            price = price,
            stockQuantity = stockQuantity,
            imageUrl = "https://example.com/product.jpg",
        )
        val response = testRestTemplate.exchange(
            ADMIN_PRODUCT_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminProductV1Dto.ProductResponse>>() {},
        )
        return response.body!!.data!!
    }

    @DisplayName("GET /api/v1/products/{productId} (상품 상세 조회)")
    @Nested
    inner class GetProduct {
        @DisplayName("ACTIVE 상품을 조회하면, 200 OK 응답을 받는다.")
        @Test
        fun returns200_whenProductIsActive() {
            val created = createProductViaAdmin()
            val response = testRestTemplate.exchange(
                "$ENDPOINT/${created.id}",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>>() {},
            )
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("감성 티셔츠") },
                { assertThat(response.body?.data?.brandName).isEqualTo("루퍼스") },
                { assertThat(response.body?.data?.soldOut).isFalse() },
            )
        }

        @DisplayName("삭제된 상품을 조회하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returns404_whenProductIsDeleted() {
            val created = createProductViaAdmin()
            testRestTemplate.exchange(
                "$ADMIN_PRODUCT_ENDPOINT/${created.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )
            val response = testRestTemplate.exchange(
                "$ENDPOINT/${created.id}",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api/v1/products (상품 목록 조회 - 커서 페이징)")
    @Nested
    inner class GetProducts {
        @DisplayName("상품 목록을 조회하면, 커서 기반 응답을 받는다.")
        @Test
        fun returnsCursorBasedList() {
            createProductViaAdmin(name = "상품1", price = 10000)
            createProductViaAdmin(name = "상품2", price = 20000)
            createProductViaAdmin(name = "상품3", price = 30000)

            val response = testRestTemplate.exchange(
                "$ENDPOINT?sort=latest&size=2",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductListResponse>>() {},
            )
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.data).hasSize(2) },
                { assertThat(response.body?.data?.hasNext).isTrue() },
                { assertThat(response.body?.data?.nextCursor).isNotNull() },
            )
        }

        @DisplayName("커서로 다음 페이지를 조회하면, 나머지 상품을 반환한다.")
        @Test
        fun returnsNextPage_whenCursorIsProvided() {
            createProductViaAdmin(name = "상품1", price = 10000)
            createProductViaAdmin(name = "상품2", price = 20000)
            createProductViaAdmin(name = "상품3", price = 30000)

            // First page
            val firstResponse = testRestTemplate.exchange(
                "$ENDPOINT?sort=latest&size=2",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductListResponse>>() {},
            )
            val nextCursor = firstResponse.body?.data?.nextCursor

            // Second page
            val secondResponse = testRestTemplate.exchange(
                "$ENDPOINT?sort=latest&size=2&cursor=$nextCursor",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductListResponse>>() {},
            )
            assertAll(
                { assertThat(secondResponse.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(secondResponse.body?.data?.data).hasSize(1) },
                { assertThat(secondResponse.body?.data?.hasNext).isFalse() },
            )
        }
    }
}
