package com.loopers.interfaces.apiadmin

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.PageQuery
import com.loopers.domain.common.SortOrder
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.product.ProductService
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
class AdminBrandApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandRepository: BrandRepository,
    private val productService: ProductService,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val BRAND_ENDPOINT = "/api-admin/v1/brands"
        private const val BRAND_DETAIL_ENDPOINT = "/api-admin/v1/brands/{brandId}"
        private const val PRODUCT_DETAIL_ENDPOINT = "/api-admin/v1/products/{productId}"
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

    private fun deleteBrand(name: String) {
        val brand = brandRepository.findAll(PageQuery(0, 100, SortOrder.UNSORTED))
            .content.first { it.name == name }
        brand.delete()
        brandRepository.save(brand)
    }

    private fun deleteBrandApi(brandId: Long) {
        testRestTemplate.exchange(
            BRAND_DETAIL_ENDPOINT,
            HttpMethod.DELETE,
            HttpEntity<Void>(adminHeaders()),
            object : ParameterizedTypeReference<ApiResponse<Any>>() {},
            brandId,
        )
    }

    private fun createBrand(name: String, description: String? = null) {
        val request = mutableMapOf<String, Any>("name" to name)
        description?.let { request["description"] = it }
        val httpEntity = HttpEntity(request, adminHeaders())
        testRestTemplate.exchange(
            BRAND_ENDPOINT,
            HttpMethod.POST,
            httpEntity,
            object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {},
        )
    }

    @DisplayName("GET /api-admin/v1/brands")
    @Nested
    inner class GetBrands {

        @DisplayName("유효한 LDAP 헤더로 조회하면, 200 OK와 브랜드 목록을 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            createBrand("나이키", "스포츠 브랜드")
            createBrand("아디다스", "스포츠 브랜드")
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$BRAND_ENDPOINT?page=0&size=20",
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

        @DisplayName("페이지 크기를 지정하면, 해당 크기만큼 반환한다.")
        @Test
        fun returnsPagedBrands_whenPageSizeSpecified() {
            // arrange
            createBrand("나이키", "스포츠 브랜드")
            createBrand("아디다스", "스포츠 브랜드")
            createBrand("무인양품")
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$BRAND_ENDPOINT?page=0&size=2",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(content).hasSize(2) },
                { assertThat(data?.get("totalElements")).isEqualTo(3) },
                { assertThat(data?.get("totalPages")).isEqualTo(2) },
            )
        }

        @DisplayName("브랜드가 없으면, 빈 페이지를 반환한다.")
        @Test
        fun returnsEmptyPage_whenNoBrandsExist() {
            // arrange
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$BRAND_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            val content = extractPageContent(data)
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(content).isEmpty() },
                { assertThat(data?.get("totalElements")).isEqualTo(0) },
            )
        }

        @DisplayName("페이지 파라미터 없이 요청하면, 기본값(page=0, size=20)이 적용된다.")
        @Test
        fun returnsDefaultPage_whenNoPageParams() {
            // arrange
            createBrand("나이키", "스포츠 브랜드")
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                BRAND_ENDPOINT,
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
                { assertThat(data?.get("page")).isEqualTo(0) },
            )
        }

        @DisplayName("삭제된 브랜드는 목록에 포함되지 않는다.")
        @Test
        fun excludesDeletedBrands() {
            // arrange
            createBrand("나이키", "스포츠 브랜드")
            createBrand("삭제될 브랜드", "설명")
            // 삭제될 브랜드를 soft delete
            deleteBrand("삭제될 브랜드")
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$BRAND_ENDPOINT?page=0&size=20",
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
                { assertThat(data?.get("totalElements")).isEqualTo(1) },
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
                "$BRAND_ENDPOINT?page=0&size=20",
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
                "$BRAND_ENDPOINT?page=0&size=20",
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

    @DisplayName("POST /api-admin/v1/brands")
    @Nested
    inner class CreateBrand {

        @DisplayName("유효한 LDAP 헤더와 요청으로 브랜드를 생성하면, 200 OK와 브랜드 정보를 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            val request = mapOf("name" to "나이키", "description" to "스포츠 브랜드")
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                BRAND_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.get("name")).isEqualTo("나이키") },
                { assertThat(response.body?.data?.get("description")).isEqualTo("스포츠 브랜드") },
            )
        }

        @DisplayName("설명 없이 요청하면, 정상적으로 생성된다.")
        @Test
        fun returnsOk_whenDescriptionIsNull() {
            // arrange
            val request = mapOf("name" to "무인양품")
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                BRAND_ENDPOINT,
                HttpMethod.POST,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.get("name")).isEqualTo("무인양품") },
                { assertThat(response.body?.data?.get("description")).isNull() },
            )
        }

        @DisplayName("이름이 비어있으면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNameIsBlank() {
            // arrange
            val request = mapOf("name" to "  ", "description" to "설명")
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_ENDPOINT,
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

        @DisplayName("name 필드가 누락되면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNameIsMissing() {
            // arrange
            val request = mapOf("description" to "설명만 있음")
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_ENDPOINT,
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

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            // arrange
            val request = mapOf("name" to "나이키", "description" to "스포츠 브랜드")
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_ENDPOINT,
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
            val request = mapOf("name" to "나이키", "description" to "스포츠 브랜드")
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_ENDPOINT,
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

    @DisplayName("GET /api-admin/v1/brands/{brandId}")
    @Nested
    inner class GetBrandDetail {

        @DisplayName("유효한 LDAP 헤더로 존재하는 브랜드를 조회하면, 200 OK와 브랜드 정보를 반환한다.")
        @Test
        fun returnsOk_whenBrandExists() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
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
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.get("name")).isEqualTo("나이키") },
                { assertThat(response.body?.data?.get("description")).isEqualTo("스포츠 브랜드") },
                { assertThat(response.body?.data?.get("createdAt")).isNotNull() },
                { assertThat(response.body?.data?.get("updatedAt")).isNotNull() },
            )
        }

        @DisplayName("설명이 없는 브랜드를 조회하면, description이 null로 반환된다.")
        @Test
        fun returnsNullDescription_whenBrandHasNoDescription() {
            // arrange
            val brand = brandRepository.save(Brand(name = "무인양품", description = null))
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
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
                { assertThat(response.body?.data?.get("name")).isEqualTo("무인양품") },
                { assertThat(response.body?.data?.get("description")).isNull() },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandNotExists() {
            // arrange
            val httpEntity = HttpEntity<Void>(adminHeaders())

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

        @DisplayName("삭제된 브랜드 ID로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandIsDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = "삭제될 브랜드", description = "설명"))
            brand.delete()
            brandRepository.save(brand)
            val httpEntity = HttpEntity<Void>(adminHeaders())

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

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
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
            val headers = HttpHeaders().apply {
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity<Void>(headers)

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
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }

    @DisplayName("PUT /api-admin/v1/brands/{brandId}")
    @Nested
    inner class UpdateBrand {

        @DisplayName("유효한 LDAP 헤더와 요청으로 브랜드를 수정하면, 200 OK와 수정된 브랜드 정보를 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val request = mapOf("name" to "아디다스", "description" to "독일 스포츠 브랜드")
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                brand.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(response.body?.data?.get("name")).isEqualTo("아디다스") },
                { assertThat(response.body?.data?.get("description")).isEqualTo("독일 스포츠 브랜드") },
                { assertThat(response.body?.data?.get("createdAt")).isNotNull() },
                { assertThat(response.body?.data?.get("updatedAt")).isNotNull() },
            )
        }

        @DisplayName("설명을 null로 수정하면, description이 null로 반환된다.")
        @Test
        fun returnsNullDescription_whenDescriptionIsNull() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val request = mapOf("name" to "나이키")
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                brand.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.get("name")).isEqualTo("나이키") },
                { assertThat(response.body?.data?.get("description")).isNull() },
            )
        }

        @DisplayName("이름이 비어있으면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNameIsBlank() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val request = mapOf("name" to "  ", "description" to "설명")
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                brand.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandNotExists() {
            // arrange
            val request = mapOf("name" to "아디다스", "description" to "설명")
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                999L,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("삭제된 브랜드를 수정하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandIsDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            brand.delete()
            brandRepository.save(brand)
            val request = mapOf("name" to "아디다스", "description" to "수정 시도")
            val httpEntity = HttpEntity(request, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                brand.id,
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
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val request = mapOf("name" to "아디다스")
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                brand.id,
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
            val request = mapOf("name" to "아디다스")
            val headers = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity(request, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.PUT,
                httpEntity,
                responseType,
                brand.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }

    @DisplayName("DELETE /api-admin/v1/brands/{brandId}")
    @Nested
    inner class DeleteBrand {

        @DisplayName("유효한 LDAP 헤더로 존재하는 브랜드를 삭제하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                responseType,
                brand.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }

        @DisplayName("브랜드 삭제 후 조회하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenGetDeletedBrand() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            deleteBrandApi(brand.id)

            // act
            val httpEntity = HttpEntity<Void>(adminHeaders())
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

        @DisplayName("브랜드 삭제 시 소속 상품도 연쇄 삭제된다.")
        @Test
        fun cascadeDeletesProducts_whenBrandIsDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val product = productService.createProduct(
                name = "에어맥스",
                description = "운동화",
                price = Money.of(100000L),
                stockQuantity = StockQuantity.of(10),
                brandId = brand.id,
            )

            // act
            deleteBrandApi(brand.id)

            // assert — 상품 상세 조회 시 404
            val httpEntity = HttpEntity<Void>(adminHeaders())
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                PRODUCT_DETAIL_ENDPOINT,
                HttpMethod.GET,
                httpEntity,
                responseType,
                product.id,
            )
            assertThat(response.statusCode.value()).isEqualTo(404)
        }

        @DisplayName("브랜드 삭제 후 목록 조회 시 해당 브랜드가 포함되지 않는다.")
        @Test
        fun excludesDeletedBrandFromList() {
            // arrange
            val brand1 = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            brandRepository.save(Brand(name = "아디다스", description = "스포츠 브랜드"))
            deleteBrandApi(brand1.id)

            // act
            val httpEntity = HttpEntity<Void>(adminHeaders())
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$BRAND_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val content = extractPageContent(response.body?.data)
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(content).hasSize(1) },
                { assertThat(content?.first()?.get("name")).isEqualTo("아디다스") },
            )
        }

        @DisplayName("이미 삭제된 브랜드를 다시 삭제하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandAlreadyDeleted() {
            // arrange
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            deleteBrandApi(brand.id)

            // act
            val httpEntity = HttpEntity<Void>(adminHeaders())
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                responseType,
                brand.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("존재하지 않는 brandId로 삭제하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandNotExists() {
            // arrange
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                responseType,
                999L,
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
            val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
            val httpEntity = HttpEntity<Void>(HttpHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                responseType,
                brand.id,
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
            val headers = HttpHeaders().apply {
                set(LDAP_HEADER, "wrong.value")
            }
            val httpEntity = HttpEntity<Void>(headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                BRAND_DETAIL_ENDPOINT,
                HttpMethod.DELETE,
                httpEntity,
                responseType,
                brand.id,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }
}
