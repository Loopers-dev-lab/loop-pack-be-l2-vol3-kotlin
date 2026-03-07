package com.loopers.interfaces.api.coupon

import com.loopers.domain.coupon.CouponType
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.user.UserV1Dto
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        private const val TEST_LOGIN_ID = "testuser1"
        private const val TEST_PASSWORD = "Password1!"
        private const val ADMIN_LDAP = "loopers.admin"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createTestUser(
        loginId: String = TEST_LOGIN_ID,
        password: String = TEST_PASSWORD,
    ) {
        val request = UserV1Dto.SignUpRequest(
            loginId = loginId,
            password = password,
            name = "홍길동",
            birthDate = LocalDate.of(1990, 1, 15),
            email = "test@example.com",
        )
        val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
        testRestTemplate.exchange(
            "/api/v1/users",
            HttpMethod.POST,
            HttpEntity(request),
            responseType,
        )
    }

    private fun authHeaders(
        loginId: String = TEST_LOGIN_ID,
        password: String = TEST_PASSWORD,
    ): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", password)
        }
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-Ldap", ADMIN_LDAP)
        }
    }

    private fun createTestCoupon(
        name: String = "5000원 할인 쿠폰",
        type: CouponType = CouponType.FIXED,
        value: BigDecimal = BigDecimal("5000"),
        minOrderAmount: BigDecimal? = BigDecimal("10000"),
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): CouponAdminV1Dto.CouponAdminResponse? {
        val request = CouponAdminV1Dto.CreateRequest(
            name = name,
            type = type,
            value = value,
            minOrderAmount = minOrderAmount,
            expiredAt = expiredAt,
        )
        val responseType = object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
        val response = testRestTemplate.exchange(
            "/api-admin/v1/coupons",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        return response.body?.data
    }

    @DisplayName("POST /api/v1/coupons/{couponId}/issue")
    @Nested
    inner class IssueCoupon {

        @DisplayName("정상적으로 쿠폰을 발급하면, 200 OK와 발급 정보를 반환한다.")
        @Test
        fun returnsOk_whenIssueSucceeds() {
            // arrange
            createTestUser()
            val coupon = createTestCoupon()!!

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssuedCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.couponId).isEqualTo(coupon.id) },
                { assertThat(response.body?.data?.status?.name).isEqualTo("AVAILABLE") },
            )
        }

        @DisplayName("인증 실패 시, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenAuthFails() {
            // arrange
            val coupon = createTestCoupon()!!

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders("invalid", "invalid")),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("만료된 쿠폰을 발급하면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenCouponExpired() {
            // arrange
            createTestUser()
            val coupon = createTestCoupon(expiredAt = ZonedDateTime.now().minusDays(1))!!

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("중복 발급하면, 409 CONFLICT를 반환한다.")
        @Test
        fun returnsConflict_whenAlreadyIssued() {
            // arrange
            createTestUser()
            val coupon = createTestCoupon()!!
            val issueType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            testRestTemplate.exchange(
                "/api/v1/coupons/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                issueType,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @DisplayName("존재하지 않는 쿠폰을 발급하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenCouponNotExists() {
            // arrange
            createTestUser()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/999/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api/v1/users/me/coupons")
    @Nested
    inner class GetMyCoupons {

        @DisplayName("발급받은 쿠폰 목록을 정상 조회하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenGetMyCouponsSucceeds() {
            // arrange
            createTestUser()
            val coupon = createTestCoupon()!!
            val issueType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            testRestTemplate.exchange(
                "/api/v1/coupons/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                issueType,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<List<CouponV1Dto.IssuedCouponResponse>>>() {}
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
            )
        }

        @DisplayName("인증 실패 시, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenAuthFails() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/users/me/coupons",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders("invalid", "invalid")),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("GET /api-admin/v1/coupons")
    @Nested
    inner class GetAllCoupons {

        @DisplayName("쿠폰이 존재하면, 200 OK와 목록을 반환한다.")
        @Test
        fun returnsOk_whenCouponsExist() {
            // arrange
            createTestCoupon(name = "쿠폰 A")
            createTestCoupon(name = "쿠폰 B")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/coupons?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull() },
            )
        }
    }

    @DisplayName("GET /api-admin/v1/coupons/{couponId}")
    @Nested
    inner class GetCoupon {

        @DisplayName("존재하는 쿠폰을 조회하면, 200 OK와 쿠폰 정보를 반환한다.")
        @Test
        fun returnsOk_whenCouponExists() {
            // arrange
            val created = createTestCoupon()!!

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/${created.id}",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(created.id) },
                { assertThat(response.body?.data?.name).isEqualTo("5000원 할인 쿠폰") },
            )
        }

        @DisplayName("존재하지 않는 쿠폰을 조회하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenCouponNotExists() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/999",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("POST /api-admin/v1/coupons")
    @Nested
    inner class CreateCoupon {

        @DisplayName("정상적인 요청이면, 200 OK와 생성된 쿠폰을 반환한다.")
        @Test
        fun returnsOk_whenCreateSucceeds() {
            // arrange
            val request = CouponAdminV1Dto.CreateRequest(
                name = "신규 가입 쿠폰",
                type = CouponType.FIXED,
                value = BigDecimal("5000"),
                minOrderAmount = BigDecimal("10000"),
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/coupons",
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("신규 가입 쿠폰") },
                { assertThat(response.body?.data?.type).isEqualTo(CouponType.FIXED) },
                { assertThat(response.body?.data?.id).isNotNull() },
                { assertThat(response.body?.data?.createdAt).isNotNull() },
            )
        }
    }

    @DisplayName("PUT /api-admin/v1/coupons/{couponId}")
    @Nested
    inner class UpdateCoupon {

        @DisplayName("정상적인 요청이면, 200 OK와 수정된 쿠폰을 반환한다.")
        @Test
        fun returnsOk_whenUpdateSucceeds() {
            // arrange
            val created = createTestCoupon()!!
            val request = CouponAdminV1Dto.UpdateRequest(
                name = "수정된 쿠폰",
                value = BigDecimal("3000"),
                minOrderAmount = BigDecimal("5000"),
                expiredAt = ZonedDateTime.now().plusDays(60),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/${created.id}",
                HttpMethod.PUT,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("수정된 쿠폰") },
                { assertThat(response.body?.data?.value).isEqualByComparingTo(BigDecimal("3000")) },
            )
        }

        @DisplayName("존재하지 않는 쿠폰을 수정하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenCouponNotExists() {
            // arrange
            val request = CouponAdminV1Dto.UpdateRequest(
                name = "수정된 쿠폰",
                value = BigDecimal("3000"),
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(60),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/999",
                HttpMethod.PUT,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("DELETE /api-admin/v1/coupons/{couponId}")
    @Nested
    inner class DeleteCoupon {

        @DisplayName("존재하는 쿠폰을 삭제하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenDeleteSucceeds() {
            // arrange
            val created = createTestCoupon()!!

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/${created.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @DisplayName("삭제된 쿠폰을 조회하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenQueryDeletedCoupon() {
            // arrange
            val created = createTestCoupon()!!
            val deleteType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            testRestTemplate.exchange(
                "/api-admin/v1/coupons/${created.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                deleteType,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/${created.id}",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("존재하지 않는 쿠폰을 삭제하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenCouponNotExists() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/999",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api-admin/v1/coupons/{couponId}/issues")
    @Nested
    inner class GetIssuedCoupons {

        @DisplayName("발급 내역이 존재하면, 200 OK와 목록을 반환한다.")
        @Test
        fun returnsOk_whenIssuedCouponsExist() {
            // arrange
            createTestUser()
            val coupon = createTestCoupon()!!
            val issueType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            testRestTemplate.exchange(
                "/api/v1/coupons/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                issueType,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/coupons/${coupon.id}/issues?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull() },
            )
        }
    }

    @DisplayName("어드민 API 인증")
    @Nested
    inner class AdminAuth {

        @DisplayName("X-Loopers-Ldap 헤더 없이 요청하면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderMissing() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/coupons?page=0&size=20",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("잘못된 LDAP 값으로 요청하면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenLdapInvalid() {
            // arrange
            val headers = HttpHeaders().apply {
                set("X-Loopers-Ldap", "invalid.ldap")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/coupons?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
