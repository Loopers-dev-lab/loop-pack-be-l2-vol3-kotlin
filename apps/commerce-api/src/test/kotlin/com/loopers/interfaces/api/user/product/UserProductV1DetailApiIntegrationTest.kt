package com.loopers.interfaces.api.user.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.Quantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStock
import com.loopers.domain.product.ProductStockRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
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
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal

@DisplayName("GET /api/v1/products/{productId} - 상품 상세 조회 integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserProductV1DetailApiIntegrationTest
@Autowired
constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val productStockRepository: ProductStockRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
    companion object {
        private const val ADMIN = "loopers.admin"
    }

    private var brandId: Long = 0

    @BeforeEach
    fun setUp() {
        val brand = brandRepository.save(Brand.register(name = "나이키"), ADMIN)
        brandId = brandRepository.save(brand.update("나이키", "ACTIVE"), ADMIN).id!!
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun createProduct(
        name: String,
        active: Boolean,
        stockQuantity: Int = 10,
    ): Product {
        val registered = Product.register(
            name = name,
            regularPrice = Money(BigDecimal.valueOf(12_000)),
            sellingPrice = Money(BigDecimal.valueOf(10_000)),
            brandId = brandId,
            imageUrl = "https://example.com/image.png",
            thumbnailUrl = "https://example.com/thumb.png",
        )
        val saved = productRepository.save(registered, ADMIN)
        val product = if (active) {
            productRepository.save(saved.activate(), ADMIN)
        } else {
            saved
        }

        productStockRepository.save(
            ProductStock.create(productId = product.id!!, initialQuantity = Quantity(stockQuantity)),
            ADMIN,
        )

        return product
    }

    @Nested
    @DisplayName("상품 상세 조회 시")
    inner class WhenGetDetail {
        @Test
        @DisplayName("활성 상품은 상세 정보를 반환한다")
        fun getDetail_activeProduct_returns200() {
            val product = createProduct(name = "러닝화", active = true, stockQuantity = 25)

            val response = testRestTemplate.exchange(
                "/api/v1/products/${product.id}",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<UserProductV1Response.Detail>>() {},
            )

            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.id).isEqualTo(product.id) },
                { assertThat(response.body?.data?.name).isEqualTo("러닝화") },
                { assertThat(response.body?.data?.brandName).isEqualTo("나이키") },
                { assertThat(response.body?.data?.stockQuantity).isEqualTo(25) },
            )
        }

        @Test
        @DisplayName("비활성 상품은 404를 반환한다")
        fun getDetail_inactiveProduct_returns404() {
            val product = createProduct(name = "숨김상품", active = false)

            val response = testRestTemplate.exchange(
                "/api/v1/products/${product.id}",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<ApiResponse<Unit>>() {},
            )

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
}
