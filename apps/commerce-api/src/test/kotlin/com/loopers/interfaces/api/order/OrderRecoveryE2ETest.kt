package com.loopers.interfaces.api.order

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.stock.Stock
import com.loopers.domain.user.User
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.stock.StockJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import java.math.BigDecimal
import kotlin.test.Test

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderRecoveryE2ETest @Autowired constructor(
    private val restTemplate: TestRestTemplate,
    private val userJpaRepository: UserJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val stockJpaRepository: StockJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val passwordEncoder: PasswordEncoder,
) {

    companion object {
        private const val ENDPOINT_ORDERS = "/api/v1/orders"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createAuthHeaders(loginId: String, password: String): HttpHeaders {
        return HttpHeaders().apply {
            set("X-Loopers-LoginId", loginId)
            set("X-Loopers-LoginPw", password)
        }
    }

    @Test
    @DisplayName("주문 생성 중 실패하면 재고가 복구된다")
    fun testStockRecoveryOnOrderCreationFailure() {
        // Arrange
        val plainPassword = "password123"

        val user = User.create(
            loginId = LoginId.of("recovery01"),
            password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
            name = Name.of("복구 테스트"),
            birthDate = BirthDate.of("20260101"),
            email = Email.of("recovery@test.com"),
        )
        val savedUser = userJpaRepository.save(user)

        val brand = Brand.create(
            name = "복구 테스트 브랜드",
            description = "복구 테스트 브랜드",
        )
        val savedBrand = brandJpaRepository.save(brand)

        // 상품 1: 재고 충분
        val product1 = Product.create(
            brand = savedBrand,
            name = "복구 테스트 상품1",
            price = BigDecimal("10000"),
            status = ProductStatus.ACTIVE,
        )
        val savedProduct1 = productJpaRepository.save(product1)

        val initialStock1 = 100
        stockJpaRepository.save(
            Stock.create(
                productId = savedProduct1.id,
                quantity = initialStock1,
            ),
        )

        // 상품 2: 재고 부족 (주문량 > 재고)
        val product2 = Product.create(
            brand = savedBrand,
            name = "복구 테스트 상품2",
            price = BigDecimal("20000"),
            status = ProductStatus.ACTIVE,
        )
        val savedProduct2 = productJpaRepository.save(product2)

        val initialStock2 = 5
        stockJpaRepository.save(
            Stock.create(
                productId = savedProduct2.id,
                quantity = initialStock2,
            ),
        )

        // Act: 재고 부족 상품이 포함된 주문 시도
        val request = OrderV1Dto.OrderRequest(
            items = listOf(
                OrderV1Dto.OrderItemRequest(
                    productId = savedProduct1.id,
                    quantity = 10,
                ),
                OrderV1Dto.OrderItemRequest(
                    productId = savedProduct2.id,
                    quantity = 10,
                ),
            ),
        )

        val headers = createAuthHeaders("recovery01", plainPassword)
        val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
        val response = restTemplate.exchange(
            ENDPOINT_ORDERS,
            HttpMethod.POST,
            HttpEntity(request, headers),
            responseType,
        )

        // Assert: 주문 생성 실패 (4xx 또는 5xx 응답)
        assertThat(response.statusCode.value()).isGreaterThanOrEqualTo(400)

        // 재고 1은 복구되어야 함 (감소했다가 롤백)
        val stock1After = stockJpaRepository.findByProductId(savedProduct1.id)
        assertThat(stock1After?.quantity).isEqualTo(initialStock1)

        // 재고 2는 변화 없음 (감소 시도도 실패)
        val stock2After = stockJpaRepository.findByProductId(savedProduct2.id)
        assertThat(stock2After?.quantity).isEqualTo(initialStock2)
    }

    @Test
    @DisplayName("주문 생성 성공 시 재고는 복구되지 않는다")
    fun testNoStockRecoveryOnOrderCreationSuccess() {
        // Arrange
        val plainPassword = "password123"

        val user = User.create(
            loginId = LoginId.of("success01"),
            password = Password.ofEncrypted(passwordEncoder.encode(plainPassword)),
            name = Name.of("성공 테스트"),
            birthDate = BirthDate.of("20260101"),
            email = Email.of("success@test.com"),
        )
        userJpaRepository.save(user)

        val brand = Brand.create(
            name = "성공 테스트 브랜드",
            description = "성공 테스트 브랜드",
        )
        val savedBrand = brandJpaRepository.save(brand)

        val product = Product.create(
            brand = savedBrand,
            name = "성공 테스트 상품",
            price = BigDecimal("10000"),
            status = ProductStatus.ACTIVE,
        )
        val savedProduct = productJpaRepository.save(product)

        val initialStock = 100
        stockJpaRepository.save(
            Stock.create(
                productId = savedProduct.id,
                quantity = initialStock,
            ),
        )

        // Act: 정상 주문
        val request = OrderV1Dto.OrderRequest(
            items = listOf(
                OrderV1Dto.OrderItemRequest(
                    productId = savedProduct.id,
                    quantity = 10,
                ),
            ),
        )

        val headers = createAuthHeaders("success01", plainPassword)
        val responseType = object : ParameterizedTypeReference<ApiResponse<Long>>() {}
        val response = restTemplate.exchange(
            ENDPOINT_ORDERS,
            HttpMethod.POST,
            HttpEntity(request, headers),
            responseType,
        )

        // Assert: 주문 성공
        assertThat(response.statusCode).isEqualTo(HttpStatus.CREATED)

        // 재고는 감소한 상태 (복구 안 됨)
        val stockAfter = stockJpaRepository.findByProductId(savedProduct.id)
        assertThat(stockAfter?.quantity).isEqualTo(initialStock - 10)
    }
}
