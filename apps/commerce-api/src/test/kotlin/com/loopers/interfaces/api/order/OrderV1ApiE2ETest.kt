package com.loopers.interfaces.api.order

import com.loopers.interfaces.api.coupon.dto.CouponAdminV1Dto
import com.loopers.interfaces.api.order.dto.OrderV1Dto
import com.loopers.interfaces.api.product.dto.ProductAdminV1Dto
import com.loopers.interfaces.api.user.dto.UserV1Dto
import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.HEADER_LDAP
import com.loopers.interfaces.support.HEADER_LOGIN_ID
import com.loopers.interfaces.support.HEADER_LOGIN_PW
import com.loopers.interfaces.support.LDAP_ADMIN_VALUE
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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun signUp(loginId: String = "testuser1") {
        val request = UserV1Dto.SignUpRequest(
            loginId = loginId,
            password = "Password1!",
            name = "홍길동",
            birthDate = LocalDate.of(1990, 1, 15),
            email = "$loginId@example.com",
        )
        testRestTemplate.exchange(
            "/api/v1/users/sign-up",
            HttpMethod.POST,
            HttpEntity(request),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
    }

    private fun authHeaders(loginId: String = "testuser1"): HttpHeaders {
        return HttpHeaders().apply {
            set(HEADER_LOGIN_ID, loginId)
            set(HEADER_LOGIN_PW, "Password1!")
            set("Content-Type", "application/json")
        }
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(HEADER_LDAP, LDAP_ADMIN_VALUE)
            set("Content-Type", "application/json")
        }
    }

    private fun createBrand(): Long {
        val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
        val response = testRestTemplate.exchange(
            "/api-admin/v1/brands?name={name}",
            HttpMethod.POST,
            HttpEntity<Void>(adminHeaders()),
            responseType,
            "나이키",
        )
        return (response.body!!.data!!["id"] as Number).toLong()
    }

    private fun createCoupon(
        name: String = "테스트 쿠폰",
        type: String = "FIXED",
        value: Long = 5000,
        totalQuantity: Int? = 100,
        expiredAt: String = ZonedDateTime.now().plusDays(30).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
    ): Long {
        val request = CouponAdminV1Dto.CreateCouponRequest(
            name = name,
            type = type,
            value = value,
            totalQuantity = totalQuantity,
            expiredAt = expiredAt,
        )
        val responseType = object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
        val response = testRestTemplate.exchange(
            "/api-admin/v1/coupons",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        return response.body!!.data!!.id
    }

    private fun issueCoupon(couponId: Long, loginId: String = "testuser1"): Long {
        val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
        val response = testRestTemplate.exchange(
            "/api/v1/coupons/$couponId/issue",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders(loginId)),
            responseType,
        )
        return (response.body!!.data!!["id"] as Number).toLong()
    }

    private fun createProduct(brandId: Long, name: String, price: BigDecimal, stock: Int): Long {
        val request = ProductAdminV1Dto.CreateProductRequest(
            brandId = brandId,
            name = name,
            price = price,
            stock = stock,
        )
        val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
        val response = testRestTemplate.exchange(
            "/api-admin/v1/products",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        return (response.body!!.data!!["id"] as Number).toLong()
    }

    @Nested
    @DisplayName("POST /api/v1/orders")
    inner class CreateOrder {

        @Test
        @DisplayName("회원가입 → 상품 등록 → 주문 생성이 성공한다")
        fun createOrder_fullFlow_success() {
            // arrange
            signUp()
            val brandId = createBrand()
            val productId = createProduct(brandId, "에어맥스 90", BigDecimal("129000"), 100)

            val orderRequest = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = productId, quantity = 2)),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(orderRequest, authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.totalPrice).isEqualByComparingTo(BigDecimal("258000")) },
                { assertThat(response.body?.data?.items).hasSize(1) },
                { assertThat(response.body?.data?.status).isEqualTo("CREATED") },
            )

            // 재고 차감 확인
            val productResponseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val productResponse = testRestTemplate.exchange(
                "/api-admin/v1/products/$productId",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                productResponseType,
            )
            assertThat((productResponse.body!!.data!!["stock"] as Number).toInt()).isEqualTo(98)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders")
    inner class GetOrders {

        @Test
        @DisplayName("from/to 파라미터 없이 조회하면 기본값(최근 1달)이 적용되어 200 OK를 반환한다")
        fun getOrders_withoutFromTo_appliesDefaultOneMonthRange() {
            // arrange
            signUp()

            // act - from/to 없이 주문 목록 조회
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert - 기본값이 적용되어 정상 응답(빈 목록)이 반환된다
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @Test
        @DisplayName("from만 입력하면 to 기본값(현재 시각)이 적용되어 200 OK를 반환한다")
        fun getOrders_withOnlyFrom_appliesToDefault() {
            // arrange
            signUp()
            val from = "2025-01-08T00:00:00+09:00"

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/orders?from={from}",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                responseType,
                from,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @Test
        @DisplayName("to만 입력하면 from 기본값(현재 시각 - 1달)이 적용되어 200 OK를 반환한다")
        fun getOrders_withOnlyTo_appliesFromDefault() {
            // arrange
            signUp()
            val to = "2099-01-15T00:00:00+09:00"

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/orders?to={to}",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                responseType,
                to,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @Test
        @DisplayName("주문 생성 후 목록 조회 시 생성한 주문이 포함된다")
        fun getOrders_afterCreatingOrder_containsCreatedOrder() {
            // arrange
            signUp()
            val brandId = createBrand()
            val productId = createProduct(brandId, "에어맥스 90", BigDecimal("129000"), 100)

            val orderRequest = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = productId, quantity = 1)),
            )
            val createResponseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val createResponse = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(orderRequest, authHeaders()),
                createResponseType,
            )
            val createdOrderId = createResponse.body!!.data!!.id

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            @Suppress("UNCHECKED_CAST")
            val content = response.body?.data?.get("content") as? List<Map<String, Any>>
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(content).isNotEmpty() },
                { assertThat(content?.map { (it["id"] as Number).toLong() }).contains(createdOrderId) },
            )
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders/{orderId}")
    inner class GetOrder {

        @Test
        @DisplayName("본인이 생성한 주문을 조회하면 정상 응답한다")
        fun getOrder_byOwner_success() {
            // arrange
            signUp()
            val brandId = createBrand()
            val productId = createProduct(brandId, "에어맥스 90", BigDecimal("129000"), 100)

            val orderRequest = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = productId, quantity = 1)),
            )
            val createResponseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val createResponse = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(orderRequest, authHeaders()),
                createResponseType,
            )
            val orderId = createResponse.body!!.data!!.id

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/orders/$orderId",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(orderId) },
                { assertThat(response.body?.data?.totalPrice).isEqualByComparingTo(BigDecimal("129000")) },
            )
        }

        @Test
        @DisplayName("다른 사용자의 주문을 조회하면 404를 반환한다")
        fun getOrder_byOtherUser_returnsNotFound() {
            // arrange - 첫 번째 사용자가 주문 생성
            signUp("testuser1")
            val brandId = createBrand()
            val productId = createProduct(brandId, "에어맥스 90", BigDecimal("129000"), 100)

            val orderRequest = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = productId, quantity = 1)),
            )
            val createResponseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val createResponse = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(orderRequest, authHeaders("testuser1")),
                createResponseType,
            )
            val orderId = createResponse.body!!.data!!.id

            // 두 번째 사용자 등록
            signUp("testuser2")

            // act - 두 번째 사용자가 첫 번째 사용자의 주문 조회
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/orders/$orderId",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders("testuser2")),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("POST /api/v1/orders - 쿠폰 적용 주문")
    inner class CreateOrderWithCoupon {

        @Test
        @DisplayName("쿠폰 적용 주문 시 할인이 적용된 totalPrice와 쿠폰 USED 상태를 확인한다")
        fun createOrderWithCoupon_success() {
            // arrange
            signUp()
            val brandId = createBrand()
            val productId = createProduct(brandId, "에어맥스 90", BigDecimal("129000"), 100)
            val couponId = createCoupon(type = "FIXED", value = 5000)
            val issuedCouponId = issueCoupon(couponId)

            val orderRequest = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = productId, quantity = 1)),
                issuedCouponId = issuedCouponId,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(orderRequest, authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.originalPrice).isEqualByComparingTo(BigDecimal("129000")) },
                { assertThat(response.body?.data?.discountAmount).isEqualByComparingTo(BigDecimal("5000")) },
                { assertThat(response.body?.data?.totalPrice).isEqualByComparingTo(BigDecimal("124000")) },
                { assertThat(response.body?.data?.couponId).isEqualTo(couponId) },
            )

            // 쿠폰 USED 상태 확인
            val myCouponsType = object : ParameterizedTypeReference<ApiResponse<List<Map<String, Any>>>>() {}
            val myCoupons = testRestTemplate.exchange(
                "/api/v1/users/me/coupons",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                myCouponsType,
            )
            assertThat(myCoupons.body?.data?.first()?.get("status")).isEqualTo("USED")
        }

        @Test
        @DisplayName("쿠폰 미적용 주문 시 originalPrice와 totalPrice가 동일하다")
        fun createOrderWithoutCoupon_pricesMatch() {
            // arrange
            signUp()
            val brandId = createBrand()
            val productId = createProduct(brandId, "에어맥스 90", BigDecimal("129000"), 100)

            val orderRequest = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = productId, quantity = 1)),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(orderRequest, authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.originalPrice).isEqualByComparingTo(BigDecimal("129000")) },
                {
                    assertThat(response.body?.data?.totalPrice).isEqualByComparingTo(
                        response.body?.data?.originalPrice,
                    )
                },
                { assertThat(response.body?.data?.discountAmount).isEqualByComparingTo(BigDecimal.ZERO) },
                { assertThat(response.body?.data?.couponId).isNull() },
            )
        }

        @Test
        @DisplayName("타인의 쿠폰으로 주문하면 400을 반환한다")
        fun createOrderWithOtherUserCoupon_returnsBadRequest() {
            // arrange
            signUp("user1")
            signUp("user2")
            val brandId = createBrand()
            val productId = createProduct(brandId, "에어맥스 90", BigDecimal("129000"), 100)
            val couponId = createCoupon()
            val issuedCouponId = issueCoupon(couponId, "user1")

            // user2가 user1의 쿠폰으로 주문 시도
            val orderRequest = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = productId, quantity = 1)),
                issuedCouponId = issuedCouponId,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(orderRequest, authHeaders("user2")),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("이미 사용된 쿠폰으로 주문하면 400을 반환한다")
        fun createOrderWithUsedCoupon_returnsBadRequest() {
            // arrange
            signUp()
            val brandId = createBrand()
            val productId = createProduct(brandId, "에어맥스 90", BigDecimal("129000"), 100)
            val couponId = createCoupon()
            val issuedCouponId = issueCoupon(couponId)

            // 첫 번째 주문으로 쿠폰 사용
            val firstOrder = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = productId, quantity = 1)),
                issuedCouponId = issuedCouponId,
            )
            testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(firstOrder, authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {},
            )

            // act - 동일 쿠폰으로 두 번째 주문 시도
            val secondOrder = OrderV1Dto.CreateOrderRequest(
                items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = productId, quantity = 1)),
                issuedCouponId = issuedCouponId,
            )
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                HttpEntity(secondOrder, authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
