package com.loopers.interfaces.api.admin.brand

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
import org.springframework.http.MediaType

@DisplayName("POST /api-admin/v1/brands - 브랜드 등록 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminBrandV1RegisterE2ETest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api-admin/v1/brands"
        private const val VALID_LDAP = "loopers.admin"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerRequest(name: String = "나이키", ldap: String? = VALID_LDAP): HttpEntity<String> {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            ldap?.let { set("X-Loopers-Ldap", it) }
        }
        val body = """{"name": "$name"}"""
        return HttpEntity(body, headers)
    }

    @Nested
    @DisplayName("브랜드 등록 성공 시")
    inner class WhenSuccess {
        @Test
        @DisplayName("201 Created와 BrandResponse를 반환한다")
        fun register_success_returns201() {
            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                registerRequest(),
                object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Response.Register>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.meta?.result?.name).isEqualTo("SUCCESS") },
                { assertThat(response.body?.data?.name).isEqualTo("나이키") },
                { assertThat(response.body?.data?.status).isEqualTo("INACTIVE") },
            )
        }
    }

    @Nested
    @DisplayName("유효하지 않은 요청 시")
    inner class WhenInvalidRequest {
        @Test
        @DisplayName("빈 이름이면 400 Bad Request를 반환한다")
        fun register_emptyName_returns400() {
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                registerRequest(name = ""),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("LDAP 헤더가 없으면 400을 반환한다")
        fun register_noLdapHeader_returns400() {
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                registerRequest(ldap = null),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("잘못된 LDAP 형식이면 401을 반환한다")
        fun register_invalidLdapHeader_returns401() {
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                registerRequest(ldap = "invalid"),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
