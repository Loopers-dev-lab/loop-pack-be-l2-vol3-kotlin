package com.loopers.interfaces.api.admin.brand

import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageResponse
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
import org.springframework.http.MediaType

@DisplayName("GET /api-admin/v1/brands - 브랜드 목록 조회 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminBrandV1ListE2ETest
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

    private fun registerBrand(name: String) {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-Loopers-Ldap", VALID_LDAP)
        }
        testRestTemplate.exchange(
            ENDPOINT,
            HttpMethod.POST,
            HttpEntity("""{"name": "$name"}""", headers),
            object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Response.Register>>() {},
        )
    }

    private fun getListResponse(
        page: Int? = null,
        size: Int? = null,
    ): org.springframework.http.ResponseEntity<ApiResponse<PageResponse<AdminBrandV1Response.Summary>>> {
        val headers = HttpHeaders().apply { set("X-Loopers-Ldap", VALID_LDAP) }
        val params = mutableListOf<String>()
        if (page != null) params.add("page=$page")
        if (size != null) params.add("size=$size")
        val url = if (params.isEmpty()) ENDPOINT else "$ENDPOINT?${params.joinToString("&")}"

        return testRestTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity(Unit, headers),
            object : ParameterizedTypeReference<ApiResponse<PageResponse<AdminBrandV1Response.Summary>>>() {},
        )
    }

    @Nested
    @DisplayName("기본 페이지네이션 조회 시")
    inner class WhenDefaultPagination {
        @Test
        @DisplayName("200 OK와 함께 PageResponse를 반환한다")
        fun getList_success_returns200WithPageResponse() {
            registerBrand("나이키")
            registerBrand("아디다스")

            val response = getListResponse()

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val pageData = response.body?.data!!
            assertThat(pageData.content).hasSize(2)
            assertThat(pageData.totalElements).isEqualTo(2)
            assertThat(pageData.page).isEqualTo(0)
            assertThat(pageData.size).isEqualTo(20)
        }
    }

    @Nested
    @DisplayName("페이지 크기를 지정하여 조회 시")
    inner class WhenCustomPageSize {
        @Test
        @DisplayName("지정된 size만큼 content를 반환한다")
        fun getList_withSize_returnsLimitedContent() {
            repeat(15) { registerBrand("브랜드$it") }

            val response = getListResponse(page = 0, size = 10)

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val pageData = response.body?.data!!
            assertThat(pageData.content).hasSize(10)
            assertThat(pageData.totalElements).isEqualTo(15)
            assertThat(pageData.totalPages).isEqualTo(2)
        }
    }
}
