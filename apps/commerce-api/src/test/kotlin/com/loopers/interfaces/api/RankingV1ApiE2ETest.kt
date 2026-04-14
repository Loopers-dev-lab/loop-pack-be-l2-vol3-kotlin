package com.loopers.interfaces.api

import com.loopers.application.catalog.AdminRegisterProductUseCase
import com.loopers.application.catalog.RegisterProductCriteria
import com.loopers.application.catalog.RegisterProductResult
import com.loopers.config.redis.RedisConfig
import com.loopers.domain.catalog.BrandInfo
import com.loopers.domain.catalog.BrandService
import com.loopers.domain.catalog.RegisterBrandCommand
import com.loopers.domain.user.RegisterCommand
import com.loopers.domain.user.UserService
import com.loopers.interfaces.api.ranking.RankingV1Dto
import com.loopers.utils.DatabaseCleanUp
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
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RankingV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandService: BrandService,
    private val adminRegisterProductUseCase: AdminRegisterProductUseCase,
    private val userService: UserService,
    private val databaseCleanUp: DatabaseCleanUp,
    @param:Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val ENDPOINT = "/api/v1/rankings"
        private const val KEY_PREFIX = "rank"
        private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd")

        private const val DEFAULT_BRAND_NAME = "나이키"

        private const val DEFAULT_USERNAME = "testuser"
        private const val DEFAULT_PASSWORD = "password1234!"
        private const val DEFAULT_NAME = "테스트유저"
        private const val DEFAULT_EMAIL = "test@loopers.com"
        private val DEFAULT_BIRTH_DATE = ZonedDateTime.of(1995, 5, 29, 0, 0, 0, 0, ZoneId.of("Asia/Seoul"))
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisTemplate.keys("$KEY_PREFIX:*")?.forEach { redisTemplate.delete(it) }
    }

    private fun registerUser() {
        userService.register(
            RegisterCommand(
                username = DEFAULT_USERNAME,
                password = DEFAULT_PASSWORD,
                name = DEFAULT_NAME,
                email = DEFAULT_EMAIL,
                birthDate = DEFAULT_BIRTH_DATE,
            ),
        )
    }

    private fun registerBrand(name: String = DEFAULT_BRAND_NAME): BrandInfo =
        brandService.register(RegisterBrandCommand(name = name))

    private fun registerProduct(brandId: Long, name: String, price: BigDecimal = BigDecimal("10000")): RegisterProductResult =
        adminRegisterProductUseCase.execute(
            RegisterProductCriteria(brandId = brandId, name = name, quantity = 100, price = price),
        )

    private fun seedRankAll(productId: Long, score: Double, date: LocalDate = LocalDate.now()) {
        val key = "$KEY_PREFIX:all:${date.format(DATE_FORMAT)}"
        redisTemplate.opsForZSet().incrementScore(key, productId.toString(), score)
    }

    private fun createAuthHeaders(): HttpHeaders = HttpHeaders().apply {
        set("X-Loopers-LoginId", DEFAULT_USERNAME)
        set("X-Loopers-LoginPw", DEFAULT_PASSWORD)
    }

    @DisplayName("GET /api/v1/rankings")
    @Nested
    inner class GetRanking {
        @DisplayName("랭킹이 점수 내림차순으로 페이지 단위 반환되며, 상품 정보가 함께 aggregation 된다.")
        @Test
        fun returnsRankedProductsWithProductInfo() {
            // arrange
            registerUser()
            val brand = registerBrand()
            val productA = registerProduct(brand.id, "상품A", BigDecimal("10000"))
            val productB = registerProduct(brand.id, "상품B", BigDecimal("20000"))
            val productC = registerProduct(brand.id, "상품C", BigDecimal("30000"))
            seedRankAll(productA.id, score = 5.0)
            seedRankAll(productB.id, score = 9.0)
            seedRankAll(productC.id, score = 1.0)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT?page=0&size=10",
                HttpMethod.GET,
                HttpEntity(null, createAuthHeaders()),
                responseType,
            )

            // assert
            val items = response.body?.data?.items.orEmpty()
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(response.body?.data?.totalCount).isEqualTo(3L) },
                { assertThat(response.body?.data?.hasNext).isFalse() },
                { assertThat(items).hasSize(3) },
                // 점수 내림차순: B(9.0) > A(5.0) > C(1.0)
                { assertThat(items[0].productId).isEqualTo(productB.id) },
                { assertThat(items[0].rank).isEqualTo(0L) },
                { assertThat(items[0].score).isEqualTo(9.0) },
                { assertThat(items[0].name).isEqualTo("상품B") },
                { assertThat(items[0].brandName).isEqualTo(DEFAULT_BRAND_NAME) },
                { assertThat(items[1].productId).isEqualTo(productA.id) },
                { assertThat(items[1].rank).isEqualTo(1L) },
                { assertThat(items[2].productId).isEqualTo(productC.id) },
                { assertThat(items[2].rank).isEqualTo(2L) },
            )
        }

        @DisplayName("페이지 파라미터로 랭킹의 일부만 조회할 수 있다.")
        @Test
        fun returnsPagedRanking() {
            // arrange
            registerUser()
            val brand = registerBrand()
            val productA = registerProduct(brand.id, "상품A")
            val productB = registerProduct(brand.id, "상품B")
            val productC = registerProduct(brand.id, "상품C")
            seedRankAll(productA.id, score = 1.0)
            seedRankAll(productB.id, score = 2.0)
            seedRankAll(productC.id, score = 3.0)

            // act — page=1, size=2 (즉, 두 번째 페이지의 한 개)
            val responseType = object : ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT?page=1&size=2",
                HttpMethod.GET,
                HttpEntity(null, createAuthHeaders()),
                responseType,
            )

            // assert
            val data = response.body?.data
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(data?.page).isEqualTo(1) },
                { assertThat(data?.size).isEqualTo(2) },
                { assertThat(data?.totalCount).isEqualTo(3L) },
                { assertThat(data?.hasNext).isFalse() },
                // C(3) → A(1)이 1번째 페이지 → 두 번째 페이지에는 가장 점수 낮은 productA(score=1.0)
                { assertThat(data?.items).hasSize(1) },
                { assertThat(data?.items?.get(0)?.productId).isEqualTo(productA.id) },
                { assertThat(data?.items?.get(0)?.rank).isEqualTo(2L) },
            )
        }

        @DisplayName("date 파라미터로 이전 날짜의 랭킹을 조회할 수 있다.")
        @Test
        fun returnsRankingForGivenDate() {
            // arrange
            registerUser()
            val brand = registerBrand()
            val productA = registerProduct(brand.id, "상품A")
            val productB = registerProduct(brand.id, "상품B")

            // 어제 데이터: B가 1등
            val yesterday = LocalDate.now().minusDays(1)
            seedRankAll(productA.id, score = 1.0, date = yesterday)
            seedRankAll(productB.id, score = 5.0, date = yesterday)

            // 오늘 데이터: A가 1등 (반대)
            seedRankAll(productA.id, score = 9.0)
            seedRankAll(productB.id, score = 2.0)

            // act — date 파라미터로 어제 조회
            val responseType = object : ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT?date=${yesterday.format(DATE_FORMAT)}&page=0&size=10",
                HttpMethod.GET,
                HttpEntity(null, createAuthHeaders()),
                responseType,
            )

            // assert — 어제 기준이므로 B가 1등
            val items = response.body?.data?.items.orEmpty()
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(items).hasSize(2) },
                { assertThat(items[0].productId).isEqualTo(productB.id) },
                { assertThat(items[0].score).isEqualTo(5.0) },
                { assertThat(items[1].productId).isEqualTo(productA.id) },
                { assertThat(items[1].score).isEqualTo(1.0) },
            )
        }

        @DisplayName("ZSET이 비어있으면 빈 페이지를 반환한다.")
        @Test
        fun returnsEmptyWhenNoData() {
            // arrange
            registerUser()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT?page=0&size=10",
                HttpMethod.GET,
                HttpEntity(null, createAuthHeaders()),
                responseType,
            )

            // assert
            val data = response.body?.data
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.OK) },
                { assertThat(data?.totalCount).isEqualTo(0L) },
                { assertThat(data?.items).isEmpty() },
                { assertThat(data?.hasNext).isFalse() },
            )
        }

        @DisplayName("date 형식이 잘못되면 400 BAD_REQUEST.")
        @Test
        fun returnsBadRequestWhenDateFormatInvalid() {
            // arrange
            registerUser()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>>() {}
            val response = testRestTemplate.exchange(
                "$ENDPOINT?date=invalid&page=0&size=10",
                HttpMethod.GET,
                HttpEntity(null, createAuthHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }
}
