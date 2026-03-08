package com.loopers.interfaces.api

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponQuantity
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.Discount
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.interfaces.api.user.UserDto
import com.loopers.interfaces.common.ApiResponse
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
import org.springframework.http.MediaType
import java.time.LocalDate
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserCouponApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val MY_COUPONS_ENDPOINT = "/api/v1/users/me/coupons"
        private const val ISSUE_ENDPOINT = "/api/v1/coupons/{couponId}/issue"
        private const val SIGNUP_ENDPOINT = "/api/v1/users/signup"
        private const val LOGIN_ID_HEADER = "X-Loopers-LoginId"
        private const val LOGIN_PW_HEADER = "X-Loopers-LoginPw"
        private val MY_COUPONS_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<List<Map<String, Any>>>>() {}
        private val ISSUE_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<Any>>() {}
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun signUp(
        loginId: String = "testuser123",
        password: String = "Test1234!@",
        name: String = "홍길동",
        email: String = "test@example.com",
        birthday: LocalDate = LocalDate.of(1990, 1, 15),
    ) {
        val request = UserDto.SignUpRequest(
            loginId = loginId,
            password = password,
            name = name,
            email = email,
            birthday = birthday,
        )
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        testRestTemplate.exchange(
            SIGNUP_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(request, headers),
            object : ParameterizedTypeReference<ApiResponse<UserDto.SignUpResponse>>() {},
        )
    }

    private fun authHeaders(
        loginId: String = "testuser123",
        password: String = "Test1234!@",
    ): HttpHeaders {
        return HttpHeaders().apply {
            set(LOGIN_ID_HEADER, loginId)
            set(LOGIN_PW_HEADER, password)
        }
    }

    private fun issueCoupon(couponId: Long) {
        testRestTemplate.exchange(
            ISSUE_ENDPOINT,
            HttpMethod.POST,
            HttpEntity<Void>(authHeaders()),
            ISSUE_RESPONSE_TYPE,
            couponId,
        )
    }

    private fun createCoupon(
        name: String = "신규가입 할인",
        discount: Discount = Discount(DiscountType.FIXED_AMOUNT, 5000L),
        totalQuantity: Int = 100,
        expiresAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Coupon {
        return couponRepository.save(
            Coupon(
                name = name,
                discount = discount,
                quantity = CouponQuantity(totalQuantity, 0),
                expiresAt = expiresAt,
            ),
        )
    }

    @DisplayName("GET /api/v1/users/me/coupons")
    @Nested
    inner class GetMyCoupons {

        @DisplayName("발급받은 쿠폰이 있으면, 200 OK와 함께 쿠폰 목록이 반환된다.")
        @Test
        fun returnsOk_whenUserHasCoupons() {
            // arrange
            signUp()
            val coupon = createCoupon()
            issueCoupon(coupon.id)

            // act
            val response = testRestTemplate.exchange(
                MY_COUPONS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                MY_COUPONS_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).hasSize(1) },
            )
        }

        @DisplayName("발급받은 쿠폰이 없으면, 200 OK와 빈 목록이 반환된다.")
        @Test
        fun returnsOk_withEmptyList_whenNoCoupons() {
            // arrange
            signUp()

            // act
            val response = testRestTemplate.exchange(
                MY_COUPONS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                MY_COUPONS_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isEmpty() },
            )
        }

        @DisplayName("각 쿠폰에 상태 정보(AVAILABLE)가 포함되어 있다.")
        @Test
        fun includesStatusInResponse() {
            // arrange
            signUp()
            val coupon = createCoupon()
            issueCoupon(coupon.id)

            // act
            val response = testRestTemplate.exchange(
                MY_COUPONS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                MY_COUPONS_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.get(0)?.get("status")).isEqualTo("AVAILABLE") },
            )
        }

        @DisplayName("만료된 쿠폰은 EXPIRED 상태로 표시된다.")
        @Test
        fun returnsExpiredStatus_whenCouponIsExpired() {
            // arrange
            signUp()
            val coupon = createCoupon(expiresAt = ZonedDateTime.now().minusDays(1))
            issuedCouponRepository.save(IssuedCoupon(couponId = coupon.id, userId = 1L))

            // act
            val response = testRestTemplate.exchange(
                MY_COUPONS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                MY_COUPONS_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.get(0)?.get("status")).isEqualTo("EXPIRED") },
            )
        }

        @DisplayName("응답에 쿠폰의 상세 정보가 포함된다.")
        @Test
        fun returnsDetailedCouponInfo() {
            // arrange
            signUp()
            val coupon = createCoupon(
                name = "여름 세일 할인",
                discount = Discount(DiscountType.FIXED_AMOUNT, 3000L),
            )
            issueCoupon(coupon.id)

            // act
            val response = testRestTemplate.exchange(
                MY_COUPONS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                MY_COUPONS_RESPONSE_TYPE,
            )

            // assert
            val couponData = response.body?.data?.get(0)!!
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(couponData["couponId"]).isEqualTo(coupon.id.toInt()) },
                { assertThat(couponData["name"]).isEqualTo("여름 세일 할인") },
                { assertThat(couponData["discountType"]).isEqualTo("FIXED_AMOUNT") },
                { assertThat(couponData["discountValue"]).isEqualTo(3000) },
                { assertThat(couponData["status"]).isEqualTo("AVAILABLE") },
                { assertThat(couponData["expiresAt"]).isNotNull() },
                { assertThat(couponData["issuedAt"]).isNotNull() },
            )
        }

        @DisplayName("여러 쿠폰을 발급받았으면, 모두 반환된다.")
        @Test
        fun returnsAllCoupons_whenUserHasMultipleCoupons() {
            // arrange
            signUp()
            val coupon1 = createCoupon(name = "신규가입 할인")
            val coupon2 = createCoupon(name = "여름 세일 할인")
            issueCoupon(coupon1.id)
            issueCoupon(coupon2.id)

            // act
            val response = testRestTemplate.exchange(
                MY_COUPONS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                MY_COUPONS_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).hasSize(2) },
            )
        }

        @DisplayName("사용된 쿠폰은 USED 상태로 표시된다.")
        @Test
        fun returnsUsedStatus_whenCouponIsUsed() {
            // arrange
            signUp()
            val coupon = createCoupon()
            val issuedCoupon = issuedCouponRepository.save(
                IssuedCoupon(couponId = coupon.id, userId = 1L),
            )
            issuedCoupon.use()
            issuedCouponRepository.save(issuedCoupon)

            // act
            val response = testRestTemplate.exchange(
                MY_COUPONS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(authHeaders()),
                MY_COUPONS_RESPONSE_TYPE,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.get(0)?.get("status")).isEqualTo("USED") },
            )
        }

        @DisplayName("인증 헤더가 없으면, 401 Unauthorized를 반환한다.")
        @Test
        fun returnsUnauthorized_whenNotAuthenticated() {
            // act
            val response = testRestTemplate.exchange(
                MY_COUPONS_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(HttpHeaders()),
                MY_COUPONS_RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode.value()).isEqualTo(401)
        }
    }
}
