package com.loopers.interfaces.apiadmin

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.interfaces.common.ApiResponse
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
import org.springframework.http.MediaType

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminBrandApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val CREATE_BRAND_ENDPOINT = "/api-admin/v1/brands"
        private const val BRAND_DETAIL_ENDPOINT = "/api-admin/v1/brands/{brandId}"
        private const val LDAP_HEADER = "X-Loopers-Ldap"
        private const val LDAP_VALUE = "loopers.admin"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set(LDAP_HEADER, LDAP_VALUE)
        }
    }

    @DisplayName("POST /api-admin/v1/brands")
    @Nested
    inner class CreateBrand {

        @DisplayName("유효한 LDAP 헤더와 요청으로 브랜드를 생성하면, 200 OK와 브랜드 정보를 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            val request = mapOf("name" to "나이키", "description" to "스포츠 브랜드")
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                CREATE_BRAND_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.get("name")).isEqualTo("나이키") },
                { assertThat(response.body?.data?.get("description")).isEqualTo("스포츠 브랜드") },
            )
        }

        @DisplayName("설명 없이 요청하면, 정상적으로 생성된다.")
        @Test
        fun returnsOk_whenDescriptionIsNull() {
            // arrange
            val request = mapOf("name" to "무인양품")
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                CREATE_BRAND_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.get("name")).isEqualTo("무인양품") },
                { assertThat(response.body?.data?.get("description")).isNull() },
            )
        }

        @DisplayName("이름이 비어있으면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNameIsBlank() {
            // arrange
            val request = mapOf("name" to "  ", "description" to "설명")
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                CREATE_BRAND_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("name 필드가 누락되면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNameIsMissing() {
            // arrange
            val request = mapOf("description" to "설명만 있음")
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                CREATE_BRAND_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            // arrange
            val request = mapOf("name" to "나이키", "description" to "스포츠 브랜드")
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                CREATE_BRAND_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더 값이 올바르지 않으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsInvalid() {
            // arrange
            val request = mapOf("name" to "나이키", "description" to "스포츠 브랜드")
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                CREATE_BRAND_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }

    @DisplayName("GET /api-admin/v1/brands/{brandId}")
    @Nested
    inner class GetBrandDetail {

        @DisplayName("유효한 LDAP 헤더로 존재하는 브랜드를 조회하면, 200 OK와 브랜드 정보를 반환한다.")
        @Test
        fun returnsOk_whenBrandExists() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                brand.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.get("name")).isEqualTo("나이키") },
                { assertThat(response.body?.data?.get("description")).isEqualTo("스포츠 브랜드") },
            )
        }

        @DisplayName("설명이 없는 브랜드를 조회하면, description이 null로 반환된다.")
        @Test
        fun returnsNullDescription_whenBrandHasNoDescription() {
            // arrange
            val brand = brandRepository.save(Brand(name = "무인양품", description = null))
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                brand.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.get("name")).isEqualTo("무인양품") },
                { assertThat(response.body?.data?.get("description")).isNull() },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandNotExists() {
            // arrange
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                999L,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
                { assertThat(response.body?.meta?.message).contains("브랜드를 찾을 수 없습니다") },
            )
        }

        @DisplayName("삭제된 브랜드 ID로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandIsDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = "삭제될 브랜드", description = "설명"))
            brand.delete()
            brandRepository.save(brand)
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                brand.id,
            )

            // assert
            assertThat(response.statusCode.value()).isEqualTo(404)
        }

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val httpEntity = HttpEntity<Void>(HttpHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                brand.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더 값이 올바르지 않으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsInvalid() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val headers = HttpHeaders().apply {
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity<Void>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                brand.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }
}
