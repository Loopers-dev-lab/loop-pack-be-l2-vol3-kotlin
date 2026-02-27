package com.loopers.interfaces.api

import com.loopers.domain.brand.Brand
import com.loopers.domain.order.Order
import com.loopers.domain.order.OrderItem
import com.loopers.domain.product.Product
import com.loopers.domain.user.User
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.order.OrderJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
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
class OrderV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val orderJpaRepository: OrderJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private lateinit var userHeaders: HttpHeaders
    private lateinit var user: User
    private lateinit var product1: Product
    private lateinit var product2: Product

    companion object {
        private const val PASSWORD = "abcd1234"
    }

    @BeforeEach
    fun setUp() {
        user = userJpaRepository.save(User(loginId = "testuser1", password = PASSWORD, name = "테스트유저", birth = "2000-01-01", email = "test@test.com"))
        val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        product1 = productJpaRepository.save(Product(brandId = brand.id, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100))
        product2 = productJpaRepository.save(Product(brandId = brand.id, name = "에어포스", description = "스니커즈", price = 119000, stockQuantity = 50))
        userHeaders = HttpHeaders()
        userHeaders.set("X-Loopers-LoginId", "testuser1")
        userHeaders.set("X-Loopers-LoginPw", PASSWORD)
    }

    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /api/v1/orders")
    @Nested
    inner class CreateOrder {
        @DisplayName("유효한 주문 요청이 주어지면, 주문을 생성하고 201 응답을 반환한다.")
        @Test
        fun createsOrder_whenValidRequest() {
            // arrange
            val req = OrderV1Dto.CreateOrderRequest(
                items = listOf(
                    OrderV1Dto.OrderItemRequest(productId = product1.id, quantity = 2),
                    OrderV1Dto.OrderItemRequest(productId = product2.id, quantity = 1),
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange("/api/v1/orders", HttpMethod.POST, HttpEntity(req, userHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.totalPrice).isEqualTo(139000 * 2 + 119000) },
                { assertThat(response.body?.data?.items).hasSize(2) },
            )
        }

        @DisplayName("인증되지 않은 요청은 404 응답을 받는다.")
        @Test
        fun returnsNotFound_whenNotAuthenticated() {
            // arrange
            val req = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.OrderItemRequest(productId = product1.id, quantity = 1)),
            )
            val headers = HttpHeaders()
            headers.set("X-Loopers-LoginId", "wronguser")
            headers.set("X-Loopers-LoginPw", "wrongpass1")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange("/api/v1/orders", HttpMethod.POST, HttpEntity(req, headers), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("재고가 부족하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenStockInsufficient() {
            // arrange
            val req = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.OrderItemRequest(productId = product1.id, quantity = 999)),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange("/api/v1/orders", HttpMethod.POST, HttpEntity(req, userHeaders), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("GET /api/v1/orders")
    @Nested
    inner class GetOrders {
        @DisplayName("날짜 범위 내 주문 목록을 조회한다.")
        @Test
        fun returnsOrderList_whenOrdersExistInDateRange() {
            // arrange
            orderJpaRepository.save(
                Order(
                    userId = user.id,
                    items = listOf(OrderItem(productId = product1.id, productName = "에어맥스", productPrice = 139000, quantity = 1)),
                ),
            )
            val today = LocalDate.now()
            val startAt = today.minusDays(1)
            val endAt = today.plusDays(1)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<List<OrderV1Dto.OrderResponse>>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/orders?startAt=$startAt&endAt=$endAt",
                HttpMethod.GET,
                HttpEntity<Any>(Unit, userHeaders),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data).hasSize(1) },
            )
        }
    }

    @DisplayName("GET /api/v1/orders/{orderId}")
    @Nested
    inner class GetOrder {
        @DisplayName("존재하는 주문 ID를 주면, 주문 상세 정보를 반환한다.")
        @Test
        fun returnsOrderInfo_whenOrderExists() {
            // arrange
            val order = orderJpaRepository.save(
                Order(
                    userId = user.id,
                    items = listOf(OrderItem(productId = product1.id, productName = "에어맥스", productPrice = 139000, quantity = 2)),
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange("/api/v1/orders/${order.id}", HttpMethod.GET, HttpEntity<Any>(Unit, userHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isEqualTo(order.id) },
                { assertThat(response.body?.data?.totalPrice).isEqualTo(139000 * 2L) },
                { assertThat(response.body?.data?.items).hasSize(1) },
            )
        }

        @DisplayName("존재하지 않는 주문 ID를 주면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun throwsNotFound_whenOrderNotExists() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange("/api/v1/orders/999", HttpMethod.GET, HttpEntity<Any>(Unit, userHeaders), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
