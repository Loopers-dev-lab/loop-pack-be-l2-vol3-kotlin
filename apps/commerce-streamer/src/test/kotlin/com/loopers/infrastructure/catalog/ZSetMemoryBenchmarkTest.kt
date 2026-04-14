package com.loopers.infrastructure.catalog

import com.loopers.config.redis.RedisConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.connection.RedisConnection
import org.springframework.data.redis.core.RedisTemplate

/**
 * ZSET 메모리 사용량 벤치마크.
 *
 * Redis INFO memory의 used_memory를 비교하여 ZSET N건의 실제 메모리 증가분을 측정한다.
 */
@SpringBootTest
class ZSetMemoryBenchmarkTest @Autowired constructor(
    @param:Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val KEY = "benchmark:zset:memory"
        private val PRODUCT_COUNTS = listOf(1_000, 10_000, 100_000)
    }

    @AfterEach
    fun tearDown() {
        redisTemplate.delete(KEY)
    }

    @DisplayName("ZSET 크기별 메모리 사용량 측정 (1K / 10K / 100K)")
    @Test
    fun measureZSetMemoryBySize() {
        val results = mutableListOf<String>()
        results.add("")
        results.add("=== ZSET Memory Benchmark Results ===")
        results.add("")
        results.add("| 상품 수 | ZSET 메모리 증가분 | 1건당 | 4 ZSET × 2일 |")
        results.add("|--------|-----------------|------|-------------|")

        for (count in PRODUCT_COUNTS) {
            redisTemplate.delete(KEY)

            // 측정 시작 — GC 등의 영향을 줄이기 위해 적재 전 memory 측정
            val memBefore = getUsedMemory()

            // 적재 — productId 1~N, score는 가중치 합산값 시뮬레이션
            val zSetOps = redisTemplate.opsForZSet()
            for (i in 1L..count) {
                zSetOps.add(KEY, i.toString(), i * 0.7 + (i % 100) * 0.2 + (i % 1000) * 0.1)
            }

            // 측정 완료
            val memAfter = getUsedMemory()
            val diffBytes = memAfter - memBefore

            val perEntry = if (diffBytes > 0) diffBytes.toDouble() / count else 0.0
            val eightKeys = diffBytes * 8 // 4 ZSET × 2일

            results.add(
                "| ${formatCount(count)} | ${formatBytes(diffBytes)} | " +
                    "${String.format("%.1f", perEntry)}B | ${formatBytes(eightKeys)} |",
            )

            redisTemplate.delete(KEY)
        }

        results.add("")
        results.add("※ INFO memory used_memory 차분 기준. Redis 내부 오버헤드(skiplist, dict) 포함.")
        results.add("※ 4 ZSET = rank:view/like/order/all, 2일 = TTL 2일치 키가 동시 존재하는 최대 경우.")
        results.add("")

        results.forEach { println(it) }
    }

    private fun getUsedMemory(): Long {
        return redisTemplate.execute { connection: RedisConnection ->
            val info = connection.serverCommands().info("memory")
            val usedMemory = info?.getProperty("used_memory")?.toLongOrNull() ?: 0L
            usedMemory
        } ?: 0L
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes < 0 -> "N/A"
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${String.format("%.1f", bytes / 1024.0)}KB"
        else -> "${String.format("%.1f", bytes / 1024.0 / 1024.0)}MB"
    }

    private fun formatCount(count: Int): String = when {
        count >= 1000 -> "${count / 1000}K"
        else -> count.toString()
    }
}
