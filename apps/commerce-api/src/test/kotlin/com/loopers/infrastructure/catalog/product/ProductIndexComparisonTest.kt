package com.loopers.infrastructure.catalog.product

import com.loopers.domain.catalog.product.model.Product
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.sql.Connection
import javax.sql.DataSource
import kotlin.math.pow
import kotlin.random.Random

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductIndexComparisonTest @Autowired constructor(
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val dataSource: DataSource,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val random = Random(42)

    /**
     * 멱법칙(power law) 분포로 좋아요 수를 생성한다.
     * 소수의 상품에 좋아요가 집중되는 실제 데이터 분포를 모사한다.
     */
    private fun powerLawLikeCount(): Int {
        val u = random.nextDouble()
        val maxLike = 10_000.0
        return (maxLike * (1 - u).pow(3.0)).toInt().coerceIn(0, 10_000)
    }

    private fun randomStatus(): Product.ProductStatus {
        val roll = random.nextInt(100)
        return when {
            roll < 90 -> Product.ProductStatus.ON_SALE
            roll < 98 -> Product.ProductStatus.SOLD_OUT
            else -> Product.ProductStatus.HIDDEN
        }
    }

    @BeforeEach
    fun seedData() {
        // 브랜드 50~100개, 브랜드별 상품 수 편차 (10 ~ 5000개)
        val brandCount = random.nextInt(50, 101)
        val brandIds = (1L..brandCount.toLong()).toList()

        // 브랜드별 상품 비율(가중치)을 미리 정해서 편차를 만든다
        val brandWeights = brandIds.map { random.nextInt(10, 5001) }
        val totalWeight = brandWeights.sum().toDouble()

        val allEntities = mutableListOf<ProductEntity>()
        val totalCount = 100_000
        var remaining = totalCount

        brandIds.forEachIndexed { index, brandId ->
            val count = if (index == brandIds.lastIndex) {
                remaining
            } else {
                (totalCount * brandWeights[index] / totalWeight).toInt().coerceAtLeast(1)
            }
            remaining -= count

            repeat(count) {
                val price = BigDecimal(random.nextLong(1_000L, 1_000_001L))
                allEntities.add(
                    ProductEntity(
                        refBrandId = brandId,
                        name = "상품-$brandId-$it",
                        price = price,
                        stock = random.nextInt(0, 1000),
                        status = randomStatus(),
                        likeCount = powerLawLikeCount(),
                    ),
                )
            }
        }

        // 1000개씩 배치 저장
        allEntities.chunked(1_000).forEach { chunk ->
            productJpaRepository.saveAll(chunk)
        }
        productJpaRepository.flush()
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    /**
     * EXPLAIN 결과 한 행에서 비교에 필요한 필드를 추출한다.
     *
     * MySQL EXPLAIN 컬럼 순서:
     * id, select_type, table, partitions, type, possible_keys, key, key_len, ref, rows, filtered, Extra
     * ResultSet은 1-based이므로: type=5, rows=10, Extra=12
     */
    private data class ExplainRow(
        val type: String,
        val rows: String,
        val extra: String,
    )

    private fun explain(sql: String): List<ExplainRow> {
        return dataSource.connection.use { conn ->
            conn.createStatement().executeQuery("EXPLAIN $sql").use { rs ->
                val results = mutableListOf<ExplainRow>()
                while (rs.next()) {
                    results.add(
                        ExplainRow(
                            type = rs.getString("type") ?: "",
                            rows = rs.getString("rows") ?: "",
                            extra = rs.getString("Extra") ?: "",
                        ),
                    )
                }
                results
            }
        }
    }

    private fun executeNativeDdl(sql: String) {
        dataSource.connection.use { conn: Connection ->
            conn.createStatement().use { stmt ->
                stmt.execute(sql)
            }
        }
    }

    /**
     * 인덱스가 존재하는 경우에만 DROP한다.
     * MySQL 5.7에서는 DROP INDEX IF EXISTS 문법을 지원하지 않으므로
     * information_schema로 존재 여부를 먼저 확인한다.
     */
    private fun dropIndexIfExists(indexName: String, tableName: String) {
        val exists = dataSource.connection.use { conn ->
            conn.createStatement().executeQuery(
                """
                SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = '$tableName'
                  AND INDEX_NAME = '$indexName'
                """.trimIndent(),
            ).use { rs ->
                rs.next() && rs.getInt(1) > 0
            }
        }
        if (exists) {
            executeNativeDdl("DROP INDEX $indexName ON $tableName")
        }
    }

    @Nested
    @DisplayName("인덱스 AS-IS / TO-BE 비교")
    inner class IndexComparison {

        @Test
        @DisplayName("인덱스 유무에 따른 EXPLAIN 결과 비교")
        fun compareExplainWithAndWithoutIndexes() {
            // 비교할 4개 쿼리 정의
            data class QueryCase(val label: String, val sql: String)

            val queries = listOf(
                QueryCase(
                    label = "브랜드 필터 + 좋아요 정렬",
                    sql = "SELECT * FROM products WHERE deleted_at IS NULL AND status != 'HIDDEN' AND ref_brand_id = 1 ORDER BY like_count DESC LIMIT 20",
                ),
                QueryCase(
                    label = "브랜드 필터 + 가격 정렬",
                    sql = "SELECT * FROM products WHERE deleted_at IS NULL AND status != 'HIDDEN' AND ref_brand_id = 1 ORDER BY price ASC LIMIT 20",
                ),
                QueryCase(
                    label = "최신순 전체 조회",
                    sql = "SELECT * FROM products WHERE deleted_at IS NULL AND status != 'HIDDEN' ORDER BY created_at DESC LIMIT 20",
                ),
                QueryCase(
                    label = "좋아요 내림차순 깊은 페이지",
                    sql = "SELECT * FROM products WHERE deleted_at IS NULL AND status != 'HIDDEN' ORDER BY like_count DESC LIMIT 20 OFFSET 10000",
                ),
            )

            // 1. AS-IS: 커스텀 인덱스 DROP
            // DDL은 MySQL에서 implicit commit을 유발하므로 @Transactional 없이 실행한다
            dropIndexIfExists("idx_products_active_like_count", "products")
            dropIndexIfExists("idx_products_active_created_at", "products")
            dropIndexIfExists("idx_products_active_price", "products")

            // 2. AS-IS EXPLAIN 수집
            val asIsResults = queries.map { query ->
                query.label to explain(query.sql).first()
            }

            // 3. TO-BE: 인덱스 재생성
            executeNativeDdl(
                "CREATE INDEX idx_products_active_like_count ON products (deleted_at, status, like_count DESC)",
            )
            executeNativeDdl(
                "CREATE INDEX idx_products_active_created_at ON products (deleted_at, status, created_at DESC)",
            )
            executeNativeDdl(
                "CREATE INDEX idx_products_active_price ON products (deleted_at, status, price ASC)",
            )

            // 4. TO-BE EXPLAIN 수집
            val toBeResults = queries.map { query ->
                query.label to explain(query.sql).first()
            }

            // 5. 비교 로그 출력 (표 형태)
            log.info("=== 인덱스 비교 결과 ===")
            val fmt = "| %-30s | %-12s | %-10s | %-30s | %-12s | %-10s | %-30s |"
            val header = String.format(
                fmt,
                "쿼리",
                "AS-IS type",
                "AS-IS rows",
                "AS-IS Extra",
                "TO-BE type",
                "TO-BE rows",
                "TO-BE Extra",
            )
            log.info(header)
            log.info("-".repeat(header.length))

            queries.indices.forEach { i ->
                val (label, asIs) = asIsResults[i]
                val (_, toBe) = toBeResults[i]
                log.info(
                    String.format(
                        fmt,
                        label,
                        asIs.type,
                        asIs.rows,
                        asIs.extra,
                        toBe.type,
                        toBe.rows,
                        toBe.extra,
                    ),
                )
            }

            // 6. assertion: TO-BE에서 ALL 타입이 없어야 함
            val toBeTypes = toBeResults.map { (_, row) -> row.type }
            assertThat(toBeTypes)
                .withFailMessage("TO-BE 인덱스 적용 후에도 풀 스캔(ALL)이 발생한 쿼리가 있습니다: $toBeResults")
                .noneMatch { it == "ALL" }
        }
    }
}
