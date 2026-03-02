package com.loopers.interfaces.api.product

import com.loopers.application.brand.BrandFacade
import com.loopers.application.product.ProductFacade
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
class ProductV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandFacade: BrandFacade,
    private val productFacade: ProductFacade,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val PUBLIC_PRODUCTS = "/api/v1/products"
        private const val ADMIN_PRODUCTS = "/api-admin/v1/products"
        private const val LDAP_HEADER = "X-Loopers-Ldap"
        private const val LDAP_VALUE = "loopers.admin"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand() = brandFacade.createBrand(
        name = "Nike",
        logoImageUrl = "logo.png",
        description = "테스트 브랜드",
        zipCode = "12345",
        roadAddress = "서울특별시 중구 테스트길 1",
        detailAddress = "1층",
        email = "nike@google.com",
        phoneNumber = "02-3783-4401",
        businessNumber = "123-45-67890",
    )

    private fun createProduct(brandId: Long, name: String = "Air Max") = productFacade.createProduct(
        brandId = brandId,
        name = name,
        imageUrl = "image.png",
        description = "설명",
        price = 50_000L,
        quantity = 100L,
    )

    private fun adminHeaders() = HttpHeaders().apply {
        set(LDAP_HEADER, LDAP_VALUE)
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    inner class GetProducts {

        @Test
        @DisplayName("요청하면 200과 상품 목록을 반환한다")
        fun getProducts_thenReturnsProductList() {
            val brand = createBrand()
            createProduct(brand.id, "Air Max")
            createProduct(brand.id, "Air Force")

            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                PUBLIC_PRODUCTS,
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data).isNotNull },
            )
        }
    }

    @DisplayName("GET /api/v1/products/{id}")
    @Nested
    inner class GetProductById {

        @Test
        @DisplayName("존재하는 상품 ID로 요청하면 200과 상품 상세를 반환한다")
        fun getProductById_whenValidId_thenReturnsProduct() {
            val brand = createBrand()
            val product = createProduct(brand.id)

            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PUBLIC_PRODUCTS/${product.id}",
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(product.id) },
                { assertThat(response.body?.data?.name).isEqualTo("Air Max") },
                { assertThat(response.body?.data?.price).isEqualTo(50_000L) },
                { assertThat(response.body?.data?.brand?.id).isEqualTo(brand.id) },
            )
        }

        @Test
        @DisplayName("존재하지 않는 ID로 요청하면 404를 반환한다")
        fun getProductById_whenInvalidId_thenReturns404() {
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>>() {}
            val response = testRestTemplate.exchange(
                "$PUBLIC_PRODUCTS/99999",
                HttpMethod.GET,
                HttpEntity<Any>(Unit),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("POST /api-admin/v1/products")
    @Nested
    inner class CreateProduct {

        @Test
        @DisplayName("LDAP 헤더로 요청하면 200과 생성된 상품을 반환한다")
        fun createProduct_whenLdapHeader_thenReturnsProduct() {
            val brand = createBrand()

            val request = ProductAdminV1Dto.CreateProductRequest(
                brandId = brand.id,
                name = "Air Max 2024",
                imageUrl = "am2024.png",
                description = "새 에어맥스",
                price = 150_000L,
                quantity = 50L,
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductAdminV1Dto.ProductResponse>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_PRODUCTS,
                HttpMethod.POST,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isNotNull() },
                { assertThat(response.body?.data?.name).isEqualTo("Air Max 2024") },
                { assertThat(response.body?.data?.price).isEqualTo(150_000L) },
            )
        }

        @Test
        @DisplayName("LDAP 헤더 없이 요청하면 401을 반환한다")
        fun createProduct_whenNoLdapHeader_thenReturns401() {
            val brand = createBrand()

            val request = ProductAdminV1Dto.CreateProductRequest(
                brandId = brand.id,
                name = "Air Max 2024",
                imageUrl = "am2024.png",
                description = "새 에어맥스",
                price = 150_000L,
                quantity = 50L,
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductAdminV1Dto.ProductResponse>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_PRODUCTS,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("PUT /api-admin/v1/products/{id}")
    @Nested
    inner class UpdateProduct {

        @Test
        @DisplayName("LDAP 헤더로 요청하면 200과 수정된 상품을 반환한다")
        fun updateProduct_whenLdapHeader_thenReturnsUpdatedProduct() {
            val brand = createBrand()
            val product = createProduct(brand.id)

            val request = ProductAdminV1Dto.UpdateProductRequest(
                name = "Air Max Updated",
                imageUrl = "updated.png",
                description = "업데이트된 설명",
                price = 200_000L,
                quantity = 50L,
            )

            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductAdminV1Dto.ProductResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_PRODUCTS/${product.id}",
                HttpMethod.PUT,
                HttpEntity(request, adminHeaders()),
                responseType,
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.name).isEqualTo("Air Max Updated") },
                { assertThat(response.body?.data?.price).isEqualTo(200_000L) },
            )
        }
    }

    @DisplayName("DELETE /api-admin/v1/products/{id}")
    @Nested
    inner class DeleteProduct {

        @Test
        @DisplayName("LDAP 헤더로 요청하면 200을 반환한다")
        fun deleteProduct_whenLdapHeader_thenReturns200() {
            val brand = createBrand()
            val product = createProduct(brand.id)

            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_PRODUCTS/${product.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(adminHeaders()),
                responseType,
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        }
    }
}
