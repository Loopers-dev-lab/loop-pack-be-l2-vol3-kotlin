package com.loopers.interfaces.api

import com.loopers.domain.brand.Brand
import com.loopers.interfaces.common.ApiResponse
import com.loopers.domain.brand.BrandRepository
import com.loopers.interfaces.api.brand.BrandDto
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrandApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val BRAND_DETAIL_ENDPOINT = "/api/v1/brands/{brandId}"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("GET /api/v1/brands/{brandId}")
    @Nested
    inner class GetBrandDetail {

        @DisplayName("존재하는 브랜드 ID로 요청하면, 200 OK와 브랜드 정보를 반환한다.")
        @Test
        fun returnsOk_whenBrandExists() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val httpEntity = HttpEntity<Void>(HttpHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandDto.DetailResponse>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                brand.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isEqualTo(brand.id) },
                { assertThat(response.body?.data?.name).isEqualTo("나이키") },
                { assertThat(response.body?.data?.description).isEqualTo("스포츠 브랜드") },
            )
        }

        @DisplayName("설명이 없는 브랜드를 조회하면, description이 null로 반환된다.")
        @Test
        fun returnsNullDescription_whenBrandHasNoDescription() {
            // arrange
            val brand = brandRepository.save(Brand(name = "무인양품", description = null))
            val httpEntity = HttpEntity<Void>(HttpHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandDto.DetailResponse>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                brand.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.name).isEqualTo("무인양품") },
                { assertThat(response.body?.data?.description).isNull() },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID로 요청하면, 404 Not Found와 에러 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandNotExists() {
            // arrange
            val httpEntity = HttpEntity<Void>(HttpHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                999L,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
                { assertThat(response.body?.meta?.message).contains("브랜드를 찾을 수 없습니다") },
            )
        }

        @DisplayName("삭제된 브랜드 ID로 요청하면, 404 Not Found 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandIsDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = "삭제될 브랜드", description = "설명"))
            brand.delete()
            brandRepository.save(brand)
            val httpEntity = HttpEntity<Void>(HttpHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                brand.id,
            )

            // assert
            assertThat(response.statusCode.value()).isEqualTo(404)
        }
    }
}
