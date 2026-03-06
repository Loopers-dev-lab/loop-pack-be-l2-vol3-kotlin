package com.loopers.interfaces.api

import com.loopers.interfaces.api.admin.brand.AdminBrandV1Dto
import com.loopers.interfaces.api.admin.coupon.AdminCouponV1Dto
import com.loopers.interfaces.api.admin.product.AdminProductV1Dto
import com.loopers.interfaces.api.coupon.CouponV1Dto
import com.loopers.interfaces.api.like.LikeV1Dto
import com.loopers.interfaces.api.member.MemberV1Dto
import com.loopers.interfaces.api.order.OrderV1Dto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConcurrencyTest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val MEMBER_ENDPOINT = "/api/v1/members"
        private const val ADMIN_BRAND_ENDPOINT = "/api-admin/v1/brands"
        private const val ADMIN_PRODUCT_ENDPOINT = "/api-admin/v1/products"
        private const val ADMIN_COUPON_ENDPOINT = "/api-admin/v1/coupons"
        private const val ORDER_ENDPOINT = "/api/v1/orders"
        private const val HEADER_LDAP = "X-Loopers-Ldap"
        private const val LDAP_VALUE = "loopers.admin"
        private const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
        private const val HEADER_LOGIN_PW = "X-Loopers-LoginPw"
    }

    private var brandId: Long = 0
    private var productId: Long = 0
    private val loginId = "concurrencyuser"
    private val password = "Password1!"

    @BeforeEach
    fun setUp() {
        // 회원 등록
        val memberRequest = MemberV1Dto.RegisterRequest(
            loginId = loginId,
            password = password,
            name = "동시성테스터",
            birthday = LocalDate.of(2000, 1, 1),
            email = "concurrency@example.com",
        )
        testRestTemplate.exchange(
            MEMBER_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(memberRequest),
            object : ParameterizedTypeReference<ApiResponse<Void>>() {},
        )

        // 브랜드 생성
        val brandRequest = AdminBrandV1Dto.CreateRequest(
            name = "동시성브랜드",
            description = "테스트",
            imageUrl = "https://example.com/brand.jpg",
        )
        val brandResponse = testRestTemplate.exchange(
            ADMIN_BRAND_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(brandRequest, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Dto.BrandResponse>>() {},
        )
        brandId = brandResponse.body!!.data!!.id
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders =
        HttpHeaders().apply { set(HEADER_LDAP, LDAP_VALUE) }

    private fun memberHeaders(): HttpHeaders =
        HttpHeaders().apply {
            set(HEADER_LOGIN_ID, loginId)
            set(HEADER_LOGIN_PW, password)
        }

    private fun createProduct(stock: Int): Long {
        val productRequest = AdminProductV1Dto.CreateRequest(
            brandId = brandId,
            name = "동시성테스트상품",
            description = "동시성 테스트용",
            price = 10000,
            stockQuantity = stock,
            imageUrl = "https://example.com/product.jpg",
        )
        val response = testRestTemplate.exchange(
            ADMIN_PRODUCT_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(productRequest, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminProductV1Dto.ProductResponse>>() {},
        )
        return response.body!!.data!!.id
    }

    private fun createCouponTemplate(): Long {
        val request = AdminCouponV1Dto.CreateRequest(
            name = "동시성테스트쿠폰",
            type = "FIXED",
            value = 1000L,
            minOrderAmount = null,
            maxDiscountAmount = null,
            expirationPolicy = "FIXED_DATE",
            expiredAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).plusDays(30),
            validDays = null,
        )
        val response = testRestTemplate.exchange(
            ADMIN_COUPON_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Dto.TemplateResponse>>() {},
        )
        return response.body!!.data!!.id
    }

    private fun issueCouponToMember(templateId: Long): Long {
        val response = testRestTemplate.exchange(
            "/api/v1/coupons/templates/$templateId/issue",
            HttpMethod.POST,
            HttpEntity<Any>(memberHeaders()),
            object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssuedCouponResponse>>() {},
        )
        return response.body!!.data!!.id
    }

    // -----------------------------------------------------------------------

    @DisplayName("재고 차감 동시성 테스트")
    @Nested
    inner class StockConcurrency {

        @DisplayName("10개 재고에 10개 스레드가 동시에 1개씩 주문하면, 모두 성공하고 재고가 0이 된다.")
        @Test
        fun deductsStockCorrectly_whenConcurrentOrders() {
            // arrange
            val stock = 10
            val pid = createProduct(stock)
            val threadCount = stock
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val statusCodes = ConcurrentHashMap<Int, HttpStatus>()

            // act
            (0 until threadCount).forEach { i ->
                executorService.submit {
                    try {
                        latch.countDown()
                        latch.await()
                        val request = OrderV1Dto.CreateRequest(
                            items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = pid, quantity = 1)),
                        )
                        val response = testRestTemplate.exchange(
                            ORDER_ENDPOINT,
                            HttpMethod.POST,
                            HttpEntity(request, memberHeaders()),
                            object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {},
                        )
                        statusCodes[i] = response.statusCode as HttpStatus
                    } catch (e: Exception) {
                        statusCodes[i] = HttpStatus.INTERNAL_SERVER_ERROR
                    }
                }
            }
            executorService.shutdown()
            executorService.awaitTermination(30, TimeUnit.SECONDS)

            // assert — 10개 모두 201 CREATED
            assertThat(statusCodes).hasSize(threadCount)
            assertThat(statusCodes.values.count { it == HttpStatus.CREATED }).isEqualTo(threadCount)

            // 재고가 0인지 admin API로 확인
            val productResponse = testRestTemplate.exchange(
                "$ADMIN_PRODUCT_ENDPOINT/$pid",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<AdminProductV1Dto.ProductResponse>>() {},
            )
            assertThat(productResponse.body!!.data!!.stockQuantity).isEqualTo(0)
        }

        @DisplayName("재고(10개)보다 많은 11개 스레드가 동시에 주문하면, 정확히 10개만 성공하고 1개는 실패한다.")
        @Test
        fun rejectsExcess_whenConcurrentOrdersExceedStock() {
            // arrange
            val stock = 10
            val pid = createProduct(stock)
            val threadCount = stock + 1
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val statusCodes = ConcurrentHashMap<Int, HttpStatus>()

            // act
            (0 until threadCount).forEach { i ->
                executorService.submit {
                    try {
                        latch.countDown()
                        latch.await()
                        val request = OrderV1Dto.CreateRequest(
                            items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = pid, quantity = 1)),
                        )
                        val response = testRestTemplate.exchange(
                            ORDER_ENDPOINT,
                            HttpMethod.POST,
                            HttpEntity(request, memberHeaders()),
                            object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {},
                        )
                        statusCodes[i] = response.statusCode as HttpStatus
                    } catch (e: Exception) {
                        statusCodes[i] = HttpStatus.INTERNAL_SERVER_ERROR
                    }
                }
            }
            executorService.shutdown()
            executorService.awaitTermination(30, TimeUnit.SECONDS)

            // assert — 10개 성공, 1개 실패(400)
            assertThat(statusCodes).hasSize(threadCount)
            assertThat(statusCodes.values.count { it == HttpStatus.CREATED }).isEqualTo(stock)
            assertThat(statusCodes.values.count { it == HttpStatus.BAD_REQUEST }).isEqualTo(1)

            // 재고가 0인지 확인
            val productResponse = testRestTemplate.exchange(
                "$ADMIN_PRODUCT_ENDPOINT/$pid",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<AdminProductV1Dto.ProductResponse>>() {},
            )
            assertThat(productResponse.body!!.data!!.stockQuantity).isEqualTo(0)
        }
    }

    // -----------------------------------------------------------------------

    @DisplayName("쿠폰 중복 사용 동시성 테스트")
    @Nested
    inner class CouponConcurrency {

        @DisplayName("동일 쿠폰을 5개 스레드가 동시에 주문에 사용하면, 정확히 1개만 성공하고 나머지는 409 CONFLICT를 받는다.")
        @Test
        fun onlyOneSucceeds_whenConcurrentCouponUse() {
            // arrange
            val pid = createProduct(stock = 100)
            val templateId = createCouponTemplate()
            val couponId = issueCouponToMember(templateId)

            val threadCount = 5
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val statusCodes = ConcurrentHashMap<Int, HttpStatus>()

            // act
            (0 until threadCount).forEach { i ->
                executorService.submit {
                    try {
                        latch.countDown()
                        latch.await()
                        val request = OrderV1Dto.CreateRequest(
                            items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = pid, quantity = 1)),
                            couponId = couponId,
                        )
                        val response = testRestTemplate.exchange(
                            ORDER_ENDPOINT,
                            HttpMethod.POST,
                            HttpEntity(request, memberHeaders()),
                            object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {},
                        )
                        statusCodes[i] = response.statusCode as HttpStatus
                    } catch (e: Exception) {
                        statusCodes[i] = HttpStatus.INTERNAL_SERVER_ERROR
                    }
                }
            }
            executorService.shutdown()
            executorService.awaitTermination(30, TimeUnit.SECONDS)

            // assert — 정확히 1개만 201, 나머지는 409
            assertThat(statusCodes).hasSize(threadCount)
            assertThat(statusCodes.values.count { it == HttpStatus.CREATED }).isEqualTo(1)
            assertThat(statusCodes.values.count { it == HttpStatus.CONFLICT }).isEqualTo(threadCount - 1)
        }
    }

    // -----------------------------------------------------------------------

    @DisplayName("좋아요 멱등 동시성 테스트")
    @Nested
    inner class LikeConcurrency {

        @DisplayName("동일 상품에 같은 회원이 10개 스레드로 동시에 좋아요하면, 200 OK 또는 409 CONFLICT를 받고 좋아요는 정확히 1건이다.")
        @Test
        fun allSucceed_whenConcurrentLikes() {
            // arrange
            val pid = createProduct(stock = 100)
            val threadCount = 10
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val statusCodes = ConcurrentHashMap<Int, HttpStatus>()

            // act
            (0 until threadCount).forEach { i ->
                executorService.submit {
                    try {
                        latch.countDown()
                        latch.await()
                        val response = testRestTemplate.exchange(
                            "/api/v1/products/$pid/likes",
                            HttpMethod.POST,
                            HttpEntity<Any>(memberHeaders()),
                            object : ParameterizedTypeReference<ApiResponse<Void>>() {},
                        )
                        statusCodes[i] = response.statusCode as HttpStatus
                    } catch (e: Exception) {
                        statusCodes[i] = HttpStatus.INTERNAL_SERVER_ERROR
                    }
                }
            }
            executorService.shutdown()
            executorService.awaitTermination(30, TimeUnit.SECONDS)

            // assert — 200 OK 또는 409 CONFLICT (사전 조회 통과 후 UNIQUE 제약 충돌)
            assertThat(statusCodes).hasSize(threadCount)
            assertThat(statusCodes.values).allMatch { it == HttpStatus.OK || it == HttpStatus.CONFLICT }

            // 좋아요 목록 API로 정확히 1개 좋아요만 존재하는지 확인 (likeCount는 배치 갱신이므로 목록으로 검증)
            val likesResponse = testRestTemplate.exchange(
                "/api/v1/likes",
                HttpMethod.GET,
                HttpEntity<Any>(memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<List<LikeV1Dto.LikedProductResponse>>>() {},
            )
            assertThat(likesResponse.body!!.data).hasSize(1)
            assertThat(likesResponse.body!!.data!![0].productId).isEqualTo(pid)
        }
    }

    // -----------------------------------------------------------------------

    @DisplayName("비관적 락 vs 낙관적 락 비교")
    @Nested
    inner class LockStrategyComparison {

        // === Scenario 1: 동일 자원 1개에 N스레드 경쟁 ===

        @DisplayName("[동일 자원 1개] 비관적 락(재고): 5스레드가 재고 1개를 경쟁 → SELECT FOR UPDATE로 직렬화, 1 성공 4 실패(400)")
        @Test
        fun pessimistic_singleResource_5threads() {
            // arrange
            val pid = createProduct(stock = 1)
            val threadCount = 5
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val statusCodes = ConcurrentHashMap<Int, HttpStatus>()

            // act
            (0 until threadCount).forEach { i ->
                executorService.submit {
                    try {
                        latch.countDown()
                        latch.await()
                        val request = OrderV1Dto.CreateRequest(
                            items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = pid, quantity = 1)),
                        )
                        val response = testRestTemplate.exchange(
                            ORDER_ENDPOINT,
                            HttpMethod.POST,
                            HttpEntity(request, memberHeaders()),
                            object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {},
                        )
                        statusCodes[i] = response.statusCode as HttpStatus
                    } catch (e: Exception) {
                        statusCodes[i] = HttpStatus.INTERNAL_SERVER_ERROR
                    }
                }
            }
            executorService.shutdown()
            executorService.awaitTermination(30, TimeUnit.SECONDS)

            // assert — 비관적 락: 순차 처리되어 정확히 1개 성공, 4개는 재고 부족(400)
            assertThat(statusCodes).hasSize(threadCount)
            assertThat(statusCodes.values.count { it == HttpStatus.CREATED }).isEqualTo(1)
            assertThat(statusCodes.values.count { it == HttpStatus.BAD_REQUEST }).isEqualTo(4)

            val productResponse = testRestTemplate.exchange(
                "$ADMIN_PRODUCT_ENDPOINT/$pid",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<AdminProductV1Dto.ProductResponse>>() {},
            )
            assertThat(productResponse.body!!.data!!.stockQuantity).isEqualTo(0)
        }

        @DisplayName("[동일 자원 1개] 낙관적 락(쿠폰): 5스레드가 쿠폰 1장을 경쟁 → @Version 충돌 감지, 1 성공 4 실패(409)")
        @Test
        fun optimistic_singleResource_5threads() {
            // arrange
            val pid = createProduct(stock = 100)
            val templateId = createCouponTemplate()
            val couponId = issueCouponToMember(templateId)
            val threadCount = 5
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val statusCodes = ConcurrentHashMap<Int, HttpStatus>()

            // act
            (0 until threadCount).forEach { i ->
                executorService.submit {
                    try {
                        latch.countDown()
                        latch.await()
                        val request = OrderV1Dto.CreateRequest(
                            items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = pid, quantity = 1)),
                            couponId = couponId,
                        )
                        val response = testRestTemplate.exchange(
                            ORDER_ENDPOINT,
                            HttpMethod.POST,
                            HttpEntity(request, memberHeaders()),
                            object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {},
                        )
                        statusCodes[i] = response.statusCode as HttpStatus
                    } catch (e: Exception) {
                        statusCodes[i] = HttpStatus.INTERNAL_SERVER_ERROR
                    }
                }
            }
            executorService.shutdown()
            executorService.awaitTermination(30, TimeUnit.SECONDS)

            // assert — 낙관적 락: 동시 읽기 후 version 충돌, 정확히 1개 성공, 4개는 CONFLICT(409)
            assertThat(statusCodes).hasSize(threadCount)
            assertThat(statusCodes.values.count { it == HttpStatus.CREATED }).isEqualTo(1)
            assertThat(statusCodes.values.count { it == HttpStatus.CONFLICT }).isEqualTo(4)
        }

        // === Scenario 2: N개 자원에 N+1스레드 초과 경쟁 ===

        @DisplayName("[초과 경쟁] 비관적 락(재고): 6스레드가 재고 5개를 경쟁 → 순차 차감으로 5 성공, 1 실패(400)")
        @Test
        fun pessimistic_excessCompetition() {
            // arrange
            val stock = 5
            val pid = createProduct(stock)
            val threadCount = stock + 1
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val statusCodes = ConcurrentHashMap<Int, HttpStatus>()

            // act
            (0 until threadCount).forEach { i ->
                executorService.submit {
                    try {
                        latch.countDown()
                        latch.await()
                        val request = OrderV1Dto.CreateRequest(
                            items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = pid, quantity = 1)),
                        )
                        val response = testRestTemplate.exchange(
                            ORDER_ENDPOINT,
                            HttpMethod.POST,
                            HttpEntity(request, memberHeaders()),
                            object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {},
                        )
                        statusCodes[i] = response.statusCode as HttpStatus
                    } catch (e: Exception) {
                        statusCodes[i] = HttpStatus.INTERNAL_SERVER_ERROR
                    }
                }
            }
            executorService.shutdown()
            executorService.awaitTermination(30, TimeUnit.SECONDS)

            // assert — 비관적 락: 5개 순차 차감 성공, 1개 재고 부족(400)
            assertThat(statusCodes).hasSize(threadCount)
            assertThat(statusCodes.values.count { it == HttpStatus.CREATED }).isEqualTo(stock)
            assertThat(statusCodes.values.count { it == HttpStatus.BAD_REQUEST }).isEqualTo(1)

            val productResponse = testRestTemplate.exchange(
                "$ADMIN_PRODUCT_ENDPOINT/$pid",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<AdminProductV1Dto.ProductResponse>>() {},
            )
            assertThat(productResponse.body!!.data!!.stockQuantity).isEqualTo(0)
        }

        @DisplayName("[초과 경쟁] 낙관적 락(쿠폰): 6스레드가 쿠폰 5장을 경쟁 → 5 성공, 중복 사용 1 실패(409)")
        @Test
        fun optimistic_excessCompetition() {
            // arrange — 쿠폰 5장 발급, 6번째 스레드는 첫 번째 쿠폰을 중복 사용 시도
            val pid = createProduct(stock = 100)
            val templateId = createCouponTemplate()
            val couponIds = (0 until 5).map { issueCouponToMember(templateId) }
            val threadCount = 6
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)
            val statusCodes = ConcurrentHashMap<Int, HttpStatus>()

            // act — thread 0~4: 고유 쿠폰, thread 5: couponIds[0] 중복
            (0 until threadCount).forEach { i ->
                executorService.submit {
                    try {
                        latch.countDown()
                        latch.await()
                        val couponForThread = if (i < 5) couponIds[i] else couponIds[0]
                        val request = OrderV1Dto.CreateRequest(
                            items = listOf(OrderV1Dto.CreateOrderItemRequest(productId = pid, quantity = 1)),
                            couponId = couponForThread,
                        )
                        val response = testRestTemplate.exchange(
                            ORDER_ENDPOINT,
                            HttpMethod.POST,
                            HttpEntity(request, memberHeaders()),
                            object : ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {},
                        )
                        statusCodes[i] = response.statusCode as HttpStatus
                    } catch (e: Exception) {
                        statusCodes[i] = HttpStatus.INTERNAL_SERVER_ERROR
                    }
                }
            }
            executorService.shutdown()
            executorService.awaitTermination(30, TimeUnit.SECONDS)

            // assert — 낙관적 락: 고유 쿠폰 4장 + 경쟁 쿠폰 1장 = 5 성공, 중복 사용 1 실패(409)
            assertThat(statusCodes).hasSize(threadCount)
            assertThat(statusCodes.values.count { it == HttpStatus.CREATED }).isEqualTo(5)
            assertThat(statusCodes.values.count { it == HttpStatus.CONFLICT }).isEqualTo(1)
        }
    }
}
