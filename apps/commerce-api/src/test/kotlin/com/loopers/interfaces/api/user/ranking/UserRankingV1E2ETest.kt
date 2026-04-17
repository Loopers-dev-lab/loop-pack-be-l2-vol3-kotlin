package com.loopers.interfaces.api.user.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.infrastructure.ranking.MonthlyProductRankEntity
import com.loopers.infrastructure.ranking.MonthlyProductRankJpaRepository
import com.loopers.infrastructure.ranking.RedisProductRankingQueryRepository
import com.loopers.infrastructure.ranking.WeeklyProductRankEntity
import com.loopers.infrastructure.ranking.WeeklyProductRankJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.support.page.PageResponse
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("GET /api/v1/rankings — period 파라미터 E2E")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserRankingV1E2ETest
    @Autowired
    constructor(
        private val testRestTemplate: TestRestTemplate,
        private val brandRepository: BrandRepository,
        private val productRepository: ProductRepository,
        private val weeklyProductRankJpaRepository: WeeklyProductRankJpaRepository,
        private val monthlyProductRankJpaRepository: MonthlyProductRankJpaRepository,
        @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
        private val redisTemplate: RedisTemplate<String, String>,
        private val databaseCleanUp: DatabaseCleanUp,
        private val redisCleanUp: RedisCleanUp,
    ) {
        private val admin = "loopers.admin"
        private val date = LocalDate.of(2026, 4, 16)
        private val dateParam = "20260416"

        @AfterEach
        fun tearDown() {
            databaseCleanUp.truncateAllTables()
            redisCleanUp.truncateAll()
        }

        private val responseType =
            object : ParameterizedTypeReference<ApiResponse<PageResponse<UserRankingV1Response.RankedProduct>>>() {}

        @Nested
        @DisplayName("period 파라미터별 랭킹 조회")
        inner class GetListByPeriod {

            @Test
            @DisplayName("period=daily → Redis 기반 일간 랭킹을 반환한다")
            fun getList_daily() {
                val productId = persistActiveProductAndBrand()
                val key = RedisProductRankingQueryRepository.buildKey(date)
                redisTemplate.opsForZSet().add(key, productId.toString(), 10.5)

                val response = testRestTemplate.exchange(
                    "/api/v1/rankings?date=$dateParam&period=DAILY",
                    HttpMethod.GET,
                    null,
                    responseType,
                )

                assertAll(
                    { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                    { assertThat(response.body?.data?.content).hasSize(1) },
                    { assertThat(response.body?.data?.content?.first()?.productId).isEqualTo(productId) },
                    { assertThat(response.body?.data?.content?.first()?.rank).isEqualTo(1L) },
                    { assertThat(response.body?.data?.content?.first()?.score).isEqualTo(10.5) },
                )
            }

            @Test
            @DisplayName("period=weekly → DB MV 기반 주간 랭킹을 반환한다")
            fun getList_weekly() {
                val productId = persistActiveProductAndBrand()
                // 2026-04-16 → ISO (WEEK_BASED_YEAR=2026, WEEK=16)
                weeklyProductRankJpaRepository.save(
                    WeeklyProductRankEntity(
                        productId = productId,
                        year = 2026,
                        week = 16,
                        totalScore = 42.0,
                        rankNumber = 1,
                        viewCount = 10,
                        likeCount = 1,
                        unitsSold = 0,
                        salesAmount = 0L,
                        orderScore = 0.0,
                    ),
                )

                val response = testRestTemplate.exchange(
                    "/api/v1/rankings?date=$dateParam&period=WEEKLY",
                    HttpMethod.GET,
                    null,
                    responseType,
                )

                assertAll(
                    { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                    { assertThat(response.body?.data?.content).hasSize(1) },
                    { assertThat(response.body?.data?.content?.first()?.productId).isEqualTo(productId) },
                    { assertThat(response.body?.data?.content?.first()?.rank).isEqualTo(1L) },
                    { assertThat(response.body?.data?.content?.first()?.score).isEqualTo(42.0) },
                )
            }

            @Test
            @DisplayName("period=monthly → DB MV 기반 월간 랭킹을 반환한다")
            fun getList_monthly() {
                val productId = persistActiveProductAndBrand()
                monthlyProductRankJpaRepository.save(
                    MonthlyProductRankEntity(
                        productId = productId,
                        year = 2026,
                        month = 4,
                        totalScore = 77.0,
                        rankNumber = 1,
                        viewCount = 20,
                        likeCount = 2,
                        unitsSold = 0,
                        salesAmount = 0L,
                        orderScore = 0.0,
                    ),
                )

                val response = testRestTemplate.exchange(
                    "/api/v1/rankings?date=$dateParam&period=MONTHLY",
                    HttpMethod.GET,
                    null,
                    responseType,
                )

                assertAll(
                    { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                    { assertThat(response.body?.data?.content).hasSize(1) },
                    { assertThat(response.body?.data?.content?.first()?.productId).isEqualTo(productId) },
                    { assertThat(response.body?.data?.content?.first()?.rank).isEqualTo(1L) },
                    { assertThat(response.body?.data?.content?.first()?.score).isEqualTo(77.0) },
                )
            }

            @Test
            @DisplayName("period 미제공 시 daily가 기본값이다 (Redis ZSET 조회)")
            fun getList_defaultDaily() {
                val productId = persistActiveProductAndBrand()
                val key = RedisProductRankingQueryRepository.buildKey(date)
                redisTemplate.opsForZSet().add(key, productId.toString(), 3.14)
                // weekly MV에도 동일 날짜에 데이터 넣어 기본값이 weekly로 잘못 가면 구분되도록 함
                weeklyProductRankJpaRepository.save(
                    WeeklyProductRankEntity(
                        productId = productId,
                        year = 2026,
                        week = 16,
                        totalScore = 999.0,
                        rankNumber = 1,
                        viewCount = 0,
                        likeCount = 0,
                        unitsSold = 0,
                        salesAmount = 0L,
                        orderScore = 0.0,
                    ),
                )

                val response = testRestTemplate.exchange(
                    "/api/v1/rankings?date=$dateParam",
                    HttpMethod.GET,
                    null,
                    responseType,
                )

                assertAll(
                    { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                    { assertThat(response.body?.data?.content).hasSize(1) },
                    // daily (Redis) 응답인지 확인: score=3.14
                    { assertThat(response.body?.data?.content?.first()?.score).isEqualTo(3.14) },
                )
            }

            @Test
            @DisplayName("period 소문자로 전달해도 정상 동작한다 (Converter 대소문자 무관)")
            fun getList_lowercasePeriod() {
                val productId = persistActiveProductAndBrand()
                weeklyProductRankJpaRepository.save(
                    WeeklyProductRankEntity(
                        productId = productId,
                        year = 2026,
                        week = 16,
                        totalScore = 42.0,
                        rankNumber = 1,
                        viewCount = 0,
                        likeCount = 0,
                        unitsSold = 0,
                        salesAmount = 0L,
                        orderScore = 0.0,
                    ),
                )

                val response = testRestTemplate.exchange(
                    "/api/v1/rankings?date=$dateParam&period=weekly",
                    HttpMethod.GET,
                    null,
                    responseType,
                )

                assertAll(
                    { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                    { assertThat(response.body?.data?.content).hasSize(1) },
                    { assertThat(response.body?.data?.content?.first()?.score).isEqualTo(42.0) },
                )
            }

            @Test
            @DisplayName("period=XYZ (잘못된 값) → 400 BAD_REQUEST")
            fun getList_invalidPeriod_returns400() {
                val response = testRestTemplate.exchange(
                    "/api/v1/rankings?date=$dateParam&period=XYZ",
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<ApiResponse<Any>>() {},
                )

                assertAll(
                    { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                    { assertThat(response.body?.meta?.errorCode).isEqualTo("Bad Request") },
                )
            }

            @Test
            @DisplayName("weekly MV가 해당 주에 없으면 200 OK + 빈 페이지를 반환한다")
            fun getList_weeklyEmpty_returnsEmptyPage() {
                val response = testRestTemplate.exchange(
                    "/api/v1/rankings?date=$dateParam&period=WEEKLY",
                    HttpMethod.GET,
                    null,
                    responseType,
                )

                assertAll(
                    { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                    { assertThat(response.body?.data?.content).isEmpty() },
                    { assertThat(response.body?.data?.totalElements).isEqualTo(0) },
                )
            }

            @Test
            @DisplayName("monthly MV가 해당 월에 없으면 200 OK + 빈 페이지를 반환한다")
            fun getList_monthlyEmpty_returnsEmptyPage() {
                val response = testRestTemplate.exchange(
                    "/api/v1/rankings?date=$dateParam&period=MONTHLY",
                    HttpMethod.GET,
                    null,
                    responseType,
                )

                assertAll(
                    { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                    { assertThat(response.body?.data?.content).isEmpty() },
                    { assertThat(response.body?.data?.totalElements).isEqualTo(0) },
                )
            }

            @Test
            @DisplayName("date 형식이 잘못되었으면 period와 무관하게 400 BAD_REQUEST")
            fun getList_invalidDateWithPeriod_returns400() {
                val response = testRestTemplate.exchange(
                    "/api/v1/rankings?date=bad-date&period=WEEKLY",
                    HttpMethod.GET,
                    null,
                    object : ParameterizedTypeReference<ApiResponse<Any>>() {},
                )

                assertAll(
                    { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
                    { assertThat(response.body?.meta?.errorCode).isEqualTo("Bad Request") },
                )
            }
        }

        /**
         * UseCase가 ACTIVE 상품·브랜드만 합성에 포함하므로, E2E 검증을 위해
         * 활성 상품 + 활성 브랜드 조합을 실제 DB에 저장하고 생성된 productId를 반환한다.
         */
        private fun persistActiveProductAndBrand(): Long {
            val brand = brandRepository.save(Brand.register(name = "나이키"), admin)
                .update(name = "나이키", status = "ACTIVE")
                .let { brandRepository.save(it, admin) }
            val product = Product.register(
                name = "테스트 상품",
                regularPrice = Money(BigDecimal.valueOf(20_000)),
                sellingPrice = Money(BigDecimal.valueOf(15_000)),
                brandId = brand.id!!,
            ).activate()
            return productRepository.save(product, admin).id!!
        }
    }
