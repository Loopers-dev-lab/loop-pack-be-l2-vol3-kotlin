package com.loopers.interfaces.api.brand

import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.admin.brand.AdminBrandV1Dto
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
class BrandV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api/v1/brands"
        private const val ADMIN_ENDPOINT = "/api-admin/v1/brands"
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

    private fun createBrandViaAdmin(
        name: String = "루퍼스",
        description: String = "루퍼스 브랜드 설명",
        imageUrl: String = "https://example.com/brand.jpg",
    ): AdminBrandV1Dto.BrandResponse {
        val request = AdminBrandV1Dto.CreateRequest(name = name, description = description, imageUrl = imageUrl)
        val response = testRestTemplate.exchange(
            ADMIN_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Dto.BrandResponse>>() {},
        )
        return response.body!!.data!!
    }

    @DisplayName("GET /api/v1/brands/{brandId} (브랜드 조회)")
    @Nested
    inner class GetBrand {
        @DisplayName("ACTIVE 브랜드를 조회하면, 200 OK 응답을 받는다.")
        @Test
        fun returns200_whenBrandIsActive() {
            // arrange
            val created = createBrandViaAdmin()

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT/${created.id}",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("루퍼스") },
                { assertThat(response.body?.data?.description).isEqualTo("루퍼스 브랜드 설명") },
                { assertThat(response.body?.data?.imageUrl).isEqualTo("https://example.com/brand.jpg") },
            )
        }

        @DisplayName("삭제된 브랜드를 조회하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returns404_whenBrandIsDeleted() {
            // arrange
            val created = createBrandViaAdmin()
            testRestTemplate.exchange(
                "$ADMIN_ENDPOINT/${created.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT/${created.id}",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("존재하지 않는 브랜드를 조회하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returns404_whenBrandDoesNotExist() {
            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT/999",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
