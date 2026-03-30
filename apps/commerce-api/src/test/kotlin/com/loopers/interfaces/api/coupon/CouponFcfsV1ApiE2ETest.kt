package com.loopers.interfaces.api.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponType
import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.user.UserV1Dto
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
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponFcfsV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val couponJpaRepository: CouponJpaRepository,
    private val couponIssueRequestJpaRepository: CouponIssueRequestJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        private const val TEST_LOGIN_ID = "testuser1"
        private const val TEST_PASSWORD = "Password1!"
    }

    private lateinit var savedCoupon: Coupon

    @BeforeEach
    fun setUp() {
        testRestTemplate.exchange(
            "/api/v1/users",
            HttpMethod.POST,
            HttpEntity(
                UserV1Dto.SignUpRequest(
                    loginId = TEST_LOGIN_ID,
                    password = TEST_PASSWORD,
                    name = "테스트유저",
                    birthDate = LocalDate.of(1990, 1, 15),
                    email = "test@example.com",
                ),
            ),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
        val coupon = Coupon(
            name = "선착순 100장 쿠폰",
            type = CouponType.FIXED,
            value = BigDecimal("5000"),
            minOrderAmount = null,
            expiredAt = ZonedDateTime.now().plusDays(7),
        )
        ReflectionTestUtils.setField(coupon, "maxIssueCount", 100)
        savedCoupon = couponJpaRepository.save(coupon)
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun authHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", TEST_LOGIN_ID)
            set("X-Loopers-LoginPw", TEST_PASSWORD)
        }
    }

    @DisplayName("선착순 쿠폰 발급을 요청할 때,")
    @Nested
    inner class RequestFcfsIssue {

        @DisplayName("정상 요청이면, 202 Accepted와 requestId를 반환한다.")
        @Test
        fun returns202_whenValidRequest() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/${savedCoupon.id}/fcfs-issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED) },
                { assertThat(response.body?.data?.get("requestId")).isNotNull() },
            )

            val requests = couponIssueRequestJpaRepository.findAll()
            assertThat(requests).hasSize(1)
            assertThat(requests[0].status.name).isEqualTo("PENDING")
        }

        @DisplayName("선착순 쿠폰이 아니면, 400을 반환한다.")
        @Test
        fun returns400_whenNotFcfsCoupon() {
            // arrange
            val normalCoupon = couponJpaRepository.save(
                Coupon(
                    name = "일반 쿠폰",
                    type = CouponType.FIXED,
                    value = BigDecimal("3000"),
                    minOrderAmount = null,
                    expiredAt = ZonedDateTime.now().plusDays(7),
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/${normalCoupon.id}/fcfs-issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("발급 상태를 조회할 때,")
    @Nested
    inner class GetFcfsStatus {

        @DisplayName("존재하는 requestId이면, 상태를 반환한다.")
        @Test
        fun returnsStatus_whenValidRequestId() {
            // arrange — 발급 요청
            val issueResponseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val issueResponse = testRestTemplate.exchange(
                "/api/v1/coupons/${savedCoupon.id}/fcfs-issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                issueResponseType,
            )
            val requestId = issueResponse.body?.data?.get("requestId") as String

            // act — 상태 조회
            val statusResponseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val statusResponse = testRestTemplate.exchange(
                "/api/v1/coupons/fcfs-issue/status?requestId=$requestId",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                statusResponseType,
            )

            // assert
            assertAll(
                { assertThat(statusResponse.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(statusResponse.body?.data?.get("requestId")).isEqualTo(requestId) },
                { assertThat(statusResponse.body?.data?.get("status")).isEqualTo("PENDING") },
            )
        }
    }
}
