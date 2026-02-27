package com.loopers.interfaces.api

import com.loopers.interfaces.api.admin.brand.AdminBrandRegisterRequest
import com.loopers.interfaces.api.admin.brand.AdminBrandResponse
import com.loopers.interfaces.api.admin.order.AdminOrderDetailResponse
import com.loopers.interfaces.api.admin.order.AdminOrderSummaryResponse
import com.loopers.interfaces.api.admin.product.AdminProductRegisterRequest
import com.loopers.interfaces.api.admin.product.AdminProductResponse
import com.loopers.interfaces.api.order.OrderCreateRequest
import com.loopers.interfaces.api.order.OrderCreateResponse
import com.loopers.interfaces.api.order.OrderItemRequest
import com.loopers.interfaces.api.user.UserV1Dto
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
import com.loopers.support.constant.AuthHeaders
import com.loopers.support.error.OrderErrorCode
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
class AdminOrderV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private val testLoginId = "testuser"
    private val testPassword = "Test123!"

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(AuthHeaders.Admin.LDAP, AuthHeaders.Admin.LDAP_VALUE)
        }
    }

    private fun userHeaders(loginId: String = testLoginId, password: String = testPassword): HttpHeaders {
        return HttpHeaders().apply {
            set(AuthHeaders.User.LOGIN_ID, loginId)
            set(AuthHeaders.User.LOGIN_PW, password)
        }
    }

    private fun registerUser(loginId: String = testLoginId, password: String = testPassword) {
        val request = UserV1Dto.RegisterRequest(
            loginId = loginId,
            password = password,
            name = "홍길동",
            birthDate = "1990-01-01",
            email = "$loginId@example.com",
        )
        testRestTemplate.postForEntity(ApiPaths.Users.REGISTER, request, Any::class.java)
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
        return requireNotNull(response.body?.data)
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
        return requireNotNull(response.body?.data)
    }

    private fun createOrderAsUser(
        productId: Long,
        quantity: Int = 1,
        loginId: String = testLoginId,
    ): OrderCreateResponse? {
        val request = OrderCreateRequest(
            items = listOf(OrderItemRequest(productId = productId, quantity = quantity)),
        )
        val responseType = object : ParameterizedTypeReference<ApiResponse<OrderCreateResponse>>() {}
        val response = testRestTemplate.exchange(
            ApiPaths.Orders.BASE,
            HttpMethod.POST,
            HttpEntity(request, userHeaders(loginId)),
            responseType,
        )
        return response.body?.data
    }

    @DisplayName("GET /api/admin/v1/orders - 전체 주문 목록 조회")
    @Nested
    inner class GetAllOrders {

        @DisplayName("전체 주문을 페이지네이션으로 반환한다")
        @Test
        fun success() {
            registerUser()
            val brand = registerBrandViaAdmin()
            val product1 = registerProductViaAdmin(brand.id, name = "상품1")
            val product2 = registerProductViaAdmin(brand.id, name = "상품2")
            createOrderAsUser(product1.id)
            createOrderAsUser(product2.id)

            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResult<AdminOrderSummaryResponse>>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminOrders.BASE}?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(2) },
            )
        }

        @DisplayName("주문이 없으면 빈 목록을 반환한다")
        @Test
        fun returnsEmpty() {
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResult<AdminOrderSummaryResponse>>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminOrders.BASE}?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).isEmpty() },
            )
        }
    }

    @DisplayName("GET /api/admin/v1/orders/{orderId} - 주문 상세 조회")
    @Nested
    inner class GetOrderDetail {

        @DisplayName("정상 조회 시 주문자 정보를 포함하여 반환한다")
        @Test
        fun success() {
            registerUser()
            val brand = registerBrandViaAdmin()
            val product = registerProductViaAdmin(brand.id, name = "에어맥스", price = 15000)
            val order = createOrderAsUser(product.id, quantity = 2)

            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminOrderDetailResponse>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminOrders.BASE}/${order?.orderId}",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.userId).isNotNull() },
                { assertThat(response.body?.data?.totalAmount).isEqualTo(30000) },
                { assertThat(response.body?.data?.items).hasSize(1) },
            )
        }

        @DisplayName("존재하지 않는 주문 시 404를 반환한다")
        @Test
        fun failWhenOrderNotFound() {
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminOrders.BASE}/999",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                ApiResponse::class.java,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(OrderErrorCode.ORDER_NOT_FOUND.code) },
            )
        }
    }
}
