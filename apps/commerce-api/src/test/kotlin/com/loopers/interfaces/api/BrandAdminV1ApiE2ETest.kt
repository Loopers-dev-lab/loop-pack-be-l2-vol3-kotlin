package com.loopers.interfaces.api

import com.loopers.application.catalog.AdminRegisterBrandUseCase
import com.loopers.application.catalog.RegisterBrandCriteria
import com.loopers.application.catalog.RegisterBrandResult
import com.loopers.interfaces.api.catalog.BrandV1AdminDto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import kotlin.test.Test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrandAdminV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val adminRegisterBrandUseCase: AdminRegisterBrandUseCase,
    private val databaseCleanUp: DatabaseCleanUp,
){
    companion object {
        private const val LDAP_HEADER = "loopers.admin"
        private const val ENDPOINT_BASE = "/api-admin/v1/brands"

        private const val DEFAULT_BRAND_NAME = "brand"
        private const val DEFAULT_BRAND_DESCRIPTION = "description"
        private const val DEFAULT_BRAND_LOGO_URL = "https://logo.example"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerBrand(
        name: String = DEFAULT_BRAND_NAME,
        description: String = DEFAULT_BRAND_DESCRIPTION,
        logoUrl: String = DEFAULT_BRAND_LOGO_URL
    ): RegisterBrandResult {
        val criteria = RegisterBrandCriteria(
            name = name,
            description = description,
            logoUrl = logoUrl,
        )
        return adminRegisterBrandUseCase.execute(criteria)
    }

    private fun createAuthAdminHeader(): HttpHeaders {
        return org.springframework.http.HttpHeaders().apply {
            set("X-Loopers-Ldap", LDAP_HEADER)
        }
    }

    @DisplayName("POST /api-admin/v1/brands")
    @Nested
    inner class Register {
        @DisplayName("유효한 정보가 주어지면, 201 CREATED를 반환한다.")
        @Test
        fun returnsCreatedWhenValidInfoIsProvided() {
            // arrange
            val request = BrandV1AdminDto.RegisterRequest(
                name = DEFAULT_BRAND_NAME,
                description = DEFAULT_BRAND_DESCRIPTION,
                logoUrl = DEFAULT_BRAND_LOGO_URL,
            )
            val headers = createAuthAdminHeader()

            // act
            val response = testRestTemplate.exchange(ENDPOINT_BASE, HttpMethod.POST, HttpEntity(request, headers), Void::class.java)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body).isNull() },
            )
        }

        @DisplayName("이미 등록된 브랜드이면 409 CONFLICT 응답을 받는다.")
        @Test
        fun returnConflictWhenDuplicateNameIsProvided() {
            // arrange
            registerBrand()

            val request = BrandV1AdminDto.RegisterRequest(
                name = DEFAULT_BRAND_NAME,
                description = "",
                logoUrl = ""
            )

            // act
            val headers = createAuthAdminHeader()
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT_BASE, HttpMethod.POST, HttpEntity(request, headers), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }
    }

    @DisplayName("GET /api-admin/v1/brands/{brandId}")
    @Nested
    inner class GetBrand {
        @DisplayName("유효한 정보가 주어지면, 200 OK와 브랜드 상세 정보를 반환한다.")
        @Test
        fun returnBrandDetailAndOkWhenValidInfoIsProvided() {
            // arrange
            val brand = registerBrand()

            // act
            val headers = createAuthAdminHeader()
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandDetailResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT_BASE/${brand.id}", HttpMethod.GET, HttpEntity(null, headers), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(brand.id) },
                { assertThat(response.body?.data?.name).isEqualTo(DEFAULT_BRAND_NAME) },
                { assertThat(response.body?.data?.description).isEqualTo(DEFAULT_BRAND_DESCRIPTION) },
                { assertThat(response.body?.data?.logoUrl).isEqualTo(DEFAULT_BRAND_LOGO_URL) },
            )
        }

        @DisplayName("브랜드가 존재하지 않으면 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnNotFoundWhenBrandDoesNotExist() {
            // arrange
            val brandId = 999

            // act
            val headers = createAuthAdminHeader()
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandDetailResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT_BASE/${brandId}", HttpMethod.GET, HttpEntity(null, headers), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

    }

    @DisplayName("PUT /api-admin/v1/brands/{brandId}")
    @Nested
    inner class ModifyBrand {

        @DisplayName("유효한 정보가 주어지면, 204 NO_CONTENT를 반환한다.")
        @Test
        fun returnsNoContentWhenValidInfoIsProvided() {
            // arrange
            val brand = registerBrand()
            val request = BrandV1AdminDto.UpdateRequest(
                newName = "updated-brand",
                newDescription = "updated-description",
                newLogoUrl = "https://logo.example/updated",
            )
            val headers = createAuthAdminHeader()
            val url = "$ENDPOINT_BASE/${brand.id}"

            // act
            val response = testRestTemplate.exchange(url, HttpMethod.PUT, HttpEntity(request, headers), Void::class.java)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @DisplayName("존재하지 않는 브랜드이면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFoundWhenBrandDoesNotExist() {
            // arrange
            val request = BrandV1AdminDto.UpdateRequest(newName = "updated-brand")
            val headers = createAuthAdminHeader()
            val url = "$ENDPOINT_BASE/999"

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(url, HttpMethod.PUT, HttpEntity(request, headers), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("이미 존재하는 이름으로 수정하면, 409 CONFLICT 응답을 받는다.")
        @Test
        fun returnsConflictWhenDuplicateNameIsProvided() {
            // arrange
            registerBrand()
            val anotherBrand = registerBrand(name = "another-brand")
            val request = BrandV1AdminDto.UpdateRequest(newName = DEFAULT_BRAND_NAME)
            val headers = createAuthAdminHeader()
            val url = "$ENDPOINT_BASE/${anotherBrand.id}"

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(url, HttpMethod.PUT, HttpEntity(request, headers), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }
    }

    @DisplayName("GET /api-admin/v1/brands")
    @Nested
    inner class GetBrands {

        @DisplayName("브랜드 목록을 조회하면, 200 OK와 Slice 페이지네이션 응답을 반환한다.")
        @Test
        fun returnsBrandSliceAndOk() {
            // arrange
            registerBrand(name = "brand-1")
            registerBrand(name = "brand-2")
            val headers = createAuthAdminHeader()
            val url = "$ENDPOINT_BASE?page=0&size=10"

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandSliceResponse>>() {}
            val response = testRestTemplate.exchange(url, HttpMethod.GET, HttpEntity(null, headers), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(2) },
                { assertThat(response.body?.data?.hasNext).isFalse() },
            )
        }

        @DisplayName("페이지 크기보다 많은 브랜드가 있으면, hasNext가 true인 응답을 반환한다.")
        @Test
        fun returnsHasNextTrueWhenMoreBrandsExist() {
            // arrange
            registerBrand(name = "brand-1")
            registerBrand(name = "brand-2")
            registerBrand(name = "brand-3")
            val headers = createAuthAdminHeader()
            val url = "$ENDPOINT_BASE?page=0&size=2"

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandSliceResponse>>() {}
            val response = testRestTemplate.exchange(url, HttpMethod.GET, HttpEntity(null, headers), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.content).hasSize(2) },
                { assertThat(response.body?.data?.hasNext).isTrue() },
            )
        }
    }

    @DisplayName("DELETE /api-admin/v1/brands/{brandId}")
    @Nested
    inner class DeleteBrand {

        @DisplayName("존재하는 브랜드를 삭제하면, 204 NO_CONTENT를 반환한다.")
        @Test
        fun returnsNoContentWhenBrandIsDeleted() {
            // arrange
            val brand = registerBrand()
            val headers = createAuthAdminHeader()
            val url = "$ENDPOINT_BASE/${brand.id}"

            // act
            val response = testRestTemplate.exchange(url, HttpMethod.DELETE, HttpEntity(null, headers), Void::class.java)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        }

        @DisplayName("존재하지 않는 브랜드를 삭제하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFoundWhenBrandDoesNotExist() {
            // arrange
            val headers = createAuthAdminHeader()
            val url = "$ENDPOINT_BASE/999"

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(url, HttpMethod.DELETE, HttpEntity(null, headers), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
