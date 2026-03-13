package com.loopers.interfaces.api.user.brand

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@DisplayName("GET /api/v1/brands/{brandId} - 브랜드 상세 조회 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserBrandV1DetailE2ETest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ADMIN = "loopers.admin"
    }

    private var activeBrandId: Long = 0
    private var inactiveBrandId: Long = 0
    private var deletedBrandId: Long = 0

    @BeforeEach
    fun setUp() {
        val activeBrand = brandRepository.save(Brand.register(name = "나이키"), ADMIN)
            .update(name = "나이키", status = "ACTIVE")
        activeBrandId = brandRepository.save(activeBrand, ADMIN).id!!

        inactiveBrandId = brandRepository.save(Brand.register(name = "비활성브랜드"), ADMIN).id!!

        val deletedBrand = brandRepository.save(Brand.register(name = "삭제된브랜드"), ADMIN)
        deletedBrandId = deletedBrand.id!!
        brandRepository.delete(deletedBrandId, ADMIN)
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("브랜드 상세 조회 성공 시")
    inner class WhenGetDetailSuccess {
        @Test
        @DisplayName("활성 브랜드는 200 OK와 상세 정보를 반환한다")
        fun getDetail_activeBrand_returns200() {
            val response = testRestTemplate.exchange(
                "/api/v1/brands/$activeBrandId",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<UserBrandV1Response.Detail>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.data?.id).isEqualTo(activeBrandId)
            assertThat(response.body?.data?.name).isEqualTo("나이키")
        }
    }

    @Nested
    @DisplayName("브랜드 상세 조회 실패 시")
    inner class WhenGetDetailFail {
        @Test
        @DisplayName("존재하지 않는 브랜드는 404를 반환한다")
        fun getDetail_notFound_returns404() {
            val response = testRestTemplate.exchange(
                "/api/v1/brands/999999",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(response.body?.meta?.errorCode).isEqualTo("BRAND_NOT_FOUND")
        }

        @Test
        @DisplayName("비활성 브랜드는 404를 반환한다")
        fun getDetail_inactive_returns404() {
            val response = testRestTemplate.exchange(
                "/api/v1/brands/$inactiveBrandId",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(response.body?.meta?.errorCode).isEqualTo("BRAND_NOT_FOUND")
        }

        @Test
        @DisplayName("soft delete된 브랜드는 404와 BRAND_NOT_FOUND를 반환한다")
        fun getDetail_softDeleted_returns404() {
            val response = testRestTemplate.exchange(
                "/api/v1/brands/$deletedBrandId",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(response.body?.meta?.errorCode).isEqualTo("BRAND_NOT_FOUND")
        }
    }
}
