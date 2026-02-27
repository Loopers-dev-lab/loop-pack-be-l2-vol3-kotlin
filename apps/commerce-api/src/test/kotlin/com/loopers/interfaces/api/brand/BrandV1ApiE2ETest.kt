package com.loopers.interfaces.api.brand

import com.loopers.application.brand.BrandFacade
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
    private val brandFacade: BrandFacade,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ADMIN_BRANDS = "/api-admin/v1/brands"
        private const val PUBLIC_BRANDS = "/api/v1/brands"
        private const val LDAP_HEADER = "X-Loopers-Ldap"
        private const val LDAP_VALUE = "loopers.admin"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(
        name: String = "Nike",
        businessNumber: String = "123-45-67890",
    ) = brandFacade.createBrand(
        name = name,
        logoImageUrl = "logo.png",
        description = "테스트 브랜드",
        zipCode = "12345",
        roadAddress = "서울특별시 중구 테스트길 1",
        detailAddress = "1층",
        email = "nike@google.com",
        phoneNumber = "02-3783-4401",
        businessNumber = businessNumber,
    )

    private fun adminHeaders() = HttpHeaders().apply {
        set(LDAP_HEADER, LDAP_VALUE)
    }

    @DisplayName("GET /api/v1/brands/{id}")
    @Nested
    inner class GetBrandById {

        @Test
        @DisplayName("존재하는 브랜드 ID로 요청하면 200과 브랜드 정보를 반환한다 (businessNumber 미포함)")
        fun getBrandById_whenValidId_thenReturnsBrandInfo() {
            val brand = createBrand()

            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PUBLIC_BRANDS/${brand.id}",
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(brand.id) },
                { assertThat(response.body?.data?.name).isEqualTo("Nike") },
                { assertThat(response.body?.data?.zipCode).isEqualTo("12345") },
            )
        }

        @Test
        @DisplayName("존재하지 않는 ID로 요청하면 404를 반환한다")
        fun getBrandById_whenInvalidId_thenReturns404() {
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PUBLIC_BRANDS/99999",
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api-admin/v1/brands")
    @Nested
    inner class GetBrands {

        @Test
        @DisplayName("LDAP 헤더로 요청하면 200과 브랜드 목록을 반환한다")
        fun getBrands_whenLdapHeader_thenReturnsList() {
            createBrand(name = "Nike", businessNumber = "123-45-00001")
            createBrand(name = "Adidas", businessNumber = "123-45-00002")

            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_BRANDS,
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull },
            )
        }

        @Test
        @DisplayName("LDAP 헤더 없이 요청하면 401을 반환한다")
        fun getBrands_whenNoLdapHeader_thenReturns401() {
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_BRANDS,
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("GET /api-admin/v1/brands/{id}")
    @Nested
    inner class GetAdminBrandById {

        @Test
        @DisplayName("LDAP 헤더로 요청하면 200과 브랜드 상세 (businessNumber 포함)를 반환한다")
        fun getAdminBrandById_whenLdapHeader_thenReturnsBrandWithBusinessNumber() {
            val brand = createBrand()

            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandAdminV1Dto.BrandResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_BRANDS/${brand.id}",
                HttpMethod.GET,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(brand.id) },
                { assertThat(response.body?.data?.businessNumber).isEqualTo("123-45-67890") },
            )
        }
    }

    @DisplayName("POST /api-admin/v1/brands")
    @Nested
    inner class CreateBrand {

        @Test
        @DisplayName("LDAP 헤더로 요청하면 200과 생성된 브랜드를 반환한다")
        fun createBrand_whenLdapHeader_thenReturnsBrand() {
            val request = BrandAdminV1Dto.CreateBrandRequest(
                name = "NewBalance",
                logoImageUrl = "nb.png",
                description = "뉴발란스",
                zipCode = "12345",
                roadAddress = "서울특별시 강남구 테헤란로 1",
                detailAddress = "1층",
                email = "nb@example.com",
                phoneNumber = "02-1111-2222",
                businessNumber = "111-22-33333",
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandAdminV1Dto.BrandResponse>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_BRANDS,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isNotNull() },
                { assertThat(response.body?.data?.name).isEqualTo("NewBalance") },
                { assertThat(response.body?.data?.businessNumber).isEqualTo("111-22-33333") },
            )
        }

        @Test
        @DisplayName("LDAP 헤더 없이 요청하면 401을 반환한다")
        fun createBrand_whenNoLdapHeader_thenReturns401() {
            val request = BrandAdminV1Dto.CreateBrandRequest(
                name = "NewBalance",
                logoImageUrl = "nb.png",
                description = "뉴발란스",
                zipCode = "12345",
                roadAddress = "서울특별시 강남구 테헤란로 1",
                detailAddress = "1층",
                email = "nb@example.com",
                phoneNumber = "02-1111-2222",
                businessNumber = "111-22-33333",
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandAdminV1Dto.BrandResponse>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_BRANDS,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("PUT /api-admin/v1/brands/{id}")
    @Nested
    inner class UpdateBrand {

        @Test
        @DisplayName("LDAP 헤더로 요청하면 200과 수정된 브랜드를 반환한다")
        fun updateBrand_whenLdapHeader_thenReturnsUpdatedBrand() {
            val brand = createBrand()

            val request = BrandAdminV1Dto.UpdateBrandRequest(
                name = "Nike Updated",
                logoImageUrl = "new_logo.png",
                description = "업데이트된 설명",
                zipCode = "54321",
                roadAddress = "서울특별시 강남구 새로운길 1",
                detailAddress = "2층",
                email = "updated@google.com",
                phoneNumber = "02-9876-5432",
                businessNumber = "123-45-67890",
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandAdminV1Dto.BrandResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_BRANDS/${brand.id}",
                HttpMethod.PUT,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("Nike Updated") },
                { assertThat(response.body?.data?.description).isEqualTo("업데이트된 설명") },
            )
        }
    }

    @DisplayName("DELETE /api-admin/v1/brands/{id}")
    @Nested
    inner class DeleteBrand {

        @Test
        @DisplayName("LDAP 헤더로 요청하면 200을 반환한다")
        fun deleteBrand_whenLdapHeader_thenReturns200() {
            val brand = createBrand()

            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_BRANDS/${brand.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }
    }
}
