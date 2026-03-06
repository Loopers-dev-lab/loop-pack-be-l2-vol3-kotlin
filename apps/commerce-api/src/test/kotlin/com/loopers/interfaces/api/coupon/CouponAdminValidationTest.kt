package com.loopers.interfaces.api.coupon

import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.HEADER_LDAP
import com.loopers.interfaces.support.LDAP_ADMIN_VALUE
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("CouponAdminV1Controller - Bean Validation 인터페이스 어노테이션 상속 검증")
class CouponAdminValidationTest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        private const val ENDPOINT_COUPONS = "/api-admin/v1/coupons"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders = HttpHeaders().apply {
        set(HEADER_LDAP, LDAP_ADMIN_VALUE)
        set("Content-Type", "application/json")
    }

    private fun getCoupons(page: Int, size: Int) = testRestTemplate.exchange(
        "$ENDPOINT_COUPONS?page=$page&size=$size",
        HttpMethod.GET,
        HttpEntity<Any>(adminHeaders()),
        object : ParameterizedTypeReference<ApiResponse<Any>>() {},
    )

    @Nested
    @DisplayName("GET /api-admin/v1/coupons - page/size 파라미터 검증")
    inner class GetCouponsValidation {

        @Test
        @DisplayName("page=-1 이면 400을 반환한다 (ApiSpec의 @PositiveOrZero 상속 여부 확인)")
        fun getCoupons_negativePage_returns400() {
            // arrange & act
            val response = getCoupons(page = -1, size = 20)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("size=0 이면 400을 반환한다 (ApiSpec의 @Positive 상속 여부 확인)")
        fun getCoupons_zeroSize_returns400() {
            // arrange & act
            val response = getCoupons(page = 0, size = 0)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("size=101 이면 400을 반환한다 (ApiSpec의 @Max(100) 상속 여부 확인)")
        fun getCoupons_sizeExceedsMax_returns400() {
            // arrange & act
            val response = getCoupons(page = 0, size = 101)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("page=0, size=20 이면 200을 반환한다 (정상 케이스)")
        fun getCoupons_validParams_returns200() {
            // arrange & act
            val response = getCoupons(page = 0, size = 20)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }
    }
}
