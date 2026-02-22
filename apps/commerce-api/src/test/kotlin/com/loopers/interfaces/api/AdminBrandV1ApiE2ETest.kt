package com.loopers.interfaces.api

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
class AdminBrandV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api-admin/v1/brands"
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

    private fun createBrandRequest(
        name: String = "루퍼스",
        description: String = "루퍼스 브랜드 설명",
        imageUrl: String = "https://example.com/brand.jpg",
    ) = AdminBrandV1Dto.CreateRequest(name = name, description = description, imageUrl = imageUrl)

    private fun createBrandViaApi(
        name: String = "루퍼스",
        description: String = "루퍼스 브랜드 설명",
        imageUrl: String = "https://example.com/brand.jpg",
    ): AdminBrandV1Dto.BrandResponse {
        val response = testRestTemplate.exchange(
            ENDPOINT,
            HttpMethod.POST,
            HttpEntity(createBrandRequest(name, description, imageUrl), adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Dto.BrandResponse>>() {},
        )
        return response.body!!.data!!
    }

    @DisplayName("POST /api-admin/v1/brands (브랜드 등록)")
    @Nested
    inner class CreateBrand {
        @DisplayName("유효한 정보로 등록하면, 201 CREATED 응답을 받는다.")
        @Test
        fun returns201_whenValidInfoIsProvided() {
            // arrange
            val request = createBrandRequest()

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Dto.BrandResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.name).isEqualTo("루퍼스") },
                { assertThat(response.body?.data?.description).isEqualTo("루퍼스 브랜드 설명") },
                { assertThat(response.body?.data?.imageUrl).isEqualTo("https://example.com/brand.jpg") },
            )
        }

        @DisplayName("인증 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returns401_whenNoAdminHeader() {
            // arrange
            val request = createBrandRequest()

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

        @DisplayName("브랜드명이 빈 값이면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returns400_whenNameIsBlank() {
            // arrange
            val request = createBrandRequest(name = "")

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Void>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("GET /api-admin/v1/brands (브랜드 목록 조회)")
    @Nested
    inner class GetBrands {
        @DisplayName("브랜드가 존재하면, 페이징된 결과를 반환한다.")
        @Test
        fun returnsPaginatedBrands_whenBrandsExist() {
            // arrange
            createBrandViaApi(name = "브랜드1")
            createBrandViaApi(name = "브랜드2")
            createBrandViaApi(name = "브랜드3")

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT?page=0&size=2",
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

    @DisplayName("GET /api-admin/v1/brands/{brandId} (브랜드 상세 조회)")
    @Nested
    inner class GetBrand {
        @DisplayName("존재하는 브랜드를 조회하면, 200 OK 응답을 받는다.")
        @Test
        fun returns200_whenBrandExists() {
            // arrange
            val created = createBrandViaApi()

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT/${created.id}",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Dto.BrandResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("루퍼스") },
            )
        }

        @DisplayName("존재하지 않는 브랜드를 조회하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returns404_whenBrandDoesNotExist() {
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
    }

    @DisplayName("PUT /api-admin/v1/brands/{brandId} (브랜드 수정)")
    @Nested
    inner class UpdateBrand {
        @DisplayName("유효한 정보로 수정하면, 200 OK 응답을 받는다.")
        @Test
        fun returns200_whenValidInfoIsProvided() {
            // arrange
            val created = createBrandViaApi()
            val updateRequest = AdminBrandV1Dto.UpdateRequest(
                name = "새 브랜드",
                description = "새 설명",
                imageUrl = "https://example.com/new.jpg",
            )

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT/${created.id}",
                HttpMethod.PUT,
                HttpEntity(updateRequest, adminHeaders()),
                object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Dto.BrandResponse>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("새 브랜드") },
                { assertThat(response.body?.data?.description).isEqualTo("새 설명") },
            )
        }
    }

    @DisplayName("DELETE /api-admin/v1/brands/{brandId} (브랜드 삭제)")
    @Nested
    inner class DeleteBrand {
        @DisplayName("존재하는 브랜드를 삭제하면, 200 OK 응답을 받는다.")
        @Test
        fun returns200_whenBrandExists() {
            // arrange
            val created = createBrandViaApi()

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
    }
}
