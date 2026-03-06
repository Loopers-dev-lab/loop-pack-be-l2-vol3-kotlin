package com.loopers.interfaces.apiadmin

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.order.OrderItemCommand
import com.loopers.domain.order.OrderService
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
class AdminOrderApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val orderService: OrderService,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ORDER_ENDPOINT = "/api-admin/v1/orders"
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

    private fun createBrand(name: String = "나이키"): Brand {
        return brandRepository.save(Brand(name = name, description = "스포츠 브랜드"))
    }

    private fun createProduct(brand: Brand, name: String = "에어맥스", price: Money = Money.of(159000L)): Product {
        return productRepository.save(
            Product(name = name, description = "러닝화", price = price, likes = LikeCount.of(0), stockQuantity = StockQuantity.of(100), brandId = brand.id),
        )
    }

    private fun createOrder(userId: Long, brand: Brand, product: Product): Long {
        val items = listOf(
            OrderItemCommand(
                productId = product.id,
                quantity = Quantity.of(1),
                productName = product.name,
                productPrice = product.price,
                brandName = brand.name,
            ),
        )
        return orderService.createOrder(userId, items).id
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractPageContent(data: Map<String, Any>?): List<Map<String, Any>>? {
        return data?.get("content") as? List<Map<String, Any>>
    }

    @DisplayName("GET /api-admin/v1/orders")
    @Nested
    inner class GetOrders {

        @DisplayName("유효한 요청으로 조회하면, 200 OK와 주문 목록을 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            createOrder(1L, brand, product)
            createOrder(2L, brand, product)
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$ORDER_ENDPOINT?page=0&size=20",
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

        @DisplayName("응답에 orderId, userId, totalAmount, status, orderedAt이 포함된다.")
        @Test
        fun returnsOrderFields() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            createOrder(1L, brand, product)
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$ORDER_ENDPOINT?page=0&size=20",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val content = extractPageContent(response.body?.data)
            val firstItem = content?.first()
            assertAll(
                { assertThat(firstItem?.get("orderId")).isNotNull() },
                { assertThat(firstItem?.get("userId")).isNotNull() },
                { assertThat(firstItem?.get("totalAmount")).isEqualTo(159000) },
                { assertThat(firstItem?.get("status")).isEqualTo("ORDERED") },
                { assertThat(firstItem?.get("orderedAt")).isNotNull() },
            )
        }

        @DisplayName("페이지 크기를 지정하면, 해당 크기만큼 반환한다.")
        @Test
        fun returnsPaginatedOrders() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            repeat(3) { i -> createOrder((i + 1).toLong(), brand, product) }
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$ORDER_ENDPOINT?page=0&size=2",
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

        @DisplayName("페이지 파라미터 없이 요청하면, 기본값(page=0, size=20)이 적용된다.")
        @Test
        fun returnsDefaultPage_whenNoPageParams() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            createOrder(1L, brand, product)
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                ORDER_ENDPOINT,
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
                "$ORDER_ENDPOINT?page=0&size=20",
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
                "$ORDER_ENDPOINT?page=0&size=20",
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

    @DisplayName("GET /api-admin/v1/orders/{orderId}")
    @Nested
    inner class GetOrder {

        @DisplayName("유효한 요청으로 조회하면, 200 OK와 주문 상세를 반환한다.")
        @Test
        fun returnsOk_whenValidRequest() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            val orderId = createOrder(1L, brand, product)
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$ORDER_ENDPOINT/$orderId",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
                { assertThat(data?.get("orderId")).isEqualTo(orderId.toInt()) },
                { assertThat(data?.get("userId")).isEqualTo(1) },
                { assertThat(data?.get("totalAmount")).isEqualTo(159000) },
                { assertThat(data?.get("status")).isEqualTo("ORDERED") },
                { assertThat(data?.get("orderedAt")).isNotNull() },
            )
        }

        @Suppress("UNCHECKED_CAST")
        @DisplayName("응답에 주문 항목 목록이 포함된다.")
        @Test
        fun returnsOrderItems() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            val orderId = createOrder(1L, brand, product)
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$ORDER_ENDPOINT/$orderId",
                HttpMethod.GET,
                httpEntity,
                responseType,
            )

            // assert
            val data = response.body?.data
            val items = data?.get("items") as? List<Map<String, Any>>
            val firstItem = items?.first()
            assertAll(
                { assertThat(items).hasSize(1) },
                { assertThat(firstItem?.get("productName")).isEqualTo("에어맥스") },
                { assertThat(firstItem?.get("brandName")).isEqualTo("나이키") },
                { assertThat(firstItem?.get("productPrice")).isEqualTo(159000) },
                { assertThat(firstItem?.get("quantity")).isEqualTo(1) },
            )
        }

        @DisplayName("존재하지 않는 주문이면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenOrderNotExists() {
            // arrange
            val httpEntity = HttpEntity<Void>(adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ORDER_ENDPOINT/999",
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
                "$ORDER_ENDPOINT/1",
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

    @DisplayName("PATCH /api-admin/v1/orders/{orderId}/status")
    @Nested
    inner class ChangeOrderStatus {

        @DisplayName("유효한 상태 전이 요청이면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenValidStatusTransition() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            val orderId = createOrder(1L, brand, product)
            val requestBody = mapOf("status" to "CONFIRMED")
            val httpEntity = HttpEntity(requestBody, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$ORDER_ENDPOINT/$orderId/status",
                HttpMethod.PATCH,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }

        @DisplayName("존재하지 않는 주문이면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenOrderNotExists() {
            // arrange
            val requestBody = mapOf("status" to "CONFIRMED")
            val httpEntity = HttpEntity(requestBody, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ORDER_ENDPOINT/999/status",
                HttpMethod.PATCH,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(404) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("LDAP 헤더가 없으면, 401 UNAUTHORIZED를 반환한다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            // arrange
            val headers = HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
            val requestBody = mapOf("status" to "CONFIRMED")
            val httpEntity = HttpEntity(requestBody, headers)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ORDER_ENDPOINT/1/status",
                HttpMethod.PATCH,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(401) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("상태 변경 후 조회하면, 변경된 상태가 반환된다.")
        @Test
        fun returnsUpdatedStatus_whenGetOrderAfterStatusChange() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            val orderId = createOrder(1L, brand, product)
            val requestBody = mapOf("status" to "CONFIRMED")
            val httpEntity = HttpEntity(requestBody, adminHeaders())

            // act
            val patchResponseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            testRestTemplate.exchange(
                "$ORDER_ENDPOINT/$orderId/status",
                HttpMethod.PATCH,
                httpEntity,
                patchResponseType,
            )

            val getResponseType = object : ParameterizedTypeReference<ApiResponse<Map<String, Any>>>() {}
            val response = testRestTemplate.exchange(
                "$ORDER_ENDPOINT/$orderId",
                HttpMethod.GET,
                HttpEntity<Void>(adminHeaders()),
                getResponseType,
            )

            // assert
            assertThat(response.body?.data?.get("status")).isEqualTo("CONFIRMED")
        }

        @DisplayName("허용되지 않은 상태 전이이면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenInvalidStatusTransition() {
            // arrange
            val brand = createBrand()
            val product = createProduct(brand)
            val orderId = createOrder(1L, brand, product)
            val requestBody = mapOf("status" to "DELIVERED")
            val httpEntity = HttpEntity(requestBody, adminHeaders())

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
            val response = testRestTemplate.exchange(
                "$ORDER_ENDPOINT/$orderId/status",
                HttpMethod.PATCH,
                httpEntity,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.value()).isEqualTo(400) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }
    }
}
