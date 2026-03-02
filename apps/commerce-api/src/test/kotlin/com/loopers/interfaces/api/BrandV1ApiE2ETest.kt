package com.loopers.interfaces.api

import com.loopers.interfaces.api.admin.brand.AdminBrandRegisterRequest
import com.loopers.interfaces.api.admin.brand.AdminBrandResponse
import com.loopers.interfaces.api.brand.BrandResponse
import com.loopers.support.constant.ApiPaths
import com.loopers.support.constant.AuthHeaders
import com.loopers.support.error.BrandErrorCode
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
class BrandV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerBrandViaAdmin(name: String): AdminBrandResponse {
        val headers = HttpHeaders().apply { set(AuthHeaders.Admin.LDAP, AuthHeaders.Admin.LDAP_VALUE) }
        val request = AdminBrandRegisterRequest(name = name)
        val responseType = object : ParameterizedTypeReference<ApiResponse<AdminBrandResponse>>() {}
        val response = testRestTemplate.exchange(
            ApiPaths.AdminBrands.BASE,
            HttpMethod.POST,
            HttpEntity(request, headers),
            responseType,
        )
        return requireNotNull(response.body?.data) { "브랜드 등록 응답이 비어 있습니다." }
    }

    private fun deleteBrandViaAdmin(brandId: Long) {
        val headers = HttpHeaders().apply { set(AuthHeaders.Admin.LDAP, AuthHeaders.Admin.LDAP_VALUE) }
        testRestTemplate.exchange(
            "${ApiPaths.AdminBrands.BASE}/$brandId",
            HttpMethod.DELETE,
            HttpEntity<Void>(headers),
            ApiResponse::class.java,
        )
    }

    @DisplayName("GET /api/v1/brands - 브랜드 전체 조회")
    @Nested
    inner class GetAllBrands {

        @DisplayName("활성 브랜드만 조회된다.")
        @Test
        fun returnsOnlyActiveBrands() {
            // arrange
            registerBrandViaAdmin("나이키")
            val adidas = registerBrandViaAdmin("아디다스")
            deleteBrandViaAdmin(adidas.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<List<BrandResponse>>>() {}
            val response = testRestTemplate.exchange(
                ApiPaths.Brands.BASE,
                HttpMethod.GET,
                null,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).hasSize(1) },
                { assertThat(response.body?.data?.get(0)?.name).isEqualTo("나이키") },
            )
        }
    }

    @DisplayName("GET /api/v1/brands/{brandId} - 브랜드 단건 조회")
    @Nested
    inner class GetBrand {

        @DisplayName("존재하는 활성 브랜드를 조회하면, 200 OK와 브랜드 정보를 반환한다.")
        @Test
        fun success() {
            // arrange
            val brand = registerBrandViaAdmin("나이키")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandResponse>>() {}
            val response = testRestTemplate.exchange(
                "${ApiPaths.Brands.BASE}/${brand.id}",
                HttpMethod.GET,
                null,
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
            val response = testRestTemplate.getForEntity(
                "${ApiPaths.Brands.BASE}/999",
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
