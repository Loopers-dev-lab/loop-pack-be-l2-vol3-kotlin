package com.loopers.interfaces.api.ranking

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
class RankingApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {

    companion object {
        private const val ENDPOINT = "/api/v1/rankings"
        private val RESPONSE_TYPE = object : ParameterizedTypeReference<ApiResponse<Any>>() {}
    }

    private val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
    private val rankingKey = "${RedisRankingConstants.RANKING_KEY_PREFIX}${today.format(DateTimeFormatter.BASIC_ISO_DATE)}"

    private var productAId = 0L
    private var productBId = 0L
    private var productCId = 0L

    @BeforeEach
    fun setUp() {
        val brand = brandJpaRepository.save(BrandEntity(name = "테스트브랜드"))

        val productA = productJpaRepository.save(
            ProductEntity(refBrandId = brand.id, name = "상품A", price = BigDecimal("10000"), stock = 100, status = Product.ProductStatus.ON_SALE),
        )
        val productB = productJpaRepository.save(
            ProductEntity(refBrandId = brand.id, name = "상품B", price = BigDecimal("20000"), stock = 50, status = Product.ProductStatus.ON_SALE),
        )
        val productC = productJpaRepository.save(
            ProductEntity(refBrandId = brand.id, name = "상품C", price = BigDecimal("30000"), stock = 30, status = Product.ProductStatus.ON_SALE),
        )
        productAId = productA.id
        productBId = productB.id
        productCId = productC.id

        redisTemplate.opsForZSet().add(rankingKey, productAId.toString(), 3.0)
        redisTemplate.opsForZSet().add(rankingKey, productBId.toString(), 5.0)
        redisTemplate.opsForZSet().add(rankingKey, productCId.toString(), 1.5)
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
        databaseCleanUp.truncateAllTables()
    }

    @Nested
    @DisplayName("GET /api/v1/rankings")
    inner class GetRankings {

        @Test
        @DisplayName("랭킹 조회 시 rank, productName, score, 페이지 메타데이터가 포함된다")
        fun `응답 계약 검증`() {
            // Act
            val response = testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, null, RESPONSE_TYPE)

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val body = response.body!!
            assertThat(body.meta.result).isEqualTo(ApiResponse.Metadata.Result.SUCCESS)

            val page = body.data as Map<*, *>
            val content = page["content"] as List<*>
            assertThat(content).hasSize(3)

            // 1위: 상품B(score=5.0)
            val first = content[0] as Map<*, *>
            assertThat(first["rank"]).isEqualTo(1)
            assertThat((first["productId"] as Number).toLong()).isEqualTo(productBId)
            assertThat(first["productName"]).isEqualTo("상품B")
            assertThat((first["score"] as Number).toDouble()).isEqualTo(5.0)

            // 2위: 상품A(score=3.0)
            val second = content[1] as Map<*, *>
            assertThat(second["rank"]).isEqualTo(2)
            assertThat((second["productId"] as Number).toLong()).isEqualTo(productAId)

            // 3위: 상품C(score=1.5)
            val third = content[2] as Map<*, *>
            assertThat(third["rank"]).isEqualTo(3)
            assertThat((third["productId"] as Number).toLong()).isEqualTo(productCId)

            // 페이지 메타데이터
            assertThat((page["totalElements"] as Number).toLong()).isEqualTo(3L)
            assertThat(page["number"]).isEqualTo(0)
            assertThat(page["size"]).isEqualTo(20)
        }

        @Test
        @DisplayName("date 파라미터로 특정 날짜의 랭킹이 조회된다")
        fun `날짜 지정 조회`() {
            // Act
            val dateStr = today.format(DateTimeFormatter.BASIC_ISO_DATE)
            val response = testRestTemplate.exchange("$ENDPOINT?date=$dateStr", HttpMethod.GET, null, RESPONSE_TYPE)

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val page = response.body!!.data as Map<*, *>
            val content = page["content"] as List<*>
            assertThat(content).hasSize(3)
        }

        @Test
        @DisplayName("유효하지 않은 date 형식은 400을 반환한다")
        fun `잘못된 날짜 형식`() {
            // Act
            val response = testRestTemplate.exchange("$ENDPOINT?date=invalid", HttpMethod.GET, null, RESPONSE_TYPE)

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("페이지네이션 시 content 수와 페이지 메타가 일치한다")
        fun `페이지네이션`() {
            // Act
            val response = testRestTemplate.exchange("$ENDPOINT?page=0&size=2", HttpMethod.GET, null, RESPONSE_TYPE)

            // Assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            val page = response.body!!.data as Map<*, *>
            val content = page["content"] as List<*>
            assertThat(content).hasSize(2)
            assertThat(page["size"]).isEqualTo(2)
            assertThat(page["number"]).isEqualTo(0)

            // 1위, 2위만 포함
            val first = content[0] as Map<*, *>
            assertThat(first["rank"]).isEqualTo(1)
            val second = content[1] as Map<*, *>
            assertThat(second["rank"]).isEqualTo(2)
        }

        @Test
        @DisplayName("page에 음수를 전달하면 400을 반환한다")
        fun `page 음수`() {
            val response = testRestTemplate.exchange("$ENDPOINT?page=-1", HttpMethod.GET, null, RESPONSE_TYPE)
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("size에 0을 전달하면 400을 반환한다")
        fun `size 0`() {
            val response = testRestTemplate.exchange("$ENDPOINT?size=0", HttpMethod.GET, null, RESPONSE_TYPE)
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        @DisplayName("size에 100 초과를 전달하면 400을 반환한다")
        fun `size 초과`() {
            val response = testRestTemplate.exchange("$ENDPOINT?size=101", HttpMethod.GET, null, RESPONSE_TYPE)
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
