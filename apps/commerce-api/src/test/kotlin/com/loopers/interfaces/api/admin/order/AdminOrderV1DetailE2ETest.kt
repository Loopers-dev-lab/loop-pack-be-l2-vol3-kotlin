package com.loopers.interfaces.api.admin.order

import com.loopers.application.user.order.OrderCreateCommand
import com.loopers.application.user.order.OrderCreateUseCase
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
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@DisplayName("GET /api-admin/v1/orders/{orderId} - Admin 주문 상세 조회 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminOrderV1DetailE2ETest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userRepository: UserRepository,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val orderCreateUseCase: OrderCreateUseCase,
    private val passwordHasher: UserPasswordHasher,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ADMIN = "loopers.admin"
        private const val LOGIN_ID = "testuser1"
        private const val PASSWORD = "Password1!"
    }

    private var orderId: Long = 0

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
        val savedUser = userRepository.save(user)

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
        val productId = activeProduct.id!!

        productStockRepository.save(
            ProductStock.create(productId = productId, initialQuantity = Quantity(100)),
            ADMIN,
        )

        val result = orderCreateUseCase.create(
            OrderCreateCommand(
                userId = savedUser.id!!,
                idempotencyKey = UUID.randomUUID().toString(),
                items = listOf(OrderCreateCommand.Item(productId = productId, quantity = 2)),
            ),
        )
        orderId = result.orderId
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun endpoint(orderId: Long): String = "/api-admin/v1/orders/$orderId"

    private fun adminHeaders(ldap: String = ADMIN): HttpEntity<Unit> {
        val headers = HttpHeaders().apply {
            set("X-Loopers-Ldap", ldap)
        }
        return HttpEntity(Unit, headers)
    }

    @Nested
    @DisplayName("상세 조회 성공 시")
    inner class WhenGetDetailSuccess {
        @Test
        @DisplayName("200 OK와 주문 상세 정보를 반환한다")
        fun getDetail_success_returns200() {
            // act
            val response = testRestTemplate.exchange(
                endpoint(orderId),
                HttpMethod.GET,
                adminHeaders(),
                object : ParameterizedTypeReference<ApiResponse<Map<String, Any?>>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(response.body?.meta?.result?.name).isEqualTo("SUCCESS")
            val data = response.body?.data!!
            assertThat(data["orderId"]).isNotNull()
            assertThat(data["status"]).isEqualTo("CREATED")
            @Suppress("UNCHECKED_CAST")
            val items = data["items"] as List<Map<String, Any?>>
            assertThat(items).hasSize(1)
        }
    }

    @Nested
    @DisplayName("LDAP 헤더 누락 시")
    inner class WhenUnauthorized {
        @Test
        @DisplayName("400을 반환한다")
        fun getDetail_unauthorized_returns400() {
            // act
            val response = testRestTemplate.exchange(
                endpoint(orderId),
                HttpMethod.GET,
                HttpEntity<Unit>(HttpHeaders()),
                object : ParameterizedTypeReference<ApiResponse<Any?>>() {},
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
