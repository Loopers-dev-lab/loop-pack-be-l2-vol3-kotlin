package com.loopers.infrastructure.order

import com.loopers.domain.order.StockReservationRepository
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class StockReservationRedisRepositoryTest @Autowired constructor(
    private val stockReservationRepository: StockReservationRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val redisCleanUp: RedisCleanUp,
) {

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("reserve")
    inner class Reserve {

        @Test
        @DisplayName("재고가 충분하면 true를 반환한다")
        fun `재고가 충분하면 true를 반환한다`() {
            // given
            val productId = 1L
            stockReservationRepository.setStock(productId, 10)

            // when
            val result = stockReservationRepository.reserve(productId, 3)

            // then
            assertThat(result).isTrue()
            val remaining = redisTemplate.opsForValue().get("stock:$productId")
            assertThat(remaining).isEqualTo("7")
        }

        @Test
        @DisplayName("재고가 부족하면 false를 반환하고 재고를 복원한다")
        fun `재고가 부족하면 false를 반환하고 재고를 복원한다`() {
            // given
            val productId = 1L
            stockReservationRepository.setStock(productId, 2)

            // when
            val result = stockReservationRepository.reserve(productId, 5)

            // then
            assertThat(result).isFalse()
            val remaining = redisTemplate.opsForValue().get("stock:$productId")
            assertThat(remaining).isEqualTo("2")
        }

        @Test
        @DisplayName("재고가 정확히 0이 되면 선점에 성공한다")
        fun `재고가 정확히 0이 되면 선점에 성공한다`() {
            // given
            val productId = 1L
            stockReservationRepository.setStock(productId, 5)

            // when
            val result = stockReservationRepository.reserve(productId, 5)

            // then
            assertThat(result).isTrue()
            val remaining = redisTemplate.opsForValue().get("stock:$productId")
            assertThat(remaining).isEqualTo("0")
        }
    }

    @Nested
    @DisplayName("restore")
    inner class Restore {

        @Test
        @DisplayName("선점한 재고를 복원하면 수량이 증가한다")
        fun `선점한 재고를 복원하면 수량이 증가한다`() {
            // given
            val productId = 1L
            stockReservationRepository.setStock(productId, 10)
            stockReservationRepository.reserve(productId, 3)

            // when
            stockReservationRepository.restore(productId, 3)

            // then
            val remaining = redisTemplate.opsForValue().get("stock:$productId")
            assertThat(remaining).isEqualTo("10")
        }
    }

    @Nested
    @DisplayName("setStock")
    inner class SetStock {

        @Test
        @DisplayName("상품 재고를 Redis에 설정한다")
        fun `상품 재고를 Redis에 설정한다`() {
            // given
            val productId = 1L

            // when
            stockReservationRepository.setStock(productId, 50)

            // then
            val stored = redisTemplate.opsForValue().get("stock:$productId")
            assertThat(stored).isEqualTo("50")
        }
    }

    @Nested
    @DisplayName("동시성")
    inner class Concurrency {

        @Test
        @DisplayName("재고 1개에 10명이 동시 선점하면 1명만 성공한다")
        fun `재고 1개에 10명이 동시 선점하면 1명만 성공한다`() {
            // given
            val productId = 1L
            stockReservationRepository.setStock(productId, 1)
            val totalRequests = 10
            val executor = Executors.newFixedThreadPool(10)
            val latch = CountDownLatch(totalRequests)
            val successCount = AtomicInteger(0)

            // when
            repeat(totalRequests) {
                executor.submit {
                    try {
                        if (stockReservationRepository.reserve(productId, 1)) {
                            successCount.incrementAndGet()
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // then
            assertThat(successCount.get()).isEqualTo(1)
            val remaining = redisTemplate.opsForValue().get("stock:$productId")
            assertThat(remaining).isEqualTo("0")
        }

        @Test
        @DisplayName("재고 100개에 200명이 동시 선점하면 100명만 성공한다")
        fun `재고 100개에 200명이 동시 선점하면 100명만 성공한다`() {
            // given
            val productId = 1L
            stockReservationRepository.setStock(productId, 100)
            val totalRequests = 200
            val executor = Executors.newFixedThreadPool(32)
            val latch = CountDownLatch(totalRequests)
            val successCount = AtomicInteger(0)

            // when
            repeat(totalRequests) {
                executor.submit {
                    try {
                        if (stockReservationRepository.reserve(productId, 1)) {
                            successCount.incrementAndGet()
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // then
            assertThat(successCount.get()).isEqualTo(100)
            val remaining = redisTemplate.opsForValue().get("stock:$productId")
            assertThat(remaining).isEqualTo("0")
        }
    }
}
