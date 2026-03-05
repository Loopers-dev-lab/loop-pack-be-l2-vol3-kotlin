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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Timeout

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderConcurrencyTest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun signUp(loginId: String) {
        val request = UserV1Dto.SignUpRequest(
            loginId = loginId,
            password = "Password1!",
            name = "테스트유저",
            birthDate = LocalDate.of(1990, 1, 15),
            email = "$loginId@example.com",
        )
        val response = testRestTemplate.exchange(
            "/api/v1/users/sign-up",
            HttpMethod.POST,
            HttpEntity(request),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
        assertThat(response.statusCode.is2xxSuccessful).describedAs("회원가입 API 호출이 성공해야 합니다: ${response.statusCode}").isTrue()
    }

    private fun authHeaders(loginId: String): HttpHeaders {
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
        assertThat(response.statusCode.is2xxSuccessful).describedAs("브랜드 생성 API 호출이 성공해야 합니다: ${response.statusCode}").isTrue()
        val body = requireNotNull(response.body) { "브랜드 생성 응답 body가 null입니다" }
        val data = requireNotNull(body.data) { "브랜드 생성 응답 data가 null입니다" }
        return (data["id"] as Number).toLong()
    }

    private fun createProduct(brandId: Long, stock: Int): Long {
        val request = ProductAdminV1Dto.CreateProductRequest(
            brandId = brandId,
            name = "테스트 상품",
            price = BigDecimal("10000"),
            stock = stock,
        )
        val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
        val response = testRestTemplate.exchange(
            "/api-admin/v1/products",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        assertThat(response.statusCode.is2xxSuccessful).describedAs("상품 생성 API 호출이 성공해야 합니다: ${response.statusCode}").isTrue()
        val body = requireNotNull(response.body) { "상품 생성 응답 body가 null입니다" }
        val data = requireNotNull(body.data) { "상품 생성 응답 data가 null입니다" }
        return (data["id"] as Number).toLong()
    }

    private fun createCoupon(totalQuantity: Int): Long {
        val expiredAt = ZonedDateTime.now().plusDays(30)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val request = CouponAdminV1Dto.CreateCouponRequest(
            name = "동시성 테스트 쿠폰",
            type = "FIXED",
            value = 1000,
            totalQuantity = totalQuantity,
            expiredAt = expiredAt,
        )
        val responseType =
            object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
        val response = testRestTemplate.exchange(
            "/api-admin/v1/coupons",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        assertThat(response.statusCode.is2xxSuccessful).describedAs("쿠폰 생성 API 호출이 성공해야 합니다: ${response.statusCode}").isTrue()
        val body = requireNotNull(response.body) { "쿠폰 생성 응답 body가 null입니다" }
        val data = requireNotNull(body.data) { "쿠폰 생성 응답 data가 null입니다" }
        return data.id
    }

    private fun issueCoupon(loginId: String, couponId: Long): Long {
        val responseType =
            object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
        val response = testRestTemplate.exchange(
            "/api/v1/coupons/$couponId/issue",
            HttpMethod.POST,
            HttpEntity<Any>(authHeaders(loginId)),
            responseType,
        )
        assertThat(response.statusCode.is2xxSuccessful).describedAs("쿠폰 발급 API 호출이 성공해야 합니다: ${response.statusCode}").isTrue()
        val body = requireNotNull(response.body) { "쿠폰 발급 응답 body가 null입니다" }
        val data = requireNotNull(body.data) { "쿠폰 발급 응답 data가 null입니다" }
        return (data["id"] as Number).toLong()
    }

    @Nested
    @DisplayName("쿠폰 동시 사용 (주문) 테스트")
    inner class ConcurrentCouponUsage {

        @Test
        @Timeout(60)
        @DisplayName("동일한 발급 쿠폰으로 여러 스레드에서 동시 주문 시 1건만 성공한다")
        fun concurrentOrderWithSameCoupon_onlyOneSucceeds() {
            // arrange
            val loginId = "couponuser1"
            signUp(loginId)
            val brandId = createBrand()
            val productId = createProduct(brandId, 100)
            val couponId = createCoupon(10)
            val issuedCouponId = issueCoupon(loginId, couponId)

            val concurrentRequests = 10
            val executorService = Executors.newFixedThreadPool(concurrentRequests)
            val readyLatch = CountDownLatch(concurrentRequests)
            val startLatch = CountDownLatch(1)
            val latch = CountDownLatch(concurrentRequests)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act
            try {
                for (i in 1..concurrentRequests) {
                    executorService.submit {
                        try {
                            readyLatch.countDown()
                            startLatch.await(30, TimeUnit.SECONDS)
                            val orderRequest = OrderV1Dto.CreateOrderRequest(
                                items = listOf(
                                    OrderV1Dto.CreateOrderItemRequest(
                                        productId = productId,
                                        quantity = 1,
                                    ),
                                ),
                                issuedCouponId = issuedCouponId,
                            )
                            val responseType =
                                object : ParameterizedTypeReference<ApiResponse<Any>>() {}
                            val response = testRestTemplate.exchange(
                                "/api/v1/orders",
                                HttpMethod.POST,
                                HttpEntity(orderRequest, authHeaders(loginId)),
                                responseType,
                            )
                            if (response.statusCode == HttpStatus.OK) {
                                successCount.incrementAndGet()
                            } else {
                                failCount.incrementAndGet()
                            }
                        } catch (e: Exception) {
                            failCount.incrementAndGet()
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                val allReady = readyLatch.await(30, TimeUnit.SECONDS)
                assertThat(allReady).describedAs("모든 스레드가 준비되어야 합니다").isTrue()
                startLatch.countDown()
                val allDone = latch.await(30, TimeUnit.SECONDS)
                assertThat(allDone).describedAs("모든 스레드가 완료되어야 합니다").isTrue()
            } finally {
                executorService.shutdownNow()
                executorService.awaitTermination(5, TimeUnit.SECONDS)
            }

            // assert
            assertAll(
                { assertThat(successCount.get()).isEqualTo(1) },
                { assertThat(failCount.get()).isEqualTo(concurrentRequests - 1) },
            )
        }
    }

    @Nested
    @DisplayName("재고 동시 차감 테스트")
    inner class ConcurrentStockDeduction {

        @Test
        @Timeout(60)
        @DisplayName("재고 10개 상품에 20명이 동시 주문하면 최대 10건만 성공한다")
        fun concurrentOrder_stockLimitEnforced() {
            // arrange
            val stock = 10
            val concurrentUsers = 20
            val brandId = createBrand()
            val productId = createProduct(brandId, stock)

            for (i in 1..concurrentUsers) {
                signUp("stockuser$i")
            }

            val executorService = Executors.newFixedThreadPool(concurrentUsers)
            val readyLatch = CountDownLatch(concurrentUsers)
            val startLatch = CountDownLatch(1)
            val latch = CountDownLatch(concurrentUsers)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act
            try {
                for (i in 1..concurrentUsers) {
                    executorService.submit {
                        try {
                            readyLatch.countDown()
                            startLatch.await(30, TimeUnit.SECONDS)
                            val orderRequest = OrderV1Dto.CreateOrderRequest(
                                items = listOf(
                                    OrderV1Dto.CreateOrderItemRequest(
                                        productId = productId,
                                        quantity = 1,
                                    ),
                                ),
                            )
                            val responseType =
                                object : ParameterizedTypeReference<ApiResponse<Any>>() {}
                            val response = testRestTemplate.exchange(
                                "/api/v1/orders",
                                HttpMethod.POST,
                                HttpEntity(orderRequest, authHeaders("stockuser$i")),
                                responseType,
                            )
                            if (response.statusCode == HttpStatus.OK) {
                                successCount.incrementAndGet()
                            } else {
                                failCount.incrementAndGet()
                            }
                        } catch (e: Exception) {
                            failCount.incrementAndGet()
                        } finally {
                            latch.countDown()
                        }
                    }
                }

                val allReady = readyLatch.await(30, TimeUnit.SECONDS)
                assertThat(allReady).describedAs("모든 스레드가 준비되어야 합니다").isTrue()
                startLatch.countDown()
                val allDone = latch.await(30, TimeUnit.SECONDS)
                assertThat(allDone).describedAs("모든 스레드가 완료되어야 합니다").isTrue()
            } finally {
                executorService.shutdownNow()
                executorService.awaitTermination(5, TimeUnit.SECONDS)
            }

            // assert - 어드민 API로 최종 재고 확인
            val adminResponseType =
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val adminResponse = testRestTemplate.exchange(
                "/api-admin/v1/products/$productId",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                adminResponseType,
            )
            val adminBody = requireNotNull(adminResponse.body) { "재고 조회 응답 body가 null입니다" }
            val adminData = requireNotNull(adminBody.data) { "재고 조회 응답 data가 null입니다" }
            val remainingStock = (adminData["stock"] as Number).toInt()

            assertAll(
                { assertThat(successCount.get() + failCount.get()).isEqualTo(concurrentUsers) },
                { assertThat(successCount.get()).isEqualTo(stock) },
                { assertThat(failCount.get()).isEqualTo(concurrentUsers - stock) },
                { assertThat(remainingStock).isEqualTo(0) },
            )
        }
    }
}
