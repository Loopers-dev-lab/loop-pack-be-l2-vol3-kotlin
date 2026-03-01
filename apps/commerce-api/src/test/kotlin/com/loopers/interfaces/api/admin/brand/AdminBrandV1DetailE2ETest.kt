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

@DisplayName("GET /api-admin/v1/brands/{brandId} - 브랜드 상세 조회 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminBrandV1DetailE2ETest
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

    private fun adminHeaders(): HttpEntity<Unit> {
        val headers = HttpHeaders().apply { set("X-Loopers-Ldap", VALID_LDAP) }
        return HttpEntity(Unit, headers)
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
    @DisplayName("존재하는 브랜드 조회 시 200 OK를 반환한다")
    fun getDetail_success_returns200() {
        val brandId = registerBrand()
        val response = testRestTemplate.exchange(
            "$ENDPOINT/$brandId",
            HttpMethod.GET,
            adminHeaders(),
            object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Response.Detail>>() {},
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data?.name).isEqualTo("나이키")
    }

    @Test
    @DisplayName("존재하지 않는 브랜드 조회 시 404를 반환한다")
    fun getDetail_notFound_returns404() {
        val response = testRestTemplate.exchange(
            "$ENDPOINT/999",
            HttpMethod.GET,
            adminHeaders(),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }
}
