package com.loopers.interfaces.apiadmin

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponQuantity
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.Discount
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.IssuedCoupon
import com.loopers.domain.coupon.IssuedCouponRepository
import com.loopers.domain.user.Email
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.User
import com.loopers.domain.user.UserRepository
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
import org.springframework.http.MediaType
import java.time.LocalDate
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminCouponApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val couponRepository: CouponRepository,
    private val issuedCouponRepository: IssuedCouponRepository,
    private val userRepository: UserRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val COUPON_ENDPOINT = "/api-admin/v1/coupons"
        private const val COUPON_DETAIL_ENDPOINT = "/api-admin/v1/coupons/{couponId}"
        private const val COUPON_ISSUES_ENDPOINT = "/api-admin/v1/coupons/{couponId}/issues"
        private const val LDAP_HEADER = "X-Loopers-Ldap"
        private const val LDAP_VALUE = "loopers.admin"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set(LDAP_HEADER, LDAP_VALUE)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractPageContent(data: Map<String, Any>?): List<Map<String, Any>>? {
        return data?.get("content") as? List<Map<String, Any>>
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
                discount = Discount(discountType, discountValue),
                quantity = CouponQuantity(totalQuantity, 0),
                expiresAt = expiresAt,
            ),
        )
    }

    private fun createUser(
        loginId: String = "testuser",
        password: String = "Test1234!@",
        name: String = "홍길동",
        email: String = "test@example.com",
        birthday: LocalDate = LocalDate.of(1990, 1, 15),
    ): User {
        return userRepository.save(
            User(
                loginId = LoginId.of(loginId),
                password = password,
                name = name,
                birthday = birthday,
                email = Email.of(email),
            ),
        )
    }

    private fun createIssuedCoupon(couponId: Long, userId: Long): IssuedCoupon {
        return issuedCouponRepository.save(
            IssuedCoupon(couponId = couponId, userId = userId),
        )
    }

    @DisplayName("POST /api-admin/v1/coupons")
    @Nested
    inner class CreateCoupon {

        @DisplayName("유효한 요청으로 쿠폰을 생성하면, 200 OK와 쿠폰 정보를 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            val request = mapOf(
                "name" to "신규가입 10% 할인",
                "discountType" to "PERCENTAGE",
                "discountValue" to 10,
                "totalQuantity" to 100,
                "expiresAt" to "2026-12-31T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                COUPON_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.get("name")).isEqualTo("신규가입 10% 할인") },
                { assertThat(response.body?.data?.get("discountType")).isEqualTo("PERCENTAGE") },
                { assertThat(response.body?.data?.get("discountValue")).isEqualTo(10) },
                { assertThat(response.body?.data?.get("totalQuantity")).isEqualTo(100) },
                { assertThat(response.body?.data?.get("expiresAt")).isNotNull() },
            )
        }

        @DisplayName("FIXED_AMOUNT 타입으로 쿠폰을 생성하면, 200 OK와 쿠폰 정보를 반환한다.")
        @Test
        fun returnsOk_whenFixedAmountType() {
            // arrange
            val request = mapOf(
                "name" to "5000원 할인 쿠폰",
                "discountType" to "FIXED_AMOUNT",
                "discountValue" to 5000,
                "totalQuantity" to 50,
                "expiresAt" to "2026-12-31T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                COUPON_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.get("name")).isEqualTo("5000원 할인 쿠폰") },
                { assertThat(response.body?.data?.get("discountType")).isEqualTo("FIXED_AMOUNT") },
                { assertThat(response.body?.data?.get("discountValue")).isEqualTo(5000) },
            )
        }

        @DisplayName("쿠폰명이 비어있으면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNameIsBlank() {
            // arrange
            val request = mapOf(
                "name" to "  ",
                "discountType" to "PERCENTAGE",
                "discountValue" to 10,
                "totalQuantity" to 100,
                "expiresAt" to "2026-12-31T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("정률 쿠폰의 할인값이 100을 초과하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenPercentageValueExceeds100() {
            // arrange
            val request = mapOf(
                "name" to "잘못된 할인",
                "discountType" to "PERCENTAGE",
                "discountValue" to 101,
                "totalQuantity" to 100,
                "expiresAt" to "2026-12-31T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("생성된 쿠폰을 상세 조회하면, 동일한 정보가 반환된다.")
        @Test
        fun createdCouponCanBeRetrieved() {
            // arrange
            val request = mapOf(
                "name" to "신규가입 10% 할인",
                "discountType" to "PERCENTAGE",
                "discountValue" to 10,
                "totalQuantity" to 100,
                "expiresAt" to "2026-12-31T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act - 생성
            val createResponseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val createResponse = testRestTemplate.exchange(
                COUPON_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                createResponseType,
            )
            val createdId = createResponse.body?.data?.get("id")

            // act - 조회
            val detailResponseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val detailResponse = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                detailResponseType,
                createdId,
            )

            // assert
            val data = detailResponse.body?.data
            assertAll(
                { assertThat(detailResponse.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(data?.get("name")).isEqualTo("신규가입 10% 할인") },
                { assertThat(data?.get("discountType")).isEqualTo("PERCENTAGE") },
                { assertThat(data?.get("discountValue")).isEqualTo(10) },
                { assertThat(data?.get("totalQuantity")).isEqualTo(100) },
                { assertThat(data?.get("issuedQuantity")).isEqualTo(0) },
            )
        }

        @DisplayName("할인값이 0이면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenDiscountValueIsZero() {
            // arrange
            val request = mapOf(
                "name" to "잘못된 쿠폰",
                "discountType" to "FIXED_AMOUNT",
                "discountValue" to 0,
                "totalQuantity" to 100,
                "expiresAt" to "2026-12-31T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("총 발급 수량이 0이면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenTotalQuantityIsZero() {
            // arrange
            val request = mapOf(
                "name" to "잘못된 쿠폰",
                "discountType" to "FIXED_AMOUNT",
                "discountValue" to 5000,
                "totalQuantity" to 0,
                "expiresAt" to "2026-12-31T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            // arrange
            val request = mapOf(
                "name" to "신규가입 10% 할인",
                "discountType" to "PERCENTAGE",
                "discountValue" to 10,
                "totalQuantity" to 100,
                "expiresAt" to "2026-12-31T23:59:59+09:00",
            )
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더 값이 올바르지 않으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsInvalid() {
            // arrange
            val request = mapOf(
                "name" to "신규가입 10% 할인",
                "discountType" to "PERCENTAGE",
                "discountValue" to 10,
                "totalQuantity" to 100,
                "expiresAt" to "2026-12-31T23:59:59+09:00",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }

    @DisplayName("PUT /api-admin/v1/coupons/{couponId}")
    @Nested
    inner class UpdateCoupon {

        @DisplayName("유효한 요청으로 쿠폰을 수정하면, 200 OK와 수정된 쿠폰 정보를 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            val coupon = createCoupon(
                name = "신규가입 할인",
                discountType = DiscountType.FIXED_AMOUNT,
                discountValue = 5000L,
            )
            val request = mapOf(
                "name" to "수정된 쿠폰",
                "discountType" to "PERCENTAGE",
                "discountValue" to 20,
                "expiresAt" to "2027-06-30T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.get("name")).isEqualTo("수정된 쿠폰") },
                { assertThat(response.body?.data?.get("discountType")).isEqualTo("PERCENTAGE") },
                { assertThat(response.body?.data?.get("discountValue")).isEqualTo(20) },
            )
        }

        @DisplayName("수정한 쿠폰을 상세 조회하면, 수정된 정보가 반환된다.")
        @Test
        fun updatedCouponCanBeRetrieved() {
            // arrange
            val coupon = createCoupon(
                name = "신규가입 할인",
                discountType = DiscountType.FIXED_AMOUNT,
                discountValue = 5000L,
            )
            val request = mapOf(
                "name" to "수정된 쿠폰",
                "discountType" to "PERCENTAGE",
                "discountValue" to 15,
                "expiresAt" to "2027-06-30T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act - 수정
            val updateResponseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                updateResponseType,
                coupon.id,
            )

            // act - 조회
            val detailResponseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val detailResponse = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                detailResponseType,
                coupon.id,
            )

            // assert
            val data = detailResponse.body?.data
            assertAll(
                { assertThat(detailResponse.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(data?.get("name")).isEqualTo("수정된 쿠폰") },
                { assertThat(data?.get("discountType")).isEqualTo("PERCENTAGE") },
                { assertThat(data?.get("discountValue")).isEqualTo(15) },
            )
        }

        @DisplayName("존재하지 않는 쿠폰 ID로 수정 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenCouponNotExists() {
            // arrange
            val request = mapOf(
                "name" to "수정된 쿠폰",
                "discountType" to "FIXED_AMOUNT",
                "discountValue" to 3000,
                "expiresAt" to "2027-06-30T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                999999L,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("쿠폰명이 비어있으면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNameIsBlank() {
            // arrange
            val coupon = createCoupon()
            val request = mapOf(
                "name" to "  ",
                "discountType" to "FIXED_AMOUNT",
                "discountValue" to 3000,
                "expiresAt" to "2027-06-30T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("정률 쿠폰의 할인값이 100을 초과하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenPercentageValueExceeds100() {
            // arrange
            val coupon = createCoupon()
            val request = mapOf(
                "name" to "수정된 쿠폰",
                "discountType" to "PERCENTAGE",
                "discountValue" to 101,
                "expiresAt" to "2027-06-30T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("할인값이 0이면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenDiscountValueIsZero() {
            // arrange
            val coupon = createCoupon()
            val request = mapOf(
                "name" to "수정된 쿠폰",
                "discountType" to "FIXED_AMOUNT",
                "discountValue" to 0,
                "expiresAt" to "2027-06-30T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("삭제된 쿠폰 ID로 수정 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenCouponIsDeleted() {
            // arrange
            val coupon = createCoupon(name = "삭제될 쿠폰")
            coupon.delete()
            couponRepository.save(coupon)
            val request = mapOf(
                "name" to "수정된 쿠폰",
                "discountType" to "FIXED_AMOUNT",
                "discountValue" to 3000,
                "expiresAt" to "2027-06-30T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("수정 응답에 totalQuantity, issuedQuantity, expiresAt, createdAt이 포함된다.")
        @Test
        fun includesAllFieldsInResponse() {
            // arrange
            val coupon = createCoupon(
                name = "신규가입 할인",
                discountType = DiscountType.FIXED_AMOUNT,
                discountValue = 5000L,
                totalQuantity = 200,
            )
            val request = mapOf(
                "name" to "수정된 쿠폰",
                "discountType" to "PERCENTAGE",
                "discountValue" to 10,
                "expiresAt" to "2027-06-30T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            val data = response.body?.data
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(data?.get("id")).isNotNull() },
                { assertThat(data?.get("name")).isEqualTo("수정된 쿠폰") },
                { assertThat(data?.get("discountType")).isEqualTo("PERCENTAGE") },
                { assertThat(data?.get("discountValue")).isEqualTo(10) },
                { assertThat(data?.get("totalQuantity")).isEqualTo(200) },
                { assertThat(data?.get("issuedQuantity")).isEqualTo(0) },
                { assertThat(data?.get("expiresAt")).isNotNull() },
                { assertThat(data?.get("createdAt")).isNotNull() },
            )
        }

        @DisplayName("FIXED_AMOUNT 타입으로 수정하면, 200 OK와 수정된 쿠폰 정보를 반환한다.")
        @Test
        fun returnsOk_whenUpdatedToFixedAmount() {
            // arrange
            val coupon = createCoupon(
                name = "10% 할인",
                discountType = DiscountType.PERCENTAGE,
                discountValue = 10L,
            )
            val request = mapOf(
                "name" to "5000원 할인",
                "discountType" to "FIXED_AMOUNT",
                "discountValue" to 5000,
                "expiresAt" to "2027-06-30T23:59:59+09:00",
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.get("name")).isEqualTo("5000원 할인") },
                { assertThat(response.body?.data?.get("discountType")).isEqualTo("FIXED_AMOUNT") },
                { assertThat(response.body?.data?.get("discountValue")).isEqualTo(5000) },
            )
        }

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            // arrange
            val coupon = createCoupon()
            val request = mapOf(
                "name" to "수정된 쿠폰",
                "discountType" to "FIXED_AMOUNT",
                "discountValue" to 3000,
                "expiresAt" to "2027-06-30T23:59:59+09:00",
            )
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더 값이 올바르지 않으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsInvalid() {
            // arrange
            val coupon = createCoupon()
            val request = mapOf(
                "name" to "수정된 쿠폰",
                "discountType" to "FIXED_AMOUNT",
                "discountValue" to 3000,
                "expiresAt" to "2027-06-30T23:59:59+09:00",
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }

    @DisplayName("GET /api-admin/v1/coupons")
    @Nested
    inner class GetCoupons {

        @DisplayName("유효한 LDAP 헤더로 조회하면, 200 OK와 쿠폰 목록을 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            createCoupon(name = "신규가입 할인")
            createCoupon(name = "여름 할인", discountType = DiscountType.PERCENTAGE, discountValue = 15L)
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$COUPON_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(content).hasSize(2) },
                { assertThat(data?.get("totalElements")).isEqualTo(2) },
            )
        }

        @DisplayName("페이지 크기를 지정하면, 해당 크기만큼 반환한다.")
        @Test
        fun returnsPagedCoupons_whenPageSizeSpecified() {
            // arrange
            createCoupon(name = "쿠폰 1")
            createCoupon(name = "쿠폰 2")
            createCoupon(name = "쿠폰 3")
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$COUPON_ENDPOINT?page=0&size=2",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(content).hasSize(2) },
                { assertThat(data?.get("totalElements")).isEqualTo(3) },
                { assertThat(data?.get("totalPages")).isEqualTo(2) },
            )
        }

        @DisplayName("쿠폰이 없으면, 빈 페이지를 반환한다.")
        @Test
        fun returnsEmptyPage_whenNoCouponsExist() {
            // arrange
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$COUPON_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(content).isEmpty() },
                { assertThat(data?.get("totalElements")).isEqualTo(0) },
            )
        }

        @DisplayName("페이지 파라미터 없이 요청하면, 기본값(page=0, size=20)이 적용된다.")
        @Test
        fun returnsDefaultPage_whenNoPageParams() {
            // arrange
            createCoupon(name = "신규가입 할인")
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                COUPON_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(content).hasSize(1) },
                { assertThat(data?.get("size")).isEqualTo(20) },
                { assertThat(data?.get("page")).isEqualTo(0) },
            )
        }

        @DisplayName("삭제된 쿠폰은 목록에 포함되지 않는다.")
        @Test
        fun excludesDeletedCoupons() {
            // arrange
            createCoupon(name = "활성 쿠폰")
            val deletedCoupon = createCoupon(name = "삭제될 쿠폰")
            deletedCoupon.delete()
            couponRepository.save(deletedCoupon)
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$COUPON_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(content).hasSize(1) },
                { assertThat(data?.get("totalElements")).isEqualTo(1) },
            )
        }

        @DisplayName("응답에 쿠폰 타입, 할인값, 만료일, 생성일이 포함된다.")
        @Test
        fun includesCouponDetailsInResponse() {
            // arrange
            createCoupon(
                name = "고정액 할인",
                discountType = DiscountType.FIXED_AMOUNT,
                discountValue = 3000L,
            )
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$COUPON_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val content = extractPageContent(response.body?.data)
            val coupon = content?.first()
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(coupon?.get("name")).isEqualTo("고정액 할인") },
                { assertThat(coupon?.get("discountType")).isEqualTo("FIXED_AMOUNT") },
                { assertThat(coupon?.get("discountValue")).isEqualTo(3000) },
                { assertThat(coupon?.get("expiresAt")).isNotNull() },
                { assertThat(coupon?.get("createdAt")).isNotNull() },
            )
        }

        @DisplayName("PERCENTAGE 타입 쿠폰의 할인 정보가 올바르게 반환된다.")
        @Test
        fun returnsPercentageDiscountDetails() {
            // arrange
            createCoupon(
                name = "여름 할인",
                discountType = DiscountType.PERCENTAGE,
                discountValue = 15L,
            )
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$COUPON_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val content = extractPageContent(response.body?.data)
            val coupon = content?.first()
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(coupon?.get("name")).isEqualTo("여름 할인") },
                { assertThat(coupon?.get("discountType")).isEqualTo("PERCENTAGE") },
                { assertThat(coupon?.get("discountValue")).isEqualTo(15) },
            )
        }

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            // arrange
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val httpEntity = HttpEntity<Void>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$COUPON_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더 값이 올바르지 않으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsInvalid() {
            // arrange
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity<Void>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$COUPON_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }

    @DisplayName("GET /api-admin/v1/coupons/{couponId}")
    @Nested
    inner class GetCouponDetail {

        @DisplayName("존재하는 쿠폰을 조회하면, 200 OK와 쿠폰 상세 정보를 반환한다.")
        @Test
        fun returnsOk_whenCouponExists() {
            // arrange
            val coupon = createCoupon(
                name = "신규가입 할인",
                discountType = DiscountType.FIXED_AMOUNT,
                discountValue = 5000L,
                totalQuantity = 100,
            )
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            val data = response.body?.data
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(data?.get("name")).isEqualTo("신규가입 할인") },
                { assertThat(data?.get("discountType")).isEqualTo("FIXED_AMOUNT") },
                { assertThat(data?.get("discountValue")).isEqualTo(5000) },
                { assertThat(data?.get("totalQuantity")).isEqualTo(100) },
                { assertThat(data?.get("issuedQuantity")).isEqualTo(0) },
                { assertThat(data?.get("expiresAt")).isNotNull() },
                { assertThat(data?.get("createdAt")).isNotNull() },
            )
        }

        @DisplayName("PERCENTAGE 타입 쿠폰의 상세 정보가 올바르게 반환된다.")
        @Test
        fun returnsPercentageDiscountDetails() {
            // arrange
            val coupon = createCoupon(
                name = "여름 할인",
                discountType = DiscountType.PERCENTAGE,
                discountValue = 15L,
            )
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            val data = response.body?.data
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(data?.get("name")).isEqualTo("여름 할인") },
                { assertThat(data?.get("discountType")).isEqualTo("PERCENTAGE") },
                { assertThat(data?.get("discountValue")).isEqualTo(15) },
            )
        }

        @DisplayName("존재하지 않는 쿠폰 ID로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenCouponNotExists() {
            // arrange
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                999999L,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("삭제된 쿠폰 ID로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenCouponIsDeleted() {
            // arrange
            val coupon = createCoupon(name = "삭제될 쿠폰")
            coupon.delete()
            couponRepository.save(coupon)
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertThat(response.statusCode.value()).isEqualTo(404)
        }

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            // arrange
            val coupon = createCoupon()
            val httpEntity = HttpEntity<Void>(HttpHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더 값이 올바르지 않으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsInvalid() {
            // arrange
            val coupon = createCoupon()
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity<Void>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("404 에러 응답에 에러 메시지가 포함된다.")
        @Test
        fun returnsErrorMessage_whenCouponNotFound() {
            // arrange
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                999999L,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
                { assertThat(response.body?.meta?.message).contains("쿠폰을 찾을 수 없습니다") },
            )
        }
    }

    @DisplayName("DELETE /api-admin/v1/coupons/{couponId}")
    @Nested
    inner class DeleteCoupon {

        @DisplayName("존재하는 쿠폰을 삭제하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            val coupon = createCoupon(name = "삭제할 쿠폰")
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }

        @DisplayName("삭제 후 상세 조회하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenGetDeletedCoupon() {
            // arrange
            val coupon = createCoupon(name = "삭제할 쿠폰")
            val httpEntity = HttpEntity<Void>(adminHeaders())
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}

            // act - 삭제
            testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                responseType,
                coupon.id,
            )

            // act - 조회
            val detailResponse = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertThat(detailResponse.statusCode.value()).isEqualTo(404)
        }

        @DisplayName("삭제 후 목록 조회에서 제외된다.")
        @Test
        fun excludesDeletedCouponFromList() {
            // arrange
            createCoupon(name = "활성 쿠폰")
            val couponToDelete = createCoupon(name = "삭제할 쿠폰")
            val httpEntity = HttpEntity<Void>(adminHeaders())
            val deleteResponseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}

            // act - 삭제
            testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                deleteResponseType,
                couponToDelete.id,
            )

            // act - 목록 조회
            val listResponseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val listResponse = testRestTemplate.exchange(
                "$COUPON_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                listResponseType,
            )

            // assert
            val data = listResponse.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(listResponse.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(content).hasSize(1) },
                { assertThat(data?.get("totalElements")).isEqualTo(1) },
            )
        }

        @DisplayName("이미 삭제된 쿠폰을 다시 삭제하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenAlreadyDeleted() {
            // arrange
            val coupon = createCoupon(name = "삭제할 쿠폰")
            coupon.delete()
            couponRepository.save(coupon)
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertThat(response.statusCode.value()).isEqualTo(404)
        }

        @DisplayName("존재하지 않는 쿠폰 ID로 삭제하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenCouponNotExists() {
            // arrange
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                responseType,
                999999L,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("API로 삭제한 쿠폰을 다시 API로 삭제하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenDeletedViaApiTwice() {
            // arrange
            val coupon = createCoupon(name = "삭제할 쿠폰")
            val httpEntity = HttpEntity<Void>(adminHeaders())
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}

            // act - 첫 번째 삭제
            testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                responseType,
                coupon.id,
            )

            // act - 두 번째 삭제
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertThat(response.statusCode.value()).isEqualTo(404)
        }

        @DisplayName("404 에러 응답에 에러 메시지가 포함된다.")
        @Test
        fun returnsErrorMessage_whenCouponNotFound() {
            // arrange
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                responseType,
                999999L,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
                { assertThat(response.body?.meta?.message).contains("쿠폰을 찾을 수 없습니다") },
            )
        }

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            // arrange
            val coupon = createCoupon(name = "삭제할 쿠폰")
            val httpEntity = HttpEntity<Void>(HttpHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더 값이 올바르지 않으면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsInvalid() {
            // arrange
            val coupon = createCoupon(name = "삭제할 쿠폰")
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity<Void>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_DETAIL_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                responseType,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }

    @DisplayName("GET /api-admin/v1/coupons/{couponId}/issues")
    @Nested
    inner class GetCouponIssues {

        @DisplayName("발급 내역이 있으면, 200 OK와 페이징된 발급 내역을 반환한다.")
        @Test
        fun returnsOk_whenIssuedCouponsExist() {
            // arrange
            val coupon = createCoupon()
            val user = createUser()
            createIssuedCoupon(coupon.id, user.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$COUPON_ISSUES_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                responseType,
                coupon.id,
            )

            // assert
            val data = response.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(content).hasSize(1) },
                { assertThat(content?.get(0)?.get("userId")).isEqualTo(user.id.toInt()) },
                { assertThat(content?.get(0)?.get("userName")).isEqualTo(user.name) },
                { assertThat(content?.get(0)?.get("status")).isEqualTo("AVAILABLE") },
                { assertThat(content?.get(0)?.get("issuedAt")).isNotNull() },
                { assertThat(data?.get("totalElements")).isEqualTo(1) },
            )
        }

        @DisplayName("존재하지 않는 쿠폰 ID이면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenCouponNotExists() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_ISSUES_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                responseType,
                999999L,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("발급 내역이 없으면, 200 OK와 빈 페이지를 반환한다.")
        @Test
        fun returnsEmptyPage_whenNoIssuedCoupons() {
            // arrange
            val coupon = createCoupon()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$COUPON_ISSUES_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                responseType,
                coupon.id,
            )

            // assert
            val data = response.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(content).isEmpty() },
                { assertThat(data?.get("totalElements")).isEqualTo(0) },
            )
        }

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderMissing() {
            // arrange
            val coupon = createCoupon()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_ISSUES_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(HttpHeaders()),
                responseType,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("페이지 크기를 지정하면, 해당 크기만큼 반환하고 페이징 정보가 올바르다.")
        @Test
        fun returnsPagedIssues_whenPageSizeSpecified() {
            // arrange
            val coupon = createCoupon()
            val user1 = createUser(loginId = "user1", email = "user1@example.com")
            val user2 = createUser(loginId = "user2", email = "user2@example.com")
            val user3 = createUser(loginId = "user3", email = "user3@example.com")
            createIssuedCoupon(coupon.id, user1.id)
            createIssuedCoupon(coupon.id, user2.id)
            createIssuedCoupon(coupon.id, user3.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$COUPON_ISSUES_ENDPOINT?page=0&size=2",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                responseType,
                coupon.id,
            )

            // assert
            val data = response.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(content).hasSize(2) },
                { assertThat(data?.get("totalElements")).isEqualTo(3) },
                { assertThat(data?.get("totalPages")).isEqualTo(2) },
            )
        }

        @DisplayName("사용된 쿠폰은 USED 상태와 usedAt이 포함된다.")
        @Test
        fun returnsUsedStatus_whenCouponIsUsed() {
            // arrange
            val coupon = createCoupon()
            val user = createUser()
            val issuedCoupon = createIssuedCoupon(coupon.id, user.id)
            issuedCoupon.use()
            issuedCouponRepository.save(issuedCoupon)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$COUPON_ISSUES_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                responseType,
                coupon.id,
            )

            // assert
            val content = extractPageContent(response.body?.data)
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(content?.get(0)?.get("status")).isEqualTo("USED") },
                { assertThat(content?.get(0)?.get("usedAt")).isNotNull() },
            )
        }

        @DisplayName("삭제된 쿠폰의 발급 내역을 조회하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenCouponIsDeleted() {
            // arrange
            val coupon = createCoupon()
            val user = createUser()
            createIssuedCoupon(coupon.id, user.id)
            coupon.delete()
            couponRepository.save(coupon)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                COUPON_ISSUES_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                responseType,
                coupon.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("응답에 사용자 ID, 사용자 이름, 발급일시, 상태가 포함되고, 미사용 쿠폰은 usedAt이 없다.")
        @Test
        fun includesAllFieldsInResponse() {
            // arrange
            val coupon = createCoupon()
            val user = createUser(name = "테스트유저")
            createIssuedCoupon(coupon.id, user.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$COUPON_ISSUES_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                responseType,
                coupon.id,
            )

            // assert (NON_NULL 설정으로 usedAt이 null이면 응답에 포함되지 않음)
            val content = extractPageContent(response.body?.data)
            val item = content?.first()
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(item?.containsKey("userId")).isTrue() },
                { assertThat(item?.containsKey("userName")).isTrue() },
                { assertThat(item?.containsKey("issuedAt")).isTrue() },
                { assertThat(item?.containsKey("status")).isTrue() },
                { assertThat(item?.get("userName")).isEqualTo("테스트유저") },
                { assertThat(item?.containsKey("usedAt")).isFalse() },
                { assertThat(item?.get("status")).isEqualTo("AVAILABLE") },
            )
        }

        @DisplayName("페이지 파라미터 없이 요청하면, 기본값(page=0, size=20)이 적용된다.")
        @Test
        fun returnsDefaultPage_whenNoPageParams() {
            // arrange
            val coupon = createCoupon()
            val user = createUser()
            createIssuedCoupon(coupon.id, user.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                COUPON_ISSUES_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                responseType,
                coupon.id,
            )

            // assert
            val data = response.body?.data
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(data?.get("page")).isEqualTo(0) },
                { assertThat(data?.get("size")).isEqualTo(20) },
            )
        }
    }
}
