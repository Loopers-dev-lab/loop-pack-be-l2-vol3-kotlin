package com.loopers.interfaces.api

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
class OrderV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api/v1/orders"
        private const val ADMIN_BRAND_ENDPOINT = "/api-admin/v1/brands"
        private const val ADMIN_PRODUCT_ENDPOINT = "/api-admin/v1/products"
        private const val MEMBER_ENDPOINT = "/api/v1/members"
        private const val HEADER_LDAP = "X-Loopers-Ldap"
        private const val LDAP_VALUE = "loopers.admin"
        private const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
        private const val HEADER_LOGIN_PW = "X-Loopers-LoginPw"
    }

    private var productId: Long = 0
    private val loginId = "orderuser"
    private val password = "Password1!"

    @BeforeEach
    fun setUp() {
        // Register member
        val memberRequest = MemberV1Dto.RegisterRequest(
            loginId = loginId,
            password = password,
            name = "주문자",
            birthday = LocalDate.of(2000, 1, 1),
            email = "order@example.com",
        )
        testRestTemplate.exchange(
            MEMBER_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(memberRequest),
            object : ParameterizedTypeReference<ApiResponse<Void>>() {},
        )

        // Create brand
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

        // Create product
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
        productId = productResponse.body!!.data!!.id
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

    private fun createOrderRequest() = OrderV1Dto.CreateRequest(
        items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = productId, quantity = 2)),
    )

    private fun createOrderViaApi(): OrderV1Dto.OrderResponse {
        val response = testRestTemplate.exchange(
            ENDPOINT,
            HttpMethod.POST,
            HttpEntity(createOrderRequest(), memberHeaders()),
            object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {},
        )
        return response.body!!.data!!
    }

    @DisplayName("POST /api/v1/orders (주문 생성)")
    @Nested
    inner class CreateOrder {
        @DisplayName("유효한 정보로 주문하면, 201 CREATED 응답을 받는다.")
        @Test
        fun returns201_whenValidOrder() {
            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(createOrderRequest(), memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.totalAmount).isEqualTo(78000L) },
                { assertThat(response.body?.data?.items).hasSize(1) },
                { assertThat(response.body?.data?.items?.get(0)?.productName).isEqualTo("감성 티셔츠") },
                { assertThat(response.body?.data?.items?.get(0)?.brandName).isEqualTo("루퍼스") },
            )
        }

        @DisplayName("인증 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returns401_whenNotAuthenticated() {
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(createOrderRequest()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("재고가 부족하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returns400_whenInsufficientStock() {
            // arrange — 재고(100)보다 많은 수량으로 주문
            val request = OrderV1Dto.CreateRequest(
                items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = productId, quantity = 101)),
            )

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("존재하지 않는 상품이 포함되면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returns404_whenProductNotFound() {
            // arrange
            val request = OrderV1Dto.CreateRequest(
                items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = 999L, quantity = 1)),
            )

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("주문 항목이 비어있으면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returns400_whenItemsEmpty() {
            // arrange
            val request = OrderV1Dto.CreateRequest(items = emptyList())

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("주문 수량이 0이면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returns400_whenQuantityIsZero() {
            // arrange
            val request = OrderV1Dto.CreateRequest(
                items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = productId, quantity = 0)),
            )

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("GET /api/v1/orders/{orderId} (주문 상세 조회)")
    @Nested
    inner class GetOrder {
        @DisplayName("본인 주문을 조회하면, 200 OK 응답을 받는다.")
        @Test
        fun returns200_whenOwner() {
            // arrange
            val created = createOrderViaApi()

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT/${created.orderId}",
                HttpMethod.GET,
                HttpEntity<Any>(memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.totalAmount).isEqualTo(78000L) },
            )
        }
    }

    @DisplayName("GET /api/v1/orders (주문 목록 조회)")
    @Nested
    inner class GetOrders {
        @DisplayName("기간 내 주문을 조회하면, 주문 목록을 반환한다.")
        @Test
        fun returnsOrders_whenInDateRange() {
            // arrange
            createOrderViaApi()
            val today = LocalDate.now()
            val startAt = today.minusDays(1)
            val endAt = today.plusDays(1)

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT?startAt=$startAt&endAt=$endAt",
                HttpMethod.GET,
                HttpEntity<Any>(memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<List<OrderV1Dto.OrderResponse>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull() },
            )
        }
    }
}
