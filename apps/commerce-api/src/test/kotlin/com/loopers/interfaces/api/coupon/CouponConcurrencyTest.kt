package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.api.coupon.dto.CouponAdminV1Dto
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
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.Timeout

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponConcurrencyTest @Autowired constructor(
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
        testRestTemplate.exchange(
            "/api/v1/users/sign-up",
            HttpMethod.POST,
            HttpEntity(request),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
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
        return response.body!!.data!!.id
    }

    @Nested
    @DisplayName("쿠폰 동시 발급 테스트")
    inner class ConcurrentCouponIssue {

        @Test
        @Timeout(60)
        @DisplayName("totalQuantity=10인 쿠폰에 50명이 동시에 발급 요청하면 10명만 성공한다")
        fun concurrentIssue_onlyTotalQuantitySucceeds() {
            // arrange
            val totalQuantity = 10
            val concurrentUsers = 50
            val couponId = createCoupon(totalQuantity)

            for (i in 1..concurrentUsers) {
                signUp("user$i")
            }

            val executorService = Executors.newFixedThreadPool(concurrentUsers)
            val readyLatch = CountDownLatch(concurrentUsers)
            val startLatch = CountDownLatch(1)
            val latch = CountDownLatch(concurrentUsers)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            // act
            for (i in 1..concurrentUsers) {
                executorService.submit {
                    try {
                        readyLatch.countDown()
                        startLatch.await(30, TimeUnit.SECONDS)
                        val responseType =
                            object : ParameterizedTypeReference<ApiResponse<Any>>() {}
                        val response = testRestTemplate.exchange(
                            "/api/v1/coupons/$couponId/issue",
                            HttpMethod.POST,
                            HttpEntity<Any>(authHeaders("user$i")),
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

            readyLatch.await(30, TimeUnit.SECONDS)
            startLatch.countDown()
            latch.await(30, TimeUnit.SECONDS)
            executorService.shutdown()

            // assert - 어드민 API로 issuedCount 확인
            val adminResponseType =
                object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
            val adminResponse = testRestTemplate.exchange(
                "/api-admin/v1/coupons/$couponId",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                adminResponseType,
            )
            val issuedCount = adminResponse.body!!.data!!.issuedCount

            assertAll(
                { assertThat(successCount.get()).isEqualTo(totalQuantity) },
                { assertThat(failCount.get()).isEqualTo(concurrentUsers - totalQuantity) },
                { assertThat(issuedCount).isEqualTo(totalQuantity) },
            )
        }
    }
}
