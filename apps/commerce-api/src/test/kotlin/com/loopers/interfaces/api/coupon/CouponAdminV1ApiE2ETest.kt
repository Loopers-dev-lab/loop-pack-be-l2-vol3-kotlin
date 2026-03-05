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
class CouponAdminV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        private const val ENDPOINT_COUPONS = "/api-admin/v1/coupons"
    }

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

    private fun createCouponRequest(
        name: String = "테스트 쿠폰",
        type: String = "FIXED",
        value: Long = 5000,
        totalQuantity: Int? = 100,
        expiredAt: String = ZonedDateTime.now().plusDays(30).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
    ): CouponAdminV1Dto.CreateCouponRequest {
        return CouponAdminV1Dto.CreateCouponRequest(
            name = name,
            type = type,
            value = value,
            totalQuantity = totalQuantity,
            expiredAt = expiredAt,
        )
    }

    private fun createCoupon(
        name: String = "테스트 쿠폰",
        type: String = "FIXED",
        value: Long = 5000,
        totalQuantity: Int? = 100,
    ): Long {
        val request = createCouponRequest(name = name, type = type, value = value, totalQuantity = totalQuantity)
        val responseType = object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
        val response = testRestTemplate.exchange(
            ENDPOINT_COUPONS,
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        val body = requireNotNull(response.body) { "쿠폰 생성 응답 body가 null입니다" }
        val data = requireNotNull(body.data) { "쿠폰 생성 응답 data가 null입니다" }
        return data.id
    }

    @Nested
    @DisplayName("POST /api-admin/v1/coupons")
    inner class CreateCoupon {

        @Test
        @DisplayName("쿠폰 템플릿 생성이 성공하면 200 OK와 생성된 데이터를 반환한다")
        fun createCoupon_success() {
            // arrange
            val request = createCouponRequest(name = "5000원 할인 쿠폰", type = "FIXED", value = 5000)

            // act
            val responseType =
                object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT_COUPONS,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("5000원 할인 쿠폰") },
                { assertThat(response.body?.data?.type).isEqualTo("FIXED") },
                { assertThat(response.body?.data?.value).isEqualTo(5000L) },
                { assertThat(response.body?.data?.issuedCount).isEqualTo(0) },
            )
        }
    }

    @Nested
    @DisplayName("PUT /api-admin/v1/coupons/{couponId}")
    inner class UpdateCoupon {

        @Test
        @DisplayName("쿠폰 템플릿 수정이 성공하면 200 OK와 수정된 데이터를 반환한다")
        fun updateCoupon_success() {
            // arrange
            val couponId = createCoupon(name = "원래 쿠폰", type = "FIXED", value = 3000)
            val updateRequest = CouponAdminV1Dto.UpdateCouponRequest(
                name = "수정된 쿠폰",
                value = 7000,
            )

            // act
            val responseType =
                object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT_COUPONS/$couponId",
                HttpMethod.PUT,
                HttpEntity(updateRequest, adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("수정된 쿠폰") },
                { assertThat(response.body?.data?.value).isEqualTo(7000L) },
            )
        }
    }

    @Nested
    @DisplayName("DELETE /api-admin/v1/coupons/{couponId}")
    inner class DeleteCoupon {

        @Test
        @DisplayName("쿠폰 템플릿 삭제가 성공하면 200 OK를 반환한다")
        fun deleteCoupon_success() {
            // arrange
            val couponId = createCoupon()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT_COUPONS/$couponId",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)

            // 삭제된 쿠폰은 발급 불가 확인
            signUp("deletetest")
            val issueResponse = testRestTemplate.exchange(
                "/api/v1/coupons/$couponId/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders("deletetest")),
                responseType,
            )
            assertThat(issueResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("GET /api-admin/v1/coupons")
    inner class GetCoupons {

        @Test
        @DisplayName("쿠폰 목록 조회 시 페이징이 정상 동작한다")
        fun getCoupons_paging_success() {
            // arrange
            createCoupon(name = "쿠폰1")
            createCoupon(name = "쿠폰2")
            createCoupon(name = "쿠폰3")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT_COUPONS?page=0&size=2",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            @Suppress("UNCHECKED_CAST")
            val content = response.body?.data?.get("content") as? List<Map<String, Any>>
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(content).hasSize(2) },
                { assertThat(response.body?.data?.get("totalElements")).isEqualTo(3) },
            )
        }
    }

    @Nested
    @DisplayName("GET /api-admin/v1/coupons/{couponId}")
    inner class GetCoupon {

        @Test
        @DisplayName("쿠폰 상세 조회가 성공하면 정상 데이터를 반환한다")
        fun getCoupon_success() {
            // arrange
            val couponId = createCoupon(name = "상세 조회 쿠폰", type = "FIXED", value = 10000)

            // act
            val responseType =
                object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT_COUPONS/$couponId",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(couponId) },
                { assertThat(response.body?.data?.name).isEqualTo("상세 조회 쿠폰") },
                { assertThat(response.body?.data?.type).isEqualTo("FIXED") },
                { assertThat(response.body?.data?.value).isEqualTo(10000L) },
            )
        }
    }

    @Nested
    @DisplayName("GET /api-admin/v1/coupons/{couponId}/issues")
    inner class GetCouponIssues {

        @Test
        @DisplayName("쿠폰 발급 후 발급 내역 조회가 정상 동작한다")
        fun getCouponIssues_afterIssue_success() {
            // arrange
            val couponId = createCoupon()
            signUp("testuser1")

            // 쿠폰 발급
            testRestTemplate.exchange(
                "/api/v1/coupons/$couponId/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders("testuser1")),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT_COUPONS/$couponId/issues?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            @Suppress("UNCHECKED_CAST")
            val content = response.body?.data?.get("content") as? List<Map<String, Any>>
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(content).hasSize(1) },
                { assertThat(content?.first()?.get("status")).isEqualTo("AVAILABLE") },
            )
        }
    }
}
