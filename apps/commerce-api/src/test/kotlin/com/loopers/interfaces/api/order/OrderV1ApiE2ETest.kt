package com.loopers.interfaces.api.order

import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.brand.BrandAdminV1Dto
import com.loopers.interfaces.api.product.ProductAdminV1Dto
import com.loopers.support.constant.HttpHeaders
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
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders as SpringHttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_ORDERS = "/api/v1/orders"
        private const val ENDPOINT_SIGNUP = "/api/v1/users"
        private const val ENDPOINT_BRANDS = "/api-admin/v1/brands"
        private const val ENDPOINT_PRODUCTS = "/api-admin/v1/products"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    // ── 헬퍼 메서드 ──

    private fun signUpAndGetHeaders(
        loginId: String = "testuser1",
        password: String = "Abcd1234!",
    ): SpringHttpHeaders {
        val signUpRequest = mapOf(
            "loginId" to loginId,
            "password" to password,
            "name" to "홍길동",
            "birthday" to "1990-01-15",
            "email" to "test@example.com",
        )
        testRestTemplate.postForEntity<Any>(ENDPOINT_SIGNUP, signUpRequest)

        return SpringHttpHeaders().apply {
            set(HttpHeaders.LOGIN_ID, loginId)
            set(HttpHeaders.LOGIN_PW, password)
        }
    }

    private fun adminHeaders(): SpringHttpHeaders {
        return SpringHttpHeaders().apply {
            set(HttpHeaders.LDAP, "loopers.admin")
        }
    }

    private fun createBrandAndGetId(name: String = "테스트 브랜드"): Long {
        val request = mapOf(
            "name" to name,
            "description" to "브랜드 설명",
            "imageUrl" to "https://example.com/brand.jpg",
        )
        val responseType = object : ParameterizedTypeReference<ApiResponse<BrandAdminV1Dto.BrandAdminResponse>>() {}
        val response = testRestTemplate.exchange(
            ENDPOINT_BRANDS,
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        return response.body!!.data!!.id
    }

    private fun createProductAndGetId(
        brandId: Long,
        name: String = "테스트 상품",
        price: Long = 10000,
        stockQuantity: Int = 10,
    ): Long {
        val request = mapOf(
            "brandId" to brandId,
            "name" to name,
            "description" to "상품 설명",
            "price" to price,
            "stockQuantity" to stockQuantity,
            "displayYn" to true,
            "imageUrl" to "https://example.com/product.jpg",
        )
        val responseType = object : ParameterizedTypeReference<ApiResponse<ProductAdminV1Dto.ProductAdminResponse>>() {}
        val response = testRestTemplate.exchange(
            ENDPOINT_PRODUCTS,
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        return response.body!!.data!!.id
    }

    private fun deleteBrand(brandId: Long) {
        testRestTemplate.exchange(
            "$ENDPOINT_BRANDS/$brandId",
            HttpMethod.DELETE,
            HttpEntity<Any>(adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<Unit>>() {},
        )
    }

    private fun getProductStock(productId: Long): Int {
        val product = productJpaRepository.findById(productId).orElseThrow()
        return product.stockQuantity
    }

    @Nested
    @DisplayName("주문 생성")
    inner class CreateOrder {

        @Test
        @DisplayName("올바른 정보로 주문하면 200 OK와 주문 정보를 반환한다")
        fun createOrderSuccess() {
            // arrange
            val headers = signUpAndGetHeaders()
            val brandId = createBrandAndGetId()
            val productId = createProductAndGetId(brandId, stockQuantity = 10)

            val request = mapOf(
                "items" to listOf(
                    mapOf("productId" to productId, "quantity" to 2),
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isNotNull() },
                { assertThat(response.body?.data?.orderNumber).isNotEmpty() },
                { assertThat(response.body?.data?.items).hasSize(1) },
            )

            // verify — 재고 차감 확인
            val stock = getProductStock(productId)
            assertThat(stock).isEqualTo(8)
        }
    }

    @Nested
    @DisplayName("주문 생성 실패 시 트랜잭션 롤백")
    inner class CreateOrderRollback {

        @Test
        @DisplayName("주문 생성 중 실패하면 Atomic Update로 차감된 재고가 롤백된다")
        fun stockRollbackOnOrderFailure() {
            // arrange
            val headers = signUpAndGetHeaders()
            val brandId = createBrandAndGetId()
            val productId = createProductAndGetId(brandId, stockQuantity = 10)

            // 브랜드를 soft delete → 주문 생성 시 브랜드 조회 실패 유도
            // 흐름: findByIds(products) → decreaseStock() → brandService.findByIds() → 빈 리스트
            //       → orderDomainService에서 "브랜드 정보가 없습니다" 예외 → TX 롤백
            deleteBrand(brandId)

            val request = mapOf(
                "items" to listOf(
                    mapOf("productId" to productId, "quantity" to 3),
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert — 주문 실패
            assertThat(response.statusCode).isNotEqualTo(HttpStatus.OK)

            // verify — 재고가 원래대로 10개 (롤백 확인)
            val stock = getProductStock(productId)
            assertThat(stock).isEqualTo(10)
        }
    }
}
