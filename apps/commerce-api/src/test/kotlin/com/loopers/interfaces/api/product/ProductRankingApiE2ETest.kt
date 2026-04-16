package com.loopers.interfaces.api.product

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.product.dto.ProductInfo
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.PageResponse
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class)
@DisplayName("Product Ranking API E2E 테스트")
class ProductRankingApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    private val weeklyJpaRepository: com.loopers.infrastructure.ranking.MvProductRankWeeklyJpaRepository,
    private val monthlyJpaRepository: com.loopers.infrastructure.ranking.MvProductRankMonthlyJpaRepository,
) {

    companion object {
        private const val PRODUCTS_ENDPOINT = "/api/v1/products"
        private const val DAILY_RANKING_ENDPOINT = "/api/v1/products/rankings/daily"
        private const val WEEKLY_RANKING_ENDPOINT = "/api/v1/products/rankings/weekly"
        private const val MONTHLY_RANKING_ENDPOINT = "/api/v1/products/rankings/monthly"
        private const val RANKING_KEY_PREFIX = "ranking:all:"
        private val DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE
    }

    @BeforeEach
    fun setUp() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @Test
    @DisplayName("상품 상세 조회 응답에 오늘 랭킹을 포함한다")
    fun includesRankInProductDetail() {
        val processingDate = LocalDate.now()
        val brand = brandJpaRepository.save(Brand.create(name = "랭킹 브랜드", description = "설명"))
        val product = productJpaRepository.save(
            Product.create(
                brand = brand,
                name = "랭킹 상품",
                price = BigDecimal("15000.00"),
                status = ProductStatus.ACTIVE,
            ),
        )
        redisTemplate.opsForZSet().add(
            "${RANKING_KEY_PREFIX}${processingDate.format(DATE_FORMATTER)}",
            product.id.toString(),
            99.0,
        )

        val responseType = object : ParameterizedTypeReference<ApiResponse<ProductInfo>>() {}
        val response = testRestTemplate.exchange(
            "$PRODUCTS_ENDPOINT/${product.id}",
            HttpMethod.GET,
            HttpEntity<Any>(Unit),
            responseType,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data?.rank).isEqualTo(1L)
    }

    @Test
    @DisplayName("랭킹 조회는 Redis 순서를 유지하고 page는 0-based, rank는 1-based로 반환한다")
    fun returnsRankedProductsInRedisOrder() {
        val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
        val product1 = productJpaRepository.save(
            Product.create(brand = brand, name = "상품1", price = BigDecimal("1000.00"), status = ProductStatus.ACTIVE),
        )
        val product2 = productJpaRepository.save(
            Product.create(brand = brand, name = "상품2", price = BigDecimal("2000.00"), status = ProductStatus.ACTIVE),
        )
        val product3 = productJpaRepository.save(
            Product.create(brand = brand, name = "상품3", price = BigDecimal("3000.00"), status = ProductStatus.ACTIVE),
        )

        val key = "${RANKING_KEY_PREFIX}20260406"
        redisTemplate.opsForZSet().add(key, product2.id.toString(), 20.0)
        redisTemplate.opsForZSet().add(key, product3.id.toString(), 15.0)
        redisTemplate.opsForZSet().add(key, product1.id.toString(), 10.0)

        val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
        val response = testRestTemplate.exchange(
            "$DAILY_RANKING_ENDPOINT?page=0&size=20&date=20260406",
            HttpMethod.GET,
            HttpEntity<Any>(Unit),
            responseType,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data?.number).isEqualTo(0)
        assertThat(response.body?.data?.content?.map { it.id }).containsExactly(product2.id, product3.id, product1.id)
        assertThat(response.body?.data?.content?.map { it.rank }).containsExactly(1L, 2L, 3L)
    }

    @Test
    @DisplayName("과거 날짜를 지정하면 해당 날짜 랭킹을 조회한다")
    fun returnsHistoricalRankingWhenDateIsProvided() {
        val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
        val product = productJpaRepository.save(
            Product.create(brand = brand, name = "과거 상품", price = BigDecimal("5000.00"), status = ProductStatus.ACTIVE),
        )

        redisTemplate.opsForZSet().add("${RANKING_KEY_PREFIX}20260405", product.id.toString(), 11.0)

        val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
        val response = testRestTemplate.exchange(
            "$DAILY_RANKING_ENDPOINT?page=0&size=20&date=20260405",
            HttpMethod.GET,
            HttpEntity<Any>(Unit),
            responseType,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data?.content?.map { it.id }).containsExactly(product.id)
        assertThat(response.body?.data?.content?.map { it.rank }).containsExactly(1L)
    }

    @Test
    @DisplayName("랭킹에 없는 상품 상세 조회 시 rank는 null을 반환한다")
    fun returnsNullRankForUnrankedProduct() {
        val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
        val product = productJpaRepository.save(
            Product.create(brand = brand, name = "미등록 상품", price = BigDecimal("3000.00"), status = ProductStatus.ACTIVE),
        )

        val responseType = object : ParameterizedTypeReference<ApiResponse<ProductInfo>>() {}
        val response = testRestTemplate.exchange(
            "$PRODUCTS_ENDPOINT/${product.id}",
            HttpMethod.GET,
            HttpEntity<Any>(Unit),
            responseType,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data?.rank).isNull()
    }

    @Test
    @DisplayName("빈 랭킹 조회 시 empty page를 반환한다")
    fun emptyRankingReturnsEmptyPage() {
        val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
        val response = testRestTemplate.exchange(
            "$DAILY_RANKING_ENDPOINT?page=0&size=20&date=20260406",
            HttpMethod.GET,
            HttpEntity<Any>(Unit),
            responseType,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data?.content).isEmpty()
        assertThat(response.body?.data?.totalElements).isEqualTo(0L)
    }

    @Test
    @DisplayName("페이지 번호 파라미터를 지정할 수 있다")
    fun acceptsPageParameter() {
        val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
        val product = productJpaRepository.save(
            Product.create(brand = brand, name = "상품", price = BigDecimal("1000.00"), status = ProductStatus.ACTIVE),
        )

        val key = "${RANKING_KEY_PREFIX}20260406"
        redisTemplate.opsForZSet().add(key, product.id.toString(), 10.0)

        val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
        val response = testRestTemplate.exchange(
            "$DAILY_RANKING_ENDPOINT?page=0&size=20&date=20260406",
            HttpMethod.GET,
            HttpEntity<Any>(Unit),
            responseType,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data?.number).isEqualTo(0)
    }

    @Test
    @DisplayName("범위를 초과한 page 조회 시 empty list를 반환한다")
    fun pageOutOfRangeReturnsEmpty() {
        val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
        val product = productJpaRepository.save(
            Product.create(brand = brand, name = "상품", price = BigDecimal("1000.00"), status = ProductStatus.ACTIVE),
        )

        val key = "${RANKING_KEY_PREFIX}20260406"
        redisTemplate.opsForZSet().add(key, product.id.toString(), 10.0)

        val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
        val response = testRestTemplate.exchange(
            "$DAILY_RANKING_ENDPOINT?page=999&size=20&date=20260406",
            HttpMethod.GET,
            HttpEntity<Any>(Unit),
            responseType,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data?.content?.size).isEqualTo(0)
    }

    @Test
    @DisplayName("미래 날짜 조회 시 empty list를 반환한다")
    fun futureDateReturnsEmpty() {
        val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
        val product = productJpaRepository.save(
            Product.create(brand = brand, name = "상품", price = BigDecimal("1000.00"), status = ProductStatus.ACTIVE),
        )

        // 오늘 데이터만 있음
        val today = LocalDate.now()
        val key = "${RANKING_KEY_PREFIX}${today.format(DATE_FORMATTER)}"
        redisTemplate.opsForZSet().add(key, product.id.toString(), 10.0)

        // 내일 데이터 조회
        val tomorrow = today.plusDays(1)
        val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
        val response = testRestTemplate.exchange(
            "$DAILY_RANKING_ENDPOINT?page=0&size=20&date=${tomorrow.format(DATE_FORMATTER)}",
            HttpMethod.GET,
            HttpEntity<Any>(Unit),
            responseType,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data?.content).isEmpty()
    }

    @Test
    @DisplayName("WEEKLY 랭킹 조회: MV 테이블에서 주간 데이터를 조회한다")
    fun weeklyRankingReturnsWeeklyMVData() {
        val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
        val product1 = productJpaRepository.save(
            Product.create(brand = brand, name = "상품1", price = BigDecimal("1000.00"), status = ProductStatus.ACTIVE),
        )
        val product2 = productJpaRepository.save(
            Product.create(brand = brand, name = "상품2", price = BigDecimal("2000.00"), status = ProductStatus.ACTIVE),
        )

        // MV에 주간 랭킹 데이터 삽입 (2026-04-14는 W16)
        weeklyJpaRepository.save(
            com.loopers.domain.ranking.MvProductRankWeekly(
                productId = product1.id,
                rank = 1,
                score = 100.0,
                yearWeek = "2026-W16",
            ),
        )
        weeklyJpaRepository.save(
            com.loopers.domain.ranking.MvProductRankWeekly(
                productId = product2.id,
                rank = 2,
                score = 90.0,
                yearWeek = "2026-W16",
            ),
        )

        val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
        val response = testRestTemplate.exchange(
            "$WEEKLY_RANKING_ENDPOINT?page=0&size=20&date=20260414",
            HttpMethod.GET,
            HttpEntity<Any>(Unit),
            responseType,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data?.content).hasSize(2)
        assertThat(response.body?.data?.content?.map { it.id }).containsExactly(product1.id, product2.id)
        assertThat(response.body?.data?.content?.map { it.rank }).containsExactly(1L, 2L)
        assertThat(response.body?.data?.totalElements).isEqualTo(2L)
    }

    @Test
    @DisplayName("MONTHLY 랭킹 조회: MV 테이블에서 월간 데이터를 조회한다")
    fun monthlyRankingReturnsMonthlyMVData() {
        val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
        val product1 = productJpaRepository.save(
            Product.create(brand = brand, name = "상품1", price = BigDecimal("1000.00"), status = ProductStatus.ACTIVE),
        )
        val product2 = productJpaRepository.save(
            Product.create(brand = brand, name = "상품2", price = BigDecimal("2000.00"), status = ProductStatus.ACTIVE),
        )
        val product3 = productJpaRepository.save(
            Product.create(brand = brand, name = "상품3", price = BigDecimal("3000.00"), status = ProductStatus.ACTIVE),
        )

        // MV에 월간 랭킹 데이터 삽입 (2026-04)
        monthlyJpaRepository.save(
            com.loopers.domain.ranking.MvProductRankMonthly(
                productId = product1.id,
                rank = 1,
                score = 150.0,
                yearMonth = "2026-04",
            ),
        )
        monthlyJpaRepository.save(
            com.loopers.domain.ranking.MvProductRankMonthly(
                productId = product2.id,
                rank = 2,
                score = 120.0,
                yearMonth = "2026-04",
            ),
        )
        monthlyJpaRepository.save(
            com.loopers.domain.ranking.MvProductRankMonthly(
                productId = product3.id,
                rank = 3,
                score = 100.0,
                yearMonth = "2026-04",
            ),
        )

        val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
        val response = testRestTemplate.exchange(
            "$MONTHLY_RANKING_ENDPOINT?page=0&size=20&date=20260414",
            HttpMethod.GET,
            HttpEntity<Any>(Unit),
            responseType,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data?.content).hasSize(3)
        assertThat(response.body?.data?.content?.map { it.id }).containsExactly(product1.id, product2.id, product3.id)
        assertThat(response.body?.data?.content?.map { it.rank }).containsExactly(1L, 2L, 3L)
        assertThat(response.body?.data?.totalElements).isEqualTo(3L)
    }

    @Test
    @DisplayName("WEEKLY 페이지네이션: 주간 데이터 페이지 단위 조회")
    fun weeklyRankingPagination() {
        val brand = brandJpaRepository.save(Brand.create(name = "브랜드", description = "설명"))
        repeat(25) { index ->
            val product = productJpaRepository.save(
                Product.create(
                    brand = brand,
                    name = "상품${index + 1}",
                    price = BigDecimal("${(index + 1) * 1000}.00"),
                    status = ProductStatus.ACTIVE,
                ),
            )
            weeklyJpaRepository.save(
                com.loopers.domain.ranking.MvProductRankWeekly(
                    productId = product.id,
                    rank = index + 1,
                    score = (1000 - index).toDouble(),
                    yearWeek = "2026-W16",
                ),
            )
        }

        val responseType = object : ParameterizedTypeReference<ApiResponse<PageResponse<ProductInfo>>>() {}
        val response = testRestTemplate.exchange(
            "$WEEKLY_RANKING_ENDPOINT?page=1&size=20&date=20260414",
            HttpMethod.GET,
            HttpEntity<Any>(Unit),
            responseType,
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data?.content).hasSize(5) // 25개 중 두 번째 페이지는 5개
        assertThat(response.body?.data?.totalElements).isEqualTo(25L)
    }
}
