package com.loopers.interfaces.api.user.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
import com.loopers.interfaces.api.ApiResponse
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
import java.time.ZonedDateTime

@DisplayName("User 쿠폰 발급/목록 Integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserCouponV1ControllerIntegrationTest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userRepository: UserRepository,
    private val couponRepository: CouponRepository,
    private val passwordHasher: UserPasswordHasher,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val LOGIN_ID = "testuser1"
        private const val PASSWORD = "Password1!"
    }

    private var couponId: Long = 0

    @BeforeEach
    fun setUp() {
        val user = User.register(
            loginId = LOGIN_ID,
            rawPassword = PASSWORD,
            name = "홍길동",
            birthDate = LocalDate.of(1990, 1, 1),
            email = "test@example.com",
            passwordHasher = passwordHasher,
        )
        userRepository.save(user)

        val coupon = Coupon.register(
            name = "테스트 쿠폰",
            type = Coupon.Type.FIXED,
            discountValue = 1000,
            minOrderAmount = null,
            expiredAt = ZonedDateTime.now().plusDays(30),
        )
        couponId = couponRepository.save(coupon).id!!
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun authHeaders(): HttpHeaders = HttpHeaders().apply {
        set("X-Loopers-LoginId", LOGIN_ID)
        set("X-Loopers-LoginPw", PASSWORD)
    }

    @Nested
    @DisplayName("POST /api/v1/coupons/{couponId}/issue")
    inner class Issue {
        @Test
        @DisplayName("유효한 쿠폰 발급 → 202 Accepted, REQUESTED 상태")
        fun issue_validCoupon_returns202() {
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/$couponId/issue",
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<UserCouponV1Response.IssueRequest>>() {},
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED) },
                { assertThat(response.body?.data?.requestId).isGreaterThan(0L) },
                { assertThat(response.body?.data?.couponId).isEqualTo(couponId) },
                { assertThat(response.body?.data?.status).isEqualTo("REQUESTED") },
            )
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/me/coupon-issue-requests/{requestId}")
    inner class GetIssueRequestStatus {
        @Test
        @DisplayName("쿠폰 발급 후 요청 상태를 조회하면 REQUESTED를 반환한다")
        fun getIssueRequestStatus_afterIssue_returnsRequested() {
            val issueResponse = testRestTemplate.exchange(
                "/api/v1/coupons/$couponId/issue",
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<UserCouponV1Response.IssueRequest>>() {},
            )
            val requestId = issueResponse.body?.data?.requestId ?: error("issue request id is missing")

            val response = testRestTemplate.exchange(
                "/api/v1/users/me/coupon-issue-requests/$requestId",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                object : ParameterizedTypeReference<ApiResponse<UserCouponV1Response.IssueRequestStatus>>() {},
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.requestId).isEqualTo(requestId) },
                { assertThat(response.body?.data?.couponId).isEqualTo(couponId) },
                { assertThat(response.body?.data?.status).isEqualTo("REQUESTED") },
                { assertThat(response.body?.data?.failureReasonCode).isNull() },
                { assertThat(response.body?.data?.issuedCouponId).isNull() },
            )
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/me/coupons")
    inner class GetList {
        @Test
        @DisplayName("발급받은 쿠폰 없음 → 200 OK, 빈 목록")
        fun getList_noCoupons_returnsEmpty() {
            val response = testRestTemplate.exchange(
                "/api/v1/users/me/coupons",
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                object :
                    ParameterizedTypeReference<ApiResponse<List<UserCouponV1Response.ListItem>>>() {},
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isEmpty() },
            )
        }
    }
}
