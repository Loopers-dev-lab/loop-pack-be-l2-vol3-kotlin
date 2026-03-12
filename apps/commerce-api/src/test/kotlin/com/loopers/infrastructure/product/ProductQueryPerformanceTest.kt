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
        jdbcTemplate.execute("ANALYZE TABLE brand")
        jdbcTemplate.execute("ANALYZE TABLE product")

        val brandCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM brand", Long::class.java)
        val productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product", Long::class.java)
        println("\n=== 시드 데이터: brand $brandCount 건, product $productCount 건 ===")
        assertThat(productCount).isGreaterThanOrEqualTo(PRODUCT_COUNT.toLong())
    }

    @Test
    @Order(1)
    fun `인덱스 적용 상태에서 EXPLAIN 분석`() {
        println("\n========== EXPLAIN (인덱스 적용) ==========")

        println("\n--- 브랜드별 인기순 ---")
        printExplain(explainBrandLikeCount())

        println("\n--- 브랜드별 최신순 ---")
        printExplain(explainBrandCreatedAt())

        println("\n--- 브랜드별 가격순 ---")
        printExplain(explainBrandPrice())

        println("\n--- 전체 인기순 ---")
        printExplain(explainAllLikeCount())
    }

    @Test
    @Order(2)
    fun `인덱스 제거 후 EXPLAIN 분석`() {
        dropIndexes()
        jdbcTemplate.execute("ANALYZE TABLE product")

        println("\n========== EXPLAIN (인덱스 없음) ==========")

        println("\n--- 브랜드별 인기순 ---")
        val explain = explainBrandLikeCount()
        printExplain(explain)
        assertThat(explain["type"]).isEqualTo("ALL")

        println("\n--- 브랜드별 최신순 ---")
        printExplain(explainBrandCreatedAt())

        println("\n--- 전체 인기순 ---")
        printExplain(explainAllLikeCount())

        recreateIndexes()
        jdbcTemplate.execute("ANALYZE TABLE product")
    }

    @Test
    @Order(3)
    fun `실행 시간 비교 - 브랜드별 인기순`() {
        val query = """
            SELECT * FROM product
            WHERE brand_id = 1 AND deleted_at IS NULL
            ORDER BY like_count DESC
            LIMIT 20
        """

        val withIndex = measureQueryTime(query)

        dropIndexes()
        val withoutIndex = measureQueryTime(query)
        recreateIndexes()

        println("\n========== 실행 시간 비교: 브랜드별 인기순 ==========")
        println("인덱스 있음: ${withIndex}ms")
        println("인덱스 없음: ${withoutIndex}ms")
        if (withoutIndex > 0) {
            println("개선율: ${((withoutIndex - withIndex).toDouble() / withoutIndex * 100).toInt()}%")
        }
    }

    @Test
    @Order(4)
    fun `실행 시간 비교 - 전체 인기순`() {
        val query = """
            SELECT * FROM product
            WHERE deleted_at IS NULL
            ORDER BY like_count DESC
            LIMIT 20
        """

        val withIndex = measureQueryTime(query)

        dropIndexes()
        val withoutIndex = measureQueryTime(query)
        recreateIndexes()

        println("\n========== 실행 시간 비교: 전체 인기순 ==========")
        println("인덱스 있음: ${withIndex}ms")
        println("인덱스 없음: ${withoutIndex}ms")
    }

    private fun explainBrandLikeCount() = explain(
        "SELECT * FROM product WHERE brand_id = 1 AND deleted_at IS NULL ORDER BY like_count DESC LIMIT 20",
    )

    private fun explainBrandCreatedAt() = explain(
        "SELECT * FROM product WHERE brand_id = 1 AND deleted_at IS NULL ORDER BY created_at DESC LIMIT 20",
    )

    private fun explainBrandPrice() = explain(
        "SELECT * FROM product WHERE brand_id = 1 AND deleted_at IS NULL ORDER BY price ASC LIMIT 20",
    )

    private fun explainAllLikeCount() = explain(
        "SELECT * FROM product WHERE deleted_at IS NULL ORDER BY like_count DESC LIMIT 20",
    )

    private fun explain(sql: String): Map<String, Any?> {
        return jdbcTemplate.queryForMap("EXPLAIN $sql")
    }

    private fun printExplain(explain: Map<String, Any?>) {
        println("  type: ${explain["type"]}, key: ${explain["key"]}, rows: ${explain["rows"]}, Extra: ${explain["Extra"]}")
    }

    private fun measureQueryTime(sql: String): Long {
        repeat(3) { jdbcTemplate.queryForList(sql) }
        val times = (1..5).map {
            val start = System.currentTimeMillis()
            jdbcTemplate.queryForList(sql)
            System.currentTimeMillis() - start
        }
        return times.sorted()[2]
    }

    private fun dropIndexes() {
        tryExecute("ALTER TABLE product DROP INDEX idx_product_brand_created")
        tryExecute("ALTER TABLE product DROP INDEX idx_product_brand_like_count")
        tryExecute("ALTER TABLE product DROP INDEX idx_product_brand_price")
    }

    private fun recreateIndexes() {
        tryExecute("CREATE INDEX idx_product_brand_created ON product(brand_id, created_at)")
        tryExecute("CREATE INDEX idx_product_brand_like_count ON product(brand_id, like_count)")
        tryExecute("CREATE INDEX idx_product_brand_price ON product(brand_id, price)")
    }

    private fun tryExecute(sql: String) {
        try {
            jdbcTemplate.execute(sql)
        } catch (_: Exception) {
        }
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
            val sql = StringBuilder(
                "INSERT INTO product (brand_id, name, description, price, stock, thumbnail_url, status, like_count, created_at, updated_at) VALUES ",
            )
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
