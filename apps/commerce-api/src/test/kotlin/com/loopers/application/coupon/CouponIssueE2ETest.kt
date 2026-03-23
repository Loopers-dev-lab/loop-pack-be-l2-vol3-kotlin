package com.loopers.application.coupon

import com.loopers.interfaces.api.coupon.dto.CouponAdminV1Dto
import com.loopers.interfaces.api.coupon.dto.CouponV1Dto
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponIssueE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders = HttpHeaders().apply {
        set(HEADER_LDAP, LDAP_ADMIN_VALUE)
        set("Content-Type", "application/json")
    }

    private fun authHeaders(loginId: String = "testuser1"): HttpHeaders = HttpHeaders().apply {
        set(HEADER_LOGIN_ID, loginId)
        set(HEADER_LOGIN_PW, "Password1!")
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

    private fun createCoupon(): Long {
        val request = CouponAdminV1Dto.CreateCouponRequest(
            name = "선착순 쿠폰",
            type = "FIXED",
            value = 5000,
            totalQuantity = 100,
            expiredAt = ZonedDateTime.now().plusDays(30).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        )
        val response = testRestTemplate.exchange(
            "/api-admin/v1/coupons",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {},
        )
        return response.body!!.data!!.id
    }

    @Nested
    @DisplayName("POST /api/v1/coupons/{couponId}/issue-async")
    inner class IssueAsync {

        @Test
        fun `쿠폰 발급 요청 시 202와 requestId가 반환된다`() {
            signUp()
            val couponId = createCoupon()

            val response = testRestTemplate.exchange(
                "/api/v1/coupons/$couponId/issue-async",
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssueAsyncResponse>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
            assertThat(response.body!!.data!!.requestId).isNotBlank()
        }
    }

    @Nested
    @DisplayName("GET /api/v1/coupons/issue/{requestId}")
    inner class GetIssueStatus {

        @Test
        fun `발급 상태를 조회하면 PENDING이 반환된다`() {
            signUp()
            val couponId = createCoupon()

            val issueResponse = testRestTemplate.exchange(
                "/api/v1/coupons/$couponId/issue-async",
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssueAsyncResponse>>() {},
            )
            val requestId = issueResponse.body!!.data!!.requestId

            val response = testRestTemplate.exchange(
                "/api/v1/coupons/issue/$requestId",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssueStatusResponse>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body!!.data!!.requestId).isEqualTo(requestId)
            assertThat(response.body!!.data!!.status).isEqualTo("PENDING")
        }

        @Test
        fun `존재하지 않는 requestId 조회 시 404가 반환된다`() {
            signUp()

            val response = testRestTemplate.exchange(
                "/api/v1/coupons/issue/nonexistent-id",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
