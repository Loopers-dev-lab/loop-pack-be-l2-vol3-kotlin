package com.loopers.application.metrics

import com.loopers.domain.event.repository.EventHandledRepository
import com.loopers.domain.metrics.repository.ProductMetricsRepository
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * M-Concurrency: products 행 PESSIMISTIC_WRITE 락 앵커 동시성 검증.
 *
 * 두 스레드가 같은 productId로 cross-topic 이벤트(PRODUCT_VIEWED + PAYMENT_COMPLETED)를
 * 동시에 처리할 때 lost update 없이 직렬화되는지 검증한다.
 *
 * - 락 미도입 시: 두 트랜잭션이 동시에 findOrCreate → insert를 시도하거나
 *   같은 카운터 값을 읽어 +1이 유실되는 lost update 발생.
 * - 락 도입 시: SELECT FOR UPDATE가 두 트랜잭션을 직렬화해 최종 카운터가 합산값과 일치.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UpdateProductMetricsUseCaseConcurrencyIT @Autowired constructor(
    private val useCase: UpdateProductMetricsUseCase,
    private val productMetricsRepository: ProductMetricsRepository,
    private val eventHandledRepository: EventHandledRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
    private val jdbcTemplate: JdbcTemplate,
) {

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update("INSERT INTO products (id) VALUES (1)")
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @Test
    @DisplayName("같은 productId에 cross-topic 이벤트를 동시 처리해도 카운터 합산값이 보장된다")
    fun `동시 이벤트 처리 시 lost update 없이 카운터가 정확히 합산된다`() {
        val productId = 1L
        val quantity = 3L
        val executor = Executors.newFixedThreadPool(2)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(2)

        // Thread 1: catalog-events (PRODUCT_VIEWED)
        executor.submit {
            try {
                startLatch.await()
                useCase.handleCatalogEvent("evt-view-1", UpdateProductMetricsUseCase.PRODUCT_VIEWED, productId)
            } finally {
                doneLatch.countDown()
            }
        }

        // Thread 2: order-events (PAYMENT_COMPLETED)
        executor.submit {
            try {
                startLatch.await()
                useCase.handleOrderEvent("evt-order-1", UpdateProductMetricsUseCase.PAYMENT_COMPLETED, productId, quantity)
            } finally {
                doneLatch.countDown()
            }
        }

        // 두 스레드를 동시에 출발
        startLatch.countDown()
        assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()

        // 최종 카운터 검증 — lost update가 없으면 view=1, sales=quantity
        val metrics = productMetricsRepository.findByProductId(productId)
        assertThat(metrics).isNotNull()
        assertThat(metrics!!.viewCount).isEqualTo(1)
        assertThat(metrics.salesCount).isEqualTo(quantity)

        // 멱등 처리 기록 2건 보장
        assertThat(eventHandledRepository.existsByEventId("evt-view-1")).isTrue()
        assertThat(eventHandledRepository.existsByEventId("evt-order-1")).isTrue()
    }

    @Test
    @DisplayName("같은 eventId가 동시 재전달되어도 DIVEx 없이 한 번만 반영된다")
    fun `동일 eventId 중복 도달 시 post-check로 DIVEx 노이즈가 제거된다`() {
        val productId = 1L
        val eventId = "evt-dup-catalog"
        val executor = Executors.newFixedThreadPool(2)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(2)
        val thrown = ConcurrentLinkedQueue<Throwable>()

        repeat(2) {
            executor.submit {
                try {
                    startLatch.await()
                    useCase.handleCatalogEvent(eventId, UpdateProductMetricsUseCase.PRODUCT_VIEWED, productId)
                } catch (t: Throwable) {
                    thrown.add(t)
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        startLatch.countDown()
        assertThat(doneLatch.await(10, TimeUnit.SECONDS)).isTrue()
        executor.shutdown()

        // RD-048 2차 개정 사후 fix: 동일 eventId 동시 도달 시 event_handled PK 충돌로 인한
        // DataIntegrityViolationException이 발생하면 안 된다. productLock 획득 직후의 post-check가
        // 두 번째 스레드를 조기 return 시켜야 한다.
        assertThat(thrown)
            .describedAs("동일 eventId 중복 도달 시 DIVEx 런타임 노이즈가 발생하면 안 된다")
            .isEmpty()

        // 카운터는 한 번만 증가 (T1만 반영)
        val metrics = productMetricsRepository.findByProductId(productId)
        assertThat(metrics).isNotNull()
        assertThat(metrics!!.viewCount).isEqualTo(1)

        // event_handled에 기록은 정확히 1건만 남는다
        assertThat(eventHandledRepository.existsByEventId(eventId)).isTrue()
    }
}
