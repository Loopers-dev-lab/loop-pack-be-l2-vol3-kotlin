package com.loopers.interfaces.api

import com.loopers.application.product.ProductCacheManager
import com.loopers.config.redis.RedisConfig
import com.loopers.config.redis.RedisKeys
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.interfaces.api.ranking.RankingDto
import com.loopers.interfaces.common.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RankingApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
    private val productCacheManager: ProductCacheManager,
) {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val RESPONSE_TYPE = object : ParameterizedTypeReference<ApiResponse<RankingDto.Response>>() {}
        private val HTTP_ENTITY = HttpEntity<Void>(HttpHeaders())
    }

    private val today = LocalDate.now().format(DATE_FORMATTER)
    private val rankingKey = RedisKeys.rankingKey(today)

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
        productCacheManager.evictAllCaches()
    }

    @DisplayName("GET /api/v1/rankings")
    @Nested
    inner class GetRankings {

        @DisplayName("date 파라미터 없이 요청하면, 오늘 날짜 기준으로 랭킹을 반환한다.")
        @Test
        fun returnsRankingsWithDefaultDate() {
            // arrange
            val brand = brandRepository.save(Brand(name = "테스트 브랜드", description = "설명"))
            val productA = productRepository.save(
                Product(name = "상품A", description = null, price = Money.of(10000), likes = LikeCount.of(0), stockQuantity = StockQuantity.of(100), brandId = brand.id),
            )
            val productB = productRepository.save(
                Product(name = "상품B", description = null, price = Money.of(20000), likes = LikeCount.of(0), stockQuantity = StockQuantity.of(100), brandId = brand.id),
            )
            redisTemplate.opsForZSet().add(rankingKey, productA.id.toString(), 80.0)
            redisTemplate.opsForZSet().add(rankingKey, productB.id.toString(), 50.0)

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/rankings?page=1&size=20",
                HttpMethod.GET,
                HTTP_ENTITY,
                RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode.is2xxSuccessful)
                .withFailMessage("Expected 2xx but got ${response.statusCode}, body=${response.body}")
                .isTrue()
            val data = response.body?.data
            assertThat(data).isNotNull()
            assertThat(data!!.items).hasSize(2)
            assertThat(data.items[0].productName).isEqualTo("상품A")
            assertThat(data.items[0].rank).isEqualTo(1L)
            assertThat(data.items[1].productName).isEqualTo("상품B")
            assertThat(data.items[1].rank).isEqualTo(2L)
        }

        @DisplayName("date 파라미터로 이전 날짜를 지정하면, 해당 날짜의 랭킹을 반환한다.")
        @Test
        fun returnsRankingsForPreviousDate() {
            // arrange
            val yesterday = LocalDate.now().minusDays(1).format(DATE_FORMATTER)
            val yesterdayKey = RedisKeys.rankingKey(yesterday)
            val brand = brandRepository.save(Brand(name = "테스트 브랜드", description = "설명"))
            val product = productRepository.save(
                Product(name = "어제의 상품", description = null, price = Money.of(5000), likes = LikeCount.of(0), stockQuantity = StockQuantity.of(50), brandId = brand.id),
            )
            redisTemplate.opsForZSet().add(yesterdayKey, product.id.toString(), 100.0)

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/rankings?date=$yesterday&page=1&size=20",
                HttpMethod.GET,
                HTTP_ENTITY,
                RESPONSE_TYPE,
            )

            // assert
            assertThat(response.statusCode.is2xxSuccessful).isTrue()
            val data = response.body?.data
            assertThat(data!!.items).hasSize(1)
            assertThat(data.items[0].productName).isEqualTo("어제의 상품")
            assertThat(data.items[0].rank).isEqualTo(1L)
        }

        @DisplayName("주문 1건의 점수가 좋아요 3건의 점수보다 높다.")
        @Test
        fun orderScoreIsHigherThanThreeLikes() {
            // arrange
            val brand = brandRepository.save(Brand(name = "테스트 브랜드", description = "설명"))
            val likedProduct = productRepository.save(
                Product(name = "좋아요 상품", description = null, price = Money.of(10000), likes = LikeCount.of(0), stockQuantity = StockQuantity.of(100), brandId = brand.id),
            )
            val orderedProduct = productRepository.save(
                Product(name = "주문 상품", description = null, price = Money.of(10000), likes = LikeCount.of(0), stockQuantity = StockQuantity.of(100), brandId = brand.id),
            )

            // 좋아요 3건: 0.2 × 3 = 0.6
            val likeScore = 0.2 * 3
            // 주문 1건 (10000원 × 1개): 0.7 × log10(10000) = 0.7 × 4 = 2.8
            val orderScore = 0.7 * kotlin.math.log10(10000.0)

            redisTemplate.opsForZSet().add(rankingKey, likedProduct.id.toString(), likeScore)
            redisTemplate.opsForZSet().add(rankingKey, orderedProduct.id.toString(), orderScore)

            // act
            val response = testRestTemplate.exchange(
                "/api/v1/rankings?page=1&size=20",
                HttpMethod.GET,
                HTTP_ENTITY,
                RESPONSE_TYPE,
            )

            // assert — 주문 상품이 1위
            assertThat(response.statusCode.is2xxSuccessful).isTrue()
            val data = response.body?.data
            assertThat(data!!.items).hasSize(2)
            assertThat(data.items[0].productName).isEqualTo("주문 상품")
            assertThat(data.items[0].score).isGreaterThan(data.items[1].score)
        }
    }
}
