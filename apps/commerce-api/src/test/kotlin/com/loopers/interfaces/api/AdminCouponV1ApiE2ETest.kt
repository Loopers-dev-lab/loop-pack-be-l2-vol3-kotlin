package com.loopers.interfaces.api

import com.loopers.interfaces.api.admin.coupon.AdminCouponRegisterRequest
import com.loopers.interfaces.api.admin.coupon.AdminCouponResponse
import com.loopers.interfaces.api.admin.coupon.AdminCouponUpdateRequest
import com.loopers.interfaces.api.admin.coupon.AdminUserCouponResponse
import com.loopers.interfaces.api.user.UserV1Dto
import com.loopers.support.PageResult
import com.loopers.support.constant.ApiPaths
import com.loopers.support.constant.AuthHeaders
import com.loopers.support.error.CommonErrorCode
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
class AdminCouponV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(AuthHeaders.Admin.LDAP, AuthHeaders.Admin.LDAP_VALUE)
        }
    }

    private fun userHeaders(loginId: String = "testuser", password: String = "Test123!"): HttpHeaders {
        return HttpHeaders().apply {
            set(AuthHeaders.User.LOGIN_ID, loginId)
            set(AuthHeaders.User.LOGIN_PW, password)
        }
    }

    private fun registerUser(loginId: String = "testuser") {
        val request = UserV1Dto.RegisterRequest(
            loginId = loginId, password = "Test123!",
            name = "홍길동", birthDate = "1990-01-01", email = "$loginId@example.com",
        )
        testRestTemplate.postForEntity(ApiPaths.Users.REGISTER, request, Any::class.java)
    }

    private fun registerCoupon(
        name: String = "테스트 쿠폰",
        type: com.loopers.domain.coupon.CouponType = com.loopers.domain.coupon.CouponType.FIXED,
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

    @DisplayName("POST /api/admin/v1/coupons - 쿠폰 등록")
    @Nested
    inner class Register {

        @DisplayName("정상 등록 시 201 CREATED를 반환한다")
        @Test
        fun success() {
            val request = AdminCouponRegisterRequest(
                name = "1000원 할인", type = com.loopers.domain.coupon.CouponType.FIXED,
                value = 1000, minOrderAmount = 10000, expiredAt = ZonedDateTime.now().plusDays(30),
            )
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                ApiPaths.AdminCoupons.BASE, HttpMethod.POST,
                HttpEntity(request, adminHeaders()), responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.name).isEqualTo("1000원 할인") },
                { assertThat(response.body?.data?.type).isEqualTo("FIXED") },
            )
        }

        @DisplayName("어드민 인증 실패 시 401을 반환한다")
        @Test
        fun failWhenNotAdmin() {
            val request = AdminCouponRegisterRequest(
                name = "쿠폰", type = com.loopers.domain.coupon.CouponType.FIXED,
                value = 1000, minOrderAmount = null, expiredAt = ZonedDateTime.now().plusDays(30),
            )
            val response = testRestTemplate.exchange(
                ApiPaths.AdminCoupons.BASE, HttpMethod.POST,
                HttpEntity(request), ApiResponse::class.java,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(CommonErrorCode.ADMIN_AUTHENTICATION_FAILED.code) },
            )
        }
    }

    @DisplayName("GET /api/admin/v1/coupons - 쿠폰 목록 조회")
    @Nested
    inner class GetList {

        @DisplayName("등록된 쿠폰 목록을 반환한다")
        @Test
        fun success() {
            registerCoupon("쿠폰A")
            registerCoupon("쿠폰B")

            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResult<AdminCouponResponse>>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminCoupons.BASE}?page=0&size=20", HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()), responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(2) },
            )
        }
    }

    @DisplayName("GET /api/admin/v1/coupons/{couponId} - 쿠폰 상세 조회")
    @Nested
    inner class GetCoupon {

        @DisplayName("정상 조회 시 200을 반환한다")
        @Test
        fun success() {
            val coupon = registerCoupon()

            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminCoupons.BASE}/${coupon.id}", HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()), responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(coupon.id) },
            )
        }

        @DisplayName("존재하지 않는 쿠폰 조회 시 404를 반환한다")
        @Test
        fun failWhenNotFound() {
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminCoupons.BASE}/999", HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()), ApiResponse::class.java,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(CouponErrorCode.COUPON_NOT_FOUND.code) },
            )
        }
    }

    @DisplayName("PUT /api/admin/v1/coupons/{couponId} - 쿠폰 수정")
    @Nested
    inner class Update {

        @DisplayName("정상 수정 시 200을 반환한다")
        @Test
        fun success() {
            val coupon = registerCoupon()
            val request = AdminCouponUpdateRequest(
                name = "수정 쿠폰", value = 2000,
                minOrderAmount = 5000, expiredAt = ZonedDateTime.now().plusDays(60),
            )
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminCoupons.BASE}/${coupon.id}", HttpMethod.PUT,
                HttpEntity(request, adminHeaders()), responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("수정 쿠폰") },
                { assertThat(response.body?.data?.value).isEqualTo(2000) },
            )
        }
    }

    @DisplayName("DELETE /api/admin/v1/coupons/{couponId} - 쿠폰 삭제")
    @Nested
    inner class Delete {

        @DisplayName("정상 삭제 시 204를 반환한다")
        @Test
        fun success() {
            val coupon = registerCoupon()

            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminCoupons.BASE}/${coupon.id}", HttpMethod.DELETE,
                HttpEntity<Void>(adminHeaders()), ApiResponse::class.java,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }
    }

    @DisplayName("GET /api/admin/v1/coupons/{couponId}/issues - 발급 내역 조회")
    @Nested
    inner class GetIssues {

        @DisplayName("쿠폰 발급 내역을 반환한다")
        @Test
        fun success() {
            registerUser()
            val coupon = registerCoupon()

            testRestTemplate.exchange(
                "${ApiPaths.Coupons.BASE}/${coupon.id}/issue", HttpMethod.POST,
                HttpEntity<Void>(userHeaders()), ApiResponse::class.java,
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResult<AdminUserCouponResponse>>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminCoupons.BASE}/${coupon.id}/issues?page=0&size=20", HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()), responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(1) },
            )
        }
    }
}
