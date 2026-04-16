package com.loopers.job.ranking

import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate

/**
 * 벤치마크용 product_metrics 데이터 시딩.
 *
 * productCount개 상품 × days일치 데이터를 JdbcTemplate.batchUpdate로 INSERT한다.
 * JPA saveAll 대신 JDBC batch를 쓰는 이유: 대량 INSERT 시 Hibernate flush 비용 회피.
 */
object ProductMetricsSeeder {

    private const val BATCH_SIZE = 1000

    fun seed(
        jdbcTemplate: JdbcTemplate,
        productCount: Int,
        startDate: LocalDate,
        days: Int,
        seed: Long = 42L,
    ): Int {
        val random = java.util.Random(seed)
        var totalRows = 0

        val sql = """
            INSERT INTO product_metrics (product_id, metric_date, like_count, order_count, view_count,
                                         last_event_at, version, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, NOW(), 0, NOW(), NOW())
        """.trimIndent()

        val batch = mutableListOf<Array<Any>>()

        for (day in 0 until days) {
            val date = startDate.plusDays(day.toLong())
            for (productId in 1..productCount) {
                val likeCount = random.nextInt(20).toLong()
                val orderCount = random.nextInt(10).toLong()
                val viewCount = random.nextInt(200).toLong()

                batch.add(arrayOf(productId.toLong(), date.toString(), likeCount, orderCount, viewCount))
                totalRows++

                if (batch.size >= BATCH_SIZE) {
                    flushBatch(jdbcTemplate, sql, batch)
                    batch.clear()
                }
            }
        }

        if (batch.isNotEmpty()) {
            flushBatch(jdbcTemplate, sql, batch)
        }

        return totalRows
    }

    private fun flushBatch(jdbcTemplate: JdbcTemplate, sql: String, batch: List<Array<Any>>) {
        jdbcTemplate.batchUpdate(sql, batch.map { row ->
            arrayOf(row[0], row[1], row[2], row[3], row[4])
        })
    }
}
