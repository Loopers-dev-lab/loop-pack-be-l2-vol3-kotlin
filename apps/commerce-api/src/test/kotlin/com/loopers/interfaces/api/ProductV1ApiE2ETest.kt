package com.loopers.interfaces.api

import com.loopers.interfaces.api.admin.brand.AdminBrandRegisterRequest
import com.loopers.interfaces.api.admin.brand.AdminBrandResponse
import com.loopers.interfaces.api.admin.product.AdminProductRegisterRequest
import com.loopers.interfaces.api.admin.product.AdminProductResponse
import com.loopers.interfaces.api.product.ProductDetailResponse
import com.loopers.interfaces.api.product.ProductResponse
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
import com.loopers.support.constant.AuthHeaders
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
class ProductV1ApiE2ETest @Autowired constructor(
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

    private fun registerBrandViaAdmin(name: String = "나이키"): AdminBrandResponse {
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

    private fun registerProductViaAdmin(
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

    private fun deleteProductViaAdmin(productId: Long) {
        testRestTemplate.exchange(
            "${ApiPaths.AdminProducts.BASE}/$productId",
            HttpMethod.DELETE,
            HttpEntity<Void>(adminHeaders()),
            ApiResponse::class.java,
        )
    }

    @DisplayName("GET /api/v1/products - 상품 목록 조회")
    @Nested
    inner class GetProducts {

        @DisplayName("활성 상품만 조회된다.")
        @Test
        fun returnsOnlyActiveProducts() {
            // arrange
            val brand = registerBrandViaAdmin()
            registerProductViaAdmin(brand.id, name = "활성 상품")
            val deleted = registerProductViaAdmin(brand.id, name = "삭제 상품")
            deleteProductViaAdmin(deleted.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResult<ProductResponse>>>() {}
            val response = testRestTemplate.exchange(
                ApiPaths.Products.BASE,
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(1) },
                { assertThat(response.body?.data?.content?.get(0)?.name).isEqualTo("활성 상품") },
            )
        }

        @DisplayName("브랜드 필터로 조회할 수 있다.")
        @Test
        fun filterByBrand() {
            // arrange
            val nike = registerBrandViaAdmin("나이키")
            val adidas = registerBrandViaAdmin("아디다스")
            registerProductViaAdmin(nike.id, name = "나이키 신발")
            registerProductViaAdmin(adidas.id, name = "아디다스 신발")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResult<ProductResponse>>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.Products.BASE}?brandId=${nike.id}",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(1) },
                { assertThat(response.body?.data?.content?.get(0)?.brandName).isEqualTo("나이키") },
            )
        }

        @DisplayName("고객 응답에 stock 수량이 포함되지 않는다.")
        @Test
        fun doesNotExposeStock() {
            // arrange
            val brand = registerBrandViaAdmin()
            registerProductViaAdmin(brand.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResult<Map<String, Any>>>>() {}
            val response = testRestTemplate.exchange(
                ApiPaths.Products.BASE,
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            val productMap = response.body?.data?.content?.get(0)
            assertThat(productMap).doesNotContainKey("stock")
        }
    }

    @DisplayName("GET /api/v1/products/{productId} - 상품 상세 조회")
    @Nested
    inner class GetProduct {

        @DisplayName("존재하는 활성 상품을 조회하면, 200 OK와 상세 정보를 반환한다.")
        @Test
        fun success() {
            // arrange
            val brand = registerBrandViaAdmin("나이키")
            val product = registerProductViaAdmin(brand.id, name = "에어맥스")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductDetailResponse>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.Products.BASE}/${product.id}",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("에어맥스") },
                { assertThat(response.body?.data?.brandName).isEqualTo("나이키") },
                { assertThat(response.body?.data?.description).isEqualTo("상품 설명") },
            )
        }

        @DisplayName("존재하지 않는 상품을 조회하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun failWhenNotFound() {
            // act
            val response = testRestTemplate.getForEntity(
                "${ApiPaths.Products.BASE}/999",
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND.code) },
            )
        }

        @DisplayName("삭제된 상품을 조회하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun failWhenDeleted() {
            // arrange
            val brand = registerBrandViaAdmin()
            val product = registerProductViaAdmin(brand.id)
            deleteProductViaAdmin(product.id)

            // act
            val response = testRestTemplate.getForEntity(
                "${ApiPaths.Products.BASE}/${product.id}",
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
