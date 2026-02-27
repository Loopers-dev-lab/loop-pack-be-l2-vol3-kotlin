package com.loopers.interfaces.api.brand

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrandV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        private const val TEST_BRAND_NAME = "나이키"
        private const val TEST_BRAND_DESCRIPTION = "스포츠 브랜드"
        private const val ADMIN_LDAP = "loopers.admin"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-Ldap", ADMIN_LDAP)
        }
    }

    private fun createTestBrand(
        name: String = TEST_BRAND_NAME,
        description: String? = TEST_BRAND_DESCRIPTION,
    ): BrandV1Dto.BrandAdminResponse? {
        val request = BrandV1Dto.CreateRequest(name = name, description = description)
        val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandAdminResponse>>() {}
        val response = testRestTemplate.exchange(
            "/api-admin/v1/brands",
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        return response.body?.data
    }

    @DisplayName("GET /api/v1/brands/{brandId}")
    @Nested
    inner class GetBrand {

        @DisplayName("존재하는 브랜드를 조회하면, 200 OK와 브랜드 정보를 반환한다.")
        @Test
        fun returnsOk_whenBrandExists() {
            // arrange
            val created = createTestBrand()!!

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/brands/${created.id}",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(created.id) },
                { assertThat(response.body?.data?.name).isEqualTo(TEST_BRAND_NAME) },
                { assertThat(response.body?.data?.description).isEqualTo(TEST_BRAND_DESCRIPTION) },
            )
        }

        @DisplayName("존재하지 않는 브랜드를 조회하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenBrandNotExists() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/brands/999",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("어드민 API 인증")
    @Nested
    inner class AdminAuth {

        @DisplayName("X-Loopers-Ldap 헤더 없이 요청하면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderMissing() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/brands?page=0&size=20",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("잘못된 LDAP 값으로 요청하면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenLdapInvalid() {
            // arrange
            val headers = HttpHeaders().apply {
                set("X-Loopers-Ldap", "invalid.ldap")
            }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/brands?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Any>(headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("GET /api-admin/v1/brands")
    @Nested
    inner class GetAllBrands {

        @DisplayName("브랜드가 존재하면, 200 OK와 목록을 반환한다.")
        @Test
        fun returnsOk_whenBrandsExist() {
            // arrange
            createTestBrand(name = "나이키")
            createTestBrand(name = "아디다스")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/brands?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull() },
            )
        }
    }

    @DisplayName("POST /api-admin/v1/brands")
    @Nested
    inner class CreateBrand {

        @DisplayName("정상적인 요청이면, 200 OK와 생성된 브랜드를 반환한다.")
        @Test
        fun returnsOk_whenCreateSucceeds() {
            // arrange
            val request = BrandV1Dto.CreateRequest(name = "나이키", description = "스포츠 브랜드")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandAdminResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/brands",
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("나이키") },
                { assertThat(response.body?.data?.description).isEqualTo("스포츠 브랜드") },
                { assertThat(response.body?.data?.id).isNotNull() },
                { assertThat(response.body?.data?.createdAt).isNotNull() },
                { assertThat(response.body?.data?.updatedAt).isNotNull() },
            )
        }

        @DisplayName("이미 존재하는 브랜드명이면, 409 CONFLICT를 반환한다.")
        @Test
        fun returnsConflict_whenBrandNameAlreadyExists() {
            // arrange
            createTestBrand(name = "나이키")
            val request = BrandV1Dto.CreateRequest(name = "나이키", description = "다른 설명")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/brands",
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @DisplayName("브랜드명이 빈 값이면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenNameIsBlank() {
            // arrange
            val request = BrandV1Dto.CreateRequest(name = "  ", description = "설명")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/brands",
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("PUT /api-admin/v1/brands/{brandId}")
    @Nested
    inner class UpdateBrand {

        @DisplayName("정상적인 요청이면, 200 OK와 수정된 브랜드를 반환한다.")
        @Test
        fun returnsOk_whenUpdateSucceeds() {
            // arrange
            val created = createTestBrand()!!
            val request = BrandV1Dto.UpdateRequest(name = "아디다스", description = "독일 스포츠 브랜드")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandAdminResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/brands/${created.id}",
                HttpMethod.PUT,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("아디다스") },
                { assertThat(response.body?.data?.description).isEqualTo("독일 스포츠 브랜드") },
                { assertThat(response.body?.data?.createdAt).isNotNull() },
                { assertThat(response.body?.data?.updatedAt).isNotNull() },
            )
        }

        @DisplayName("다른 브랜드와 같은 이름으로 수정하면, 409 CONFLICT를 반환한다.")
        @Test
        fun returnsConflict_whenNameAlreadyExists() {
            // arrange
            createTestBrand(name = "나이키")
            val target = createTestBrand(name = "아디다스")!!
            val request = BrandV1Dto.UpdateRequest(name = "나이키", description = "설명")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/brands/${target.id}",
                HttpMethod.PUT,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @DisplayName("존재하지 않는 브랜드를 수정하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenBrandNotExists() {
            // arrange
            val request = BrandV1Dto.UpdateRequest(name = "아디다스", description = "설명")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/brands/999",
                HttpMethod.PUT,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("DELETE /api-admin/v1/brands/{brandId}")
    @Nested
    inner class DeleteBrand {

        @DisplayName("존재하는 브랜드를 삭제하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenDeleteSucceeds() {
            // arrange
            val created = createTestBrand()!!

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/brands/${created.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }

        @DisplayName("삭제된 브랜드를 조회하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenQueryDeletedBrand() {
            // arrange
            val created = createTestBrand()!!
            val deleteType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            testRestTemplate.exchange(
                "/api-admin/v1/brands/${created.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                deleteType,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/brands/${created.id}",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("존재하지 않는 브랜드를 삭제하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenBrandNotExists() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "/api-admin/v1/brands/999",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
