package com.loopers.interfaces.api

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandService
import com.loopers.domain.brand.RegisterCommand
import com.loopers.interfaces.api.brand.BrandV1AdminDto
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
    private val brandService: BrandService,
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
    ): BrandModel {
        val command = RegisterCommand(
            name = name,
            description = description,
            logoUrl = logoUrl
        )
        return brandService.register(command)
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

    @DisplayName("인증 헤더가 누락되면, 401 UNAUTHORIZED 응답을 받는다.")
    @Test
    fun returnsUnauthorizedWhenAuthHeaderIsMissing() {
        // arrange
        val request = BrandV1AdminDto.RegisterRequest(
            name = DEFAULT_BRAND_NAME,
            description = DEFAULT_BRAND_DESCRIPTION,
            logoUrl = DEFAULT_BRAND_LOGO_URL,
        )

        // act
        val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
        val response = testRestTemplate.exchange(ENDPOINT_BASE, HttpMethod.POST, HttpEntity(request), responseType)

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
