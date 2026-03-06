package com.loopers.interfaces.api.brand

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
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrandV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private val ENDPOINT_GET: (Long) -> String = { id: Long -> "/api/v1/brands/$id" }
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("GET /api/v1/brands/{brandId}")
    @Nested
    inner class GetBrandInfo {
        @DisplayName("존재하는 브랜드 ID를 주면, 해당 브랜드 정보를 반환한다.")
        @Test
        fun returnsBrandInfo_whenValidIdIsProvided() {
            // arrange
            val brand = brandJpaRepository.save(Brand.create(name = "테스트 브랜드", description = "테스트 브랜드 설명"))
            val requestUrl = ENDPOINT_GET(brand.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandInfo>>() {}
            val response = testRestTemplate.exchange(requestUrl, HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.brandId).isEqualTo(brand.id) },
                { assertThat(response.body?.data?.brandName).isEqualTo(brand.name) },
                { assertThat(response.body?.data?.description).isEqualTo(brand.description) },
            )
        }

        @DisplayName("숫자가 아닌 ID로 요청하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun throwsBadRequest_whenIdIsNotNumeric() {
            // arrange
            val requestUrl = "/api/v1/brands/잘못된ID"

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandInfo>>() {}
            val response = testRestTemplate.exchange(requestUrl, HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is4xxClientError).isTrue },
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID를 주면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun throwsNotFound_whenBrandDoesNotExist() {
            // arrange
            val invalidId = 999999L
            val requestUrl = ENDPOINT_GET(invalidId)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandInfo>>() {}
            val response = testRestTemplate.exchange(requestUrl, HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is4xxClientError).isTrue },
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
            )
        }

        @DisplayName("삭제된 브랜드 ID를 주면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun throwsNotFound_whenBrandIsDeleted() {
            // arrange
            val brand = brandJpaRepository.save(Brand.create(name = "삭제될 브랜드", description = "설명"))
            brand.delete()
            brandJpaRepository.save(brand)
            val requestUrl = ENDPOINT_GET(brand.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandInfo>>() {}
            val response = testRestTemplate.exchange(requestUrl, HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is4xxClientError).isTrue },
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
            )
        }
    }
}
