package com.loopers.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.PreparedStatement
import kotlin.random.Random

@Profile("local")
@Component
class ProductSeedRunner(
    private val jdbcTemplate: JdbcTemplate,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BRAND_COUNT = 20
        private const val PRODUCT_COUNT = 100_000
        private const val LIKE_BATCH_SIZE = 1_000
        private const val PRODUCT_BATCH_SIZE = 1_000
    }

    override fun run(args: ApplicationArguments) {
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM product", Long::class.java) ?: 0
        if (count > 0) {
            log.info("[ProductSeedRunner] 이미 상품 데이터가 존재합니다 ({}건). 시드 스킵.", count)
            return
        }

        log.info("[ProductSeedRunner] 시드 데이터 생성 시작...")
        val startTime = System.currentTimeMillis()

        seedBrands()
        seedProducts()
        seedProductLikes()

        val elapsed = System.currentTimeMillis() - startTime
        log.info("[ProductSeedRunner] 시드 데이터 생성 완료 ({}ms)", elapsed)
    }

    private fun seedBrands() {
        val existingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM brand", Long::class.java) ?: 0
        if (existingCount >= BRAND_COUNT) return

        val sql = """
            INSERT INTO brand (name, description, image_url, status, created_at, updated_at)
            VALUES (?, ?, ?, 'ACTIVE', NOW(), NOW())
        """.trimIndent()
        jdbcTemplate.batchUpdate(
            sql,
            object : BatchPreparedStatementSetter {
                override fun setValues(ps: PreparedStatement, i: Int) {
                    ps.setString(1, "Brand_${i + 1}")
                    ps.setString(2, "Brand ${i + 1} 설명")
                    ps.setString(3, "https://example.com/brand_${i + 1}.jpg")
                }

                override fun getBatchSize() = BRAND_COUNT
            },
        )
        log.info("[ProductSeedRunner] 브랜드 {}개 생성 완료", BRAND_COUNT)
    }

    private fun seedProducts() {
        val brandIds = jdbcTemplate.queryForList("SELECT id FROM brand ORDER BY id", Long::class.java)
        val random = Random(42)

        val sql = """
            INSERT INTO product (brand_id, name, description, price, stock_quantity, like_count, image_url, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', NOW(), NOW())
        """.trimIndent()

        (0 until PRODUCT_COUNT).chunked(PRODUCT_BATCH_SIZE).forEachIndexed { chunkIdx, chunk ->
            jdbcTemplate.batchUpdate(
                sql,
                object : BatchPreparedStatementSetter {
                    override fun setValues(ps: PreparedStatement, i: Int) {
                        val idx = chunk[i]
                        ps.setLong(1, brandIds[idx % brandIds.size])
                        ps.setString(2, "Product_$idx")
                        ps.setString(3, "Product $idx 설명")
                        ps.setLong(4, random.nextLong(1_000, 500_000))
                        ps.setInt(5, random.nextInt(0, 1_000))
                        ps.setInt(6, 0)
                        ps.setString(7, "https://example.com/product_$idx.jpg")
                    }

                    override fun getBatchSize() = chunk.size
                },
            )

            if ((chunkIdx + 1) % 10 == 0) {
                log.info("[ProductSeedRunner] 상품 {}건 생성 완료", (chunkIdx + 1) * PRODUCT_BATCH_SIZE)
            }
        }
        log.info("[ProductSeedRunner] 상품 총 {}건 생성 완료", PRODUCT_COUNT)
    }

    private fun seedProductLikes() {
        val productIds = jdbcTemplate.queryForList("SELECT id FROM product ORDER BY id", Long::class.java)
        val random = Random(42)

        val sql = "INSERT IGNORE INTO product_like (product_id, member_id, created_at, updated_at) VALUES (?, ?, NOW(), NOW())"

        var totalLikes = 0
        val likes = mutableListOf<Pair<Long, Long>>()

        for (productId in productIds) {
            val likeCount = if (random.nextFloat() < 0.1f) {
                random.nextInt(50, 200)
            } else {
                random.nextInt(0, 10)
            }
            repeat(likeCount) { j ->
                likes.add(productId to (j + 1).toLong())
            }
        }

        likes.chunked(LIKE_BATCH_SIZE).forEachIndexed { chunkIdx, chunk ->
            jdbcTemplate.batchUpdate(
                sql,
                object : BatchPreparedStatementSetter {
                    override fun setValues(ps: PreparedStatement, i: Int) {
                        ps.setLong(1, chunk[i].first)
                        ps.setLong(2, chunk[i].second)
                    }

                    override fun getBatchSize() = chunk.size
                },
            )
            totalLikes += chunk.size

            if ((chunkIdx + 1) % 100 == 0) {
                log.info("[ProductSeedRunner] 좋아요 {}건 생성 완료", totalLikes)
            }
        }
        log.info("[ProductSeedRunner] 좋아요 총 {}건 생성 완료", totalLikes)
    }
}
