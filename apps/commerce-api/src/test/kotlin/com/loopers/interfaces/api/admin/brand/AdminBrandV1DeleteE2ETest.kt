package com.loopers.interfaces.api.admin.brand

import com.loopers.interfaces.api.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

@DisplayName("DELETE /api-admin/v1/brands/{brandId} - 브랜드 삭제 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminBrandV1DeleteE2ETest
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

    private fun registerBrand(name: String = "나이키"): Long {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Loopers-Ldap", VALID_LDAP)
        }
        val response = testRestTemplate.exchange(
            ENDPOINT,
            HttpMethod.POST,
            HttpEntity("""{"name": "$name"}""", headers),
            object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Response.Register>>() {},
        )
        return response.body!!.data!!.id
    }

    @Test
    @DisplayName("브랜드 삭제 성공 시 200 OK를 반환한다")
    fun delete_success_returns200() {
        val brandId = registerBrand()
        val headers = HttpHeaders().apply { set("X-Loopers-Ldap", VALID_LDAP) }
        val response = testRestTemplate.exchange(
            "$ENDPOINT/$brandId",
            HttpMethod.DELETE,
            HttpEntity(Unit, headers),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    @DisplayName("삭제 후 조회하면 404를 반환한다")
    fun delete_thenGetDetail_returns404() {
        val brandId = registerBrand()
        val headers = HttpHeaders().apply { set("X-Loopers-Ldap", VALID_LDAP) }

        // 삭제
        testRestTemplate.exchange(
            "$ENDPOINT/$brandId",
            HttpMethod.DELETE,
            HttpEntity(Unit, headers),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )

        // 조회
        val response = testRestTemplate.exchange(
            "$ENDPOINT/$brandId",
            HttpMethod.GET,
            HttpEntity(Unit, headers),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
}
