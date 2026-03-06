package com.loopers.interfaces.api

import com.loopers.domain.admin.Admin
import com.loopers.domain.brand.Brand
import com.loopers.infrastructure.admin.AdminJpaRepository
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.interfaces.api.admin.brand.AdminBrandV1Dto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
    private val adminJpaRepository: AdminJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private lateinit var adminHeaders: HttpHeaders

    @BeforeEach
    fun setUp() {
        adminJpaRepository.save(Admin(ldap = "loopers.admin", name = "관리자"))
        adminHeaders = HttpHeaders()
        adminHeaders.set("X-Loopers-Ldap", "loopers.admin")
    }

    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("인증되지 않은 요청은 401 UNAUTHORIZED 응답을 받는다.")
    @Test
    fun returnsUnauthorized_whenNoLdapHeader() {
        // act
        val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
        val response = testRestTemplate.exchange("/api-admin/v1/brands", HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @DisplayName("GET /api-admin/v1/brands")
    @Nested
    inner class GetBrands {
        @DisplayName("브랜드 목록을 페이지네이션하여 조회한다.")
        @Test
        fun returnsBrandList() {
            // arrange
            brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            brandJpaRepository.save(Brand(name = "아디다스", description = "독일 스포츠 브랜드"))

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<AdminBrandV1Dto.BrandResponse>>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/brands?page=0&size=20", HttpMethod.GET, HttpEntity<Any>(Unit, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.content).hasSize(2) },
                { assertThat(response.body?.data?.totalElements).isEqualTo(2) },
            )
        }
    }

    @DisplayName("GET /api-admin/v1/brands/{brandId}")
    @Nested
    inner class GetBrand {
        @DisplayName("존재하는 브랜드 ID를 주면, 브랜드 상세 정보를 반환한다.")
        @Test
        fun returnsBrandInfo_whenBrandExists() {
            // arrange
            val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Dto.BrandResponse>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/brands/${brand.id}", HttpMethod.GET, HttpEntity<Any>(Unit, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isEqualTo(brand.id) },
                { assertThat(response.body?.data?.name).isEqualTo("나이키") },
            )
        }
    }

    @DisplayName("POST /api-admin/v1/brands")
    @Nested
    inner class CreateBrand {
        @DisplayName("유효한 정보가 주어지면, 브랜드를 생성한다.")
        @Test
        fun createsBrand_whenValidRequest() {
            // arrange
            val req = AdminBrandV1Dto.CreateBrandRequest(name = "나이키", description = "스포츠 브랜드")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Dto.BrandResponse>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/brands", HttpMethod.POST, HttpEntity(req, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.name).isEqualTo("나이키") },
                { assertThat(response.body?.data?.description).isEqualTo("스포츠 브랜드") },
            )
        }
    }

    @DisplayName("PUT /api-admin/v1/brands/{brandId}")
    @Nested
    inner class UpdateBrand {
        @DisplayName("존재하는 브랜드를 수정하면, 수정된 정보를 반환한다.")
        @Test
        fun updatesBrand_whenBrandExists() {
            // arrange
            val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val req = AdminBrandV1Dto.UpdateBrandRequest(name = "아디다스", description = "독일 스포츠 브랜드")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminBrandV1Dto.BrandResponse>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/brands/${brand.id}", HttpMethod.PUT, HttpEntity(req, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.name).isEqualTo("아디다스") },
                { assertThat(response.body?.data?.description).isEqualTo("독일 스포츠 브랜드") },
            )
        }
    }

    @DisplayName("DELETE /api-admin/v1/brands/{brandId}")
    @Nested
    inner class DeleteBrand {
        @DisplayName("존재하는 브랜드를 삭제하면, 성공 응답을 반환한다.")
        @Test
        fun deletesBrand_whenBrandExists() {
            // arrange
            val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/brands/${brand.id}", HttpMethod.DELETE, HttpEntity<Any>(Unit, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(brandJpaRepository.findById(brand.id).get().deletedAt).isNotNull() },
            )
        }
    }
}
