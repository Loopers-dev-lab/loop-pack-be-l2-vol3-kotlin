package com.loopers.interfaces.api

import com.loopers.domain.brand.Brand
import com.loopers.interfaces.common.ApiResponse
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponQuantity
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.Discount
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.UserRepository
import com.loopers.interfaces.api.user.UserDto
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
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
    private val userRepository: UserRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        private const val ORDER_ENDPOINT = "/api/v1/orders"
        private const val SIGNUP_ENDPOINT = "/api/v1/users/signup"
        private const val ISSUE_COUPON_ENDPOINT = "/api/v1/coupons/{couponId}/issue"
        private const val LOGIN_ID_HEADER = "X-Loopers-LoginId"
        private const val LOGIN_PW_HEADER = "X-Loopers-LoginPw"
        private val ORDER_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<Any>>() {}
        private val ORDER_LIST_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<List<Any>>>() {}
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun signUp(
        loginId: String = "testuser123",
        password: String = "Test1234!@",
        name: String = "홍길동",
        email: String = "test@example.com",
        birthday: LocalDate = LocalDate.of(1990, 1, 15),
    ) {
        val request = UserDto.SignUpRequest(
            loginId = loginId,
            password = password,
            name = name,
            email = email,
            birthday = birthday,
        )
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        testRestTemplate.exchange(
            SIGNUP_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(request, headers),
            object : ParameterizedTypeReference<ApiResponse<UserDto.SignUpResponse>>() {},
        )
    }

    private fun authHeaders(
        loginId: String = "testuser123",
        password: String = "Test1234!@",
    ): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set(LOGIN_ID_HEADER, loginId)
            set(LOGIN_PW_HEADER, password)
        }
    }

    private fun createBrand(name: String = "나이키"): Brand {
        return brandRepository.save(Brand(name = name, description = "스포츠 브랜드"))
    }

    private fun createProduct(
        name: String = "에어맥스",
        description: String? = "러닝화",
        price: Money = Money.of(159000L),
        stockQuantity: StockQuantity = StockQuantity.of(100),
        brand: Brand? = null,
    ): Product {
        val resolvedBrand = brand ?: createBrand()
        return productRepository.save(
            Product(name = name, description = description, price = price, likes = LikeCount.of(0), stockQuantity = stockQuantity, brandId = resolvedBrand.id),
        )
    }

    private data class PlaceOrderRequest(
        val items: List<OrderItemRequest>,
        val couponId: Long? = null,
    )

    private data class OrderItemRequest(
        val productId: Long,
        val quantity: Int,
    )

    private fun createCoupon(
        name: String = "테스트 쿠폰",
        discount: Discount = Discount(DiscountType.FIXED_AMOUNT, 5000L),
        totalQuantity: Int = 100,
        expiresAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Coupon {
        return couponRepository.save(
            Coupon(
                name = name,
                discount = discount,
                quantity = CouponQuantity(totalQuantity, 0),
                expiresAt = expiresAt,
            ),
        )
    }

    private fun issueCouponViaApi(couponId: Long, loginId: String = "testuser123", password: String = "Test1234!@") {
        testRestTemplate.exchange(
            ISSUE_COUPON_ENDPOINT,
            HttpMethod.POST,
            HttpEntity<Void>(authHeaders(loginId = loginId, password = password)),
            ORDER_RESPONSE_TYPE,
            couponId,
        )
    }

    private fun placeOrder(
        request: PlaceOrderRequest,
        headers: HttpHeaders = authHeaders(),
    ) = testRestTemplate.exchange(
        ORDER_ENDPOINT,
        HttpMethod.POST,
        HttpEntity(request, headers),
        ORDER_RESPONSE_TYPE,
    )

    private fun getOrders(
        startAt: String,
        endAt: String,
        headers: HttpHeaders = authHeaders(),
    ) = testRestTemplate.exchange(
        "$ORDER_ENDPOINT?startAt=$startAt&endAt=$endAt",
        HttpMethod.GET,
        HttpEntity<Void>(headers),
        ORDER_LIST_RESPONSE_TYPE,
    )

    private fun getOrderDetail(
        orderId: Long,
        headers: HttpHeaders = authHeaders(),
    ) = testRestTemplate.exchange(
        "$ORDER_ENDPOINT/$orderId",
        HttpMethod.GET,
        HttpEntity<Void>(headers),
        ORDER_RESPONSE_TYPE,
    )

    private fun placeOrderAndGetId(
        request: PlaceOrderRequest,
        headers: HttpHeaders = authHeaders(),
    ): Long {
        placeOrder(request, headers)
        val startAt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val endAt = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val ordersResponse = getOrders(startAt, endAt, headers)
        val order = ordersResponse.body?.data?.first() as Map<*, *>
        return (order["orderId"] as Number).toLong()
    }

    @DisplayName("POST /api/v1/orders")
    @Nested
    inner class PlaceOrderApi {

        @DisplayName("로그인한 사용자가 주문하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenAuthenticatedUserPlacesOrder() {
            // arrange
            signUp()
            val product = createProduct()
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 2)),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }

        @DisplayName("items가 비어있으면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenItemsEmpty() {
            // arrange
            signUp()
            val request = PlaceOrderRequest(items = emptyList())

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("quantity가 0 이하이면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenQuantityIsZeroOrNegative() {
            // arrange
            signUp()
            val product = createProduct()
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 0)),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("productId가 중복되면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenProductIdDuplicated() {
            // arrange
            signUp()
            val product = createProduct()
            val request = PlaceOrderRequest(
                items = listOf(
                    OrderItemRequest(productId = product.id, quantity = 1),
                    OrderItemRequest(productId = product.id, quantity = 2),
                ),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("존재하지 않는 상품이면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenProductNotExists() {
            // arrange
            signUp()
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = 999999L, quantity = 1)),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("재고가 부족하면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenInsufficientStock() {
            // arrange
            signUp()
            val product = createProduct(stockQuantity = StockQuantity.of(5))
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 10)),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("인증 헤더가 없으면, 401 Unauthorized를 반환한다.")
        @Test
        fun returnsUnauthorized_whenNotAuthenticated() {
            // arrange
            val product = createProduct()
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
            )
            val unauthHeaders = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            // act
            val response = placeOrder(request, unauthHeaders)

            // assert
            assertThat(response.statusCode.value()).isEqualTo(401)
        }

        @DisplayName("주문 성공 시 재고가 차감된다.")
        @Test
        fun deductsStock_whenOrderIsSuccessful() {
            // arrange
            signUp()
            val product = createProduct(stockQuantity = StockQuantity.of(100))
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 3)),
            )

            // act
            placeOrder(request)

            // assert
            val updatedProduct = productRepository.findById(product.id)
            assertThat(updatedProduct?.stockQuantity).isEqualTo(StockQuantity.of(97))
        }

        @DisplayName("삭제된 상품에 주문하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenProductIsDeleted() {
            // arrange
            signUp()
            val product = createProduct()
            product.delete()
            productRepository.save(product)
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("여러 상품을 주문하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenMultipleItemsOrdered() {
            // arrange
            signUp()
            val brand = createBrand()
            val product1 = createProduct(name = "에어맥스", price = Money.of(159000L), brand = brand)
            val product2 = createProduct(name = "에어포스", price = Money.of(139000L), brand = brand)
            val request = PlaceOrderRequest(
                items = listOf(
                    OrderItemRequest(productId = product1.id, quantity = 2),
                    OrderItemRequest(productId = product2.id, quantity = 1),
                ),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }

        @DisplayName("여러 상품 주문 시 모든 상품의 재고가 차감된다.")
        @Test
        fun deductsAllStock_whenMultipleItemsOrdered() {
            // arrange
            signUp()
            val brand = createBrand()
            val product1 = createProduct(name = "에어맥스", price = Money.of(159000L), stockQuantity = StockQuantity.of(50), brand = brand)
            val product2 = createProduct(name = "에어포스", price = Money.of(139000L), stockQuantity = StockQuantity.of(30), brand = brand)
            val request = PlaceOrderRequest(
                items = listOf(
                    OrderItemRequest(productId = product1.id, quantity = 5),
                    OrderItemRequest(productId = product2.id, quantity = 3),
                ),
            )

            // act
            placeOrder(request)

            // assert
            val updatedProduct1 = productRepository.findById(product1.id)
            val updatedProduct2 = productRepository.findById(product2.id)
            assertAll(
                { assertThat(updatedProduct1?.stockQuantity).isEqualTo(StockQuantity.of(45)) },
                { assertThat(updatedProduct2?.stockQuantity).isEqualTo(StockQuantity.of(27)) },
            )
        }

        @DisplayName("재고 부족 시 이미 차감된 재고도 롤백된다.")
        @Test
        fun rollsBackStock_whenAnyItemHasInsufficientStock() {
            // arrange
            signUp()
            val brand = createBrand()
            val product1 = createProduct(name = "에어맥스", price = Money.of(159000L), stockQuantity = StockQuantity.of(100), brand = brand)
            val product2 = createProduct(name = "에어포스", price = Money.of(139000L), stockQuantity = StockQuantity.of(2), brand = brand)
            val request = PlaceOrderRequest(
                items = listOf(
                    OrderItemRequest(productId = product1.id, quantity = 5),
                    OrderItemRequest(productId = product2.id, quantity = 10),
                ),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            val updatedProduct1 = productRepository.findById(product1.id)
            assertThat(updatedProduct1?.stockQuantity).isEqualTo(StockQuantity.of(100))
        }
    }

    @DisplayName("GET /api/v1/orders")
    @Nested
    inner class GetOrdersApi {

        @DisplayName("기간 내 주문 목록을 조회하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenGetOrdersWithinPeriod() {
            // arrange
            signUp()
            val product = createProduct()
            placeOrder(PlaceOrderRequest(items = listOf(OrderItemRequest(productId = product.id, quantity = 1))))

            val startAt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val endAt = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            // act
            val response = getOrders(startAt, endAt)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data).hasSize(1) },
            )
        }

        @DisplayName("조회된 주문의 응답 데이터가 올바르다.")
        @Test
        fun returnsCorrectOrderData() {
            // arrange
            signUp()
            val brand = createBrand()
            val product = createProduct(name = "에어맥스", price = Money.of(159000L), brand = brand)
            placeOrder(PlaceOrderRequest(items = listOf(OrderItemRequest(productId = product.id, quantity = 2))))

            val startAt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val endAt = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            // act
            val response = getOrders(startAt, endAt)

            // assert
            val order = response.body?.data?.first() as Map<*, *>
            assertAll(
                { assertThat(order["orderId"]).isNotNull() },
                { assertThat((order["totalAmount"] as Number).toLong()).isEqualTo(318000L) },
                { assertThat(order["status"]).isEqualTo("ORDERED") },
                { assertThat(order["orderedAt"]).isNotNull() },
            )
        }

        @DisplayName("여러 건 주문 후 조회하면, 모든 주문이 반환된다.")
        @Test
        fun returnsMultipleOrders() {
            // arrange
            signUp()
            val brand = createBrand()
            val product1 = createProduct(name = "에어맥스", price = Money.of(159000L), brand = brand)
            val product2 = createProduct(name = "에어포스", price = Money.of(139000L), brand = brand)
            placeOrder(PlaceOrderRequest(items = listOf(OrderItemRequest(productId = product1.id, quantity = 1))))
            placeOrder(PlaceOrderRequest(items = listOf(OrderItemRequest(productId = product2.id, quantity = 1))))

            val startAt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val endAt = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            // act
            val response = getOrders(startAt, endAt)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).hasSize(2) },
            )
        }

        @DisplayName("기간 외 주문은 조회되지 않는다.")
        @Test
        fun doesNotReturnOrdersOutsidePeriod() {
            // arrange
            signUp()
            val product = createProduct()
            placeOrder(PlaceOrderRequest(items = listOf(OrderItemRequest(productId = product.id, quantity = 1))))

            val startAt = LocalDateTime.now().minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val endAt = LocalDateTime.now().minusDays(29).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            // act
            val response = getOrders(startAt, endAt)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isEmpty() },
            )
        }

        @DisplayName("startAt이 endAt보다 크면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenStartAtIsAfterEndAt() {
            // arrange
            signUp()
            val startAt = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val endAt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            // act
            val response = getOrders(startAt, endAt)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("인증 헤더가 없으면, 401 Unauthorized를 반환한다.")
        @Test
        fun returnsUnauthorized_whenNotAuthenticated() {
            // arrange
            val startAt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val endAt = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val unauthHeaders = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            // act
            val response = getOrders(startAt, endAt, unauthHeaders)

            // assert
            assertThat(response.statusCode.value()).isEqualTo(401)
        }

        @DisplayName("다른 유저의 주문은 조회되지 않는다.")
        @Test
        fun doesNotReturnOtherUsersOrders() {
            // arrange
            signUp(loginId = "user1", name = "유저1", email = "user1@example.com")
            signUp(loginId = "user2", name = "유저2", email = "user2@example.com")
            val product = createProduct()

            // user1이 주문
            placeOrder(
                PlaceOrderRequest(items = listOf(OrderItemRequest(productId = product.id, quantity = 1))),
                authHeaders(loginId = "user1"),
            )

            val startAt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val endAt = LocalDateTime.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            // act - user2가 조회
            val response = getOrders(startAt, endAt, authHeaders(loginId = "user2"))

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isEmpty() },
            )
        }
    }

    @DisplayName("GET /api/v1/orders/{orderId}")
    @Nested
    inner class GetOrderDetailApi {

        @DisplayName("주문 상세 조회 시, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenGetOrderDetail() {
            // arrange
            signUp()
            val product = createProduct()
            val orderId = placeOrderAndGetId(PlaceOrderRequest(items = listOf(OrderItemRequest(productId = product.id, quantity = 2))))

            // act
            val response = getOrderDetail(orderId)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }

        @DisplayName("응답 데이터에 주문 정보와 주문 항목이 포함된다.")
        @Test
        fun returnsCorrectOrderDetailData() {
            // arrange
            signUp()
            val brand = createBrand()
            val product = createProduct(name = "에어맥스", price = Money.of(159000L), brand = brand)
            val orderId = placeOrderAndGetId(PlaceOrderRequest(items = listOf(OrderItemRequest(productId = product.id, quantity = 2))))

            // act
            val response = getOrderDetail(orderId)

            // assert
            val data = response.body?.data as Map<*, *>
            assertAll(
                { assertThat(data["orderId"]).isNotNull() },
                { assertThat((data["totalAmount"] as Number).toLong()).isEqualTo(318000L) },
                { assertThat(data["status"]).isEqualTo("ORDERED") },
                { assertThat(data["orderedAt"]).isNotNull() },
                { assertThat(data["items"]).isNotNull() },
            )
            val items = data["items"] as List<*>
            assertThat(items).hasSize(1)
            val item = items[0] as Map<*, *>
            assertAll(
                { assertThat((item["productId"] as Number).toLong()).isEqualTo(product.id) },
                { assertThat((item["quantity"] as Number).toInt()).isEqualTo(2) },
                { assertThat(item["productName"]).isEqualTo("에어맥스") },
                { assertThat((item["productPrice"] as Number).toLong()).isEqualTo(159000L) },
                { assertThat(item["brandName"]).isEqualTo("나이키") },
            )
        }

        @DisplayName("여러 상품을 주문한 경우, 모든 주문 항목이 포함된다.")
        @Test
        fun returnsAllOrderItems_whenMultipleProducts() {
            // arrange
            signUp()
            val brand = createBrand()
            val product1 = createProduct(name = "에어맥스", price = Money.of(159000L), brand = brand)
            val product2 = createProduct(name = "에어포스", price = Money.of(139000L), brand = brand)
            val orderId = placeOrderAndGetId(
                PlaceOrderRequest(
                    items = listOf(
                        OrderItemRequest(productId = product1.id, quantity = 2),
                        OrderItemRequest(productId = product2.id, quantity = 3),
                    ),
                ),
            )

            // act
            val response = getOrderDetail(orderId)

            // assert
            val data = response.body?.data as Map<*, *>
            // 159000 * 2 + 139000 * 3 = 318000 + 417000 = 735000
            assertThat((data["totalAmount"] as Number).toLong()).isEqualTo(735000L)
            val items = data["items"] as List<*>
            assertThat(items).hasSize(2)
        }

        @DisplayName("존재하지 않는 주문이면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenOrderDoesNotExist() {
            // arrange
            signUp()

            // act
            val response = getOrderDetail(999999L)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("타 유저의 주문이면, 403 FORBIDDEN을 반환한다.")
        @Test
        fun returnsForbidden_whenOtherUsersOrder() {
            // arrange
            signUp(loginId = "user1", name = "유저1", email = "user1@example.com")
            signUp(loginId = "user2", name = "유저2", email = "user2@example.com")
            val product = createProduct()
            val orderId = placeOrderAndGetId(
                PlaceOrderRequest(items = listOf(OrderItemRequest(productId = product.id, quantity = 1))),
                headers = authHeaders(loginId = "user1"),
            )

            // act - user2가 user1의 주문을 조회
            val response = getOrderDetail(orderId, authHeaders(loginId = "user2"))

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("인증 헤더가 없으면, 401 Unauthorized를 반환한다.")
        @Test
        fun returnsUnauthorized_whenNotAuthenticated() {
            // arrange
            val unauthHeaders = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            // act
            val response = getOrderDetail(1L, unauthHeaders)

            // assert
            assertThat(response.statusCode.value()).isEqualTo(401)
        }
    }

    @DisplayName("POST /api/v1/orders - 쿠폰 적용 주문")
    @Nested
    inner class PlaceOrderWithCouponApi {

        private fun placeOrderAndGetDetail(
            request: PlaceOrderRequest,
            headers: HttpHeaders = authHeaders(),
        ): Map<*, *> {
            val orderId = placeOrderAndGetId(request, headers)
            val response = getOrderDetail(orderId, headers)
            return response.body?.data as Map<*, *>
        }

        @DisplayName("정액 할인 쿠폰을 적용하면, 200 OK와 할인된 주문이 생성된다.")
        @Test
        fun returnsOk_withFixedDiscountApplied() {
            // arrange
            signUp()
            val product = createProduct(price = Money.of(100000L))
            val coupon = createCoupon(discount = Discount(DiscountType.FIXED_AMOUNT, 5000L))
            issueCouponViaApi(coupon.id)

            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 2)),
                couponId = coupon.id,
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }

        @DisplayName("쿠폰 적용 주문의 상세 조회 시, 할인 정보가 포함된다.")
        @Test
        fun returnsDiscountInfo_whenOrderDetailQueried() {
            // arrange
            signUp()
            val product = createProduct(price = Money.of(100000L))
            val coupon = createCoupon(discount = Discount(DiscountType.FIXED_AMOUNT, 5000L))
            issueCouponViaApi(coupon.id)

            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 2)),
                couponId = coupon.id,
            )

            // act
            val detail = placeOrderAndGetDetail(request)

            // assert
            assertAll(
                { assertThat((detail["totalAmount"] as Number).toLong()).isEqualTo(200000L) },
                { assertThat((detail["discountAmount"] as Number).toLong()).isEqualTo(5000L) },
                { assertThat((detail["paymentAmount"] as Number).toLong()).isEqualTo(195000L) },
            )
        }

        @DisplayName("정률 할인 쿠폰 적용 시, 비율만큼 할인된다.")
        @Test
        fun appliesPercentageDiscount() {
            // arrange
            signUp()
            val product = createProduct(price = Money.of(100000L))
            val coupon = createCoupon(discount = Discount(DiscountType.PERCENTAGE, 10L))
            issueCouponViaApi(coupon.id)

            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 2)),
                couponId = coupon.id,
            )

            // act
            val detail = placeOrderAndGetDetail(request)

            // assert
            assertAll(
                { assertThat((detail["totalAmount"] as Number).toLong()).isEqualTo(200000L) },
                { assertThat((detail["discountAmount"] as Number).toLong()).isEqualTo(20000L) },
                { assertThat((detail["paymentAmount"] as Number).toLong()).isEqualTo(180000L) },
            )
        }

        @DisplayName("couponId를 생략하면, 쿠폰 없이 정상 주문된다.")
        @Test
        fun createsOrderWithoutCoupon_whenCouponIdIsNull() {
            // arrange
            signUp()
            val product = createProduct(price = Money.of(100000L))
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 2)),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }

        @DisplayName("존재하지 않는 쿠폰으로 주문하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenCouponDoesNotExist() {
            // arrange
            signUp()
            val product = createProduct()
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                couponId = 999999L,
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("이미 사용된 쿠폰으로 주문하면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenCouponAlreadyUsed() {
            // arrange
            signUp()
            val product = createProduct()
            val coupon = createCoupon()
            issueCouponViaApi(coupon.id)

            // 발급된 쿠폰을 직접 사용 처리
            val user = userRepository.findByLoginId(LoginId.of("testuser123"))!!
            val issuedCoupons = issuedCouponRepository.findByUserId(user.id)
            val issuedCoupon = issuedCoupons.first { it.couponId == coupon.id }
            issuedCoupon.use()
            issuedCouponRepository.save(issuedCoupon)

            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                couponId = coupon.id,
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("다른 사용자의 쿠폰으로 주문하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenCouponBelongsToOtherUser() {
            // arrange
            signUp(loginId = "user1", name = "유저1", email = "user1@example.com")
            signUp(loginId = "user2", name = "유저2", email = "user2@example.com")
            val product = createProduct()
            val coupon = createCoupon()
            issueCouponViaApi(coupon.id, loginId = "user2")

            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                couponId = coupon.id,
            )

            // act - user1이 user2의 쿠폰으로 주문
            val response = placeOrder(request, authHeaders(loginId = "user1"))

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("쿠폰 적용 주문 성공 시, 쿠폰이 사용 처리된다.")
        @Test
        fun marksCouponAsUsed_whenOrderSucceeds() {
            // arrange
            signUp()
            val product = createProduct()
            val coupon = createCoupon()
            issueCouponViaApi(coupon.id)

            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                couponId = coupon.id,
            )

            // act
            placeOrder(request)

            // assert
            val user = userRepository.findByLoginId(LoginId.of("testuser123"))!!
            val issuedCoupons = issuedCouponRepository.findByUserId(user.id)
            val issuedCoupon = issuedCoupons.first { it.couponId == coupon.id }
            assertThat(issuedCoupon.usedAt).isNotNull()
        }

        @DisplayName("만료된 쿠폰으로 주문하면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenCouponIsExpired() {
            // arrange
            signUp()
            val product = createProduct()
            val coupon = createCoupon()
            issueCouponViaApi(coupon.id)

            // 발급 후 쿠폰을 만료 처리
            val savedCoupon = couponRepository.findById(coupon.id)!!
            ReflectionTestUtils.setField(savedCoupon, "expiresAt", ZonedDateTime.now().minusDays(1))
            couponRepository.save(savedCoupon)

            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                couponId = coupon.id,
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("쿠폰 미적용 주문 상세에서 할인 금액은 0이고, 결제 금액은 총 금액과 같다.")
        @Test
        fun returnsZeroDiscount_whenNoCouponApplied() {
            // arrange
            signUp()
            val product = createProduct(price = Money.of(100000L))
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 2)),
            )

            // act
            val detail = placeOrderAndGetDetail(request)

            // assert
            assertAll(
                { assertThat((detail["totalAmount"] as Number).toLong()).isEqualTo(200000L) },
                { assertThat((detail["discountAmount"] as Number).toLong()).isEqualTo(0L) },
                { assertThat((detail["paymentAmount"] as Number).toLong()).isEqualTo(200000L) },
            )
        }

        @DisplayName("쿠폰을 사용한 주문 후, 같은 쿠폰으로 재주문하면 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenCouponAlreadyUsedByPreviousOrder() {
            // arrange
            signUp()
            val brand = createBrand()
            val product1 = createProduct(name = "에어맥스", price = Money.of(100000L), brand = brand)
            val product2 = createProduct(name = "에어포스", price = Money.of(80000L), brand = brand)
            val coupon = createCoupon()
            issueCouponViaApi(coupon.id)

            // 첫 번째 주문에서 쿠폰 사용
            placeOrder(
                PlaceOrderRequest(
                    items = listOf(OrderItemRequest(productId = product1.id, quantity = 1)),
                    couponId = coupon.id,
                ),
            )

            // act - 같은 쿠폰으로 두 번째 주문
            val response = placeOrder(
                PlaceOrderRequest(
                    items = listOf(OrderItemRequest(productId = product2.id, quantity = 1)),
                    couponId = coupon.id,
                ),
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("쿠폰 적용 실패 시, 재고는 변경되지 않는다.")
        @Test
        fun doesNotDeductStock_whenCouponApplicationFails() {
            // arrange
            signUp()
            val product = createProduct(stockQuantity = StockQuantity.of(100))
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 3)),
                couponId = 999999L,
            )

            // act
            placeOrder(request)

            // assert
            val updatedProduct = productRepository.findById(product.id)
            assertThat(updatedProduct?.stockQuantity).isEqualTo(StockQuantity.of(100))
        }
    }

    @DisplayName("POST /api/v1/orders - 동시 주문")
    @Nested
    inner class ConcurrentPlaceOrderApi {

        @DisplayName("동시에 여러 사용자가 같은 상품을 주문하면, 재고가 정확히 차감된다.")
        @Test
        fun deductsStockCorrectly_whenConcurrentOrders() {
            // arrange
            val threadCount = 10
            (1..threadCount).forEach { i ->
                signUp(loginId = "user$i", password = "Test1234!@", name = "사용자$i", email = "user$i@example.com")
            }
            val product = createProduct(stockQuantity = StockQuantity.of(100))
            val latch = CountDownLatch(threadCount)
            val executor = Executors.newFixedThreadPool(threadCount)
            val successCount = AtomicInteger(0)

            // act
            (1..threadCount).forEach { i ->
                executor.submit {
                    try {
                        val request = PlaceOrderRequest(
                            items = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                        )
                        val response = testRestTemplate.exchange(
                            ORDER_ENDPOINT,
                            HttpMethod.POST,
                            HttpEntity(request, authHeaders(loginId = "user$i")),
                            ORDER_RESPONSE_TYPE,
                        )
                        if (response.statusCode == HttpStatus.OK) {
                            successCount.incrementAndGet()
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            val updatedProduct = productRepository.findById(product.id)
            assertAll(
                { assertThat(successCount.get()).isEqualTo(10) },
                { assertThat(updatedProduct?.stockQuantity).isEqualTo(StockQuantity.of(90)) },
            )
        }

        @DisplayName("재고보다 많은 동시 주문이 들어오면, 재고가 음수가 되지 않는다.")
        @Test
        fun doesNotGoNegative_whenConcurrentOrdersExceedStock() {
            // arrange
            val threadCount = 10
            (1..threadCount).forEach { i ->
                signUp(loginId = "user$i", password = "Test1234!@", name = "사용자$i", email = "user$i@example.com")
            }
            val product = createProduct(stockQuantity = StockQuantity.of(5))
            val latch = CountDownLatch(threadCount)
            val executor = Executors.newFixedThreadPool(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act
            (1..threadCount).forEach { i ->
                executor.submit {
                    try {
                        val request = PlaceOrderRequest(
                            items = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
                        )
                        val response = testRestTemplate.exchange(
                            ORDER_ENDPOINT,
                            HttpMethod.POST,
                            HttpEntity(request, authHeaders(loginId = "user$i")),
                            ORDER_RESPONSE_TYPE,
                        )
                        when (response.statusCode) {
                            HttpStatus.OK -> successCount.incrementAndGet()
                            HttpStatus.BAD_REQUEST -> failCount.incrementAndGet()
                            else -> {}
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            val updatedProduct = productRepository.findById(product.id)
            assertAll(
                { assertThat(successCount.get()).isEqualTo(5) },
                { assertThat(failCount.get()).isEqualTo(5) },
                { assertThat(updatedProduct?.stockQuantity).isEqualTo(StockQuantity.of(0)) },
            )
        }
    }
}
