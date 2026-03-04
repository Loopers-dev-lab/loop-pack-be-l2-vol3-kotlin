package com.loopers.interfaces.apiadmin

import com.loopers.domain.coupon.Coupon
import com.loopers.domain.coupon.CouponQuantity
import com.loopers.domain.coupon.CouponRepository
import com.loopers.domain.coupon.Discount
import com.loopers.domain.coupon.DiscountType
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
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminCouponApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val couponRepository: CouponRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val COUPON_ENDPOINT = "/api-admin/v1/coupons"
        private const val COUPON_DETAIL_ENDPOINT = "/api-admin/v1/coupons/{couponId}"
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
}
