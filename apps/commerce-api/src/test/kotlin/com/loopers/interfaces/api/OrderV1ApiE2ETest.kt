package com.loopers.interfaces.api

import com.loopers.interfaces.api.admin.brand.AdminBrandRegisterRequest
import com.loopers.interfaces.api.admin.brand.AdminBrandResponse
import com.loopers.interfaces.api.admin.product.AdminProductRegisterRequest
import com.loopers.interfaces.api.admin.product.AdminProductResponse
import com.loopers.interfaces.api.order.OrderCreateRequest
import com.loopers.interfaces.api.order.OrderCreateResponse
import com.loopers.interfaces.api.order.OrderDetailResponse
import com.loopers.interfaces.api.order.OrderItemRequest
import com.loopers.interfaces.api.order.OrderSummaryResponse
import com.loopers.interfaces.api.user.UserV1Dto
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
import com.loopers.support.constant.AuthHeaders
import com.loopers.support.error.OrderErrorCode
import com.loopers.support.error.UserErrorCode
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
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestContainersConfig::class)
class OrderV1ApiE2ETest @Autowired constructor(
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

    private fun deleteProductViaAdmin(productId: Long) {
        testRestTemplate.exchange(
            "${ApiPaths.AdminProducts.BASE}/$productId",
            HttpMethod.DELETE,
            HttpEntity<Void>(adminHeaders()),
            ApiResponse::class.java,
        )
    }

    private fun createOrder(
        items: List<OrderItemRequest>,
        headers: HttpHeaders = userHeaders(),
    ): OrderCreateResponse? {
        val request = OrderCreateRequest(items = items)
        val responseType = object : ParameterizedTypeReference<ApiResponse<OrderCreateResponse>>() {}
        val response = testRestTemplate.exchange(
            ApiPaths.Orders.BASE,
            HttpMethod.POST,
            HttpEntity(request, headers),
            responseType,
        )
        return response.body?.data
    }

    @DisplayName("POST /api/v1/orders - 주문 생성")
    @Nested
    inner class CreateOrder {

        @DisplayName("정상 주문 시 201 CREATED를 반환한다")
        @Test
        fun success() {
            registerUser()
            val brand = registerBrandViaAdmin()
            val product = registerProductViaAdmin(brand.id, price = 10000, stock = 50)

            val request = OrderCreateRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 3)),
            )
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderCreateResponse>>() {}
            val response = testRestTemplate.exchange(
                ApiPaths.Orders.BASE,
                HttpMethod.POST,
                HttpEntity(request, userHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.totalAmount).isEqualTo(30000) },
            )
        }

        @DisplayName("미존재 상품 포함 시 400을 반환한다")
        @Test
        fun failWhenProductNotFound() {
            registerUser()

            val request = OrderCreateRequest(
                items = listOf(OrderItemRequest(productId = 999L, quantity = 1)),
            )
            val response = testRestTemplate.exchange(
                ApiPaths.Orders.BASE,
                HttpMethod.POST,
                HttpEntity(request, userHeaders()),
                ApiResponse::class.java,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(OrderErrorCode.ORDER_VALIDATION_FAILED.code) },
            )
        }

        @DisplayName("삭제된 상품 포함 시 400을 반환한다")
        @Test
        fun failWhenProductDeleted() {
            registerUser()
            val brand = registerBrandViaAdmin()
            val product = registerProductViaAdmin(brand.id)
            deleteProductViaAdmin(product.id)

            val request = OrderCreateRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
            )
            val response = testRestTemplate.exchange(
                ApiPaths.Orders.BASE,
                HttpMethod.POST,
                HttpEntity(request, userHeaders()),
                ApiResponse::class.java,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(OrderErrorCode.ORDER_VALIDATION_FAILED.code) },
            )
        }

        @DisplayName("재고 부족 시 400을 반환한다")
        @Test
        fun failWhenInsufficientStock() {
            registerUser()
            val brand = registerBrandViaAdmin()
            val product = registerProductViaAdmin(brand.id, stock = 5)

            val request = OrderCreateRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 10)),
            )
            val response = testRestTemplate.exchange(
                ApiPaths.Orders.BASE,
                HttpMethod.POST,
                HttpEntity(request, userHeaders()),
                ApiResponse::class.java,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(OrderErrorCode.ORDER_VALIDATION_FAILED.code) },
            )
        }

        @DisplayName("중복 상품 시 400을 반환한다")
        @Test
        fun failWhenDuplicateProduct() {
            registerUser()
            val brand = registerBrandViaAdmin()
            val product = registerProductViaAdmin(brand.id)

            val request = OrderCreateRequest(
                items = listOf(
                    OrderItemRequest(productId = product.id, quantity = 1),
                    OrderItemRequest(productId = product.id, quantity = 2),
                ),
            )
            val response = testRestTemplate.exchange(
                ApiPaths.Orders.BASE,
                HttpMethod.POST,
                HttpEntity(request, userHeaders()),
                ApiResponse::class.java,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(OrderErrorCode.DUPLICATE_ORDER_ITEM.code) },
            )
        }

        @DisplayName("인증 실패 시 401을 반환한다")
        @Test
        fun failWhenNotAuthenticated() {
            val request = OrderCreateRequest(
                items = listOf(OrderItemRequest(productId = 1L, quantity = 1)),
            )
            val response = testRestTemplate.exchange(
                ApiPaths.Orders.BASE,
                HttpMethod.POST,
                HttpEntity(request),
                ApiResponse::class.java,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(UserErrorCode.AUTHENTICATION_FAILED.code) },
            )
        }
    }

    @DisplayName("GET /api/v1/orders/me - 내 주문 목록 조회")
    @Nested
    inner class GetMyOrders {

        @DisplayName("기간 내 주문 목록을 반환한다")
        @Test
        fun success() {
            registerUser()
            val brand = registerBrandViaAdmin()
            val product = registerProductViaAdmin(brand.id)
            createOrder(listOf(OrderItemRequest(productId = product.id, quantity = 1)))

            val today = LocalDate.now()
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResult<OrderSummaryResponse>>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.Orders.ME}?startDate=${today.minusDays(1)}&endDate=$today&page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Void>(userHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(1) },
            )
        }

        @DisplayName("인증 실패 시 401을 반환한다")
        @Test
        fun failWhenNotAuthenticated() {
            val today = LocalDate.now()
            val response = testRestTemplate.getForEntity(
                "${ApiPaths.Orders.ME}?startDate=${today.minusDays(1)}&endDate=$today&page=0&size=20",
                ApiResponse::class.java,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(UserErrorCode.AUTHENTICATION_FAILED.code) },
            )
        }
    }

    @DisplayName("GET /api/v1/orders/me/{orderId} - 내 주문 상세 조회")
    @Nested
    inner class GetMyOrder {

        @DisplayName("정상 조회 시 스냅샷 데이터를 반환한다")
        @Test
        fun success() {
            registerUser()
            val brand = registerBrandViaAdmin()
            val product = registerProductViaAdmin(brand.id, name = "에어맥스", price = 15000)
            val order = createOrder(listOf(OrderItemRequest(productId = product.id, quantity = 2)))

            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderDetailResponse>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.Orders.ME}/${order?.orderId}",
                HttpMethod.GET,
                HttpEntity<Void>(userHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.totalAmount).isEqualTo(30000) },
                { assertThat(response.body?.data?.items).hasSize(1) },
                { assertThat(response.body?.data?.items?.get(0)?.productName).isEqualTo("에어맥스") },
            )
        }

        @DisplayName("타 유저 주문 접근 시 403을 반환한다")
        @Test
        fun failWhenAccessDenied() {
            registerUser("user1")
            registerUser("user2")
            val brand = registerBrandViaAdmin()
            val product = registerProductViaAdmin(brand.id)
            val order = createOrder(
                listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                headers = userHeaders("user1"),
            )

            val response = testRestTemplate.exchange(
                "${ApiPaths.Orders.ME}/${order?.orderId}",
                HttpMethod.GET,
                HttpEntity<Void>(userHeaders("user2")),
                ApiResponse::class.java,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(OrderErrorCode.ORDER_ACCESS_DENIED.code) },
            )
        }

        @DisplayName("존재하지 않는 주문 시 404를 반환한다")
        @Test
        fun failWhenOrderNotFound() {
            registerUser()

            val response = testRestTemplate.exchange(
                "${ApiPaths.Orders.ME}/999",
                HttpMethod.GET,
                HttpEntity<Void>(userHeaders()),
                ApiResponse::class.java,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(OrderErrorCode.ORDER_NOT_FOUND.code) },
            )
        }
    }
}
