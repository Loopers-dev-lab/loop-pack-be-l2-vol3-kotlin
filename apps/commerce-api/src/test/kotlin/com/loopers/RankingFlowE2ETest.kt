package com.loopers

import com.loopers.application.event.KafkaIntegrationEventPublisher
import com.loopers.infrastructure.brand.BrandEntity
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductEntity
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.interfaces.api.product.ProductV1Dto
import com.loopers.interfaces.api.ranking.RankingV1Dto
import com.loopers.kafka.IntegrationEvent
import com.loopers.kafka.KafkaTopics
import com.loopers.kafka.ProductViewedPayload
import com.loopers.testcontainers.KafkaTestContainersConfig
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.apache.kafka.clients.admin.NewTopic
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.kafka.config.TopicBuilder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import java.util.UUID

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [RankingFlowE2ETest.TestApplication::class],
)
@ActiveProfiles("test")
class RankingFlowE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val kafkaIntegrationEventPublisher: KafkaIntegrationEventPublisher,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
    companion object {
        private val KOREA_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        @Suppress("unused")
        private val mysqlTestContainersConfig = MySqlTestContainersConfig()

        @Suppress("unused")
        private val kafkaTestContainersConfig = KafkaTestContainersConfig()

        @Suppress("unused")
        private val redisTestContainersConfig = RedisTestContainersConfig()

        init {
            TimeZone.setDefault(TimeZone.getTimeZone(KOREA_ZONE))
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("datasource.mysql-jpa.main.jdbc-url") {
                System.getProperty("datasource.mysql-jpa.main.jdbc-url")
            }
            registry.add("datasource.mysql-jpa.main.username") {
                System.getProperty("datasource.mysql-jpa.main.username")
            }
            registry.add("datasource.mysql-jpa.main.password") {
                System.getProperty("datasource.mysql-jpa.main.password")
            }

            registry.add("spring.kafka.bootstrap-servers") {
                System.getProperty("spring.kafka.bootstrap-servers")
            }
            registry.add("BOOTSTRAP_SERVERS") {
                System.getProperty("BOOTSTRAP_SERVERS")
            }
            registry.add("spring.kafka.consumer.auto-offset-reset") { "earliest" }
            registry.add("spring.kafka.listener.missing-topics-fatal") { "false" }
            registry.add("demo-kafka.test.topic-name") { "demo.internal.topic-v1" }

            registry.add("datasource.redis.database") {
                System.getProperty("datasource.redis.database")
            }
            registry.add("datasource.redis.master.host") {
                System.getProperty("datasource.redis.master.host")
            }
            registry.add("datasource.redis.master.port") {
                System.getProperty("datasource.redis.master.port")
            }
            registry.add("datasource.redis.replicas[0].host") {
                System.getProperty("datasource.redis.replicas[0].host")
            }
            registry.add("datasource.redis.replicas[0].port") {
                System.getProperty("datasource.redis.replicas[0].port")
            }

            registry.add("queue.experiment.scheduler.enabled") { "false" }
            registry.add("ranking.carry-over.scheduler.enabled") { "false" }
        }
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
        databaseCleanUp.truncateAllTables()
    }

    @Test
    fun `상품_조회_이벤트가_랭킹_API와_상품_상세_순위까지_반영된다`() {
        val brand = saveBrand()
        val topProduct = saveProduct(brandId = requireNotNull(brand.id), name = "인기 상품")
        val lowProduct = saveProduct(brandId = requireNotNull(brand.id), name = "일반 상품")
        val today = DATE_FORMATTER.format(ZonedDateTime.now(KOREA_ZONE))

        repeat(3) {
            assertThat(getProductDetail(requireNotNull(topProduct.id)).statusCode).isEqualTo(HttpStatus.OK)
        }
        repeat(1) {
            assertThat(getProductDetail(requireNotNull(lowProduct.id)).statusCode).isEqualTo(HttpStatus.OK)
        }

        eventually {
            val rankings = getRankingPage(today)
            assertThat(rankings.map { it.productId })
                .containsExactly(requireNotNull(topProduct.id), requireNotNull(lowProduct.id))
            assertThat(rankings[0].score).isGreaterThan(rankings[1].score)
        }

        val rankedDetail = getProductDetail(requireNotNull(topProduct.id))

        assertThat(rankedDetail.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(rankedDetail.body?.data?.ranking).isEqualTo(1L)
    }

    @Test
    fun `이전_날짜로_발행된_이벤트도_해당_날짜_랭킹으로_조회된다`() {
        val brand = saveBrand(name = "날짜 브랜드")
        val product = saveProduct(brandId = requireNotNull(brand.id), name = "어제 인기 상품")
        val yesterdayOccurredAt = ZonedDateTime.now(KOREA_ZONE).minusDays(1).withHour(23).withMinute(59).withSecond(0).withNano(0)
        val yesterday = DATE_FORMATTER.format(yesterdayOccurredAt)

        kafkaIntegrationEventPublisher.publish(
            topic = KafkaTopics.CATALOG_EVENTS,
            event = IntegrationEvent(
                eventId = "test-product-viewed:${UUID.randomUUID()}",
                eventType = "ProductViewed",
                aggregateType = "product",
                aggregateId = requireNotNull(product.id).toString(),
                key = requireNotNull(product.id).toString(),
                version = yesterdayOccurredAt.toInstant().toEpochMilli(),
                occurredAt = yesterdayOccurredAt,
                payload = ProductViewedPayload(
                    productId = requireNotNull(product.id),
                    memberId = null,
                ),
            ),
        )

        eventually {
            val rankings = getRankingPage(yesterday)
            assertThat(rankings).hasSize(1)
            assertThat(rankings.single().productId).isEqualTo(requireNotNull(product.id))
        }
    }

    private fun getProductDetail(productId: Long) =
        testRestTemplate.exchange(
            "/api/v1/products/$productId",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.DetailResponse>>() {},
        )

    private fun getRankingPage(date: String): List<RankingV1Dto.RankedProductResponse> {
        return testRestTemplate.exchange(
            "/api/v1/rankings?date=$date&size=20&page=1",
            HttpMethod.GET,
            HttpEntity.EMPTY,
            object : ParameterizedTypeReference<ApiResponse<List<RankingV1Dto.RankedProductResponse>>>() {},
        ).let { response ->
            assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
            response.body?.data.orEmpty()
        }
    }

    private fun saveBrand(name: String = "랭킹 브랜드"): BrandEntity {
        return brandJpaRepository.save(
            BrandEntity(
                name = name,
                status = "ACTIVE",
            ),
        )
    }

    private fun saveProduct(
        brandId: Long,
        name: String,
    ): ProductEntity {
        return productJpaRepository.save(
            ProductEntity(
                brandId = brandId,
                name = name,
                price = 10_000L,
                description = "$name 설명",
                stock = 100,
                status = "SELLING",
            ),
        )
    }

    private fun eventually(
        timeout: Duration = Duration.ofSeconds(10),
        interval: Duration = Duration.ofMillis(200),
        assertion: () -> Unit,
    ) {
        val deadline = System.nanoTime() + timeout.toNanos()
        var lastError: Throwable? = null

        while (System.nanoTime() < deadline) {
            try {
                assertion()
                return
            } catch (throwable: Throwable) {
                lastError = throwable
                Thread.sleep(interval.toMillis())
            }
        }

        throw AssertionError("condition not met within $timeout", lastError)
    }

    @SpringBootApplication(
        scanBasePackages = [
            "com.loopers.application",
            "com.loopers.config",
            "com.loopers.domain",
            "com.loopers.infrastructure",
            "com.loopers.interfaces",
            "com.loopers.support",
            "com.loopers.utils",
        ],
    )
    @ConfigurationPropertiesScan("com.loopers")
    @Import(KafkaTopicTestConfig::class)
    class TestApplication

    @TestConfiguration
    class KafkaTopicTestConfig {
        @Bean
        fun catalogEventsTopic(): NewTopic =
            TopicBuilder.name(KafkaTopics.CATALOG_EVENTS).partitions(1).replicas(1).build()

        @Bean
        fun orderEventsTopic(): NewTopic =
            TopicBuilder.name(KafkaTopics.ORDER_EVENTS).partitions(1).replicas(1).build()

        @Bean
        fun couponIssueRequestsTopic(): NewTopic =
            TopicBuilder.name(KafkaTopics.COUPON_ISSUE_REQUESTS).partitions(1).replicas(1).build()

        @Bean
        fun orderEntryEventsTopic(): NewTopic =
            TopicBuilder.name(KafkaTopics.ORDER_ENTRY_EVENTS).partitions(1).replicas(1).build()

        @Bean
        fun orderEntryAdmissionEventsTopic(): NewTopic =
            TopicBuilder.name(KafkaTopics.ORDER_ENTRY_ADMISSION_EVENTS).partitions(1).replicas(1).build()

        @Bean
        fun demoTopic(): NewTopic =
            TopicBuilder.name("demo.internal.topic-v1").partitions(1).replicas(1).build()
    }
}
