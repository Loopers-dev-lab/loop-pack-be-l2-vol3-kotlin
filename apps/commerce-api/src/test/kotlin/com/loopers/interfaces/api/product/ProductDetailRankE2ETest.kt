package com.loopers.interfaces.api.product

import com.loopers.domain.catalog.product.model.Product
import com.loopers.infrastructure.catalog.brand.BrandEntity
import com.loopers.infrastructure.catalog.brand.BrandJpaRepository
import com.loopers.infrastructure.catalog.product.ProductEntity
import com.loopers.infrastructure.catalog.product.ProductJpaRepository
import com.loopers.config.redis.RedisRankingConstants
import com.loopers.interfaces.support.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
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
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductDetailRankE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {

    companion object {
        private val RESPONSE_TYPE = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
    }

    private val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
    private val rankingKey = "${RedisRankingConstants.RANKING_KEY_PREFIX}${today.format(DateTimeFormatter.BASIC_ISO_DATE)}"

    private var productId = 0L

    @BeforeEach
    fun setUp() {
        val brand = brandJpaRepository.save(BrandEntity(name = "테스트브랜드"))
        val product = productJpaRepository.save(
            ProductEntity(
                refBrandId = brand.id,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
                status = Product.ProductStatus.ON_SALE,
            ),
        )
        productId = product.id
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("GET /api/v1/products/{productId} 순위 포함")
    inner class ProductDetailWithRank {

        @Test
        @DisplayName("랭킹이 있는 상품 조회 시 응답에 rank 필드가 포함된다")
        fun `순위 있는 상품 상세 조회`() {
            // arrange
            redisTemplate.opsForZSet().add(rankingKey, productId.toString(), 50.0)

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/products/$productId",
                HttpMethod.GET,
                null,
                RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val data = response.body!!.data as Map<*, *>
            assertThat(data["rank"]).isEqualTo(1)
        }

        @Test
        @DisplayName("랭킹이 없는 상품 조회 시 응답에 rank 필드가 생략된다 (NON_NULL)")
        fun `순위 없는 상품 상세 조회`() {
            // act
            val response = testRestTemplate.exchange(
                "/api/v1/products/$productId",
                HttpMethod.GET,
                null,
                RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val data = response.body!!.data as Map<*, *>
            assertThat(data.containsKey("rank")).isFalse()
        }
    }
}
