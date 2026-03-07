package com.loopers.interfaces.api.coupon

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponType
import com.loopers.infrastructure.coupon.CouponJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.constant.HttpHeaders
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
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders as SpringHttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val couponJpaRepository: CouponJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_SIGNUP = "/api/v1/users"
        private const val ENDPOINT_ISSUE = "/api/v1/coupons"
        private const val ENDPOINT_MY_COUPONS = "/api/v1/users/me/coupons"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun signUpAndGetHeaders(
        loginId: String = "testuser1",
        password: String = "Abcd1234!",
    ): SpringHttpHeaders {
        val signUpRequest = mapOf(
            "loginId" to loginId,
            "password" to password,
            "name" to "홍길동",
            "birthday" to "1990-01-15",
            "email" to "test@example.com",
        )
        testRestTemplate.postForEntity<Any>(ENDPOINT_SIGNUP, signUpRequest)

        return SpringHttpHeaders().apply {
            set(HttpHeaders.LOGIN_ID, loginId)
            set(HttpHeaders.LOGIN_PW, password)
        }
    }

    private fun createCoupon(
        name: String = "테스트 쿠폰",
        type: CouponType = CouponType.FIXED,
        value: Long = 5000,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(7),
    ): Coupon {
        return couponJpaRepository.save(
            Coupon(
                name = name,
                type = type,
                value = value,
                expiredAt = expiredAt,
            ),
        )
    }

    @Nested
    @DisplayName("쿠폰 발급")
    inner class IssueCoupon {

        @Test
        @DisplayName("유효한 쿠폰을 발급하면 200 OK와 발급 정보를 반환한다")
        fun issueCouponSuccess() {
            // arrange
            val headers = signUpAndGetHeaders()
            val coupon = createCoupon()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.CouponIssueResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT_ISSUE/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.couponId).isEqualTo(coupon.id) },
                { assertThat(response.body?.data?.status).isEqualTo("AVAILABLE") },
            )
        }

        @Test
        @DisplayName("이미 발급받은 쿠폰을 다시 발급하면 409 CONFLICT를 반환한다")
        fun issueCouponDuplicate() {
            // arrange
            val headers = signUpAndGetHeaders()
            val coupon = createCoupon()

            // 첫 번째 발급
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.CouponIssueResponse>>() {}
            testRestTemplate.exchange(
                "$ENDPOINT_ISSUE/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                responseType,
            )

            // act — 같은 쿠폰 다시 발급
            val duplicateResponseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT_ISSUE/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                duplicateResponseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @Test
        @DisplayName("만료된 쿠폰을 발급하면 400 BAD_REQUEST를 반환한다")
        fun issueCouponExpired() {
            // arrange
            val headers = signUpAndGetHeaders()
            val expiredCoupon = createCoupon(
                name = "만료 쿠폰",
                expiredAt = ZonedDateTime.now().minusDays(1),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT_ISSUE/${expiredCoupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("인증 없이 쿠폰을 발급하면 401 UNAUTHORIZED를 반환한다")
        fun issueCouponUnauthorized() {
            // arrange
            val coupon = createCoupon()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT_ISSUE/${coupon.id}/issue",
                HttpMethod.POST,
                null,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @Nested
    @DisplayName("내 쿠폰 목록 조회")
    inner class GetMyCoupons {

        @Test
        @DisplayName("발급받은 쿠폰 목록을 조회하면 200 OK와 쿠폰 정보를 반환한다")
        fun getMyCouponsSuccess() {
            // arrange
            val headers = signUpAndGetHeaders()
            val coupon1 = createCoupon(name = "5000원 쿠폰", type = CouponType.FIXED, value = 5000)
            val coupon2 = createCoupon(name = "10% 쿠폰", type = CouponType.RATE, value = 10)

            // 쿠폰 2장 발급
            val issueType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.CouponIssueResponse>>() {}
            testRestTemplate.exchange(
                "$ENDPOINT_ISSUE/${coupon1.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                issueType,
            )
            testRestTemplate.exchange(
                "$ENDPOINT_ISSUE/${coupon2.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(headers),
                issueType,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<List<CouponV1Dto.MyCouponResponse>>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_MY_COUPONS,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).hasSize(2) },
            )
        }

        @Test
        @DisplayName("발급받은 쿠폰이 없으면 200 OK와 빈 목록을 반환한다")
        fun getMyCouponsEmpty() {
            // arrange
            val headers = signUpAndGetHeaders()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<List<CouponV1Dto.MyCouponResponse>>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_MY_COUPONS,
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isEmpty() },
            )
        }
    }
}
