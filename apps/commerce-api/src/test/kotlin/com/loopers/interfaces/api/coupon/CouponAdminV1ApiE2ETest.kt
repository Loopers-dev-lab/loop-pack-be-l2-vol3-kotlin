package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.constant.HttpHeaders
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
import org.springframework.http.HttpHeaders as SpringHttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponAdminV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api-admin/v1/coupons"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): SpringHttpHeaders {
        return SpringHttpHeaders().apply {
            set(HttpHeaders.LDAP, "loopers.admin")
        }
    }

    private fun adminHeadersWithJson(): SpringHttpHeaders {
        return SpringHttpHeaders().apply {
            set(HttpHeaders.LDAP, "loopers.admin")
            set("Content-Type", "application/json")
        }
    }

    private fun createCouponRequest(
        name: String = "테스트 쿠폰",
        type: String = "FIXED",
        value: Long = 5000,
        expiredAt: ZonedDateTime = ZonedDateTime.now().plusDays(7),
    ): CouponAdminV1Dto.CreateCouponRequest {
        return CouponAdminV1Dto.CreateCouponRequest(
            name = name,
            type = type,
            value = value,
            expiredAt = expiredAt,
        )
    }

    private fun createCouponAndGetId(): Long {
        val responseType = object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
        val response = testRestTemplate.exchange(
            ENDPOINT,
            HttpMethod.POST,
            HttpEntity(createCouponRequest(), adminHeaders()),
            responseType,
        )
        return response.body!!.data!!.id
    }

    @Nested
    @DisplayName("쿠폰 생성")
    inner class CreateCoupon {

        @Test
        @DisplayName("정액 쿠폰을 생성하면 200 OK와 쿠폰 정보를 반환한다")
        fun createFixedCoupon() {
            // arrange
            val request = createCouponRequest(name = "5000원 할인", type = "FIXED", value = 5000)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("5000원 할인") },
                { assertThat(response.body?.data?.type).isEqualTo("FIXED") },
                { assertThat(response.body?.data?.value).isEqualTo(5000L) },
            )
        }

        @Test
        @DisplayName("정률 쿠폰을 생성하면 200 OK와 쿠폰 정보를 반환한다")
        fun createRateCoupon() {
            // arrange
            val request = createCouponRequest(name = "10% 할인", type = "RATE", value = 10)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("10% 할인") },
                { assertThat(response.body?.data?.type).isEqualTo("RATE") },
                { assertThat(response.body?.data?.value).isEqualTo(10L) },
            )
        }
    }

    @Nested
    @DisplayName("쿠폰 조회")
    inner class GetCoupon {

        @Test
        @DisplayName("존재하는 쿠폰을 조회하면 200 OK와 쿠폰 정보를 반환한다")
        fun getCouponById() {
            // arrange
            val couponId = createCouponAndGetId()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT/$couponId",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(couponId) },
                { assertThat(response.body?.data?.name).isEqualTo("테스트 쿠폰") },
            )
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰을 조회하면 404 NOT_FOUND를 반환한다")
        fun getCouponNotFound() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT/999",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("쿠폰 목록 조회")
    inner class GetCoupons {

        @Test
        @DisplayName("쿠폰 목록을 조회하면 200 OK와 쿠폰 목록을 반환한다")
        fun getCouponList() {
            // arrange
            createCouponAndGetId()
            createCouponAndGetId()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT?page=0&size=10",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            val content = response.body?.data?.get("content") as? List<*>
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(content).hasSize(2) },
            )
        }
    }

    @Nested
    @DisplayName("쿠폰 수정")
    inner class UpdateCoupon {

        @Test
        @DisplayName("쿠폰 정보를 수정하면 200 OK와 수정된 정보를 반환한다")
        fun updateCoupon() {
            // arrange
            val couponId = createCouponAndGetId()
            val updateRequest = CouponAdminV1Dto.UpdateCouponRequest(
                name = "수정된 쿠폰",
                type = "RATE",
                value = 15,
                expiredAt = ZonedDateTime.now().plusDays(30),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponAdminV1Dto.CouponAdminResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT/$couponId",
                HttpMethod.PUT,
                HttpEntity(updateRequest, adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("수정된 쿠폰") },
                { assertThat(response.body?.data?.type).isEqualTo("RATE") },
                { assertThat(response.body?.data?.value).isEqualTo(15L) },
            )
        }
    }

    @Nested
    @DisplayName("쿠폰 삭제")
    inner class DeleteCoupon {

        @Test
        @DisplayName("쿠폰을 삭제하면 200 OK를 반환하고, 이후 조회 시 404를 반환한다")
        fun deleteCoupon() {
            // arrange
            val couponId = createCouponAndGetId()

            // act
            val deleteResponseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val deleteResponse = testRestTemplate.exchange(
                "$ENDPOINT/$couponId",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                deleteResponseType,
            )

            // assert — 삭제 성공
            assertThat(deleteResponse.statusCode).isEqualTo(HttpStatus.OK)

            // verify — 삭제 후 조회 시 404
            val getResponseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val getResponse = testRestTemplate.exchange(
                "$ENDPOINT/$couponId",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                getResponseType,
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
