package com.loopers.interfaces.api

import com.loopers.application.catalog.AdminRegisterBrandUseCase
import com.loopers.application.catalog.RegisterBrandCriteria
import com.loopers.application.catalog.RegisterBrandResult
import com.loopers.domain.user.RegisterCommand
import com.loopers.domain.user.UserService
import com.loopers.interfaces.api.catalog.BrandV1Dto
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
import java.time.ZoneId
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrandV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val adminRegisterBrandUseCase: AdminRegisterBrandUseCase,
    private val userService: UserService,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_BASE = "/api/v1/brands"

        private const val DEFAULT_BRAND_NAME = "나이키"
        private const val DEFAULT_BRAND_DESCRIPTION = "스포츠 브랜드"
        private const val DEFAULT_BRAND_LOGO_URL = "https://logo.example/nike"

        private const val DEFAULT_USERNAME = "testuser"
        private const val DEFAULT_PASSWORD = "password1234!"
        private const val DEFAULT_NAME = "테스트유저"
        private const val DEFAULT_EMAIL = "test@loopers.com"
        private val DEFAULT_BIRTH_DATE = ZonedDateTime.of(1995, 5, 29, 0, 0, 0, 0, ZoneId.of("Asia/Seoul"))
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerUser() {
        userService.register(
            RegisterCommand(
                username = DEFAULT_USERNAME,
                password = DEFAULT_PASSWORD,
                name = DEFAULT_NAME,
                email = DEFAULT_EMAIL,
                birthDate = DEFAULT_BIRTH_DATE,
            ),
        )
    }

    private fun registerBrand(
        name: String = DEFAULT_BRAND_NAME,
        description: String = DEFAULT_BRAND_DESCRIPTION,
        logoUrl: String = DEFAULT_BRAND_LOGO_URL,
    ): RegisterBrandResult {
        return adminRegisterBrandUseCase.execute(
            RegisterBrandCriteria(
                name = name,
                description = description,
                logoUrl = logoUrl,
            ),
        )
    }

    private fun createAuthHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", DEFAULT_USERNAME)
            set("X-Loopers-LoginPw", DEFAULT_PASSWORD)
        }
    }

    @DisplayName("GET /api/v1/brands/{brandId}")
    @Nested
    inner class GetBrand {
        @DisplayName("유효한 인증 정보와 브랜드 ID가 주어지면, 200 OK와 브랜드 상세 정보를 반환한다.")
        @Test
        fun returnsBrandDetailAndOkWhenValidInfoIsProvided() {
            // arrange
            registerUser()
            val brand = registerBrand()
            val headers = createAuthHeaders()
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>>() {}

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT_BASE/${brand.id}",
                HttpMethod.GET,
                HttpEntity(null, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(brand.id) },
                { assertThat(response.body?.data?.name).isEqualTo(DEFAULT_BRAND_NAME) },
            )
        }

        @DisplayName("존재하지 않는 브랜드이면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFoundWhenBrandDoesNotExist() {
            // arrange
            registerUser()
            val headers = createAuthHeaders()
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>>() {}

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT_BASE/999",
                HttpMethod.GET,
                HttpEntity(null, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("인증 헤더가 누락되면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorizedWhenAuthHeaderIsMissing() {
            // arrange
            val brand = registerBrand()
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>>() {}

            // act
            val response = testRestTemplate.exchange(
                "$ENDPOINT_BASE/${brand.id}",
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
