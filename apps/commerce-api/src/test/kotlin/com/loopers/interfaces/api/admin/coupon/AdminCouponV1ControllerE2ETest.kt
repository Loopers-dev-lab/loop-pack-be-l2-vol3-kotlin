package com.loopers.interfaces.api.admin.coupon

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageResponse
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

@DisplayName("Admin 쿠폰 CRUD E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminCouponV1ControllerE2ETest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api-admin/v1/coupons"
        private const val VALID_LDAP = "loopers.admin"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun headers(ldap: String? = VALID_LDAP): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            ldap?.let { set("X-Loopers-Ldap", it) }
        }
    }

    private fun registerCoupon(
        name: String = "테스트 쿠폰",
        type: String = "FIXED",
        discountValue: Long = 1000,
        minOrderAmount: Long? = null,
        expiredAt: String = "2099-12-31T23:59:59+09:00",
    ): HttpEntity<String> {
        val minOrderAmountJson = minOrderAmount?.let { "\"minOrderAmount\": $it," } ?: ""
        val body = """
            {
                "name": "$name",
                "type": "$type",
                "discountValue": $discountValue,
                $minOrderAmountJson
                "expiredAt": "$expiredAt"
            }
        """.trimIndent()
        return HttpEntity(body, headers())
    }

    private fun createCouponAndGetId(): Long {
        val response = testRestTemplate.exchange(
            ENDPOINT,
            HttpMethod.POST,
            registerCoupon(),
            object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Response.Register>>() {},
        )
        return response.body!!.data!!.id
    }

    @Nested
    @DisplayName("POST /api-admin/v1/coupons")
    inner class Register {
        @Test
        @DisplayName("유효한 FIXED 쿠폰 등록 → 201 Created")
        fun register_fixedCoupon_returns201() {
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                registerCoupon(),
                object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Response.Register>>() {},
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.meta?.result?.name).isEqualTo("SUCCESS") },
                { assertThat(response.body?.data?.name).isEqualTo("테스트 쿠폰") },
                { assertThat(response.body?.data?.type).isEqualTo("FIXED") },
                { assertThat(response.body?.data?.discountValue).isEqualTo(1000L) },
            )
        }

        @Test
        @DisplayName("유효한 RATE 쿠폰 등록 → 201 Created")
        fun register_rateCoupon_returns201() {
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                registerCoupon(type = "RATE", discountValue = 10, minOrderAmount = 10000),
                object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Response.Register>>() {},
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.type).isEqualTo("RATE") },
                { assertThat(response.body?.data?.discountValue).isEqualTo(10L) },
            )
        }

        @Test
        @DisplayName("잘못된 LDAP → 401")
        fun register_invalidLdap_returns401() {
            val body = """{"name":"쿠폰","type":"FIXED","discountValue":1000,"expiredAt":"2099-12-31T23:59:59+09:00"}"""
            val request = HttpEntity(body, headers(ldap = "invalid"))
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                request,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @Nested
    @DisplayName("GET /api-admin/v1/coupons/{couponId}")
    inner class Detail {
        @Test
        @DisplayName("존재하는 쿠폰 조회 → 200 OK")
        fun getDetail_existingCoupon_returns200() {
            val couponId = createCouponAndGetId()

            val response = testRestTemplate.exchange(
                "$ENDPOINT/$couponId",
                HttpMethod.GET,
                HttpEntity<Any>(headers()),
                object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Response.Detail>>() {},
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(couponId) },
                { assertThat(response.body?.data?.name).isEqualTo("테스트 쿠폰") },
            )
        }
    }

    @Nested
    @DisplayName("GET /api-admin/v1/coupons")
    inner class List {
        @Test
        @DisplayName("쿠폰 목록 조회 → 200 OK")
        fun getList_returns200() {
            createCouponAndGetId()
            createCouponAndGetId()

            val response = testRestTemplate.exchange(
                "$ENDPOINT?page=0&size=10",
                HttpMethod.GET,
                HttpEntity<Any>(headers()),
                object :
                    ParameterizedTypeReference<ApiResponse<PageResponse<AdminCouponV1Response.Summary>>>() {},
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(2) },
                { assertThat(response.body?.data?.totalElements).isEqualTo(2L) },
            )
        }
    }

    @Nested
    @DisplayName("PUT /api-admin/v1/coupons/{couponId}")
    inner class Update {
        @Test
        @DisplayName("쿠폰 수정 → 200 OK")
        fun update_returns200() {
            val couponId = createCouponAndGetId()
            val body = """
                {
                    "name": "수정된 쿠폰",
                    "discountValue": 2000,
                    "expiredAt": "2099-12-31T23:59:59+09:00"
                }
            """.trimIndent()
            val request = HttpEntity(body, headers())

            val response = testRestTemplate.exchange(
                "$ENDPOINT/$couponId",
                HttpMethod.PUT,
                request,
                object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Response.Update>>() {},
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("수정된 쿠폰") },
                { assertThat(response.body?.data?.discountValue).isEqualTo(2000L) },
            )
        }
    }

    @Nested
    @DisplayName("DELETE /api-admin/v1/coupons/{couponId}")
    inner class Delete {
        @Test
        @DisplayName("쿠폰 삭제 → 200 OK, 이후 조회 시 404")
        fun delete_returns200() {
            val couponId = createCouponAndGetId()

            val deleteResponse = testRestTemplate.exchange(
                "$ENDPOINT/$couponId",
                HttpMethod.DELETE,
                HttpEntity<Any>(headers()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )
            assertThat(deleteResponse.statusCode).isEqualTo(HttpStatus.OK)

            val getResponse = testRestTemplate.exchange(
                "$ENDPOINT/$couponId",
                HttpMethod.GET,
                HttpEntity<Any>(headers()),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
