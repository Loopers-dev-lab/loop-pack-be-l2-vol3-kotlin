package com.loopers.infrastructure.product

import com.loopers.testcontainers.MySqlTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ProductQueryPerformanceTest {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeAll
    fun setUp() {
        seedBrands()
        seedProducts()
    }

    @Test
    @Order(1)
    fun `인덱스 적용 상태에서 EXPLAIN 분석 - 브랜드별 인기순`() {
        val explain = explain(
            """
            SELECT * FROM product
            WHERE brand_id = 1 AND deleted_at IS NULL
            ORDER BY like_count DESC
            LIMIT 20
            """,
        )
        println("\n=== 브랜드별 인기순 (인덱스 적용) ===")
        printExplain(explain)

        assertThat(explain["key"]).isNotNull
        assertThat(explain["type"]).isNotEqualTo("ALL")
    }

    @Test
    @Order(2)
    fun `인덱스 적용 상태에서 EXPLAIN 분석 - 브랜드별 최신순`() {
        val explain = explain(
            """
            SELECT * FROM product
            WHERE brand_id = 1 AND deleted_at IS NULL
            ORDER BY created_at DESC
            LIMIT 20
            """,
        )
        println("\n=== 브랜드별 최신순 (인덱스 적용) ===")
        printExplain(explain)

        assertThat(explain["key"]).isNotNull
    }

    @Test
    @Order(3)
    fun `인덱스 적용 상태에서 EXPLAIN 분석 - 브랜드별 가격순`() {
        val explain = explain(
            """
            SELECT * FROM product
            WHERE brand_id = 1 AND deleted_at IS NULL
            ORDER BY price ASC
            LIMIT 20
            """,
        )
        println("\n=== 브랜드별 가격순 (인덱스 적용) ===")
        printExplain(explain)

        assertThat(explain["key"]).isNotNull
    }

    @Test
    @Order(4)
    fun `인덱스 적용 상태에서 EXPLAIN 분석 - 전체 인기순`() {
        val explain = explain(
            """
            SELECT * FROM product
            WHERE deleted_at IS NULL
            ORDER BY like_count DESC
            LIMIT 20
            """,
        )
        println("\n=== 전체 인기순 (인덱스 적용) ===")
        printExplain(explain)
    }

    @Test
    @Order(5)
    fun `인덱스 제거 후 EXPLAIN 분석 - 브랜드별 인기순`() {
        dropIndexes()

        val explain = explain(
            """
            SELECT * FROM product
            WHERE brand_id = 1 AND deleted_at IS NULL
            ORDER BY like_count DESC
            LIMIT 20
            """,
        )
        println("\n=== 브랜드별 인기순 (인덱스 없음) ===")
        printExplain(explain)

        assertThat(explain["key"]).isNull()
        assertThat(explain["type"]).isEqualTo("ALL")

        recreateIndexes()
    }

    @Test
    @Order(6)
    fun `인덱스 제거 후 EXPLAIN 분석 - 전체 인기순`() {
        dropIndexes()

        val explain = explain(
            """
            SELECT * FROM product
            WHERE deleted_at IS NULL
            ORDER BY like_count DESC
            LIMIT 20
            """,
        )
        println("\n=== 전체 인기순 (인덱스 없음) ===")
        printExplain(explain)

        assertThat(explain["type"]).isEqualTo("ALL")

        recreateIndexes()
    }

    @Test
    @Order(7)
    fun `실행 시간 비교 - 브랜드별 인기순`() {
        val withIndex = measureQueryTime(
            """
            SELECT * FROM product
            WHERE brand_id = 1 AND deleted_at IS NULL
            ORDER BY like_count DESC
            LIMIT 20
            """,
        )

        dropIndexes()
        val withoutIndex = measureQueryTime(
            """
            SELECT * FROM product
            WHERE brand_id = 1 AND deleted_at IS NULL
            ORDER BY like_count DESC
            LIMIT 20
            """,
        )
        recreateIndexes()

        println("\n=== 실행 시간 비교: 브랜드별 인기순 ===")
        println("인덱스 있음: ${withIndex}ms")
        println("인덱스 없음: ${withoutIndex}ms")
        println("개선율: ${((withoutIndex - withIndex).toDouble() / withoutIndex * 100).toInt()}%")
    }

    private fun explain(sql: String): Map<String, Any?> {
        val result = jdbcTemplate.queryForMap("EXPLAIN $sql")
        return result
    }

    private fun printExplain(explain: Map<String, Any?>) {
        println("  type: ${explain["type"]}")
        println("  key: ${explain["key"]}")
        println("  rows: ${explain["rows"]}")
        println("  filtered: ${explain["filtered"]}")
        println("  Extra: ${explain["Extra"]}")
    }

    private fun measureQueryTime(sql: String): Long {
        // 워밍업
        repeat(3) { jdbcTemplate.queryForList(sql) }

        val times = (1..5).map {
            val start = System.currentTimeMillis()
            jdbcTemplate.queryForList(sql)
            System.currentTimeMillis() - start
        }
        return times.sorted()[2] // 중앙값
    }

    private fun dropIndexes() {
        tryExecute("ALTER TABLE product DROP INDEX idx_product_brand_created")
        tryExecute("ALTER TABLE product DROP INDEX idx_product_brand_like_count")
        tryExecute("ALTER TABLE product DROP INDEX idx_product_brand_price")
    }

    private fun tryExecute(sql: String) {
        try {
            jdbcTemplate.execute(sql)
        } catch (_: Exception) {
        }
    }

    private fun recreateIndexes() {
        tryExecute("CREATE INDEX idx_product_brand_created ON product(brand_id, created_at)")
        tryExecute("CREATE INDEX idx_product_brand_like_count ON product(brand_id, like_count)")
        tryExecute("CREATE INDEX idx_product_brand_price ON product(brand_id, price)")
    }

    private fun seedBrands() {
        val sql = StringBuilder("INSERT INTO brand (name, description, logo_url, status, created_at, updated_at) VALUES ")
        for (i in 1..BRAND_COUNT) {
            if (i > 1) sql.append(",")
            sql.append("('brand_$i', '브랜드 $i 설명', 'https://logo.com/$i.png', 'ACTIVE', NOW(), NOW())")
        }
        jdbcTemplate.execute(sql.toString())
    }

    private fun seedProducts() {
        val batchSize = 5000
        var inserted = 0
        while (inserted < PRODUCT_COUNT) {
            val currentBatch = minOf(batchSize, PRODUCT_COUNT - inserted)
            val sql = StringBuilder("INSERT INTO product (brand_id, name, description, price, stock, thumbnail_url, status, like_count, created_at, updated_at) VALUES ")
            for (i in 1..currentBatch) {
                val seq = inserted + i
                val brandId = (seq % BRAND_COUNT) + 1
                val price = (seq % 100 + 1) * 1000L
                val likeCount = (seq * 7 + 13) % 10000
                if (i > 1) sql.append(",")
                sql.append(
                    "($brandId, '상품_$seq', '상품 $seq 설명', $price, 100, " +
                        "'https://thumb.com/$seq.png', 'ACTIVE', $likeCount, " +
                        "DATE_ADD('2024-01-01', INTERVAL $seq SECOND), NOW())",
                )
            }
            jdbcTemplate.execute(sql.toString())
            inserted += currentBatch
        }
    }

    companion object {
        private const val BRAND_COUNT = 50
        private const val PRODUCT_COUNT = 100_000
    }
}
