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

@DisplayName("PUT /api-admin/v1/brands/{brandId} - 브랜드 수정 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminBrandV1UpdateE2ETest
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
    @DisplayName("브랜드 수정 성공 시 200 OK와 변경된 이름, 상태를 반환한다")
    fun update_success_returns200() {
        val brandId = registerBrand()
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Loopers-Ldap", VALID_LDAP)
        }
        val response = testRestTemplate.exchange(
            "$ENDPOINT/$brandId",
            HttpMethod.PUT,
            HttpEntity("""{"name": "아디다스", "status": "ACTIVE"}""", headers),
            object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Response.Update>>() {},
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data?.name).isEqualTo("아디다스")
        assertThat(response.body?.data?.status).isEqualTo("ACTIVE")
    }

    @Test
    @DisplayName("존재하지 않는 브랜드 수정 시 404를 반환한다")
    fun update_notFound_returns404() {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Loopers-Ldap", VALID_LDAP)
        }
        val response = testRestTemplate.exchange(
            "$ENDPOINT/999",
            HttpMethod.PUT,
            HttpEntity("""{"name": "아디다스", "status": "ACTIVE"}""", headers),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    @DisplayName("유효하지 않은 status 값이면 400을 반환한다")
    fun update_invalidStatus_returns400() {
        val brandId = registerBrand()
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Loopers-Ldap", VALID_LDAP)
        }
        val response = testRestTemplate.exchange(
            "$ENDPOINT/$brandId",
            HttpMethod.PUT,
            HttpEntity("""{"name": "아디다스", "status": "INVALID"}""", headers),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }
}
