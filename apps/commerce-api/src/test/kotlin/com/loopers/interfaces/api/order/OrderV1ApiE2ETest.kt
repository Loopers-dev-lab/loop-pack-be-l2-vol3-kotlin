package com.loopers.interfaces.api.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.user.User
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import com.loopers.interfaces.api.PageResponse
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.math.BigDecimal
import kotlin.test.Test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val orderJpaRepository: OrderJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val passwordEncoder: PasswordEncoder,
) {
    companion object {
        private const val ENDPOINT_ORDERS = "/api/v1/orders"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createAuthHeaders(loginId: String, password: String): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", password)
        }
    }

    @DisplayName("POST /api/v1/orders")
    @Nested
    inner class CreateOrder {

        @Test
        @DisplayName("유효한 주문 정보를 전달하면, 주문을 생성하고 201 CREATED 응답을 받는다")
        fun createOrderWithValidInfo() {
            // given
            val plainPassword = "encryptedPassword"
            val user = User.create(
                loginId = LoginId.of("test123"),
                password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
                name = Name.of("테스트"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("test@test.com"),
            )
            val savedUser = userJpaRepository.save(user)

            val brand = Brand.create(
                name = "테스트 브랜드",
                description = "테스트 브랜드 설명",
            )
            val savedBrand = brandJpaRepository.save(brand)

            val product = Product.create(
                brand = savedBrand,
                name = "테스트 상품",
                price = BigDecimal("10000"),
                stock = 10,
                status = ProductStatus.ACTIVE,
            )
            val savedProduct = productJpaRepository.save(product)

            val request = OrderV1Dto.OrderRequest(
                orderItems = listOf(
                    OrderV1Dto.OrderItemRequest(
                        productId = savedProduct.id,
                        quantity = 2,
                    ),
                ),
            )

            val headers = createAuthHeaders("test123", "encryptedPassword")

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.CREATED, response.statusCode) },
                { assertThat(response.body?.data).isNotNull },
            )

            val savedOrder = orderJpaRepository.findById(response.body?.data!!)
            assertThat(savedOrder).isPresent
            assertThat(savedOrder.get().userId).isEqualTo(savedUser.id)
        }

        @Test
        @DisplayName("주문 상품이 비어있으면 400 BAD_REQUEST 응답을 받는다")
        fun createOrderWithEmptyItems() {
            // given
            val plainPassword = "encryptedPassword"
            val user = User.create(
                loginId = LoginId.of("test123"),
                password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
                name = Name.of("테스트"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("test@test.com"),
            )
            userJpaRepository.save(user)

            val request = OrderV1Dto.OrderRequest(
                orderItems = emptyList(),
            )

            val headers = createAuthHeaders("test123", "encryptedPassword")

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // then
            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        @DisplayName("인증 헤더 없이 요청하면 401 Unauthorized 응답을 받는다")
        fun createOrderWithoutAuthHeader() {
            // given
            val request = OrderV1Dto.OrderRequest(
                orderItems = listOf(
                    OrderV1Dto.OrderItemRequest(
                        productId = 1L,
                        quantity = 1,
                    ),
                ),
            )

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                ENDPOINT_ORDERS,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // then
            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        }
    }

    @DisplayName("GET /api/v1/orders")
    @Nested
    inner class GetOrders {

        @Test
        @DisplayName("사용자의 주문 목록을 페이징으로 조회할 수 있다")
        fun getOrdersWithPagination() {
            // given
            val plainPassword = "encryptedPassword"
            val user = User.create(
                loginId = LoginId.of("test123"),
                password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
                name = Name.of("테스트"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("test@test.com"),
            )
            val savedUser = userJpaRepository.save(user)

            val order1 = Order.create(savedUser.id)
            val savedOrder1 = orderJpaRepository.save(order1)
            val orderItem1 = OrderItem.create(
                orderId = savedOrder1.id,
                productId = 1L,
                quantity = 1,
                price = BigDecimal("10000"),
                productName = "상품1",
            )
            savedOrder1.addOrderItem(orderItem1)
            orderJpaRepository.save(savedOrder1)

            val order2 = Order.create(savedUser.id)
            val savedOrder2 = orderJpaRepository.save(order2)
            val orderItem2 = OrderItem.create(
                orderId = savedOrder2.id,
                productId = 2L,
                quantity = 2,
                price = BigDecimal("20000"),
                productName = "상품2",
            )
            savedOrder2.addOrderItem(orderItem2)
            orderJpaRepository.save(savedOrder2)

            val headers = createAuthHeaders("test123", "encryptedPassword")

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<OrderV1Dto.OrderResponse>>>() {}
            val response = restTemplate.exchange(
                "$ENDPOINT_ORDERS?page=0&size=10",
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.OK, response.statusCode) },
                { assertThat(response.body?.data?.content).hasSize(2) },
            )
        }

        @Test
        @DisplayName("인증 헤더 없이 요청하면 401 Unauthorized 응답을 받는다")
        fun getOrdersWithoutAuthHeader() {
            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                "$ENDPOINT_ORDERS?page=0&size=10",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                responseType,
            )

            // then
            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        }
    }

    @DisplayName("GET /api/v1/orders/{orderId}")
    @Nested
    inner class GetOrder {

        @Test
        @DisplayName("특정 주문을 조회할 수 있다")
        fun getOrderById() {
            // given
            val plainPassword = "encryptedPassword"
            val user = User.create(
                loginId = LoginId.of("test123"),
                password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
                name = Name.of("테스트"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("test@test.com"),
            )
            val savedUser = userJpaRepository.save(user)

            val order = Order.create(savedUser.id)
            val savedOrder = orderJpaRepository.save(order)
            val orderItem = OrderItem.create(
                orderId = savedOrder.id,
                productId = 1L,
                quantity = 1,
                price = BigDecimal("10000"),
                productName = "상품1",
            )
            savedOrder.addOrderItem(orderItem)
            orderJpaRepository.save(savedOrder)

            val headers = createAuthHeaders("test123", "encryptedPassword")

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = restTemplate.exchange(
                "$ENDPOINT_ORDERS/${order.id}",
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // then
            assertAll(
                { assertEquals(HttpStatus.OK, response.statusCode) },
                { assertThat(response.body?.data?.orderId).isEqualTo(order.id) },
                { assertThat(response.body?.data?.orderItems).hasSize(1) },
            )
        }

        @Test
        @DisplayName("존재하지 않는 주문을 조회하면 404 NOT_FOUND 응답을 받는다")
        fun getOrderByIdNotFound() {
            // given
            val plainPassword = "encryptedPassword"
            val user = User.create(
                loginId = LoginId.of("test123"),
                password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
                name = Name.of("테스트"),
                birthDate = BirthDate.of("20260101"),
                email = Email.of("test@test.com"),
            )
            userJpaRepository.save(user)

            val headers = createAuthHeaders("test123", "encryptedPassword")

            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                "$ENDPOINT_ORDERS/99999",
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // then
            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        }

        @Test
        @DisplayName("인증 헤더 없이 요청하면 401 Unauthorized 응답을 받는다")
        fun getOrderByIdWithoutAuthHeader() {
            // when
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = restTemplate.exchange(
                "$ENDPOINT_ORDERS/1",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                responseType,
            )

            // then
            assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        }
    }
}
