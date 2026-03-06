package com.loopers.interfaces.api

import com.loopers.domain.admin.Admin
import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.infrastructure.admin.AdminJpaRepository
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.interfaces.api.admin.product.AdminProductV1Dto
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
class AdminProductV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val adminJpaRepository: AdminJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private lateinit var adminHeaders: HttpHeaders
    private lateinit var brand: Brand

    @BeforeEach
    fun setUp() {
        adminJpaRepository.save(Admin(ldap = "loopers.admin", name = "관리자"))
        adminHeaders = HttpHeaders()
        adminHeaders.set("X-Loopers-Ldap", "loopers.admin")
        brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
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
        val response = testRestTemplate.exchange("/api-admin/v1/products", HttpMethod.GET, HttpEntity<Any>(Unit), responseType)

        // assert
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @DisplayName("GET /api-admin/v1/products")
    @Nested
    inner class GetProducts {
        @DisplayName("상품 목록을 페이지네이션하여 조회한다.")
        @Test
        fun returnsProductList() {
            // arrange
            productJpaRepository.save(Product(brandId = brand.id, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100))
            productJpaRepository.save(Product(brandId = brand.id, name = "에어포스", description = "스니커즈", price = 119000, stockQuantity = 50))

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<AdminProductV1Dto.ProductResponse>>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/products?page=0&size=20", HttpMethod.GET, HttpEntity<Any>(Unit, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.content).hasSize(2) },
            )
        }
    }

    @DisplayName("GET /api-admin/v1/products/{productId}")
    @Nested
    inner class GetProduct {
        @DisplayName("존재하는 상품 ID를 주면, 상품 상세 정보를 반환한다.")
        @Test
        fun returnsProductInfo_whenProductExists() {
            // arrange
            val product = productJpaRepository.save(Product(brandId = brand.id, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100))

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminProductV1Dto.ProductResponse>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/products/${product.id}", HttpMethod.GET, HttpEntity<Any>(Unit, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isEqualTo(product.id) },
                { assertThat(response.body?.data?.name).isEqualTo("에어맥스") },
                { assertThat(response.body?.data?.stockQuantity).isEqualTo(100) },
            )
        }
    }

    @DisplayName("POST /api-admin/v1/products")
    @Nested
    inner class CreateProduct {
        @DisplayName("유효한 정보가 주어지면, 상품을 생성한다.")
        @Test
        fun createsProduct_whenValidRequest() {
            // arrange
            val req = AdminProductV1Dto.CreateProductRequest(
                brandId = brand.id,
                name = "에어맥스",
                description = "운동화",
                price = 139000,
                stockQuantity = 100,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminProductV1Dto.ProductResponse>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/products", HttpMethod.POST, HttpEntity(req, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.name).isEqualTo("에어맥스") },
                { assertThat(response.body?.data?.brandName).isEqualTo("나이키") },
                { assertThat(response.body?.data?.price).isEqualTo(139000L) },
            )
        }
    }

    @DisplayName("PUT /api-admin/v1/products/{productId}")
    @Nested
    inner class UpdateProduct {
        @DisplayName("존재하는 상품을 수정하면, 수정된 정보를 반환한다.")
        @Test
        fun updatesProduct_whenProductExists() {
            // arrange
            val product = productJpaRepository.save(Product(brandId = brand.id, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100))
            val req = AdminProductV1Dto.UpdateProductRequest(name = "에어맥스 90", description = "클래식 운동화", price = 149000, stockQuantity = 50)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<AdminProductV1Dto.ProductResponse>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/products/${product.id}", HttpMethod.PUT, HttpEntity(req, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.name).isEqualTo("에어맥스 90") },
                { assertThat(response.body?.data?.price).isEqualTo(149000L) },
                { assertThat(response.body?.data?.stockQuantity).isEqualTo(50) },
            )
        }
    }

    @DisplayName("DELETE /api-admin/v1/products/{productId}")
    @Nested
    inner class DeleteProduct {
        @DisplayName("존재하는 상품을 삭제하면, 성공 응답을 반환한다.")
        @Test
        fun deletesProduct_whenProductExists() {
            // arrange
            val product = productJpaRepository.save(Product(brandId = brand.id, name = "에어맥스", description = "운동화", price = 139000, stockQuantity = 100))

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange("/api-admin/v1/products/${product.id}", HttpMethod.DELETE, HttpEntity<Any>(Unit, adminHeaders), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(productJpaRepository.findById(product.id).get().deletedAt).isNotNull() },
            )
        }
    }
}
