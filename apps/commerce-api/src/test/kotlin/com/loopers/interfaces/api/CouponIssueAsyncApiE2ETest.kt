package com.loopers.interfaces.api

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponQuantity
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.Discount
import com.loopers.domain.coupon.DiscountType
import com.loopers.interfaces.api.user.UserDto
import com.loopers.interfaces.common.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
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
import org.springframework.http.MediaType
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponIssueAsyncApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val couponRepository: CouponRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
    companion object {
        private const val ISSUE_ASYNC_ENDPOINT = "/api/v1/coupons/{couponId}/issue-async"
        private const val GET_ISSUE_REQUEST_ENDPOINT = "/api/v1/coupons/issue-requests/{requestId}"
        private const val SIGNUP_ENDPOINT = "/api/v1/users/signup"
        private const val LOGIN_ID_HEADER = "X-Loopers-LoginId"
        private const val LOGIN_PW_HEADER = "X-Loopers-LoginPw"
        private val RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun signUp(loginId: String = "testuser123", password: String = "Test1234!@") {
        val request = UserDto.SignUpRequest(
            loginId = loginId,
            password = password,
            name = "홍길동",
            email = "test@example.com",
            birthday = java.time.LocalDate.of(1990, 1, 15),
        )
        val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        testRestTemplate.exchange(
            SIGNUP_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(request, headers),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
    }

    private fun authHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(LOGIN_ID_HEADER, "testuser123")
            set(LOGIN_PW_HEADER, "Test1234!@")
        }
    }

    private fun createCoupon(
        totalQuantity: Int = 100,
        expiresAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Coupon {
        return couponRepository.save(
            Coupon(
                name = "선착순 쿠폰",
                discount = Discount(DiscountType.FIXED_AMOUNT, 5000L),
                quantity = CouponQuantity(totalQuantity, 0),
                expiresAt = expiresAt,
            ),
        )
    }

    @Nested
    @DisplayName("POST /api/v1/coupons/{couponId}/issue-async")
    inner class IssueAsync {

        @Test
        @DisplayName("유효한 쿠폰 발급 요청 시 202 Accepted와 requestId를 반환한다")
        fun `유효한 쿠폰 발급 요청 시 202 Accepted와 requestId를 반환한다`() {
            // given
            signUp()
            val coupon = createCoupon()

            // when
            val response = testRestTemplate.exchange(
                ISSUE_ASYNC_ENDPOINT,
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                RESPONSE_TYPE,
                coupon.id,
            )

            // then
            assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
            assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS)
            assertThat(response.body?.data?.get("requestId")).isNotNull()
            assertThat(response.body?.data?.get("status")).isEqualTo("PENDING")
        }
    }

    @Nested
    @DisplayName("GET /api/v1/coupons/issue-requests/{requestId}")
    inner class GetIssueRequest {

        @Test
        @DisplayName("발급 요청 후 결과를 조회하면 200 OK와 PENDING 상태를 반환한다")
        fun `발급 요청 후 결과를 조회하면 200 OK와 PENDING 상태를 반환한다`() {
            // given
            signUp()
            val coupon = createCoupon()
            val issueResponse = testRestTemplate.exchange(
                ISSUE_ASYNC_ENDPOINT,
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                RESPONSE_TYPE,
                coupon.id,
            )
            val requestId = issueResponse.body?.data?.get("requestId") as String

            // when
            val response = testRestTemplate.exchange(
                GET_ISSUE_REQUEST_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                RESPONSE_TYPE,
                requestId,
            )

            // then
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS)
            assertThat(response.body?.data?.get("requestId")).isEqualTo(requestId)
            assertThat(response.body?.data?.get("status")).isEqualTo("PENDING")
            assertThat(response.body?.data?.get("createdAt")).isNotNull()
        }
    }
}
