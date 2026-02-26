package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.brand.BrandV1Dto
import com.loopers.interfaces.api.product.ProductAdminV1Dto
import com.loopers.interfaces.api.user.UserV1Dto
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
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderAdminV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        private const val TEST_LOGIN_ID = "testuser1"
        private const val TEST_PASSWORD = "Password1!"
        private const val ADMIN_LDAP = "loopers.admin"
    }

    private var testOrderId: Long = 0

    @BeforeEach
    fun setUp() {
        createTestUser()
        val brandId = createTestBrand()!!
        val productId = createTestProduct(brandId)!!
        testOrderId = createTestOrder(productId)!!
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun authHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", TEST_LOGIN_ID)
            set("X-Loopers-LoginPw", TEST_PASSWORD)
            set("Content-Type", "application/json")
        }
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-Ldap", ADMIN_LDAP)
        }
    }

    private fun createTestUser() {
        val request = UserV1Dto.SignUpRequest(
            loginId = TEST_LOGIN_ID,
            password = TEST_PASSWORD,
            name = "테스트유저",
            birthDate = LocalDate.of(1990, 1, 15),
            email = "test@example.com",
        )
        testRestTemplate.exchange(
            "/api/v1/users",
            HttpMethod.POST,
            HttpEntity(request),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
    }

    private fun createTestBrand(): Long? {
        val headers = HttpHeaders().apply {
            set("X-Loopers-Ldap", ADMIN_LDAP)
            set("Content-Type", "application/json")
        }
        val request = BrandV1Dto.CreateRequest(name = "나이키", description = "스포츠 브랜드")
        val response = testRestTemplate.exchange(
            "/api-admin/v1/brands",
            HttpMethod.POST,
            HttpEntity(request, headers),
            object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandAdminResponse>>() {},
        )
        return response.body?.data?.id
    }

    private fun createTestProduct(brandId: Long): Long? {
        val headers = HttpHeaders().apply {
            set("X-Loopers-Ldap", ADMIN_LDAP)
            set("Content-Type", "application/json")
        }
        val request = ProductAdminV1Dto.CreateRequest(
            brandId = brandId,
            name = "에어맥스 90",
            price = BigDecimal("129000"),
            stock = 100,
            description = "나이키 에어맥스 90",
            imageUrl = null,
        )
        val response = testRestTemplate.exchange(
            "/api-admin/v1/products",
            HttpMethod.POST,
            HttpEntity(request, headers),
            object : ParameterizedTypeReference<ApiResponse<ProductAdminV1Dto.ProductAdminResponse>>() {},
        )
        return response.body?.data?.id
    }

    private fun createTestOrder(productId: Long): Long? {
        val request = OrderV1Dto.CreateRequest(
            items = listOf(OrderV1Dto.OrderItemRequest(productId = productId, quantity = 2)),
        )
        val response = testRestTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.POST,
            HttpEntity(request, authHeaders()),
            object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
        )

        @Suppress("UNCHECKED_CAST")
        val data = response.body?.data as? Map<String, Any> ?: return null
        return (data["orderId"] as Number).toLong()
    }

    @DisplayName("GET /api-admin/v1/orders")
    @Nested
    inner class GetAllOrders {

        @DisplayName("전체 주문 목록을 조회하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenQueryAllOrders() {
            // act
            val response = testRestTemplate.exchange(
                "/api-admin/v1/orders?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull() },
            )
        }
    }

    @DisplayName("GET /api-admin/v1/orders/{orderId}")
    @Nested
    inner class GetOrder {

        @DisplayName("주문 상세를 조회하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenQueryOrderDetail() {
            // act
            val response = testRestTemplate.exchange(
                "/api-admin/v1/orders/$testOrderId",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull() },
            )
        }
    }
}
