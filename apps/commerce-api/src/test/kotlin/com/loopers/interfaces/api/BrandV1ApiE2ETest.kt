package com.loopers.interfaces.api

import com.loopers.domain.brand.Brand
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.interfaces.api.brand.BrandV1Dto
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
    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("GET /api/v1/brands/{brandId}")
    @Nested
    inner class GetBrand {
        @DisplayName("존재하는 브랜드 ID를 주면, 브랜드 정보를 반환한다.")
        @Test
        fun returnsBrandInfo_whenBrandExists() {
            // arrange
            val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>>() {}
            val response = testRestTemplate.exchange("/api/v1/brands/${brand.id}", HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isEqualTo(brand.id) },
                { assertThat(response.body?.data?.name).isEqualTo("나이키") },
                { assertThat(response.body?.data?.description).isEqualTo("스포츠 브랜드") },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID를 주면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun throwsNotFound_whenBrandNotExists() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>>() {}
            val response = testRestTemplate.exchange("/api/v1/brands/999", HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
