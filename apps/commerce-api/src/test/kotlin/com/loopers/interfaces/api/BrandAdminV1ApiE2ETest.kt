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
        private const val ENDPOINT_REGISTER = "/api-admin/v1/brands"

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
            val response = testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, HttpEntity(request, headers), Void::class.java)

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
            val response = testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, HttpEntity(request, headers), responseType)

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
        val response = testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, HttpEntity(request), responseType)

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
