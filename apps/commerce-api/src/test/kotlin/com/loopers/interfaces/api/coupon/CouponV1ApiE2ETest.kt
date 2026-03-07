package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.admin.coupon.AdminCouponV1Dto
import com.loopers.interfaces.api.member.MemberV1Dto
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ISSUE_ENDPOINT = "/api/v1/coupons/templates"
        private const val MY_COUPONS_ENDPOINT = "/api/v1/members/me/coupons"
        private const val ADMIN_COUPON_ENDPOINT = "/api-admin/v1/coupons"
        private const val MEMBER_ENDPOINT = "/api/v1/members"
        private const val HEADER_LDAP = "X-Loopers-Ldap"
        private const val LDAP_VALUE = "loopers.admin"
        private const val HEADER_LOGIN_ID = "X-Loopers-LoginId"
        private const val HEADER_LOGIN_PW = "X-Loopers-LoginPw"
    }

    private var templateId: Long = 0
    private val loginId = "couponuser"
    private val password = "Password1!"

    @BeforeEach
    fun setUp() {
        val memberRequest = MemberV1Dto.RegisterRequest(
            loginId = loginId,
            password = password,
            name = "테스터",
            birthday = LocalDate.of(2000, 1, 1),
            email = "coupon@example.com",
        )
        testRestTemplate.exchange(
            MEMBER_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(memberRequest),
            object : ParameterizedTypeReference<ApiResponse<Void>>() {},
        )

        val templateRequest = AdminCouponV1Dto.CreateRequest(
            name = "1000원 할인 쿠폰",
            type = "FIXED",
            value = 1000L,
            minOrderAmount = 5000L,
            maxDiscountAmount = null,
            expirationPolicy = "FIXED_DATE",
            expiredAt = ZonedDateTime.now().plusDays(30),
            validDays = null,
        )
        val templateResponse = testRestTemplate.exchange(
            ADMIN_COUPON_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(templateRequest, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Dto.TemplateResponse>>() {},
        )
        templateId = templateResponse.body!!.data!!.id
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(HEADER_LDAP, LDAP_VALUE)
        }
    }

    private fun memberHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(HEADER_LOGIN_ID, loginId)
            set(HEADER_LOGIN_PW, password)
        }
    }

    private fun issueCouponViaApi(): CouponV1Dto.IssuedCouponResponse {
        val response = testRestTemplate.exchange(
            "$ISSUE_ENDPOINT/$templateId/issue",
            HttpMethod.POST,
            HttpEntity<Any>(memberHeaders()),
            object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssuedCouponResponse>>() {},
        )
        return response.body!!.data!!
    }

    @DisplayName("POST /api/v1/coupons/templates/{templateId}/issue (쿠폰 발급)")
    @Nested
    inner class IssueCoupon {
        @DisplayName("유효한 쿠폰 템플릿으로 발급하면, 201 CREATED 응답을 받는다.")
        @Test
        fun returns201_whenValidIssue() {
            // act
            val response = testRestTemplate.exchange(
                "$ISSUE_ENDPOINT/$templateId/issue",
                HttpMethod.POST,
                HttpEntity<Any>(memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssuedCouponResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.templateName).isEqualTo("1000원 할인 쿠폰") },
                { assertThat(response.body?.data?.type).isEqualTo("FIXED") },
                { assertThat(response.body?.data?.value).isEqualTo(1000L) },
                { assertThat(response.body?.data?.status).isEqualTo("AVAILABLE") },
            )
        }

        @DisplayName("동일 템플릿으로 중복 발급해도, 201 CREATED 응답을 받는다.")
        @Test
        fun returns201_whenDuplicateIssue() {
            // arrange
            issueCouponViaApi()

            // act
            val response = testRestTemplate.exchange(
                "$ISSUE_ENDPOINT/$templateId/issue",
                HttpMethod.POST,
                HttpEntity<Any>(memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssuedCouponResponse>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
        }

        @DisplayName("존재하지 않는 템플릿 ID로 발급하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returns404_whenTemplateNotFound() {
            // act
            val response = testRestTemplate.exchange(
                "$ISSUE_ENDPOINT/999/issue",
                HttpMethod.POST,
                HttpEntity<Any>(memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("인증 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returns401_whenNotAuthenticated() {
            // act
            val response = testRestTemplate.exchange(
                "$ISSUE_ENDPOINT/$templateId/issue",
                HttpMethod.POST,
                HttpEntity<Any>(null, null),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("삭제된 쿠폰 템플릿으로 발급하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returns400_whenTemplateDeleted() {
            // arrange — delete the template via admin API
            testRestTemplate.exchange(
                "$ADMIN_COUPON_ENDPOINT/$templateId",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // act
            val response = testRestTemplate.exchange(
                "$ISSUE_ENDPOINT/$templateId/issue",
                HttpMethod.POST,
                HttpEntity<Any>(memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("GET /api/v1/members/me/coupons (내 쿠폰 목록 조회)")
    @Nested
    inner class GetMyIssuedCoupons {
        @DisplayName("발급받은 쿠폰이 있으면, 200 OK와 쿠폰 목록을 반환한다.")
        @Test
        fun returns200_withIssuedCoupons() {
            // arrange
            issueCouponViaApi()

            // act
            val response = testRestTemplate.exchange(
                MY_COUPONS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Any>(memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<List<CouponV1Dto.IssuedCouponResponse>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data).hasSize(1) },
                { assertThat(response.body?.data?.get(0)?.templateName).isEqualTo("1000원 할인 쿠폰") },
                { assertThat(response.body?.data?.get(0)?.status).isEqualTo("AVAILABLE") },
            )
        }

        @DisplayName("발급받은 쿠폰이 없으면, 200 OK와 빈 목록을 반환한다.")
        @Test
        fun returns200_withEmptyList() {
            // act
            val response = testRestTemplate.exchange(
                MY_COUPONS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Any>(memberHeaders()),
                object : ParameterizedTypeReference<ApiResponse<List<CouponV1Dto.IssuedCouponResponse>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isEmpty() },
            )
        }

        @DisplayName("인증 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returns401_whenNotAuthenticated() {
            // act
            val response = testRestTemplate.exchange(
                MY_COUPONS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Any>(null, null),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
