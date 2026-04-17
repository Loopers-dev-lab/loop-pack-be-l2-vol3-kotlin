package com.loopers.infrastructure.metric

import com.loopers.domain.metric.ProductMetricDaily
import com.loopers.domain.metric.ProductMetricDailyRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

@DisplayName("ProductMetricDailyRepository integration")
@SpringBootTest(classes = [ProductMetricDailyRepositoryIntegrationTest.TestApplication::class])
@ActiveProfiles("test")
@TestPropertySource(
    properties = [
        "datasource.mysql-jpa.main.jdbc-url=jdbc:mysql://localhost:3306/loopers",
        "datasource.mysql-jpa.main.driver-class-name=com.mysql.cj.jdbc.Driver",
        "datasource.mysql-jpa.main.username=application",
        "datasource.mysql-jpa.main.password=application",
        "datasource.redis.master.host=localhost",
        "datasource.redis.master.port=6379",
        "datasource.redis.replicas[0].host=localhost",
        "datasource.redis.replicas[0].port=6380",
    ],
)
class ProductMetricDailyRepositoryIntegrationTest
@Autowired
constructor(
    private val productMetricDailyRepository: ProductMetricDailyRepository,
    private val productMetricDailyJpaRepository: ProductMetricDailyJpaRepository,
) {
    private val today = LocalDate.of(2026, 4, 16)

    @BeforeEach
    fun setUp() {
        productMetricDailyJpaRepository.deleteAll()
    }

    @AfterEach
    fun tearDown() {
        productMetricDailyJpaRepository.deleteAll()
    }

    @Nested
    @DisplayName("save")
    inner class Save {

        @Test
        @DisplayName("신규 레코드의 모든 필드가 저장/조회된다")
        fun save_newRecord_allFieldsRoundTrip() {
            val daily = ProductMetricDaily.register(productId = 1L, metricDate = today)
                .recordView()
                .recordView()
                .recordLike()
                .recordOrder(quantity = 3, amount = 10_000L)

            productMetricDailyRepository.save(daily)

            val loaded = productMetricDailyRepository.findByProductIdAndMetricDate(1L, today)!!
            assertAll(
                { assertThat(loaded.productId).isEqualTo(1L) },
                { assertThat(loaded.metricDate).isEqualTo(today) },
                { assertThat(loaded.viewCount).isEqualTo(daily.viewCount) },
                { assertThat(loaded.likeCount).isEqualTo(daily.likeCount) },
                { assertThat(loaded.unitsSold).isEqualTo(daily.unitsSold) },
                { assertThat(loaded.salesAmount).isEqualTo(daily.salesAmount) },
                { assertThat(loaded.orderScore).isEqualTo(daily.orderScore) },
            )
        }

        @Test
        @DisplayName("동일 (productId, metricDate)에 대해 upsert한다")
        fun save_upsert() {
            val daily = ProductMetricDaily.register(productId = 1L, metricDate = today).recordView()
            productMetricDailyRepository.save(daily)

            val updated = productMetricDailyRepository
                .findByProductIdAndMetricDate(1L, today)!!
                .recordView()
            productMetricDailyRepository.save(updated)

            val result = productMetricDailyRepository.findByProductIdAndMetricDate(1L, today)!!
            assertThat(result.viewCount).isEqualTo(2)
            assertThat(productMetricDailyJpaRepository.count()).isEqualTo(1)
        }

        @Test
        @DisplayName("같은 상품이라도 날짜가 다르면 별도 레코드로 저장한다")
        fun save_differentDates() {
            val yesterday = today.minusDays(1)
            productMetricDailyRepository.save(
                ProductMetricDaily.register(productId = 1L, metricDate = today).recordView(),
            )
            productMetricDailyRepository.save(
                ProductMetricDaily.register(productId = 1L, metricDate = yesterday).recordView().recordView(),
            )

            val todayResult = productMetricDailyRepository.findByProductIdAndMetricDate(1L, today)!!
            val yesterdayResult = productMetricDailyRepository.findByProductIdAndMetricDate(1L, yesterday)!!

            assertAll(
                { assertThat(todayResult.viewCount).isEqualTo(1) },
                { assertThat(yesterdayResult.viewCount).isEqualTo(2) },
                { assertThat(productMetricDailyJpaRepository.count()).isEqualTo(2) },
            )
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ConfigurationPropertiesScan("com.loopers")
    @ComponentScan(
        basePackages = [
            "com.loopers.application",
            "com.loopers.domain",
            "com.loopers.config",
            "com.loopers.infrastructure",
            "com.loopers.interfaces",
            "com.loopers.support",
        ],
    )
    class TestApplication
}
