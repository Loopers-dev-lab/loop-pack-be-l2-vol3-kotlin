package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.brand.BrandV1Dto
import com.loopers.interfaces.api.coupon.CouponAdminV1Dto
import com.loopers.interfaces.api.product.ProductAdminV1Dto
import com.loopers.interfaces.api.user.UserV1Dto
import com.loopers.infrastructure.user.UserJpaRepository
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
import com.loopers.domain.coupon.CouponType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        private const val TEST_LOGIN_ID = "testuser1"
        private const val TEST_PASSWORD = "Password1!"
        private const val ADMIN_LDAP = "loopers.admin"
        private var brandCounter = 0
    }

    private var testUserId: Long = 0
    private var testProductId: Long = 0
    private var testBrandId: Long = 0

    @BeforeEach
    fun setUp() {
        brandCounter = 0
        createTestUser()
        testUserId = userJpaRepository.findByLoginId(TEST_LOGIN_ID)!!.id

        testBrandId = createTestBrand("나이키")!!
        testProductId = createTestProduct(testBrandId)!!
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
            set("Content-Type", "application/json")
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

    private fun createTestBrand(name: String = "브랜드${++brandCounter}"): Long? {
        val request = BrandV1Dto.CreateRequest(name = name, description = "스포츠 브랜드")
        val response = testRestTemplate.exchange(
            "/api-admin/v1/brands",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandAdminResponse>>() {},
        )
        return response.body?.data?.id
    }

    private fun createTestProduct(brandId: Long, name: String = "에어맥스 90", stock: Int = 100): Long? {
        val request = ProductAdminV1Dto.CreateRequest(
            brandId = brandId,
            name = name,
            price = BigDecimal("129000"),
            stock = stock,
            description = "나이키 에어맥스 90",
            imageUrl = null,
        )
        val response = testRestTemplate.exchange(
            "/api-admin/v1/products",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<ProductAdminV1Dto.ProductAdminResponse>>() {},
        )
        return response.body?.data?.id
    }

    private fun createTestCoupon(
        type: CouponType = CouponType.FIXED,
        value: BigDecimal = BigDecimal("5000"),
        minOrderAmount: BigDecimal? = BigDecimal("10000"),
    ): Long? {
        val request = CouponAdminV1Dto.CreateRequest(
            name = "테스트 쿠폰",
            type = type,
            value = value,
            minOrderAmount = minOrderAmount,
            expiredAt = ZonedDateTime.now().plusDays(30),
        )
        val response = testRestTemplate.exchange(
            "/api-admin/v1/coupons",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {},
        )
        return response.body?.data?.id
    }

    private fun issueCoupon(couponId: Long): Long? {
        val response = testRestTemplate.exchange(
            "/api/v1/coupons/$couponId/issue",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders()),
            object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
        )

        @Suppress("UNCHECKED_CAST")
        val data = response.body?.data as? Map<String, Any>
        return (data?.get("id") as? Number)?.toLong()
    }

    private fun createOrder(productId: Long, quantity: Int = 2): Map<String, Any>? {
        val request = OrderV1Dto.CreateRequest(
            items = listOf(OrderV1Dto.OrderItemRequest(productId = productId, quantity = quantity)),
        )
        val response = testRestTemplate.exchange(
            "/api/v1/orders",
            HttpMethod.POST,
            HttpEntity(request, authHeaders()),
            object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
        )
        @Suppress("UNCHECKED_CAST")
        return response.body?.data as? Map<String, Any>
    }

    @DisplayName("POST /api/v1/orders")
    @Nested
    inner class CreateOrder {

        @DisplayName("정상적인 요청이면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenCreateOrderSucceeds() {
            // arrange
            val request = OrderV1Dto.CreateRequest(
                items = listOf(OrderV1Dto.OrderItemRequest(productId = testProductId, quantity = 2)),
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull() },
            )
        }

        @DisplayName("일부 상품 재고 부족이면, 부분 주문과 excludedItems를 반환한다.")
        @Test
        fun returnsPartialOrder_whenSomeStockInsufficient() {
            // arrange
            val outOfStockProductId = createTestProduct(testBrandId, name = "재고없는상품", stock = 0)!!
            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.OrderItemRequest(productId = testProductId, quantity = 1),
                    OrderV1Dto.OrderItemRequest(productId = outOfStockProductId, quantity = 1),
                ),
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull() },
            )
        }

        @DisplayName("모든 상품의 재고가 부족하면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenAllStockInsufficient() {
            // arrange
            val outOfStockProductId = createTestProduct(testBrandId, name = "재고없는상품", stock = 0)!!
            val request = OrderV1Dto.CreateRequest(
                items = listOf(OrderV1Dto.OrderItemRequest(productId = outOfStockProductId, quantity = 1)),
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("인증 정보가 잘못되면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenAuthFails() {
            // arrange
            val badHeaders = HttpHeaders().apply {
                set("X-Loopers-LoginId", TEST_LOGIN_ID)
                set("X-Loopers-LoginPw", "WrongPassword1!")
                set("Content-Type", "application/json")
            }
            val request = OrderV1Dto.CreateRequest(
                items = listOf(OrderV1Dto.OrderItemRequest(productId = testProductId, quantity = 1)),
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(request, badHeaders),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("GET /api/v1/orders")
    @Nested
    inner class GetUserOrders {

        @DisplayName("본인의 주문 목록을 조회하면, 200 OK와 목록을 반환한다.")
        @Test
        fun returnsOk_whenQueryOwnOrders() {
            // arrange
            createOrder(testProductId)
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")
            val startAt = LocalDateTime.now().minusDays(1).format(formatter)
            val endAt = LocalDateTime.now().plusDays(1).format(formatter)

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/orders?startAt={startAt}&endAt={endAt}",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<List<Map<String, Any>>>>() {},
                startAt,
                endAt,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull() },
                { assertThat(response.body?.data).hasSize(1) },
            )
        }

        @DisplayName("기간 외 주문은 조회되지 않는다.")
        @Test
        fun returnsEmptyList_whenNoOrdersInPeriod() {
            // arrange
            createOrder(testProductId)
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss")
            val startAt = LocalDateTime.now().minusDays(10).format(formatter)
            val endAt = LocalDateTime.now().minusDays(5).format(formatter)

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/orders?startAt={startAt}&endAt={endAt}",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<List<Map<String, Any>>>>() {},
                startAt,
                endAt,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isEmpty() },
            )
        }
    }

    @DisplayName("GET /api/v1/orders/{orderId}")
    @Nested
    inner class GetOrder {

        @DisplayName("본인의 주문을 상세 조회하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenQueryOwnOrder() {
            // arrange
            val orderData = createOrder(testProductId)!!
            val orderId = (orderData["orderId"] as Number).toLong()

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/orders/$orderId",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull() },
            )
        }

        @DisplayName("타인의 주문을 조회하면, 403 FORBIDDEN을 반환한다.")
        @Test
        fun returnsForbidden_whenQueryOtherUserOrder() {
            // arrange - 다른 유저 생성 후 주문
            val otherLoginId = "testuser2"
            val otherPassword = "Password2!"
            testRestTemplate.exchange(
                "/api/v1/users",
                HttpMethod.POST,
                HttpEntity(
                    UserV1Dto.SignUpRequest(
                        loginId = otherLoginId,
                        password = otherPassword,
                        name = "다른유저",
                        birthDate = LocalDate.of(1995, 5, 20),
                        email = "other@example.com",
                    ),
                ),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            val otherHeaders = HttpHeaders().apply {
                set("X-Loopers-LoginId", otherLoginId)
                set("X-Loopers-LoginPw", otherPassword)
                set("Content-Type", "application/json")
            }
            val otherOrderRequest = OrderV1Dto.CreateRequest(
                items = listOf(OrderV1Dto.OrderItemRequest(productId = testProductId, quantity = 1)),
            )
            val otherOrderResponse = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(otherOrderRequest, otherHeaders),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            @Suppress("UNCHECKED_CAST")
            val otherOrderData = otherOrderResponse.body?.data as Map<String, Any>
            val otherOrderId = (otherOrderData["orderId"] as Number).toLong()

            // act - 내 인증으로 타인 주문 조회
            val response = testRestTemplate.exchange(
                "/api/v1/orders/$otherOrderId",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        }
    }

    @DisplayName("POST /api/v1/orders (쿠폰 적용)")
    @Nested
    inner class CreateOrderWithCoupon {

        @DisplayName("쿠폰을 적용한 주문이면, 할인이 반영된 200 OK를 반환한다.")
        @Test
        fun returnsOkWithDiscount_whenCouponApplied() {
            // arrange
            val couponId = createTestCoupon(
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
            )!!
            val issuedCouponId = issueCoupon(couponId)!!
            val request = OrderV1Dto.CreateRequest(
                items = listOf(OrderV1Dto.OrderItemRequest(productId = testProductId, quantity = 1)),
                couponId = issuedCouponId,
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull() },
                { assertThat(response.body?.data?.get("couponId")).isNotNull() },
                { assertThat(response.body?.data?.get("discountAmount")).isNotNull() },
            )
        }

        @DisplayName("쿠폰 사용 주문에서 일부 재고 부족이면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenPartialStockWithCoupon() {
            // arrange
            val couponId = createTestCoupon()!!
            val issuedCouponId = issueCoupon(couponId)!!
            val outOfStockProductId = createTestProduct(testBrandId, name = "재고없는상품", stock = 0)!!
            val request = OrderV1Dto.CreateRequest(
                items = listOf(
                    OrderV1Dto.OrderItemRequest(productId = testProductId, quantity = 1),
                    OrderV1Dto.OrderItemRequest(productId = outOfStockProductId, quantity = 1),
                ),
                couponId = issuedCouponId,
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("최소 주문 금액 미달 시, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenBelowMinOrderAmount() {
            // arrange
            val cheapProductId = createTestProduct(testBrandId, name = "저가상품", stock = 100)!!
            // 상품 가격이 129000이므로 minOrderAmount를 200000으로 설정
            val couponId = createTestCoupon(minOrderAmount = BigDecimal("200000"))!!
            val issuedCouponId = issueCoupon(couponId)!!
            val request = OrderV1Dto.CreateRequest(
                items = listOf(OrderV1Dto.OrderItemRequest(productId = cheapProductId, quantity = 1)),
                couponId = issuedCouponId,
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("이미 사용된 쿠폰이면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenCouponAlreadyUsed() {
            // arrange
            val couponId = createTestCoupon(minOrderAmount = null)!!
            val issuedCouponId = issueCoupon(couponId)!!

            // 먼저 쿠폰 사용
            val firstRequest = OrderV1Dto.CreateRequest(
                items = listOf(OrderV1Dto.OrderItemRequest(productId = testProductId, quantity = 1)),
                couponId = issuedCouponId,
            )
            testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(firstRequest, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // 같은 쿠폰으로 재주문
            val secondRequest = OrderV1Dto.CreateRequest(
                items = listOf(OrderV1Dto.OrderItemRequest(productId = testProductId, quantity = 1)),
                couponId = issuedCouponId,
            )

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(secondRequest, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
