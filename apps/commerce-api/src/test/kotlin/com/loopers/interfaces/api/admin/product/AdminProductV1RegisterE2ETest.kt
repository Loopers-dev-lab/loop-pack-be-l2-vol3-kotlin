package com.loopers.interfaces.api.admin.product

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
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

@DisplayName("POST /api-admin/v1/products - 상품 등록 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminProductV1RegisterE2ETest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api-admin/v1/products"
        private const val VALID_LDAP = "loopers.admin"
    }

    private var brandId: Long = 0

    @BeforeEach
    fun setUp() {
        val brand = brandRepository.save(Brand.register(name = "나이키"), VALID_LDAP)
        val activeBrand = brand.update("나이키", "ACTIVE")
        val saved = brandRepository.save(activeBrand, VALID_LDAP)
        brandId = saved.id!!
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerRequest(
        name: String = "테스트 상품",
        regularPrice: String = "10000",
        sellingPrice: String = "8000",
        brandId: Long = this.brandId,
        initialStock: Int = 100,
        ldap: String? = VALID_LDAP,
    ): HttpEntity<String> {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            ldap?.let { set("X-Loopers-Ldap", it) }
        }
        val body = """
            {
                "name": "$name",
                "regularPrice": $regularPrice,
                "sellingPrice": $sellingPrice,
                "brandId": $brandId,
                "initialStock": $initialStock
            }
        """.trimIndent()
        return HttpEntity(body, headers)
    }

    @Nested
    @DisplayName("상품 등록 성공 시")
    inner class WhenSuccess {
        @Test
        @DisplayName("201 Created와 ProductResponse를 반환한다")
        fun register_success_returns201() {
            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                registerRequest(),
                object : ParameterizedTypeReference<ApiResponse<AdminProductV1Response.Register>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(response.body?.meta?.result?.name).isEqualTo("SUCCESS") },
                { assertThat(response.body?.data?.name).isEqualTo("테스트 상품") },
                { assertThat(response.body?.data?.status).isEqualTo("INACTIVE") },
                { assertThat(response.body?.data?.stockQuantity).isEqualTo(100) },
            )
        }
    }

    @Nested
    @DisplayName("유효하지 않은 요청 시")
    inner class WhenInvalidRequest {
        @Test
        @DisplayName("빈 이름이면 400 Bad Request를 반환한다")
        fun register_emptyName_returns400() {
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                registerRequest(name = ""),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("잘못된 LDAP 형식이면 401을 반환한다")
        fun register_invalidLdap_returns401() {
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                registerRequest(ldap = "invalid"),
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}
