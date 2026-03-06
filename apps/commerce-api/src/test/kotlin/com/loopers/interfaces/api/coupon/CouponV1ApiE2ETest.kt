package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponFacade
import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Email
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.Name
import com.loopers.domain.user.PasswordEncryptor
import com.loopers.domain.user.UserModel
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.ApiResponse
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
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val couponFacade: CouponFacade,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val COUPONS = "/api/v1/coupons"
        private const val ADMIN_COUPONS = "/api-admin/v1/coupons"
        private const val LDAP_HEADER = "X-Loopers-Ldap"
        private const val LDAP_VALUE = "loopers.admin"
        private const val TEST_PASSWORD = "Password123!"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createUser(loginId: String = "testuser") = userJpaRepository.save(
        UserModel(
            loginId = LoginId(loginId),
            encryptedPassword = passwordEncryptor.encrypt(TEST_PASSWORD),
            name = Name("홍길동"),
            birthDate = BirthDate("1990-01-01"),
            email = Email("$loginId@example.com"),
        )
    )

    private fun createTemplate(name: String = "테스트쿠폰") = couponFacade.createTemplate(
        name = name,
        type = "FIXED",
        value = 1000L,
        minOrderAmount = null,
        expiredAt = ZonedDateTime.now().plusDays(30),
    )

    private fun authHeaders(loginId: String = "testuser") = HttpHeaders().apply {
        set("X-Loopers-LoginId", loginId)
        set("X-Loopers-LoginPw", TEST_PASSWORD)
    }

    private fun adminHeaders() = HttpHeaders().apply {
        set(LDAP_HEADER, LDAP_VALUE)
    }

    @DisplayName("POST /api/v1/coupons/{couponId}/issue")
    @Nested
    inner class IssueCoupon {

        @Test
        @DisplayName("인증된 사용자가 쿠폰을 발급하면 200과 UserCouponResponse를 반환한다")
        fun issueCoupon_whenValidAuth_thenReturnsUserCouponResponse() {
            createUser()
            val template = createTemplate()

            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.UserCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "$COUPONS/${template.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.couponTemplateId).isEqualTo(template.id) },
                { assertThat(response.body?.data?.status).isEqualTo("AVAILABLE") },
                { assertThat(response.body?.data?.id).isNotNull() },
            )
        }

        @Test
        @DisplayName("이미 발급된 쿠폰을 재발급하면 409 CONFLICT를 반환한다")
        fun issueCoupon_whenAlreadyIssued_thenReturnsConflict() {
            createUser()
            val template = createTemplate()

            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.UserCouponResponse>>() {}

            testRestTemplate.exchange(
                "$COUPONS/${template.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            val response = testRestTemplate.exchange(
                "$COUPONS/${template.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @Test
        @DisplayName("인증 헤더 없이 요청하면 401을 반환한다")
        fun issueCoupon_whenNoAuth_thenReturns401() {
            val template = createTemplate()

            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.UserCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "$COUPONS/${template.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(Unit),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("POST /api-admin/v1/coupons")
    @Nested
    inner class CreateTemplate {

        @Test
        @DisplayName("LDAP 헤더로 요청하면 200과 생성된 쿠폰 템플릿을 반환한다")
        fun createTemplate_whenLdapHeader_thenReturnsCouponTemplate() {
            val request = CouponAdminV1Dto.CreateCouponTemplateRequest(
                name = "신규쿠폰",
                type = "FIXED",
                value = 5000L,
                minOrderAmount = 30000L,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponTemplateResponse>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_COUPONS,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isNotNull() },
                { assertThat(response.body?.data?.name).isEqualTo("신규쿠폰") },
                { assertThat(response.body?.data?.type).isEqualTo("FIXED") },
                { assertThat(response.body?.data?.value).isEqualTo(5000L) },
                { assertThat(response.body?.data?.minOrderAmount).isEqualTo(30000L) },
            )
        }

        @Test
        @DisplayName("LDAP 헤더 없이 요청하면 401을 반환한다")
        fun createTemplate_whenNoLdapHeader_thenReturns401() {
            val request = CouponAdminV1Dto.CreateCouponTemplateRequest(
                name = "신규쿠폰",
                type = "FIXED",
                value = 5000L,
                minOrderAmount = null,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponTemplateResponse>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_COUPONS,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("GET /api-admin/v1/coupons/{couponId}/issues")
    @Nested
    inner class GetIssues {

        @Test
        @DisplayName("LDAP 헤더로 요청하면 200과 발급 내역 페이지를 반환한다")
        fun getIssues_whenLdapHeader_thenReturnsIssuePage() {
            createUser()
            val template = createTemplate()
            couponFacade.issueCoupon("testuser", template.id)

            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_COUPONS/${template.id}/issues",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull },
            )
        }

        @Test
        @DisplayName("LDAP 헤더 없이 요청하면 401을 반환한다")
        fun getIssues_whenNoLdapHeader_thenReturns401() {
            val template = createTemplate()

            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_COUPONS/${template.id}/issues",
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
