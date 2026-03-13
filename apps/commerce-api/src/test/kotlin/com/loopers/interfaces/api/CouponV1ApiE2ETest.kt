package com.loopers.interfaces.api

import com.loopers.domain.coupon.CouponTemplate
import com.loopers.domain.coupon.CouponType
import com.loopers.domain.user.User
import com.loopers.infrastructure.coupon.CouponTemplateJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.coupon.CouponV1Dto
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
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val couponTemplateJpaRepository: CouponTemplateJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private lateinit var userHeaders: HttpHeaders
    private lateinit var user: User
    private lateinit var template: CouponTemplate

    companion object {
        private const val PASSWORD = "abcd1234"
    }

    @BeforeEach
    fun setUp() {
        user = userJpaRepository.save(User(loginId = "testuser1", password = PASSWORD, name = "테스트유저", birth = "2000-01-01", email = "test@test.com"))
        template = couponTemplateJpaRepository.save(
            CouponTemplate(name = "1000원 할인", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().plusDays(30)),
        )
        userHeaders = HttpHeaders()
        userHeaders.set("X-Loopers-LoginId", "testuser1")
        userHeaders.set("X-Loopers-LoginPw", PASSWORD)
    }

    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /api/v1/coupons/{couponId}/issue")
    @Nested
    inner class IssueCoupon {
        @DisplayName("유효한 쿠폰 템플릿 ID로 발급하면, 201 응답을 반환한다.")
        @Test
        fun issuesCoupon_whenValidTemplateId() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssuedCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/${template.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(null, userHeaders),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.couponName).isEqualTo("1000원 할인") },
                { assertThat(response.body?.data?.couponType).isEqualTo(CouponType.FIXED) },
                { assertThat(response.body?.data?.couponValue).isEqualTo(1000) },
            )
        }

        @DisplayName("만료된 쿠폰 템플릿이면, 400 응답을 반환한다.")
        @Test
        fun returnsBadRequest_whenTemplateExpired() {
            // arrange
            val expiredTemplate = couponTemplateJpaRepository.save(
                CouponTemplate(name = "만료 쿠폰", type = CouponType.FIXED, value = 1000, expiredAt = ZonedDateTime.now().minusDays(1)),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssuedCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/${expiredTemplate.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(null, userHeaders),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("인증되지 않은 요청은 404 응답을 받는다.")
        @Test
        fun returnsNotFound_whenNotAuthenticated() {
            // arrange
            val headers = HttpHeaders()
            headers.set("X-Loopers-LoginId", "wronguser")
            headers.set("X-Loopers-LoginPw", "wrongpass1")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssuedCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/${template.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(null, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api/v1/coupons/me")
    @Nested
    inner class GetMyCoupons {
        @DisplayName("내 쿠폰 목록을 조회한다.")
        @Test
        fun returnsMyCoupons() {
            // arrange - 쿠폰 발급
            val issueResponseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.IssuedCouponResponse>>() {}
            testRestTemplate.exchange(
                "/api/v1/coupons/${template.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(null, userHeaders),
                issueResponseType,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<List<CouponV1Dto.IssuedCouponResponse>>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/me",
                HttpMethod.GET,
                HttpEntity<Any>(null, userHeaders),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data).hasSize(1) },
                { assertThat(response.body?.data?.first()?.couponName).isEqualTo("1000원 할인") },
            )
        }
    }
}
