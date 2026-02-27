package com.loopers.interfaces.apiadmin

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.interfaces.common.ApiResponse
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
import org.springframework.http.MediaType

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminProductApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val PRODUCT_ENDPOINT = "/api-admin/v1/products"
        private const val LDAP_HEADER = "X-Loopers-Ldap"
        private const val LDAP_VALUE = "loopers.admin"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun adminHeaders(): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set(LDAP_HEADER, LDAP_VALUE)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractPageContent(data: Map<String, Any>?): List<Map<String, Any>>? {
        return data?.get("content") as? List<Map<String, Any>>
    }

    private fun createProduct(brand: Brand, name: String, price: Long = 159000L, stockQuantity: Int = 100): Product {
        return productRepository.save(
            Product(
                name = name,
                description = "$name 설명",
                price = Money.of(price),
                likes = LikeCount.of(10),
                stockQuantity = StockQuantity.of(stockQuantity),
                brandId = brand.id,
            ),
        )
    }

    @DisplayName("GET /api-admin/v1/products")
    @Nested
    inner class GetProducts {

        @DisplayName("유효한 요청으로 조회하면, 200 OK와 상품 목록을 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            createProduct(brand, "에어맥스")
            createProduct(brand, "에어포스")
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(content).hasSize(2) },
                { assertThat(data?.get("totalElements")).isEqualTo(2) },
            )
        }

        @DisplayName("응답에 brandName, createdAt, updatedAt이 포함된다.")
        @Test
        fun returnsAdminFields() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            createProduct(brand, "에어맥스")
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val content = extractPageContent(response.body?.data)
            val firstItem = content?.first()
            assertAll(
                { assertThat(firstItem?.get("brandName")).isEqualTo("나이키") },
                { assertThat(firstItem?.get("createdAt")).isNotNull() },
                { assertThat(firstItem?.get("updatedAt")).isNotNull() },
                { assertThat(firstItem?.get("likeCount")).isNotNull() },
                { assertThat(firstItem?.get("stockQuantity")).isNotNull() },
            )
        }

        @DisplayName("brandId를 지정하면, 해당 브랜드의 상품만 반환한다.")
        @Test
        fun returnsFilteredProducts_whenBrandIdProvided() {
            // arrange
            val nike = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val adidas = brandRepository.save(Brand(name = "아디다스", description = "스포츠 브랜드"))
            createProduct(nike, "에어맥스")
            createProduct(adidas, "울트라부스트")
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT?brandId=${nike.id}&page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(content).hasSize(1) },
                { assertThat(content?.first()?.get("brandName")).isEqualTo("나이키") },
                { assertThat(data?.get("totalElements")).isEqualTo(1) },
            )
        }

        @DisplayName("페이지 크기를 지정하면, 해당 크기만큼 반환한다.")
        @Test
        fun returnsPaginatedProducts() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            createProduct(brand, "에어맥스")
            createProduct(brand, "에어포스")
            createProduct(brand, "덩크")
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT?page=0&size=2",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(content).hasSize(2) },
                { assertThat(data?.get("totalElements")).isEqualTo(3) },
                { assertThat(data?.get("totalPages")).isEqualTo(2) },
            )
        }

        @DisplayName("삭제된 상품은 목록에서 제외된다.")
        @Test
        fun excludesDeletedProducts() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            createProduct(brand, "에어맥스")
            val deleted = createProduct(brand, "단종상품")
            deleted.delete()
            productRepository.save(deleted)
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(content).hasSize(1) },
                { assertThat(data?.get("totalElements")).isEqualTo(1) },
            )
        }

        @DisplayName("페이지 파라미터 없이 요청하면, 기본값(page=0, size=20)이 적용된다.")
        @Test
        fun returnsDefaultPage_whenNoPageParams() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            createProduct(brand, "에어맥스")
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(content).hasSize(1) },
                { assertThat(data?.get("size")).isEqualTo(20) },
            )
        }

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            // arrange
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val httpEntity = HttpEntity<Void>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더 값이 올바르지 않으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsInvalid() {
            // arrange
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity<Void>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }

    @DisplayName("GET /api-admin/v1/products/{productId}")
    @Nested
    inner class GetProductDetail {

        @DisplayName("유효한 요청으로 존재하는 상품을 조회하면, 200 OK와 상품 상세 정보를 반환한다.")
        @Test
        fun returnsOk_whenProductExists() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = createProduct(brand, "에어맥스")
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(data?.get("id")).isEqualTo(product.id.toInt()) },
                { assertThat(data?.get("name")).isEqualTo("에어맥스") },
                { assertThat(data?.get("description")).isEqualTo("에어맥스 설명") },
                { assertThat(data?.get("price")).isEqualTo(159000) },
                { assertThat(data?.get("brandId")).isEqualTo(brand.id.toInt()) },
                { assertThat(data?.get("brandName")).isEqualTo("나이키") },
                { assertThat(data?.get("stockQuantity")).isEqualTo(100) },
                { assertThat(data?.get("likeCount")).isEqualTo(10) },
                { assertThat(data?.get("createdAt")).isNotNull() },
                { assertThat(data?.get("updatedAt")).isNotNull() },
            )
        }

        @DisplayName("존재하지 않는 productId로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenProductNotExists() {
            // arrange
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/9999",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("삭제된 상품을 조회하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenProductIsDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = createProduct(brand, "단종상품")
            product.delete()
            productRepository.save(product)
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            // arrange
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val httpEntity = HttpEntity<Void>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/1",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더 값이 올바르지 않으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsInvalid() {
            // arrange
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity<Void>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/1",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }

    @DisplayName("PUT /api-admin/v1/products/{productId}")
    @Nested
    inner class UpdateProduct {

        @DisplayName("유효한 요청으로 상품을 수정하면, 200 OK와 수정된 상품 정보를 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = createProduct(brand, "에어맥스")
            val request = mapOf(
                "name" to "수정된 상품",
                "description" to "수정된 설명",
                "price" to 200000,
                "stockQuantity" to 50,
                "brandId" to brand.id,
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.PUT,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(data?.get("name")).isEqualTo("수정된 상품") },
                { assertThat(data?.get("description")).isEqualTo("수정된 설명") },
                { assertThat(data?.get("price")).isEqualTo(200000) },
                { assertThat(data?.get("stockQuantity")).isEqualTo(50) },
                { assertThat(data?.get("brandName")).isEqualTo("나이키") },
                { assertThat(data?.get("createdAt")).isNotNull() },
                { assertThat(data?.get("updatedAt")).isNotNull() },
            )
        }

        @DisplayName("설명을 null로 수정하면, 정상적으로 수정된다.")
        @Test
        fun returnsOk_whenDescriptionIsNull() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = createProduct(brand, "에어맥스")
            val request = mapOf(
                "name" to "수정된 상품",
                "price" to 200000,
                "stockQuantity" to 50,
                "brandId" to brand.id,
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.PUT,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.get("description")).isNull() },
            )
        }

        @DisplayName("존재하지 않는 productId로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenProductNotExists() {
            // arrange
            val request = mapOf(
                "name" to "수정",
                "price" to 100000,
                "stockQuantity" to 10,
                "brandId" to 1,
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/9999",
                HttpMethod.PUT,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("삭제된 상품을 수정하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenProductIsDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = createProduct(brand, "단종상품")
            product.delete()
            productRepository.save(product)
            val request = mapOf(
                "name" to "수정",
                "price" to 100000,
                "stockQuantity" to 10,
                "brandId" to brand.id,
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.PUT,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("상품명이 비어있으면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNameIsBlank() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = createProduct(brand, "에어맥스")
            val request = mapOf(
                "name" to "  ",
                "price" to 159000,
                "stockQuantity" to 100,
                "brandId" to brand.id,
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.PUT,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("가격이 0이면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenPriceIsZero() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = createProduct(brand, "에어맥스")
            val request = mapOf(
                "name" to "에어맥스",
                "price" to 0,
                "stockQuantity" to 100,
                "brandId" to brand.id,
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.PUT,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("재고가 음수이면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenStockQuantityIsNegative() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = createProduct(brand, "에어맥스")
            val request = mapOf(
                "name" to "에어맥스",
                "price" to 159000,
                "stockQuantity" to -1,
                "brandId" to brand.id,
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.PUT,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("brandId를 변경하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenBrandIdChanged() {
            // arrange
            val nike = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val adidas = brandRepository.save(Brand(name = "아디다스", description = "스포츠 브랜드"))
            val product = createProduct(nike, "에어맥스")
            val request = mapOf(
                "name" to "에어맥스",
                "price" to 159000,
                "stockQuantity" to 100,
                "brandId" to adidas.id,
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.PUT,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = createProduct(brand, "에어맥스")
            val request = mapOf(
                "name" to "수정",
                "price" to 100000,
                "stockQuantity" to 10,
                "brandId" to brand.id,
            )
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.PUT,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더 값이 올바르지 않으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsInvalid() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = createProduct(brand, "에어맥스")
            val request = mapOf(
                "name" to "수정",
                "price" to 100000,
                "stockQuantity" to 10,
                "brandId" to brand.id,
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.PUT,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }

    @DisplayName("DELETE /api-admin/v1/products/{productId}")
    @Nested
    inner class DeleteProduct {

        @DisplayName("유효한 LDAP 헤더로 존재하는 상품을 삭제하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = createProduct(brand, "에어맥스")
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.DELETE,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }

        @DisplayName("상품 삭제 후 조회하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenGetDeletedProduct() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = createProduct(brand, "에어맥스")
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act - 삭제
            testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.DELETE,
                httpEntity,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // act - 조회
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.GET,
                httpEntity,
                object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("존재하지 않는 productId로 삭제하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenProductNotExists() {
            // arrange
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/9999",
                HttpMethod.DELETE,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("이미 삭제된 상품을 삭제하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenProductAlreadyDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = createProduct(brand, "단종상품")
            product.delete()
            productRepository.save(product)
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/${product.id}",
                HttpMethod.DELETE,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            // arrange
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val httpEntity = HttpEntity<Void>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/1",
                HttpMethod.DELETE,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더 값이 올바르지 않으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsInvalid() {
            // arrange
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity<Void>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$PRODUCT_ENDPOINT/1",
                HttpMethod.DELETE,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }

    @DisplayName("POST /api-admin/v1/products")
    @Nested
    inner class CreateProduct {

        @DisplayName("유효한 요청으로 상품을 생성하면, 200 OK와 상품 정보를 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val request = mapOf(
                "name" to "에어맥스",
                "description" to "러닝화",
                "price" to 159000,
                "stockQuantity" to 100,
                "brandId" to brand.id,
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.get("name")).isEqualTo("에어맥스") },
                { assertThat(response.body?.data?.get("description")).isEqualTo("러닝화") },
                { assertThat(response.body?.data?.get("price")).isEqualTo(159000) },
                { assertThat(response.body?.data?.get("stockQuantity")).isEqualTo(100) },
                { assertThat(response.body?.data?.get("brandId")).isEqualTo(brand.id.toInt()) },
            )
        }

        @DisplayName("설명 없이 요청하면, 정상적으로 생성된다.")
        @Test
        fun returnsOk_whenDescriptionIsNull() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val request = mapOf(
                "name" to "에어맥스",
                "price" to 159000,
                "stockQuantity" to 100,
                "brandId" to brand.id,
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.get("name")).isEqualTo("에어맥스") },
                { assertThat(response.body?.data?.get("description")).isNull() },
            )
        }

        @DisplayName("상품명이 비어있으면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNameIsBlank() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val request = mapOf(
                "name" to "  ",
                "price" to 159000,
                "stockQuantity" to 100,
                "brandId" to brand.id,
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("가격이 0이면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenPriceIsZero() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val request = mapOf(
                "name" to "에어맥스",
                "price" to 0,
                "stockQuantity" to 100,
                "brandId" to brand.id,
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("재고가 음수이면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenStockQuantityIsNegative() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val request = mapOf(
                "name" to "에어맥스",
                "price" to 159000,
                "stockQuantity" to -1,
                "brandId" to brand.id,
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("존재하지 않는 브랜드ID로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandNotExists() {
            // arrange
            val request = mapOf(
                "name" to "에어맥스",
                "price" to 159000,
                "stockQuantity" to 100,
                "brandId" to 9999,
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("삭제된 브랜드ID로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandIsDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = "삭제될 브랜드", description = "설명"))
            brand.delete()
            brandRepository.save(brand)

            val request = mapOf(
                "name" to "에어맥스",
                "price" to 159000,
                "stockQuantity" to 100,
                "brandId" to brand.id,
            )
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            // arrange
            val request = mapOf(
                "name" to "에어맥스",
                "price" to 159000,
                "stockQuantity" to 100,
                "brandId" to 1,
            )
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더 값이 올바르지 않으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsInvalid() {
            // arrange
            val request = mapOf(
                "name" to "에어맥스",
                "price" to 159000,
                "stockQuantity" to 100,
                "brandId" to 1,
            )
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                PRODUCT_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }
}
