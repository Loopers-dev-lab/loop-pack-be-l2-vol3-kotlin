package com.loopers.interfaces.api

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.DiscountType
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ISSUE_ENDPOINT = "/api/v1/coupons/{couponId}/issue"
        private const val SIGNUP_ENDPOINT = "/api/v1/users/signup"
        private const val LOGIN_ID_HEADER = "X-Loopers-LoginId"
        private const val LOGIN_PW_HEADER = "X-Loopers-LoginPw"
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

    private fun issueCoupon(couponId: Long, loginId: String = "testuser123", password: String = "Test1234!@") {
        testRestTemplate.exchange(
            ISSUE_ENDPOINT,
            HttpMethod.POST,
            HttpEntity<Void>(authHeaders(loginId = loginId, password = password)),
            ISSUE_RESPONSE_TYPE,
            couponId,
        )
    }

    private fun createCoupon(
        name: String = "신규가입 할인",
        discountType: DiscountType = DiscountType.FIXED_AMOUNT,
        discountValue: Long = 5000L,
        totalQuantity: Int = 100,
        expiresAt: ZonedDateTime = ZonedDateTime.now().plusDays(30),
    ): Coupon {
        return couponRepository.save(
            Coupon(
                name = name,
                discountType = discountType,
                discountValue = discountValue,
                totalQuantity = totalQuantity,
                expiresAt = expiresAt,
            ),
        )
    }

    @DisplayName("POST /api/v1/coupons/{couponId}/issue")
    @Nested
    inner class IssueCoupon {

        @DisplayName("유효한 쿠폰 발급을 요청하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenValidCouponIssueRequested() {
            // arrange
            signUp()
            val coupon = createCoupon()
            val httpEntity = HttpEntity<Void>(authHeaders())

            // act
            val response = testRestTemplate.exchange(
                ISSUE_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                ISSUE_RESPONSE_TYPE,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }

        @DisplayName("존재하지 않는 쿠폰 ID로 요청하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenCouponNotExists() {
            // arrange
            signUp()
            val httpEntity = HttpEntity<Void>(authHeaders())

            // act
            val response = testRestTemplate.exchange(
                ISSUE_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                ISSUE_RESPONSE_TYPE,
                999999L,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("이미 발급받은 쿠폰으로 요청하면, 409 CONFLICT를 반환한다.")
        @Test
        fun returnsConflict_whenAlreadyIssued() {
            // arrange
            signUp()
            val coupon = createCoupon()
            issueCoupon(coupon.id)

            // act
            val response = testRestTemplate.exchange(
                ISSUE_ENDPOINT,
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                ISSUE_RESPONSE_TYPE,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("만료된 쿠폰으로 요청하면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenCouponIsExpired() {
            // arrange
            signUp()
            val coupon = createCoupon(expiresAt = ZonedDateTime.now().minusDays(1))
            val httpEntity = HttpEntity<Void>(authHeaders())

            // act
            val response = testRestTemplate.exchange(
                ISSUE_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                ISSUE_RESPONSE_TYPE,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("발급 수량이 소진된 쿠폰으로 요청하면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenCouponIsExhausted() {
            // arrange
            signUp()
            signUp(loginId = "otheruser", password = "Test1234!@", name = "김철수", email = "other@example.com")
            val coupon = createCoupon(totalQuantity = 1)
            issueCoupon(coupon.id, loginId = "otheruser", password = "Test1234!@")

            // act
            val response = testRestTemplate.exchange(
                ISSUE_ENDPOINT,
                HttpMethod.POST,
                HttpEntity<Void>(authHeaders()),
                ISSUE_RESPONSE_TYPE,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("인증 헤더가 없으면, 401 Unauthorized를 반환한다.")
        @Test
        fun returnsUnauthorized_whenNotAuthenticated() {
            // arrange
            val coupon = createCoupon()
            val httpEntity = HttpEntity<Void>(HttpHeaders())

            // act
            val response = testRestTemplate.exchange(
                ISSUE_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                ISSUE_RESPONSE_TYPE,
                coupon.id,
            )

            // assert
            assertThat(response.statusCode.value()).isEqualTo(401)
        }

        @DisplayName("쿠폰 발급에 성공하면, 발급된 쿠폰이 저장된다.")
        @Test
        fun savesIssuedCoupon_whenSuccessfullyIssued() {
            // arrange
            signUp()
            val coupon = createCoupon()

            // act
            issueCoupon(coupon.id)

            // assert
            val issuedCoupons = issuedCouponRepository.findByCouponId(coupon.id)
            assertThat(issuedCoupons).hasSize(1)
        }
    }

    @DisplayName("동시에 여러 사용자가 같은 쿠폰 발급을 요청하면,")
    @Nested
    inner class ConcurrentIssueCoupon {

        @DisplayName("정확한 수량만큼만 발급된다.")
        @Test
        fun issuesExactQuantity_whenConcurrentRequests() {
            // arrange
            val threadCount = 20
            val coupon = createCoupon(totalQuantity = 10)
            (1..threadCount).forEach { i ->
                signUp(loginId = "user$i", password = "Test1234!@", name = "사용자$i", email = "user$i@example.com")
            }
            val executorService = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)

            // act
            (1..threadCount).forEach { i ->
                executorService.submit {
                    try {
                        testRestTemplate.exchange(
                            ISSUE_ENDPOINT,
                            HttpMethod.POST,
                            HttpEntity<Void>(authHeaders(loginId = "user$i", password = "Test1234!@")),
                            ISSUE_RESPONSE_TYPE,
                            coupon.id,
                        )
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executorService.shutdown()

            // assert
            val issuedCoupons = issuedCouponRepository.findByCouponId(coupon.id)
            assertThat(issuedCoupons).hasSize(10)
        }
    }
}
