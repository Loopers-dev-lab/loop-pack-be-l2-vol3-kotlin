package com.loopers.interfaces.api.brand

import com.loopers.interfaces.support.ApiResponse
import com.loopers.interfaces.support.HEADER_LDAP
import com.loopers.interfaces.support.LDAP_ADMIN_VALUE
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrandAdminV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_BRANDS = "/api-admin/v1/brands"
    }

    private fun adminHeaders(): HttpHeaders = HttpHeaders().apply {
        set(HEADER_LDAP, LDAP_ADMIN_VALUE)
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("GET /api-admin/v1/brands/{brandId} - brandId 검증")
    inner class BrandIdValidation {

        @Test
        @DisplayName("brandId가 음수이면, 400 Bad Request 응답을 반환한다.")
        fun returnsBadRequest_whenBrandIdIsNegative() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT_BRANDS/-1",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is4xxClientError).isTrue() },
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
            )
        }

        @Test
        @DisplayName("brandId가 0이면, 400 Bad Request 응답을 반환한다.")
        fun returnsBadRequest_whenBrandIdIsZero() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT_BRANDS/0",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is4xxClientError).isTrue() },
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
            )
        }
    }
}
