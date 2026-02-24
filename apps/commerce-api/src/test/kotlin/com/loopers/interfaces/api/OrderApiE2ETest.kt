package com.loopers.interfaces.api

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.interfaces.api.user.UserDto
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
import org.springframework.http.MediaType
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        private const val ORDER_ENDPOINT = "/api/v1/orders"
        private const val SIGNUP_ENDPOINT = "/api/v1/users/signup"
        private const val LOGIN_ID_HEADER = "X-Loopers-LoginId"
        private const val LOGIN_PW_HEADER = "X-Loopers-LoginPw"
        private val ORDER_RESPONSE_TYPE =
            object : ParameterizedTypeReference<ApiResponse<Any>>() {}
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun signUp(
        loginId: String = "testuser123",
        password: String = "Test1234!@",
        name: String = "홍길동",
        email: String = "test@example.com",
        birthday: LocalDate = LocalDate.of(1990, 1, 15),
    ) {
        val request = UserDto.SignUpRequest(
            loginId = loginId,
            password = password,
            name = name,
            email = email,
            birthday = birthday,
        )
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        testRestTemplate.exchange(
            SIGNUP_ENDPOINT,
            HttpMethod.POST,
            HttpEntity(request, headers),
            object : ParameterizedTypeReference<ApiResponse<UserDto.SignUpResponse>>() {},
        )
    }

    private fun authHeaders(
        loginId: String = "testuser123",
        password: String = "Test1234!@",
    ): HttpHeaders {
        return HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set(LOGIN_ID_HEADER, loginId)
            set(LOGIN_PW_HEADER, password)
        }
    }

    private fun createBrand(name: String = "나이키"): Brand {
        return brandRepository.save(Brand(name = name, description = "스포츠 브랜드"))
    }

    private fun createProduct(
        name: String = "에어맥스",
        description: String? = "러닝화",
        price: Long = 159000,
        stockQuantity: Int = 100,
        brand: Brand? = null,
    ): Product {
        val resolvedBrand = brand ?: createBrand()
        return productRepository.save(
            Product(name = name, description = description, price = price, likes = 0, stockQuantity = stockQuantity, brandId = resolvedBrand.id),
        )
    }

    private data class PlaceOrderRequest(
        val items: List<OrderItemRequest>,
    )

    private data class OrderItemRequest(
        val productId: Long,
        val quantity: Int,
    )

    private fun placeOrder(
        request: PlaceOrderRequest,
        headers: HttpHeaders = authHeaders(),
    ) = testRestTemplate.exchange(
        ORDER_ENDPOINT,
        HttpMethod.POST,
        HttpEntity(request, headers),
        ORDER_RESPONSE_TYPE,
    )

    @DisplayName("POST /api/v1/orders")
    @Nested
    inner class PlaceOrderApi {

        @DisplayName("로그인한 사용자가 주문하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenAuthenticatedUserPlacesOrder() {
            // arrange
            signUp()
            val product = createProduct()
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 2)),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }

        @DisplayName("items가 비어있으면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenItemsEmpty() {
            // arrange
            signUp()
            val request = PlaceOrderRequest(items = emptyList())

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("quantity가 0 이하이면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenQuantityIsZeroOrNegative() {
            // arrange
            signUp()
            val product = createProduct()
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 0)),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("productId가 중복되면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenProductIdDuplicated() {
            // arrange
            signUp()
            val product = createProduct()
            val request = PlaceOrderRequest(
                items = listOf(
                    OrderItemRequest(productId = product.id, quantity = 1),
                    OrderItemRequest(productId = product.id, quantity = 2),
                ),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("존재하지 않는 상품이면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenProductNotExists() {
            // arrange
            signUp()
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = 999999L, quantity = 1)),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("재고가 부족하면, 400 BAD_REQUEST를 반환한다.")
        @Test
        fun returnsBadRequest_whenInsufficientStock() {
            // arrange
            signUp()
            val product = createProduct(stockQuantity = 5)
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 10)),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("인증 헤더가 없으면, 401 Unauthorized를 반환한다.")
        @Test
        fun returnsUnauthorized_whenNotAuthenticated() {
            // arrange
            val product = createProduct()
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
            )
            val unauthHeaders = HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }

            // act
            val response = placeOrder(request, unauthHeaders)

            // assert
            assertThat(response.statusCode.value()).isEqualTo(401)
        }

        @DisplayName("주문 성공 시 재고가 차감된다.")
        @Test
        fun deductsStock_whenOrderIsSuccessful() {
            // arrange
            signUp()
            val product = createProduct(stockQuantity = 100)
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 3)),
            )

            // act
            placeOrder(request)

            // assert
            val updatedProduct = productRepository.findById(product.id)
            assertThat(updatedProduct?.stockQuantity).isEqualTo(97)
        }

        @DisplayName("삭제된 상품에 주문하면, 404 NOT_FOUND를 반환한다.")
        @Test
        fun returnsNotFound_whenProductIsDeleted() {
            // arrange
            signUp()
            val product = createProduct()
            product.delete()
            productRepository.save(product)
            val request = PlaceOrderRequest(
                items = listOf(OrderItemRequest(productId = product.id, quantity = 1)),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.FAIL) },
            )
        }

        @DisplayName("여러 상품을 주문하면, 200 OK를 반환한다.")
        @Test
        fun returnsOk_whenMultipleItemsOrdered() {
            // arrange
            signUp()
            val brand = createBrand()
            val product1 = createProduct(name = "에어맥스", price = 159000, brand = brand)
            val product2 = createProduct(name = "에어포스", price = 139000, brand = brand)
            val request = PlaceOrderRequest(
                items = listOf(
                    OrderItemRequest(productId = product1.id, quantity = 2),
                    OrderItemRequest(productId = product2.id, quantity = 1),
                ),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.meta?.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS) },
            )
        }

        @DisplayName("여러 상품 주문 시 모든 상품의 재고가 차감된다.")
        @Test
        fun deductsAllStock_whenMultipleItemsOrdered() {
            // arrange
            signUp()
            val brand = createBrand()
            val product1 = createProduct(name = "에어맥스", price = 159000, stockQuantity = 50, brand = brand)
            val product2 = createProduct(name = "에어포스", price = 139000, stockQuantity = 30, brand = brand)
            val request = PlaceOrderRequest(
                items = listOf(
                    OrderItemRequest(productId = product1.id, quantity = 5),
                    OrderItemRequest(productId = product2.id, quantity = 3),
                ),
            )

            // act
            placeOrder(request)

            // assert
            val updatedProduct1 = productRepository.findById(product1.id)
            val updatedProduct2 = productRepository.findById(product2.id)
            assertAll(
                { assertThat(updatedProduct1?.stockQuantity).isEqualTo(45) },
                { assertThat(updatedProduct2?.stockQuantity).isEqualTo(27) },
            )
        }

        @DisplayName("재고 부족 시 이미 차감된 재고도 롤백된다.")
        @Test
        fun rollsBackStock_whenAnyItemHasInsufficientStock() {
            // arrange
            signUp()
            val brand = createBrand()
            val product1 = createProduct(name = "에어맥스", price = 159000, stockQuantity = 100, brand = brand)
            val product2 = createProduct(name = "에어포스", price = 139000, stockQuantity = 2, brand = brand)
            val request = PlaceOrderRequest(
                items = listOf(
                    OrderItemRequest(productId = product1.id, quantity = 5),
                    OrderItemRequest(productId = product2.id, quantity = 10),
                ),
            )

            // act
            val response = placeOrder(request)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            val updatedProduct1 = productRepository.findById(product1.id)
            assertThat(updatedProduct1?.stockQuantity).isEqualTo(100)
        }
    }
}
