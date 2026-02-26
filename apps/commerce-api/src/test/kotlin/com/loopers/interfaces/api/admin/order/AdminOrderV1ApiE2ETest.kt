package com.loopers.interfaces.api.admin.order

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.admin.brand.AdminBrandV1Dto
import com.loopers.interfaces.api.admin.product.AdminProductV1Dto
import com.loopers.interfaces.api.member.MemberV1Dto
import com.loopers.interfaces.api.order.OrderV1Dto
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
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminOrderV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api-admin/v1/orders"
        private const val ORDER_ENDPOINT = "/api/v1/orders"
        private const val ADMIN_BRAND_ENDPOINT = "/api-admin/v1/brands"
        private const val ADMIN_PRODUCT_ENDPOINT = "/api-admin/v1/products"
        private const val MEMBER_ENDPOINT = "/api/v1/members"
        private const val HEADER_LDAP = "X-Loopers-Ldap"
        private const val LDAP_VALUE = "loopers.admin"
        private const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
        private const val HEADER_LOGIN_PW = "X-Loopers-LoginPw"
    }

    private var orderId: Long = 0
    private val loginId = "adminorderuser"
    private val password = "Password1!"

    @BeforeEach
    fun setUp() {
        // Register member
        val memberRequest = MemberV1Dto.RegisterRequest(
            loginId = loginId,
            password = password,
            name = "주문자",
            birthday = LocalDate.of(2000, 1, 1),
            email = "adminorder@example.com",
        )
        testRestTemplate.exchange(
            MEMBER_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(memberRequest),
            object : ParameterizedTypeReference<ApiResponse<Void>>() {},
        )

        // Create brand + product
        val brandRequest = AdminBrandV1Dto.CreateRequest(
            name = "루퍼스",
            description = "테스트",
            imageUrl = "https://example.com/brand.jpg",
        )
        val brandResponse = testRestTemplate.exchange(
            ADMIN_BRAND_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(brandRequest, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Dto.BrandResponse>>() {},
        )
        val brandId = brandResponse.body!!.data!!.id

        val productRequest = AdminProductV1Dto.CreateRequest(
            brandId = brandId,
            name = "감성 티셔츠",
            description = "좋은 상품",
            price = 39000,
            stockQuantity = 100,
            imageUrl = "https://example.com/product.jpg",
        )
        val productResponse = testRestTemplate.exchange(
            ADMIN_PRODUCT_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(productRequest, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminProductV1Dto.ProductResponse>>() {},
        )
        val productId = productResponse.body!!.data!!.id

        // Create order via customer API
        val orderRequest = OrderV1Dto.CreateRequest(
            items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = productId, quantity = 2)),
        )
        val orderResponse = testRestTemplate.exchange(
            ORDER_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(orderRequest, memberHeaders()),
            object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {},
        )
        orderId = orderResponse.body!!.data!!.orderId
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply { set(HEADER_LDAP, LDAP_VALUE) }
    }

    private fun memberHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(HEADER_LOGIN_ID, loginId)
            set(HEADER_LOGIN_PW, password)
        }
    }

    @DisplayName("GET /api-admin/v1/orders/{orderId} (어드민 주문 상세 조회)")
    @Nested
    inner class GetOrder {
        @DisplayName("어드민이 주문을 조회하면, 200 OK 응답을 받는다.")
        @Test
        fun returns200_whenAdmin() {
            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT/$orderId",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<AdminOrderV1Dto.OrderResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.totalAmount).isEqualTo(78000L) },
                { assertThat(response.body?.data?.memberId).isNotNull() },
            )
        }

        @DisplayName("인증 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returns401_whenNoAdminHeader() {
            val response = testRestTemplate.exchange(
                "$ENDPOINT/$orderId",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("GET /api-admin/v1/orders (어드민 주문 목록 조회)")
    @Nested
    inner class GetOrders {
        @DisplayName("어드민이 주문 목록을 조회하면, 페이징된 결과를 반환한다.")
        @Test
        fun returnsPaginatedOrders() {
            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }
    }
}
