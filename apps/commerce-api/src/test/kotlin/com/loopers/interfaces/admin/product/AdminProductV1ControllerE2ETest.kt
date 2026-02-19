package com.loopers.interfaces.admin.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.product.dto.ProductInfo
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.admin.product.AdminProductV1Dto.CreateProductRequest
import com.loopers.interfaces.admin.product.AdminProductV1Dto.UpdateProductRequest
import org.springframework.data.domain.Page
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
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Admin Product V1 API E2E Test")
class AdminProductV1ControllerE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        private const val ADMIN_ENDPOINT = "/api/v1/admin/products"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /api/v1/admin/products")
    @Nested
    inner class CreateProduct {

        @DisplayName("유효한 요청으로 상품을 생성할 수 있다")
        @Test
        fun createsProduct_withValidRequest() {
            // arrange
            val brand = brandJpaRepository.save(
                Brand.create(name = "Test Brand", description = "Test Description"),
            )
            val request = CreateProductRequest(
                brandId = brand.id,
                name = "New Product",
                price = BigDecimal("15000"),
                stock = 50,
                status = ProductStatus.ACTIVE,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            val productId = response.body?.data
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED) },
                { assertThat(productId).isNotNull.isPositive },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )

            val savedProduct = productJpaRepository.findById(checkNotNull(productId)).orElseThrow()
            assertAll(
                { assertThat(savedProduct.name).isEqualTo("New Product") },
                { assertThat(savedProduct.price).isEqualByComparingTo(BigDecimal("15000")) },
                { assertThat(savedProduct.stock).isEqualTo(50) },
                { assertThat(savedProduct.status).isEqualTo(ProductStatus.ACTIVE) },
            )
        }

        @DisplayName("유효하지 않은 브랜드 ID로는 상품을 생성할 수 없다")
        @Test
        fun failsToCreateProduct_whenBrandNotFound() {
            // arrange
            val request = CreateProductRequest(
                brandId = 9999L,
                name = "New Product",
                price = BigDecimal("15000"),
                stock = 50,
                status = ProductStatus.ACTIVE,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("빈 상품명으로는 상품을 생성할 수 없다")
        @Test
        fun failsToCreateProduct_whenNameIsBlank() {
            // arrange
            val brand = brandJpaRepository.save(
                Brand.create(name = "Test Brand", description = "Test Description"),
            )
            val request = CreateProductRequest(
                brandId = brand.id,
                name = "",
                price = BigDecimal("15000"),
                stock = 50,
                status = ProductStatus.ACTIVE,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("0 이하의 가격으로는 상품을 생성할 수 없다")
        @Test
        fun failsToCreateProduct_whenPriceIsNegative() {
            // arrange
            val brand = brandJpaRepository.save(
                Brand.create(name = "Test Brand", description = "Test Description"),
            )
            val request = CreateProductRequest(
                brandId = brand.id,
                name = "New Product",
                price = BigDecimal("-1"),
                stock = 50,
                status = ProductStatus.ACTIVE,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("음수 재고로는 상품을 생성할 수 없다")
        @Test
        fun failsToCreateProduct_whenStockIsNegative() {
            // arrange
            val brand = brandJpaRepository.save(
                Brand.create(name = "Test Brand", description = "Test Description"),
            )
            val request = CreateProductRequest(
                brandId = brand.id,
                name = "New Product",
                price = BigDecimal("15000"),
                stock = -1,
                status = ProductStatus.ACTIVE,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
            val response = testRestTemplate.exchange(
                ADMIN_ENDPOINT,
                HttpMethod.POST,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("GET /api/v1/admin/products/{productId}")
    @Nested
    inner class GetProductInfo {

        @DisplayName("존재하는 상품의 정보를 조회할 수 있다")
        @Test
        fun retrievesProductInfo_whenProductExists() {
            // arrange
            val brand = brandJpaRepository.save(
                Brand.create(name = "Test Brand", description = "Test Description"),
            )
            val product = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "Test Product",
                    price = BigDecimal("10000"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductInfo>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT/${product.id}",
                HttpMethod.GET,
                HttpEntity<Any>(null),
                responseType,
            )

            // assert
            val productData = response.body?.data
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(productData).isNotNull },
                { assertThat(productData?.name).isEqualTo("Test Product") },
            )
        }

        @DisplayName("존재하지 않는 상품은 404를 반환한다")
        @Test
        fun failsToRetrieveProduct_whenProductNotFound() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT/9999",
                HttpMethod.GET,
                HttpEntity<Any>(null),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }

    @DisplayName("GET /api/v1/admin/products")
    @Nested
    inner class GetProducts {

        @DisplayName("상품 목록을 페이징으로 조회할 수 있다")
        @Test
        fun retrievesProducts_withPagination() {
            // arrange
            val brand = brandJpaRepository.save(
                Brand.create(name = "Test Brand", description = "Test Description"),
            )
            productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "Product 1",
                    price = BigDecimal("10000"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                ),
            )
            productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "Product 2",
                    price = BigDecimal("20000"),
                    stock = 50,
                    status = ProductStatus.ACTIVE,
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Page<ProductInfo>>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                HttpEntity<Any>(null),
                responseType,
            )

            // assert
            val pageData = response.body?.data
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(pageData).isNotNull },
                { assertThat(pageData?.totalElements).isEqualTo(2L) },
            )
        }

        @DisplayName("음수 page 파라미터로는 요청할 수 없다")
        @Test
        fun failsToRetrieveProducts_whenPageIsNegative() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT?page=-1&size=20",
                HttpMethod.GET,
                HttpEntity<Any>(null),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("유효하지 않은 size 파라미터로는 요청할 수 없다")
        @Test
        fun failsToRetrieveProducts_whenSizeIsInvalid() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT?page=0&size=999",
                HttpMethod.GET,
                HttpEntity<Any>(null),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("유효한 size 값(20, 50, 100)으로만 조회할 수 있다")
        @Test
        fun retrievesProducts_withValidSizeValues() {
            // arrange
            val brand = brandJpaRepository.save(
                Brand.create(name = "Test Brand", description = "Test Description"),
            )
            productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "Test Product",
                    price = BigDecimal("10000"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Page<ProductInfo>>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT?page=0&size=50",
                HttpMethod.GET,
                HttpEntity<Any>(null),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }
    }

    @DisplayName("PUT /api/v1/admin/products/{productId}")
    @Nested
    inner class UpdateProduct {

        @DisplayName("상품 정보를 수정할 수 있다")
        @Test
        fun updatesProduct_withValidRequest() {
            // arrange
            val brand = brandJpaRepository.save(
                Brand.create(name = "Test Brand", description = "Test Description"),
            )
            val product = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "Original Product",
                    price = BigDecimal("10000"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                ),
            )

            val request = UpdateProductRequest(
                name = "Updated Product",
                price = BigDecimal("20000"),
                stock = 50,
                status = ProductStatus.INACTIVE,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT/${product.id}",
                HttpMethod.PUT,
                HttpEntity(request),
                responseType,
            )

            // assert
            val updated = productJpaRepository.findById(product.id).get()
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(updated.name).isEqualTo("Updated Product") },
                { assertThat(updated.price).isEqualByComparingTo(BigDecimal("20000")) },
                { assertThat(updated.stock).isEqualTo(50) },
                { assertThat(updated.status).isEqualTo(ProductStatus.INACTIVE) },
            )
        }

        @DisplayName("존재하지 않는 상품은 수정할 수 없다")
        @Test
        fun failsToUpdateProduct_whenProductNotFound() {
            // arrange
            val request = UpdateProductRequest(
                name = "Updated Product",
                price = BigDecimal("20000"),
                stock = 50,
                status = ProductStatus.ACTIVE,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT/9999",
                HttpMethod.PUT,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("빈 상품명으로는 수정할 수 없다")
        @Test
        fun failsToUpdateProduct_whenNameIsBlank() {
            // arrange
            val brand = brandJpaRepository.save(
                Brand.create(name = "Test Brand", description = "Test Description"),
            )
            val product = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "Original Product",
                    price = BigDecimal("10000"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                ),
            )

            val request = UpdateProductRequest(
                name = "",
                price = BigDecimal("20000"),
                stock = 50,
                status = ProductStatus.ACTIVE,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT/${product.id}",
                HttpMethod.PUT,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @DisplayName("0 이하의 가격으로는 수정할 수 없다")
        @Test
        fun failsToUpdateProduct_whenPriceIsNegative() {
            // arrange
            val brand = brandJpaRepository.save(
                Brand.create(name = "Test Brand", description = "Test Description"),
            )
            val product = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "Original Product",
                    price = BigDecimal("10000"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                ),
            )

            val request = UpdateProductRequest(
                name = "Updated Product",
                price = BigDecimal("-1"),
                stock = 50,
                status = ProductStatus.ACTIVE,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT/${product.id}",
                HttpMethod.PUT,
                HttpEntity(request),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("DELETE /api/v1/admin/products/{productId}")
    @Nested
    inner class DeleteProduct {

        @DisplayName("상품을 삭제할 수 있다")
        @Test
        fun deletesProduct_whenProductExists() {
            // arrange
            val brand = brandJpaRepository.save(
                Brand.create(name = "Test Brand", description = "Test Description"),
            )
            val product = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "Product to Delete",
                    price = BigDecimal("10000"),
                    stock = 100,
                    status = ProductStatus.ACTIVE,
                ),
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT/${product.id}",
                HttpMethod.DELETE,
                HttpEntity<Any>(null),
                responseType,
            )

            // assert
            val deleted = productJpaRepository.findById(product.id).get()
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(deleted.isDeleted()).isTrue },
            )
        }

        @DisplayName("존재하지 않는 상품은 삭제할 수 없다")
        @Test
        fun failsToDeleteProduct_whenProductNotFound() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange(
                "$ADMIN_ENDPOINT/9999",
                HttpMethod.DELETE,
                HttpEntity<Any>(null),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }
}
