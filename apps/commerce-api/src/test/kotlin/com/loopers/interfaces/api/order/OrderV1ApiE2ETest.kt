package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderFacade
import com.loopers.application.product.ProductFacade
import com.loopers.domain.brand.BrandService
import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Email
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.Name
import com.loopers.domain.user.PasswordEncryptor
import com.loopers.domain.user.UserModel
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.ApiResponse
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
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandService: BrandService,
    private val productFacade: ProductFacade,
    private val orderFacade: OrderFacade,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ORDERS = "/api/v1/orders"
        private const val ADMIN_ORDERS = "/api-admin/v1/orders"
        private const val LDAP_HEADER = "X-Loopers-Ldap"
        private const val LDAP_VALUE = "loopers.admin"
        private const val TEST_PASSWORD = "Password123!"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createUser(loginId: String = "testuser") = userJpaRepository.save(
        UserModel(
            loginId = LoginId(loginId),
            encryptedPassword = passwordEncryptor.encrypt(TEST_PASSWORD),
            name = Name("홍길동"),
            birthDate = BirthDate("1990-01-01"),
            email = Email("$loginId@example.com"),
        )
    )

    private fun createBrand() = brandService.createBrand(
        name = "Nike",
        logoImageUrl = "logo.png",
        description = "테스트 브랜드",
        zipCode = "12345",
        roadAddress = "서울특별시 중구 테스트길 1",
        detailAddress = "1층",
        email = "nike@google.com",
        phoneNumber = "02-3783-4401",
        businessNumber = "123-45-67890",
    )

    private fun createProduct(brandId: Long, quantity: Long = 100L) = productFacade.createProduct(
        brandId = brandId,
        name = "Air Max",
        imageUrl = "image.png",
        description = "설명",
        price = 50_000L,
        quantity = quantity,
    )

    private fun authHeaders(loginId: String = "testuser") = HttpHeaders().apply {
        set("X-Loopers-LoginId", loginId)
        set("X-Loopers-LoginPw", TEST_PASSWORD)
    }

    private fun adminHeaders() = HttpHeaders().apply {
        set(LDAP_HEADER, LDAP_VALUE)
    }

    @DisplayName("POST /api/v1/orders")
    @Nested
    inner class CreateOrder {

        @Test
        @DisplayName("인증 헤더로 요청하면 200과 OrderResponse를 반환한다")
        fun createOrder_whenValidAuth_thenReturnsOrderResponse() {
            createUser()
            val brand = createBrand()
            val product = createProduct(brand.id)

            val request = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.CreateOrderRequest.OrderItemRequest(productId = product.id, quantity = 2L)),
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ORDERS,
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.totalAmount).isEqualTo(100_000L) },
                { assertThat(response.body?.data?.items).hasSize(1) },
                { assertThat(response.body?.data?.items?.first()?.quantity).isEqualTo(2L) },
            )
        }

        @Test
        @DisplayName("인증 헤더 없이 요청하면 401을 반환한다")
        fun createOrder_whenNoAuth_thenReturns401() {
            val brand = createBrand()
            val product = createProduct(brand.id)

            val request = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.CreateOrderRequest.OrderItemRequest(productId = product.id, quantity = 1L)),
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ORDERS,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        @DisplayName("재고 부족 시 400을 반환한다")
        fun createOrder_whenInsufficientStock_thenReturns400() {
            createUser()
            val brand = createBrand()
            val product = createProduct(brand.id, quantity = 1L)

            val request = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.CreateOrderRequest.OrderItemRequest(productId = product.id, quantity = 5L)),
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                ORDERS,
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("GET /api/v1/orders")
    @Nested
    inner class GetOrders {

        @Test
        @DisplayName("startAt/endAt 파라미터와 인증 헤더로 요청하면 200과 주문 목록을 반환한다")
        fun getOrders_whenValidAuth_thenReturnsOrderList() {
            createUser()
            val brand = createBrand()
            val product = createProduct(brand.id, quantity = 100L)

            orderFacade.createOrder(
                loginId = "testuser",
                items = listOf(OrderFacade.OrderItemRequest(productId = product.id, quantity = 1L)),
                couponId = null,
            )

            val startAt = LocalDate.now().minusDays(1)
            val endAt = LocalDate.now().plusDays(1)

            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$ORDERS?startAt=$startAt&endAt=$endAt",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull },
            )
        }
    }

    @DisplayName("GET /api/v1/orders/{id}")
    @Nested
    inner class GetOrderById {

        @Test
        @DisplayName("본인 주문 조회 시 200과 OrderResponse를 반환한다")
        fun getOrderById_whenOwner_thenReturnsOrder() {
            createUser()
            val brand = createBrand()
            val product = createProduct(brand.id)

            val created = orderFacade.createOrder(
                loginId = "testuser",
                items = listOf(OrderFacade.OrderItemRequest(productId = product.id, quantity = 1L)),
                couponId = null,
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ORDERS/${created.id}",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(created.id) },
            )
        }

        @Test
        @DisplayName("타인 주문 조회 시 403을 반환한다")
        fun getOrderById_whenNotOwner_thenReturns403() {
            createUser("testuser")
            createUser("otheruser")
            val brand = createBrand()
            val product = createProduct(brand.id)

            val otherOrder = orderFacade.createOrder(
                loginId = "otheruser",
                items = listOf(OrderFacade.OrderItemRequest(productId = product.id, quantity = 1L)),
                couponId = null,
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ORDERS/${otherOrder.id}",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders("testuser")),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }
    }

    @DisplayName("GET /api-admin/v1/orders")
    @Nested
    inner class GetAllOrders {

        @Test
        @DisplayName("LDAP 헤더로 요청하면 200과 전체 주문 목록을 반환한다")
        fun getAllOrders_whenLdapHeader_thenReturnsAllOrders() {
            createUser()
            val brand = createBrand()
            val product = createProduct(brand.id)
            orderFacade.createOrder(
                loginId = "testuser",
                items = listOf(OrderFacade.OrderItemRequest(productId = product.id, quantity = 1L)),
                couponId = null,
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_ORDERS,
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull },
            )
        }
    }

    @DisplayName("GET /api-admin/v1/orders/{id}")
    @Nested
    inner class GetAdminOrderById {

        @Test
        @DisplayName("LDAP 헤더로 요청하면 200과 OrderResponse를 반환한다")
        fun getAdminOrderById_whenLdapHeader_thenReturnsOrder() {
            createUser()
            val brand = createBrand()
            val product = createProduct(brand.id)

            val created = orderFacade.createOrder(
                loginId = "testuser",
                items = listOf(OrderFacade.OrderItemRequest(productId = product.id, quantity = 1L)),
                couponId = null,
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderAdminV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ORDERS/${created.id}",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(created.id) },
            )
        }
    }
}
