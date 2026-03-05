package com.loopers.interfaces.api.user.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.domain.user.User
import com.loopers.domain.user.UserPasswordHasher
import com.loopers.domain.user.UserRepository
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
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@DisplayName("POST /api/v1/orders - 주문 생성 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserOrderV1CreateE2ETest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userRepository: UserRepository,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val passwordHasher: UserPasswordHasher,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ADMIN = "loopers.admin"
        private const val LOGIN_ID = "testuser1"
        private const val PASSWORD = "Password1!"
        private const val ENDPOINT = "/api/v1/orders"
    }

    private var productId: Long = 0

    @BeforeEach
    fun setUp() {
        val user = User.register(
            loginId = LOGIN_ID,
            rawPassword = PASSWORD,
            name = "홍길동",
            birthDate = LocalDate.of(1990, 1, 1),
            email = "test@example.com",
            passwordHasher = passwordHasher,
        )
        userRepository.save(user)

        val brand = brandRepository.save(Brand.register(name = "나이키"), ADMIN)
        val activeBrand = brandRepository.save(brand.update("나이키", "ACTIVE"), ADMIN)

        val product = Product.register(
            name = "테스트 상품",
            regularPrice = Money(BigDecimal.valueOf(10000)),
            sellingPrice = Money(BigDecimal.valueOf(8000)),
            brandId = activeBrand.id!!,
        )
        val saved = productRepository.save(product, ADMIN)
        val activeProduct = productRepository.save(saved.activate(), ADMIN)
        productId = activeProduct.id!!

        productStockRepository.save(
            ProductStock.create(productId = productId, initialQuantity = Quantity(10)),
            ADMIN,
        )
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createRequest(
        loginId: String = LOGIN_ID,
        password: String = PASSWORD,
        idempotencyKey: String = UUID.randomUUID().toString(),
        body: UserOrderV1Request.Create,
    ): HttpEntity<UserOrderV1Request.Create> {
        val headers = HttpHeaders().apply {
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", password)
            set("X-Idempotency-Key", idempotencyKey)
            contentType = MediaType.APPLICATION_JSON
        }
        return HttpEntity(body, headers)
    }

    private fun createBody(productId: Long = this.productId, quantity: Int = 3): UserOrderV1Request.Create =
        UserOrderV1Request.Create(
            items = listOf(UserOrderV1Request.Create.Item(productId = productId, quantity = quantity)),
        )

    @Nested
    @DisplayName("정상 주문 생성 시")
    inner class WhenCreateSuccess {
        @Test
        @DisplayName("201 Created와 주문 정보를 반환한다")
        fun create_success_returns201() {
            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                createRequest(body = createBody()),
                object : ParameterizedTypeReference<ApiResponse<UserOrderV1Response.Created>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)
            assertThat(response.body?.meta?.result?.name).isEqualTo("SUCCESS")
            assertThat(response.body?.data?.orderId).isNotNull()
            assertThat(response.body?.data?.status).isEqualTo("CREATED")
        }
    }

    @Nested
    @DisplayName("재고 부족 시")
    inner class WhenInsufficientStock {
        @Test
        @DisplayName("400을 반환한다")
        fun create_insufficientStock_returns400() {
            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                createRequest(body = createBody(quantity = 100)),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body?.meta?.errorCode).isEqualTo("PRODUCT_STOCK_INSUFFICIENT")
        }
    }

    @Nested
    @DisplayName("인증 실패 시")
    inner class WhenUnauthorized {
        @Test
        @DisplayName("비밀번호 오류 시 401을 반환한다")
        fun create_wrongPassword_returns401() {
            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                createRequest(password = "WrongPassword1!", body = createBody()),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @Nested
    @DisplayName("멱등성 키 중복 시")
    inner class WhenDuplicateIdempotencyKey {
        @Test
        @DisplayName("409 Conflict를 반환한다")
        fun create_duplicateKey_returns409() {
            // arrange
            val idempotencyKey = UUID.randomUUID().toString()
            testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                createRequest(idempotencyKey = idempotencyKey, body = createBody()),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                createRequest(idempotencyKey = idempotencyKey, body = createBody()),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
            assertThat(response.body?.meta?.errorCode).isEqualTo("ORDER_IDEMPOTENCY_KEY_DUPLICATE")
        }
    }

    @Nested
    @DisplayName("존재하지 않는 상품 주문 시")
    inner class WhenProductNotFound {
        @Test
        @DisplayName("404를 반환한다")
        fun create_productNotFound_returns404() {
            // act
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                createRequest(body = createBody(productId = 999999L)),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(response.body?.meta?.errorCode).isEqualTo("PRODUCT_NOT_FOUND")
        }
    }

    @Nested
    @DisplayName("빈 items 주문 시")
    inner class WhenEmptyItems {
        @Test
        @DisplayName("400을 반환한다")
        fun create_emptyItems_returns400() {
            // act
            val body = UserOrderV1Request.Create(items = emptyList())
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                createRequest(body = body),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @Nested
    @DisplayName("멱등성 키 헤더 누락 시")
    inner class WhenMissingIdempotencyKey {
        @Test
        @DisplayName("400을 반환한다")
        fun create_missingIdempotencyKey_returns400() {
            // act
            val headers = HttpHeaders().apply {
                set("X-Loopers-LoginId", LOGIN_ID)
                set("X-Loopers-LoginPw", PASSWORD)
                contentType = MediaType.APPLICATION_JSON
            }
            val entity = HttpEntity(createBody(), headers)
            val response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                entity,
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
