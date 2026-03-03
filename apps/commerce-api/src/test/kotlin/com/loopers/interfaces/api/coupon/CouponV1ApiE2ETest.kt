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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponV1ApiE2ETest @Autowired constructor(
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
        set("Content-Type", "application/json")
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

    @Nested
    @DisplayName("POST /api/v1/coupons/{couponId}/issue")
    inner class IssueCoupon {

        @Test
        @DisplayName("정상적으로 쿠폰을 발급받으면 200 OK를 반환한다")
        fun issueCoupon_success() {
            // arrange
            signUp()
            val couponId = createCoupon()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/$couponId/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.get("couponId")).isEqualTo(couponId.toInt()) },
                { assertThat(response.body?.data?.get("status")).isEqualTo("AVAILABLE") },
            )
        }

        @Test
        @DisplayName("동일 쿠폰을 중복 발급하면 400을 반환한다")
        fun issueCoupon_duplicate_returnsBadRequest() {
            // arrange
            signUp()
            val couponId = createCoupon()

            // 첫 번째 발급
            testRestTemplate.exchange(
                "/api/v1/coupons/$couponId/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // act - 중복 발급 시도
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/$couponId/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("수량이 소진된 쿠폰 발급 시 400을 반환한다")
        fun issueCoupon_exhausted_returnsBadRequest() {
            // arrange
            val couponId = createCoupon(totalQuantity = 1)

            // 첫 번째 사용자가 발급
            signUp("user1")
            testRestTemplate.exchange(
                "/api/v1/coupons/$couponId/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders("user1")),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // 두 번째 사용자가 발급 시도
            signUp("user2")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/$couponId/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders("user2")),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("만료된 쿠폰 발급 시 400을 반환한다")
        fun issueCoupon_expired_returnsBadRequest() {
            // arrange
            signUp()
            val expiredAt = ZonedDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val couponId = createCoupon(expiredAt = expiredAt)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/$couponId/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/me/coupons")
    inner class GetMyCoupons {

        @Test
        @DisplayName("쿠폰 발급 후 내 쿠폰 목록에 발급된 쿠폰이 포함된다")
        fun getMyCoupons_afterIssue_containsIssuedCoupon() {
            // arrange
            signUp()
            val couponId = createCoupon(name = "내 쿠폰 테스트")
            testRestTemplate.exchange(
                "/api/v1/coupons/$couponId/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<List<Map<String, Any>>>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/users/me/coupons",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).hasSize(1) },
                { assertThat(response.body?.data?.first()?.get("couponName")).isEqualTo("내 쿠폰 테스트") },
                { assertThat(response.body?.data?.first()?.get("status")).isEqualTo("AVAILABLE") },
            )
        }
    }
}
