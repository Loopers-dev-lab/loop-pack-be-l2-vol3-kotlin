package com.loopers.interfaces.admin.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.dto.BrandInfo
import com.loopers.infrastructure.brand.BrandJpaRepository
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
class AdminBrandV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ADMIN_ENDPOINT = "/api-admin/v1/brands"

        private fun createAdminHeaders(): HttpHeaders {
            val headers = HttpHeaders()
            headers["X-LDAP-Username"] = "admin"
            headers["X-LDAP-Role"] = "ADMIN"
            return headers
        }
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("GET /api-admin/v1/brands")
    @Nested
    inner class GetAllBrands {
        @DisplayName("여러 활성 브랜드를 페이징으로 조회할 수 있다.")
        @Test
        fun returnsAllActiveBrands() {
            // arrange
            val brand1 = brandJpaRepository.save(Brand.create(name = "브랜드1", description = "설명1"))
            val brand2 = brandJpaRepository.save(Brand.create(name = "브랜드2", description = "설명2"))
            brandJpaRepository.save(Brand.create(name = "삭제된 브랜드", description = "설명").apply { delete() })

            // act
            val response = testRestTemplate.exchange(
                ADMIN_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Any>(null, createAdminHeaders()),
                String::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body).containsSubsequence("브랜드1", "브랜드2") },
            )
        }

        @DisplayName("브랜드가 없으면 빈 리스트를 반환한다.")
        @Test
        fun returnsEmptyList() {
            // act
            val response = testRestTemplate.exchange(
                ADMIN_ENDPOINT,
                HttpMethod.GET,
                HttpEntity<Any>(null, createAdminHeaders()),
                String::class.java,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body).isNotNull() },
            )
        }
    }

    @DisplayName("POST /api-admin/v1/brands")
    @Nested
    inner class CreateBrand {
        @DisplayName("유효한 요청으로 브랜드를 생성할 수 있다.")
        @Test
        fun createsBrand_withValidRequest() {
            // arrange
            val request = AdminBrandV1Dto.CreateBrandRequest(name = "새 브랜드", description = "새 브랜드 설명")
            val headers = createAdminHeaders()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandInfo>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.brandName).isEqualTo("새 브랜드") },
                { assertThat(response.body?.data?.description).isEqualTo("새 브랜드 설명") },
            )
        }

        @DisplayName("빈 브랜드명으로는 생성할 수 없다.")
        @Test
        fun throwsBadRequest_whenNameIsBlank() {
            // arrange
            val request = AdminBrandV1Dto.CreateBrandRequest(name = "", description = "설명")
            val headers = createAdminHeaders()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandInfo>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("이미 존재하는 브랜드명으로는 생성할 수 없다.")
        @Test
        fun throwsBadRequest_whenNameAlreadyExists() {
            // arrange
            brandJpaRepository.save(Brand.create(name = "기존 브랜드", description = "설명"))
            val request = AdminBrandV1Dto.CreateBrandRequest(name = "기존 브랜드", description = "새로운 설명")
            val headers = createAdminHeaders()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandInfo>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("PUT /api-admin/v1/brands/{brandId}")
    @Nested
    inner class UpdateBrand {
        @DisplayName("유효한 요청으로 브랜드를 수정할 수 있다.")
        @Test
        fun updatesBrand_withValidRequest() {
            // arrange
            val brand = brandJpaRepository.save(Brand.create(name = "기존 브랜드", description = "기존 설명"))
            val request = AdminBrandV1Dto.UpdateBrandRequest(name = "수정된 브랜드", description = "수정된 설명")
            val headers = createAdminHeaders()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT/${brand.id}",
                HttpMethod.PUT,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            val updatedBrand = brandJpaRepository.findById(brand.id).get()
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(updatedBrand.name).isEqualTo("수정된 브랜드") },
                { assertThat(updatedBrand.description).isEqualTo("수정된 설명") },
            )
        }

        @DisplayName("존재하지 않는 브랜드는 수정할 수 없다.")
        @Test
        fun throwsNotFound_whenBrandDoesNotExist() {
            // arrange
            val request = AdminBrandV1Dto.UpdateBrandRequest(name = "수정된 이름", description = "수정된 설명")
            val headers = createAdminHeaders()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT/999999",
                HttpMethod.PUT,
                HttpEntity(request, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("DELETE /api-admin/v1/brands/{brandId}")
    @Nested
    inner class DeleteBrand {
        @DisplayName("존재하는 브랜드를 삭제할 수 있다.")
        @Test
        fun deletesBrand() {
            // arrange
            val brand = brandJpaRepository.save(Brand.create(name = "삭제할 브랜드", description = "설명"))
            val headers = createAdminHeaders()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT/${brand.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(Unit, headers),
                responseType,
            )

            // assert
            val deletedBrand = brandJpaRepository.findById(brand.id).get()
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(deletedBrand.isDeleted()).isTrue() },
            )
        }

        @DisplayName("존재하지 않는 브랜드는 삭제할 수 없다.")
        @Test
        fun throwsNotFound_whenBrandDoesNotExist() {
            // act
            val headers = createAdminHeaders()
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT/999999",
                HttpMethod.DELETE,
                HttpEntity<Any>(Unit, headers),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
