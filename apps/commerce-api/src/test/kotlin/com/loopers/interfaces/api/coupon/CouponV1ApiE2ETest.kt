package com.loopers.interfaces.api.coupon

import com.loopers.domain.coupon.CouponType
import com.loopers.infrastructure.coupon.CouponTemplateEntity
import com.loopers.infrastructure.coupon.CouponTemplateJpaRepository
import com.loopers.infrastructure.coupon.UserCouponEntity
import com.loopers.infrastructure.coupon.UserCouponJpaRepository
import com.loopers.infrastructure.user.UserEntity
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
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val couponTemplateJpaRepository: CouponTemplateJpaRepository,
    private val userCouponJpaRepository: UserCouponJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val LOGIN_ID_HEADER = "X-Loopers-LoginId"
        private const val LOGIN_PW_HEADER = "X-Loopers-LoginPw"
        private const val DEFAULT_LOGIN_ID = "testUser"
        private const val DEFAULT_PASSWORD = "testPassword"
    }

    @AfterEach
    fun tearDown() = databaseCleanUp.truncateAllTables()

    private fun authHeaders(
        loginId: String = DEFAULT_LOGIN_ID,
        password: String = DEFAULT_PASSWORD,
    ): HttpHeaders = HttpHeaders().apply {
        this[LOGIN_ID_HEADER] = loginId
        this[LOGIN_PW_HEADER] = password
    }

    private fun setupUser(
        userId: String = DEFAULT_LOGIN_ID,
        password: String = DEFAULT_PASSWORD,
    ): UserEntity = userJpaRepository.save(
        UserEntity(
            userId = userId,
            encryptedPassword = passwordEncoder.encode(password),
            name = "테스트유저",
            birthDate = LocalDate.of(1990, 1, 1),
            email = "test@example.com",
        )
    )

    private fun setupTemplate(
        name: String = "10% 할인 쿠폰",
        type: CouponType = CouponType.RATE,
        discountValue: Int = 10,
        maxIssuance: Int? = null,
    ): CouponTemplateEntity = couponTemplateJpaRepository.save(
        CouponTemplateEntity(
            name = name,
            type = type,
            discountValue = discountValue,
            minOrderAmount = 0,
            maxIssuance = maxIssuance,
            expiresAt = LocalDate.now().plusDays(30),
        )
    )

    // ─── POST /api/v1/coupons/issue ───

    @DisplayName("POST /api/v1/coupons/issue")
    @Nested
    inner class IssueCoupon {

        @DisplayName("인증된 사용자가 쿠폰을 발급받으면, 200 응답을 반환한다.")
        @Test
        fun returnsSuccess() {
            val user = setupUser()
            val template = setupTemplate()

            val request = mapOf("couponTemplateId" to template.id)
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any?>>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/issue",
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.get("status")).isEqualTo("AVAILABLE") },
            )
        }

        @DisplayName("이미 발급받은 쿠폰을 다시 발급받으면, 409 CONFLICT를 반환한다.")
        @Test
        fun throwsConflict_whenAlreadyIssued() {
            val user = setupUser()
            val template = setupTemplate()
            userCouponJpaRepository.save(UserCouponEntity(userId = user.id, couponTemplateId = template.id))

            val request = mapOf("couponTemplateId" to template.id)
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any?>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/issue",
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @DisplayName("발급 수량이 초과되면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun throwsBadRequest_whenMaxIssuanceReached() {
            setupUser()
            val template = setupTemplate(maxIssuance = 0)

            val request = mapOf("couponTemplateId" to template.id)
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any?>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/issue",
                HttpMethod.POST,
                HttpEntity(request, authHeaders()),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("인증에 실패하면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun throwsUnauthorized_whenAuthFails() {
            setupUser()
            val template = setupTemplate()

            val request = mapOf("couponTemplateId" to template.id)
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any?>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/issue",
                HttpMethod.POST,
                HttpEntity(request, authHeaders(password = "wrongPassword")),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    // ─── GET /api/v1/coupons/me ───

    @DisplayName("GET /api/v1/coupons/me")
    @Nested
    inner class GetUserCoupons {

        @DisplayName("보유 쿠폰이 있으면, 200 과 쿠폰 목록을 반환한다.")
        @Test
        fun returnsUserCoupons() {
            val user = setupUser()
            val template = setupTemplate()
            userCouponJpaRepository.save(UserCouponEntity(userId = user.id, couponTemplateId = template.id))

            val responseType = object : ParameterizedTypeReference<ApiResponse<List<Map<String, Any?>>>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/me",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).hasSize(1) },
                { assertThat(response.body?.data?.first()?.get("status")).isEqualTo("AVAILABLE") },
            )
        }

        @DisplayName("보유 쿠폰이 없으면, 200 과 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyList() {
            setupUser()

            val responseType = object : ParameterizedTypeReference<ApiResponse<List<Map<String, Any?>>>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/me",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isEmpty() },
            )
        }
    }
}
