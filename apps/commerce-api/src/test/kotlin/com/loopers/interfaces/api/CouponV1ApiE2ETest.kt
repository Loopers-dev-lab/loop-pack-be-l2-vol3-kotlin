package com.loopers.interfaces.api

import com.loopers.domain.coupon.CouponType
import com.loopers.interfaces.api.admin.coupon.AdminCouponRegisterRequest
import com.loopers.interfaces.api.admin.coupon.AdminCouponResponse
import com.loopers.interfaces.api.coupon.UserCouponResponse
import com.loopers.interfaces.api.user.UserV1Dto
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
import com.loopers.support.constant.AuthHeaders
import com.loopers.support.error.CouponErrorCode
import com.loopers.testcontainers.MySqlTestContainersConfig
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
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestContainersConfig::class)
class CouponV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private val testLoginId = "testuser"
    private val testPassword = "Test123!"

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(AuthHeaders.Admin.LDAP, AuthHeaders.Admin.LDAP_VALUE)
        }
    }

    private fun userHeaders(loginId: String = testLoginId, password: String = testPassword): HttpHeaders {
        return HttpHeaders().apply {
            set(AuthHeaders.User.LOGIN_ID, loginId)
            set(AuthHeaders.User.LOGIN_PW, password)
        }
    }

    private fun registerUser(loginId: String = testLoginId) {
        val request = UserV1Dto.RegisterRequest(
            loginId = loginId, password = testPassword,
            name = "홍길동", birthDate = "1990-01-01", email = "$loginId@example.com",
        )
        testRestTemplate.postForEntity(ApiPaths.Users.REGISTER, request, Any::class.java)
    }

    private fun registerCouponViaAdmin(
        name: String = "테스트 쿠폰",
        type: CouponType = CouponType.FIXED,
        value: Long = 1000,
    ): AdminCouponResponse {
        val request = AdminCouponRegisterRequest(
            name = name, type = type, value = value,
            minOrderAmount = null, expiredAt = ZonedDateTime.now().plusDays(30),
        )
        val responseType = object : ParameterizedTypeReference<ApiResponse<AdminCouponResponse>>() {}
        val response = testRestTemplate.exchange(
            ApiPaths.AdminCoupons.BASE, HttpMethod.POST,
            HttpEntity(request, adminHeaders()), responseType,
        )
        return requireNotNull(response.body?.data)
    }

    private fun issueCoupon(couponId: Long): UserCouponResponse {
        val responseType = object : ParameterizedTypeReference<ApiResponse<UserCouponResponse>>() {}
        val response = testRestTemplate.exchange(
            "${ApiPaths.Coupons.BASE}/$couponId/issue", HttpMethod.POST,
            HttpEntity<Void>(userHeaders()), responseType,
        )
        return requireNotNull(response.body?.data)
    }

    @DisplayName("POST /api/v1/coupons/{couponId}/issue - 쿠폰 발급")
    @Nested
    inner class Issue {

        @DisplayName("정상 발급 시 201 CREATED를 반환한다")
        @Test
        fun success() {
            registerUser()
            val coupon = registerCouponViaAdmin()

            val responseType = object : ParameterizedTypeReference<ApiResponse<UserCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.Coupons.BASE}/${coupon.id}/issue", HttpMethod.POST,
                HttpEntity<Void>(userHeaders()), responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.couponId).isEqualTo(coupon.id) },
                { assertThat(response.body?.data?.status).isEqualTo("AVAILABLE") },
            )
        }

        @DisplayName("이미 발급된 쿠폰을 재발급하면 409를 반환한다")
        @Test
        fun failWhenAlreadyIssued() {
            registerUser()
            val coupon = registerCouponViaAdmin()
            issueCoupon(coupon.id)

            val response = testRestTemplate.exchange(
                "${ApiPaths.Coupons.BASE}/${coupon.id}/issue", HttpMethod.POST,
                HttpEntity<Void>(userHeaders()), ApiResponse::class.java,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(CouponErrorCode.ALREADY_ISSUED_COUPON.code) },
            )
        }

        @DisplayName("인증 없이 발급하면 401을 반환한다")
        @Test
        fun failWhenNotAuthenticated() {
            val coupon = registerCouponViaAdmin()

            val response = testRestTemplate.exchange(
                "${ApiPaths.Coupons.BASE}/${coupon.id}/issue", HttpMethod.POST,
                HttpEntity<Void>(HttpHeaders()), ApiResponse::class.java,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("GET /api/v1/users/me/coupons - 내 쿠폰 목록 조회")
    @Nested
    inner class GetMyCoupons {

        @DisplayName("발급받은 쿠폰 목록을 반환한다")
        @Test
        fun success() {
            registerUser()
            val coupon1 = registerCouponViaAdmin(name = "쿠폰A")
            val coupon2 = registerCouponViaAdmin(name = "쿠폰B")
            issueCoupon(coupon1.id)
            issueCoupon(coupon2.id)

            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResult<UserCouponResponse>>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.Users.ME_COUPONS}?page=0&size=20", HttpMethod.GET,
                HttpEntity<Void>(userHeaders()), responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(2) },
            )
        }

        @DisplayName("상태 필터로 AVAILABLE 쿠폰만 조회한다")
        @Test
        fun filterByStatus() {
            registerUser()
            val coupon1 = registerCouponViaAdmin(name = "사용할 쿠폰")
            val coupon2 = registerCouponViaAdmin(name = "안 쓸 쿠폰")
            issueCoupon(coupon1.id)
            issueCoupon(coupon2.id)

            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResult<UserCouponResponse>>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.Users.ME_COUPONS}?status=AVAILABLE&page=0&size=20", HttpMethod.GET,
                HttpEntity<Void>(userHeaders()), responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(2) },
                {
                    assertThat(response.body?.data?.content).allSatisfy { coupon ->
                        assertThat(coupon.status).isEqualTo("AVAILABLE")
                    }
                },
            )
        }

        @DisplayName("발급받은 쿠폰이 없으면 빈 목록을 반환한다")
        @Test
        fun emptyWhenNoCoupons() {
            registerUser()

            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResult<UserCouponResponse>>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.Users.ME_COUPONS}?page=0&size=20", HttpMethod.GET,
                HttpEntity<Void>(userHeaders()), responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).isEmpty() },
            )
        }

        @DisplayName("인증 없이 조회하면 401을 반환한다")
        @Test
        fun failWhenNotAuthenticated() {
            val response = testRestTemplate.exchange(
                "${ApiPaths.Users.ME_COUPONS}?page=0&size=20", HttpMethod.GET,
                HttpEntity<Void>(HttpHeaders()), ApiResponse::class.java,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
