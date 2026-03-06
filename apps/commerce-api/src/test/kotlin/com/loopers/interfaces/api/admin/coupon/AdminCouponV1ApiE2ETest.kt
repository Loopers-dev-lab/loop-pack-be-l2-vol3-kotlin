package com.loopers.interfaces.api.admin.coupon

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
class AdminCouponV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api-admin/v1/coupons"
        private const val HEADER_LDAP = "X-Loopers-Ldap"
        private const val LDAP_VALUE = "loopers.admin"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(HEADER_LDAP, LDAP_VALUE)
        }
    }

    private fun createFixedTemplateRequest(
        name: String = "1000원 할인 쿠폰",
        value: Long = 1000L,
        minOrderAmount: Long? = 5000L,
    ) = AdminCouponV1Dto.CreateRequest(
        name = name,
        type = "FIXED",
        value = value,
        minOrderAmount = minOrderAmount,
        maxDiscountAmount = null,
        expirationPolicy = "FIXED_DATE",
        expiredAt = ZonedDateTime.now().plusDays(30),
        validDays = null,
    )

    private fun createRateTemplateRequest(
        name: String = "10% 할인 쿠폰",
        value: Long = 10L,
        maxDiscountAmount: Long? = 5000L,
    ) = AdminCouponV1Dto.CreateRequest(
        name = name,
        type = "RATE",
        value = value,
        minOrderAmount = null,
        maxDiscountAmount = maxDiscountAmount,
        expirationPolicy = "DAYS_FROM_ISSUE",
        expiredAt = null,
        validDays = 7,
    )

    private fun createFixedTemplateViaApi(
        name: String = "1000원 할인 쿠폰",
    ): AdminCouponV1Dto.TemplateResponse {
        val response = testRestTemplate.exchange(
            ENDPOINT,
            HttpMethod.POST,
            HttpEntity(createFixedTemplateRequest(name = name), adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Dto.TemplateResponse>>() {},
        )
        return response.body!!.data!!
    }

    private fun createRateTemplateViaApi(
        name: String = "10% 할인 쿠폰",
    ): AdminCouponV1Dto.TemplateResponse {
        val response = testRestTemplate.exchange(
            ENDPOINT,
            HttpMethod.POST,
            HttpEntity(createRateTemplateRequest(name = name), adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Dto.TemplateResponse>>() {},
        )
        return response.body!!.data!!
    }

    @DisplayName("POST /api-admin/v1/coupons (쿠폰 템플릿 등록)")
    @Nested
    inner class CreateTemplate {
        @DisplayName("유효한 FIXED 타입 쿠폰으로 등록하면, 201 CREATED 응답을 받는다.")
        @Test
        fun returns201_whenValidFixedCoupon() {
            // arrange
            val request = createFixedTemplateRequest()

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Dto.TemplateResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.name).isEqualTo("1000원 할인 쿠폰") },
                { assertThat(response.body?.data?.type).isEqualTo("FIXED") },
                { assertThat(response.body?.data?.value).isEqualTo(1000L) },
                { assertThat(response.body?.data?.expirationPolicy).isEqualTo("FIXED_DATE") },
                { assertThat(response.body?.data?.status).isEqualTo("ACTIVE") },
            )
        }

        @DisplayName("유효한 RATE 타입 쿠폰으로 등록하면, 201 CREATED 응답을 받는다.")
        @Test
        fun returns201_whenValidRateCoupon() {
            // arrange
            val request = createRateTemplateRequest()

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Dto.TemplateResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.name).isEqualTo("10% 할인 쿠폰") },
                { assertThat(response.body?.data?.type).isEqualTo("RATE") },
                { assertThat(response.body?.data?.value).isEqualTo(10L) },
                { assertThat(response.body?.data?.maxDiscountAmount).isEqualTo(5000L) },
                { assertThat(response.body?.data?.expirationPolicy).isEqualTo("DAYS_FROM_ISSUE") },
                { assertThat(response.body?.data?.validDays).isEqualTo(7) },
            )
        }

        @DisplayName("인증 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returns401_whenNoAdminHeader() {
            // arrange
            val request = createFixedTemplateRequest()

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("GET /api-admin/v1/coupons/{couponId} (쿠폰 템플릿 상세 조회)")
    @Nested
    inner class GetTemplate {
        @DisplayName("존재하는 쿠폰 템플릿을 조회하면, 200 OK 응답을 받는다.")
        @Test
        fun returns200_whenExists() {
            // arrange
            val created = createFixedTemplateViaApi()

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT/${created.id}",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Dto.TemplateResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("1000원 할인 쿠폰") },
                { assertThat(response.body?.data?.type).isEqualTo("FIXED") },
            )
        }

        @DisplayName("존재하지 않는 쿠폰 템플릿을 조회하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returns404_whenNotFound() {
            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT/999",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("인증 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returns401_whenNoAdminHeader() {
            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT/1",
                HttpMethod.GET,
                HttpEntity<Any>(null, null),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("GET /api-admin/v1/coupons (쿠폰 템플릿 목록 조회)")
    @Nested
    inner class GetTemplates {
        @DisplayName("쿠폰 템플릿이 존재하면, 페이징된 결과를 반환한다.")
        @Test
        fun returnsPaginatedTemplates() {
            // arrange
            createFixedTemplateViaApi(name = "쿠폰1")
            createRateTemplateViaApi(name = "쿠폰2")

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }
    }

    @DisplayName("PUT /api-admin/v1/coupons/{couponId} (쿠폰 템플릿 수정)")
    @Nested
    inner class UpdateTemplate {
        @DisplayName("유효한 정보로 수정하면, 200 OK 응답을 받는다.")
        @Test
        fun returns200_whenValidUpdate() {
            // arrange
            val created = createFixedTemplateViaApi()
            val updateRequest = AdminCouponV1Dto.UpdateRequest(
                name = "수정된 쿠폰명",
                type = "FIXED",
                value = 2000L,
                minOrderAmount = 10000L,
                maxDiscountAmount = null,
                expirationPolicy = "FIXED_DATE",
                expiredAt = ZonedDateTime.now().plusDays(60),
                validDays = null,
            )

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT/${created.id}",
                HttpMethod.PUT,
                HttpEntity(updateRequest, adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<AdminCouponV1Dto.TemplateResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("수정된 쿠폰명") },
                { assertThat(response.body?.data?.value).isEqualTo(2000L) },
            )
        }
    }

    @DisplayName("DELETE /api-admin/v1/coupons/{couponId} (쿠폰 템플릿 삭제)")
    @Nested
    inner class DeleteTemplate {
        @DisplayName("존재하는 쿠폰 템플릿을 삭제하면, 200 OK 응답을 받는다.")
        @Test
        fun returns200_whenDelete() {
            // arrange
            val created = createFixedTemplateViaApi()

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT/${created.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @DisplayName("이미 삭제된 쿠폰 템플릿을 삭제하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returns404_whenAlreadyDeleted() {
            // arrange
            val created = createFixedTemplateViaApi()
            testRestTemplate.exchange(
                "$ENDPOINT/${created.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT/${created.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
