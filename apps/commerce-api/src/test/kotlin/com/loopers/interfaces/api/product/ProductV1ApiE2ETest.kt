package com.loopers.interfaces.api.product

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.like.LikeFacade
import com.loopers.config.redis.RedisConfig
import com.loopers.domain.brand.BrandModel
import com.loopers.domain.product.ProductModel
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.data.redis.core.RedisTemplate

@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Disabled("Requires Docker-backed MySQL/Redis testcontainers")
@DisplayName("ProductV1 API")
class ProductV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val likeFacade: LikeFacade,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {
    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    inner class FindAll {
        @DisplayName("sort 파라미터로 좋아요순 정렬하고 목록 캐시를 생성한다")
        @Test
        fun returnsProductsSortedByLikesAndCachesList() {
            // arrange
            val brand = brandJpaRepository.save(BrandModel(name = "round5-brand-list"))
            val lowLikeProduct = productJpaRepository.save(createProduct(brand.id, "상품A", 1L, 12000L))
            val highLikeProduct = productJpaRepository.save(createProduct(brand.id, "상품B", 9L, 11000L))

            // act
            val response = testRestTemplate.getForEntity(
                "/api/v1/products?brandId=${brand.id}&sort=likes_desc&page=0&size=20",
                String::class.java,
            )
            val body = readBody(response.body)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(body.path("meta").path("result").asText()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS.name)
            assertThat(body.path("data").path("content")[0].path("id").asLong()).isEqualTo(highLikeProduct.id)
            assertThat(body.path("data").path("content")[1].path("id").asLong()).isEqualTo(lowLikeProduct.id)
            assertThat(redisTemplate.hasKey("product:list:${brand.id}:likes_desc:0:20")).isTrue()
        }

        @DisplayName("지원하지 않는 sort 값이면 400 BAD_REQUEST를 반환한다")
        @Test
        fun returnsBadRequestWhenSortIsInvalid() {
            // act
            val response = testRestTemplate.getForEntity(
                "/api/v1/products?sort=random&page=0&size=20",
                String::class.java,
            )
            val body = readBody(response.body)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(body.path("meta").path("result").asText()).isEqualTo(ApiResponse.Metadata.Result.FAIL.name)
            assertThat(body.path("meta").path("errorCode").asText()).isEqualTo(HttpStatus.BAD_REQUEST.reasonPhrase)
        }
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    inner class FindById {
        @DisplayName("상세 조회는 캐시를 생성하고 좋아요 변경 후 무효화된다")
        @Test
        fun cachesDetailAndEvictsAfterLike() {
            // arrange
            val brand = brandJpaRepository.save(BrandModel(name = "round5-brand-detail"))
            val product = productJpaRepository.save(createProduct(brand.id, "상세 상품", 3L, 18000L))

            // act
            val detailResponse = testRestTemplate.getForEntity("/api/v1/products/${product.id}", String::class.java)
            val listResponse = testRestTemplate.getForEntity(
                "/api/v1/products?brandId=${brand.id}&sort=likes_desc&page=0&size=20",
                String::class.java,
            )
            likeFacade.likeProduct(userId = 777L, productId = product.id)

            // assert
            assertThat(detailResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(listResponse.statusCode).isEqualTo(HttpStatus.OK)
            assertThat(redisTemplate.hasKey("product:detail:${product.id}")).isFalse()
            assertThat(redisTemplate.hasKey("product:list:${brand.id}:likes_desc:0:20")).isFalse()
        }
    }

    private fun createProduct(brandId: Long, name: String, likesCount: Long, price: Long): ProductModel = ProductModel(
            name = name,
            price = price,
            brandId = brandId,
            likesCount = likesCount,
            stockQuantity = 20,
        )

    private fun readBody(body: String?): JsonNode {
        requireNotNull(body) { "response body must not be null" }
        return objectMapper.readTree(body)
    }
}
