package com.loopers.interfaces.api

import com.loopers.interfaces.api.admin.brand.AdminBrandRegisterRequest
import com.loopers.interfaces.api.admin.brand.AdminBrandResponse
import com.loopers.interfaces.api.admin.brand.AdminBrandUpdateRequest
import com.loopers.support.constant.ApiPaths
import com.loopers.support.constant.AuthHeaders
import com.loopers.support.error.BrandErrorCode
import com.loopers.support.error.CommonErrorCode
import com.loopers.testcontainers.MySqlTestContainersConfig
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
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestContainersConfig::class)
class AdminBrandV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set(AuthHeaders.Admin.LDAP, AuthHeaders.Admin.LDAP_VALUE)
        }
    }

    private fun registerBrand(name: String): AdminBrandResponse {
        val request = AdminBrandRegisterRequest(name = name)
        val responseType = object : ParameterizedTypeReference<ApiResponse<AdminBrandResponse>>() {}
        val response = testRestTemplate.exchange(
            ApiPaths.AdminBrands.BASE,
            HttpMethod.POST,
            HttpEntity(request, adminHeaders()),
            responseType,
        )
        return requireNotNull(response.body?.data) { "브랜드 등록 응답이 비어 있습니다." }
    }

    @DisplayName("POST /api/admin/v1/brands - 브랜드 등록")
    @Nested
    inner class Register {

        @DisplayName("정상적인 이름으로 등록하면, 201 CREATED와 브랜드 정보를 반환한다.")
        @Test
        fun success() {
            // arrange
            val request = AdminBrandRegisterRequest(name = "나이키")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminBrandResponse>>() {}
            val response = testRestTemplate.exchange(
                ApiPaths.AdminBrands.BASE,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.data?.id).isGreaterThan(0) },
                { assertThat(response.body?.data?.name).isEqualTo("나이키") },
            )
        }

        @DisplayName("어드민 인증 헤더가 없으면, 401 UNAUTHORIZED와 COMMON_007 에러를 반환한다.")
        @Test
        fun failWhenAdminAuthMissing() {
            // arrange
            val request = AdminBrandRegisterRequest(name = "나이키")

            // act
            val response = testRestTemplate.postForEntity(
                ApiPaths.AdminBrands.BASE,
                request,
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(CommonErrorCode.ADMIN_AUTHENTICATION_FAILED.code) },
            )
        }

        @DisplayName("브랜드명이 빈 값이면, 400 BAD_REQUEST와 COMMON_002 에러를 반환한다.")
        @Test
        fun failWhenNameBlank() {
            // arrange
            val request = AdminBrandRegisterRequest(name = "")

            // act
            val response = testRestTemplate.exchange(
                ApiPaths.AdminBrands.BASE,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE.code) },
            )
        }

        @DisplayName("브랜드명이 50자를 초과하면, 400 BAD_REQUEST와 COMMON_002 에러를 반환한다.")
        @Test
        fun failWhenNameTooLong() {
            // arrange
            val request = AdminBrandRegisterRequest(name = "가".repeat(51))

            // act
            val response = testRestTemplate.exchange(
                ApiPaths.AdminBrands.BASE,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE.code) },
            )
        }

        @DisplayName("이미 존재하는 브랜드명으로 등록하면, 409 CONFLICT와 BRAND_002 에러를 반환한다.")
        @Test
        fun failWhenDuplicateName() {
            // arrange
            registerBrand("나이키")

            val request = AdminBrandRegisterRequest(name = "나이키")

            // act
            val response = testRestTemplate.exchange(
                ApiPaths.AdminBrands.BASE,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(BrandErrorCode.DUPLICATE_BRAND_NAME.code) },
            )
        }
    }

    @DisplayName("GET /api/admin/v1/brands - 브랜드 전체 조회")
    @Nested
    inner class GetAllBrands {

        @DisplayName("등록된 브랜드 목록을 반환한다.")
        @Test
        fun success() {
            // arrange
            registerBrand("나이키")
            registerBrand("아디다스")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<List<AdminBrandResponse>>>() {}
            val response = testRestTemplate.exchange(
                ApiPaths.AdminBrands.BASE,
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).hasSize(2) },
            )
        }
    }

    @DisplayName("GET /api/admin/v1/brands/{brandId} - 브랜드 단건 조회")
    @Nested
    inner class GetBrand {

        @DisplayName("존재하는 브랜드를 조회하면, 200 OK와 브랜드 정보를 반환한다.")
        @Test
        fun success() {
            // arrange
            val brand = registerBrand("나이키")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminBrandResponse>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminBrands.BASE}/${brand.id}",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("나이키") },
            )
        }

        @DisplayName("존재하지 않는 브랜드를 조회하면, 404 NOT_FOUND와 BRAND_001 에러를 반환한다.")
        @Test
        fun failWhenNotFound() {
            // act
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminBrands.BASE}/999",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(BrandErrorCode.BRAND_NOT_FOUND.code) },
            )
        }
    }

    @DisplayName("PUT /api/admin/v1/brands/{brandId} - 브랜드 수정")
    @Nested
    inner class Update {

        @DisplayName("새 이름으로 수정하면, 200 OK와 수정된 브랜드 정보를 반환한다.")
        @Test
        fun success() {
            // arrange
            val brand = registerBrand("나이키")
            val request = AdminBrandUpdateRequest(name = "뉴나이키")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminBrandResponse>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminBrands.BASE}/${brand.id}",
                HttpMethod.PUT,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("뉴나이키") },
            )
        }

        @DisplayName("다른 활성 브랜드와 같은 이름으로 수정하면, 409 CONFLICT와 BRAND_002 에러를 반환한다.")
        @Test
        fun failWhenDuplicateName() {
            // arrange
            registerBrand("나이키")
            val adidas = registerBrand("아디다스")
            val request = AdminBrandUpdateRequest(name = "나이키")

            // act
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminBrands.BASE}/${adidas.id}",
                HttpMethod.PUT,
                HttpEntity(request, adminHeaders()),
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(BrandErrorCode.DUPLICATE_BRAND_NAME.code) },
            )
        }
    }

    @DisplayName("DELETE /api/admin/v1/brands/{brandId} - 브랜드 삭제")
    @Nested
    inner class Delete {

        @DisplayName("삭제하면 204 NO_CONTENT를 반환하고, 이후 조회 시 404를 반환한다.")
        @Test
        fun successAndVerifyNotFound() {
            // arrange
            val brand = registerBrand("나이키")

            // act - 삭제
            val deleteResponse = testRestTemplate.exchange(
                "${ApiPaths.AdminBrands.BASE}/${brand.id}",
                HttpMethod.DELETE,
                HttpEntity<Void>(adminHeaders()),
                ApiResponse::class.java,
            )

            // assert - 204
            assertThat(deleteResponse.statusCode).isEqualTo(HttpStatus.NO_CONTENT)

            // verify - 삭제 후 조회 시 404
            val getResponse = testRestTemplate.exchange(
                "${ApiPaths.AdminBrands.BASE}/${brand.id}",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                ApiResponse::class.java,
            )
            assertThat(getResponse.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("존재하지 않는 브랜드를 삭제하면, 404 NOT_FOUND와 BRAND_001 에러를 반환한다.")
        @Test
        fun failWhenNotFound() {
            // act
            val response = testRestTemplate.exchange(
                "${ApiPaths.AdminBrands.BASE}/999",
                HttpMethod.DELETE,
                HttpEntity<Void>(adminHeaders()),
                ApiResponse::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.errorCode).isEqualTo(BrandErrorCode.BRAND_NOT_FOUND.code) },
            )
        }
    }
}
