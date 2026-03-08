package com.loopers.interfaces.api.coupon

import com.loopers.domain.coupon.CouponType
import com.loopers.domain.coupon.dto.CouponInfo
import com.loopers.domain.user.User
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import com.loopers.infrastructure.coupon.CouponTemplateJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
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
import java.math.BigDecimal
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Coupon V1 API E2E Test")
class CouponV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val couponTemplateJpaRepository: CouponTemplateJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val passwordEncoder: PasswordEncoder,
) {

    companion object {
        private const val COUPONS_ENDPOINT = "/api/v1/coupons"
        private const val PLAIN_PASSWORD = "password123"

        private fun createAuthHeaders(loginId: String): HttpHeaders {
            val headers = HttpHeaders()
            headers["X-Loopers-LoginId"] = loginId
            headers["X-Loopers-LoginPw"] = PLAIN_PASSWORD
            return headers
        }
    }

    @AfterEach
    fun cleanup() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("쿠폰 발급")
    inner class IssueCouponTest {

        @Test
        @DisplayName("유효한 쿠폰 템플릿으로 쿠폰을 발급받을 수 있다")
        fun issueCoupon_success() {
            // Arrange
            val expectedValue = BigDecimal("5000")
            val template = couponTemplateJpaRepository.save(
                com.loopers.domain.coupon.CouponTemplate.create(
                    name = "신규 가입 쿠폰",
                    type = CouponType.FIXED,
                    value = expectedValue,
                    minOrderAmount = BigDecimal("10000"),
                    expiredAt = ZonedDateTime.now().plusDays(30),
                ),
            )

            // Verify template was saved correctly
            val savedTemplateOpt = couponTemplateJpaRepository.findById(template.id)
            assertThat(savedTemplateOpt.isPresent).`as`("Template should exist in DB with id: ${template.id}").isTrue()
            val savedTemplate = savedTemplateOpt.get()
            assertThat(savedTemplate).`as`("savedTemplate should not be null").isNotNull()
            assertThat(savedTemplate.value.compareTo(expectedValue))
                .`as`("Value mismatch. Expected: $expectedValue, Actual: ${savedTemplate.value}")
                .isEqualTo(0)

            val user = userJpaRepository.save(
                User.create(
                    loginId = LoginId.of("testuser"),
                    password = Password.ofEncrypted(passwordEncoder.encode(PLAIN_PASSWORD)),
                    name = Name.of("Test User"),
                    birthDate = BirthDate.of("20000101"),
                    email = Email.of("test@example.com"),
                ),
            )

            // Act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponInfo>>() {}
            val response = testRestTemplate.exchange(
                "$COUPONS_ENDPOINT/${template.id}/issue",
                HttpMethod.POST,
                HttpEntity(null, createAuthHeaders("testuser")),
                responseType,
            )

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS)

            val couponInfo = response.body?.data
            assertThat(couponInfo).isNotNull()
            assertThat(couponInfo?.id).isNotNull()
            assertThat(couponInfo?.templateName).isEqualTo(template.name)
            assertThat(couponInfo?.type).isEqualTo(CouponType.FIXED)
            assertThat(couponInfo?.value?.compareTo(expectedValue)).isEqualTo(0)
            assertThat(couponInfo?.minOrderAmount?.compareTo(BigDecimal("10000"))).isEqualTo(0)
        }

        @Test
        @DisplayName("존재하지 않는 템플릿으로 발급을 시도하면 404 NOT_FOUND 응답을 받는다")
        fun issueCoupon_throwsNotFound_whenTemplateDoesNotExist() {
            // Arrange
            userJpaRepository.save(
                User.create(
                    loginId = LoginId.of("testuser"),
                    password = Password.ofEncrypted(passwordEncoder.encode(PLAIN_PASSWORD)),
                    name = Name.of("Test User"),
                    birthDate = BirthDate.of("20000101"),
                    email = Email.of("test@example.com"),
                ),
            )
            val invalidTemplateId = 999999L

            // Act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponInfo>>() {}
            val response = testRestTemplate.exchange(
                "$COUPONS_ENDPOINT/$invalidTemplateId/issue",
                HttpMethod.POST,
                HttpEntity(null, createAuthHeaders("testuser")),
                responseType,
            )

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        @DisplayName("이미 발급받은 쿠폰을 다시 발급받을 수 없다")
        fun issueCoupon_throwsBadRequest_whenAlreadyIssued() {
            // Arrange
            val template = couponTemplateJpaRepository.save(
                com.loopers.domain.coupon.CouponTemplate.create(
                    name = "신규 가입 쿠폰",
                    type = CouponType.FIXED,
                    value = BigDecimal("5000"),
                    minOrderAmount = BigDecimal("10000"),
                    expiredAt = ZonedDateTime.now().plusDays(30),
                ),
            )
            val user = userJpaRepository.save(
                User.create(
                    loginId = LoginId.of("testuser"),
                    password = Password.ofEncrypted(passwordEncoder.encode(PLAIN_PASSWORD)),
                    name = Name.of("Test User"),
                    birthDate = BirthDate.of("20000101"),
                    email = Email.of("test@example.com"),
                ),
            )

            // 첫 번째 발급
            testRestTemplate.exchange(
                "$COUPONS_ENDPOINT/${template.id}/issue",
                HttpMethod.POST,
                HttpEntity(null, createAuthHeaders("testuser")),
                object : ParameterizedTypeReference<ApiResponse<CouponInfo>>() {},
            )

            // Act - 두 번째 발급 시도
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponInfo>>() {}
            val response = testRestTemplate.exchange(
                "$COUPONS_ENDPOINT/${template.id}/issue",
                HttpMethod.POST,
                HttpEntity(null, createAuthHeaders("testuser")),
                responseType,
            )

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("내 쿠폰 조회")
    inner class GetMyCouponsTest {

        @Test
        @DisplayName("발급받은 쿠폰 목록을 조회할 수 있다")
        fun getMyCoupons_success() {
            // Arrange
            val template = couponTemplateJpaRepository.save(
                com.loopers.domain.coupon.CouponTemplate.create(
                    name = "신규 가입 쿠폰",
                    type = CouponType.FIXED,
                    value = BigDecimal("5000"),
                    minOrderAmount = BigDecimal("10000"),
                    expiredAt = ZonedDateTime.now().plusDays(30),
                ),
            )
            val user = userJpaRepository.save(
                User.create(
                    loginId = LoginId.of("testuser"),
                    password = Password.ofEncrypted(passwordEncoder.encode(PLAIN_PASSWORD)),
                    name = Name.of("Test User"),
                    birthDate = BirthDate.of("20000101"),
                    email = Email.of("test@example.com"),
                ),
            )

            // 쿠폰 발급
            testRestTemplate.exchange(
                "$COUPONS_ENDPOINT/${template.id}/issue",
                HttpMethod.POST,
                HttpEntity(null, createAuthHeaders("testuser")),
                object : ParameterizedTypeReference<ApiResponse<CouponInfo>>() {},
            )

            // Act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<CouponInfo>>>() {}
            val response = testRestTemplate.exchange(
                COUPONS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity(null, createAuthHeaders("testuser")),
                responseType,
            )

            // Assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(1) },
                { assertThat(response.body?.data?.content?.get(0)?.templateName).isEqualTo(template.name) },
            )
        }

        @Test
        @DisplayName("쿠폰을 발급받지 않은 사용자는 빈 목록을 조회한다")
        fun getMyCoupons_returnsEmpty_whenNoCoupons() {
            // Arrange
            userJpaRepository.save(
                User.create(
                    loginId = LoginId.of("testuser"),
                    password = Password.ofEncrypted(passwordEncoder.encode(PLAIN_PASSWORD)),
                    name = Name.of("Test User"),
                    birthDate = BirthDate.of("20000101"),
                    email = Email.of("test@example.com"),
                ),
            )

            // Act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<CouponInfo>>>() {}
            val response = testRestTemplate.exchange(
                COUPONS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity(null, createAuthHeaders("testuser")),
                responseType,
            )

            // Assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).isEmpty() },
            )
        }

        @Test
        @DisplayName("페이징으로 쿠폰 목록을 조회할 수 있다")
        fun getMyCoupons_withPaging() {
            // Arrange - 여러 개의 쿠폰 생성
            val templates = (1..25).map {
                couponTemplateJpaRepository.save(
                    com.loopers.domain.coupon.CouponTemplate.create(
                        name = "쿠폰 $it",
                        type = CouponType.FIXED,
                        value = BigDecimal("1000"),
                        minOrderAmount = BigDecimal("5000"),
                        expiredAt = ZonedDateTime.now().plusDays(30),
                    ),
                )
            }
            userJpaRepository.save(
                User.create(
                    loginId = LoginId.of("testuser"),
                    password = Password.ofEncrypted(passwordEncoder.encode(PLAIN_PASSWORD)),
                    name = Name.of("Test User"),
                    birthDate = BirthDate.of("20000101"),
                    email = Email.of("test@example.com"),
                ),
            )

            // 각 템플릿마다 쿠폰 발급 (중복 발급 방지를 위해 다른 사용자들이 필요)
            // 여기서는 단순하게 처음 2개만 발급
            templates.take(2).forEach { template ->
                testRestTemplate.exchange(
                    "$COUPONS_ENDPOINT/${template.id}/issue",
                    HttpMethod.POST,
                    HttpEntity(null, createAuthHeaders("testuser")),
                    object : ParameterizedTypeReference<ApiResponse<CouponInfo>>() {},
                )
            }

            // Act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<CouponInfo>>>() {}
            val response = testRestTemplate.exchange(
                "$COUPONS_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                HttpEntity(null, createAuthHeaders("testuser")),
                responseType,
            )

            // Assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.size).isEqualTo(20) },
                { assertThat(response.body?.data?.totalElements).isEqualTo(2) },
            )
        }
    }
}
