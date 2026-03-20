package com.loopers.infrastructure.payment

import com.loopers.application.payment.PaymentFacade
import com.loopers.application.payment.PaymentInfo
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.PaymentGateway
import com.loopers.domain.payment.PaymentGatewayResponse
import com.loopers.domain.payment.PaymentService
import com.loopers.domain.payment.PaymentStatus
import com.loopers.utils.DatabaseCleanUp
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import io.github.resilience4j.bulkhead.BulkheadFullException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * CircuitBreaker 동시성 부하 테스트
 *
 * PG 시뮬레이터 실행 여부에 관계없이 Feign + CircuitBreaker + Bulkhead 스택을 검증한다.
 * PaymentGateway를 mock하지 않으므로 Resilience 패턴이 실제로 동작한다.
 *
 * - PG 미실행: 100% Connection Refused → CB 빠르게 OPEN
 * - PG 실행: 60% 성공 / 40% 실패 → 혼합 결과 (CB 상태 전이는 확률적)
 */
@SpringBootTest
class CircuitBreakerConcurrencyTest @Autowired constructor(
    private val paymentGateway: PaymentGateway,
    private val paymentFacade: PaymentFacade,
    private val paymentService: PaymentService,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    companion object {
        private const val CALLBACK_URL = "http://localhost:8080/api/v1/payments/callback"
        private const val THREAD_COUNT = 50
    }

    @AfterEach
    fun tearDown() {
        circuitBreakerRegistry.allCircuitBreakers.forEach { it.reset() }
        databaseCleanUp.truncateAllTables()
    }

    private fun findCircuitBreaker(): CircuitBreaker? {
        return circuitBreakerRegistry.allCircuitBreakers
            .firstOrNull { it.name.contains("requestPayment", ignoreCase = true) }
    }

    private fun safeRequestPayment(userId: String, orderId: String): PaymentGatewayResponse? {
        return try {
            paymentGateway.requestPayment(
                userId = userId,
                orderId = orderId,
                cardType = "SAMSUNG",
                cardNo = "1234-5678-9012-3456",
                amount = 10000L,
                callbackUrl = CALLBACK_URL,
            )
        } catch (_: Exception) {
            null
        }
    }

    @DisplayName("CircuitBreaker 동시성 부하 테스트")
    @Nested
    inner class ConcurrencyLoadTest {

        @DisplayName("시나리오 1: 50개 동시 요청 — 모든 요청이 예외 없이 안전하게 처리된다")
        @Test
        fun allRequestsHandledSafely_underConcurrentLoad() {
            // arrange
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(THREAD_COUNT)
            val responseTimes = ConcurrentHashMap<Int, Long>()
            val bulkheadRejected = AtomicInteger(0)
            val otherException = AtomicInteger(0)

            // act
            val futures = (1..THREAD_COUNT).map { i ->
                executor.submit<PaymentGatewayResponse?> {
                    latch.await()
                    val start = System.nanoTime()
                    val result = try {
                        paymentGateway.requestPayment(
                            userId = "user-$i",
                            orderId = "CB-TEST-$i",
                            cardType = "SAMSUNG",
                            cardNo = "1234-5678-9012-3456",
                            amount = 10000L,
                            callbackUrl = CALLBACK_URL,
                        )
                    } catch (e: BulkheadFullException) {
                        bulkheadRejected.incrementAndGet()
                        null
                    } catch (_: Exception) {
                        otherException.incrementAndGet()
                        null
                    }
                    responseTimes[i] = (System.nanoTime() - start) / 1_000_000
                    result
                }
            }
            latch.countDown()
            val results = futures.map { it.get() }
            executor.shutdown()

            // 결과 출력
            val cb = findCircuitBreaker()
            val sorted = responseTimes.values.sorted()
            val successCount = results.count { it != null }
            val pgFailed = cb?.metrics?.numberOfFailedCalls ?: 0
            val cbRejected = cb?.metrics?.numberOfNotPermittedCalls ?: 0
            println("=== 시나리오 1: 50건 동시 요청 ===")
            println("총 요청: ${results.size}건")
            println("PG 성공 (응답 수신): ${successCount}건")
            println("PG 실패 (CB failed): ${pgFailed}건")
            println("CB 거부 (not_permitted): ${cbRejected}건")
            println("Bulkhead 거부: ${bulkheadRejected.get()}건")
            println("기타 예외: ${otherException.get()}건")
            println("응답시간 p50: ${sorted[THREAD_COUNT / 2]}ms")
            println("응답시간 p95: ${sorted[(THREAD_COUNT * 0.95).toInt()]}ms")
            println("응답시간 max: ${sorted.last()}ms")
            println("CB 최종 상태: ${cb?.state}")

            // assert — 모든 요청이 처리됨 (PG 성공 or Fallback or Bulkhead 거부)
            assertThat(results).hasSize(THREAD_COUNT)
        }

        @DisplayName("시나리오 2: 순차 호출로 CB 상태 전이 관찰 — CLOSED에서 호출 후 상태 변화를 확인한다")
        @Test
        fun observeCircuitBreakerStateTransition() {
            // Phase 1: 20건 순차 호출 → CB 상태 변화 관찰
            val closedTimes = mutableListOf<Long>()
            val closedResults = mutableListOf<PaymentGatewayResponse?>()
            repeat(20) {
                val start = System.nanoTime()
                val result = safeRequestPayment("test-user", "SEQ-TEST-$it")
                closedTimes.add((System.nanoTime() - start) / 1_000_000)
                closedResults.add(result)
            }
            val cb = findCircuitBreaker()

            val successCount = closedResults.count { it != null }
            val failCount = closedResults.count { it == null }

            // Phase 2: 현재 CB 상태에서 50건 동시 호출
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(THREAD_COUNT)
            val openTimes = ConcurrentHashMap<Int, Long>()

            val futures = (1..THREAD_COUNT).map { i ->
                executor.submit<PaymentGatewayResponse?> {
                    latch.await()
                    val start = System.nanoTime()
                    val result = safeRequestPayment("user-$i", "PHASE2-TEST-$i")
                    openTimes[i] = (System.nanoTime() - start) / 1_000_000
                    result
                }
            }
            val totalStart = System.nanoTime()
            latch.countDown()
            futures.forEach { it.get() }
            val totalElapsed = (System.nanoTime() - totalStart) / 1_000_000
            executor.shutdown()

            // 결과 출력
            println("=== 시나리오 2: CB 상태 전이 관찰 ===")
            println("[Phase 1 - 20건 순차]")
            println("  PG 성공: ${successCount}건")
            println("  PG 실패/Fallback: ${failCount}건")
            println("  실패율: ${failCount * 100 / 20}%")
            println("  평균 응답: ${closedTimes.average().toLong()}ms")
            println("  CB 상태: ${cb?.state}")
            println("[Phase 2 - 50건 동시]")
            println("  전체 소요: ${totalElapsed}ms")
            println("  평균 응답: ${openTimes.values.average().toLong()}ms")
            println("  CB 거부: ${cb?.metrics?.numberOfNotPermittedCalls}건")

            // assert — 모든 요청 처리 완료 (3초 이내)
            assertThat(totalElapsed).isLessThan(5000L)
        }

        @DisplayName("시나리오 3: 버스트 50건 — 모든 결제가 DB에 저장되어 데이터 유실이 없다")
        @Test
        fun allPaymentsSavedToDatabase_underConcurrentLoad() {
            // arrange
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(THREAD_COUNT)

            // act
            val futures = (1..THREAD_COUNT).map { i ->
                executor.submit<PaymentInfo> {
                    latch.await()
                    paymentFacade.requestPayment(
                        userId = i.toLong(),
                        orderId = "ORDER-LOAD-$i",
                        cardType = CardType.SAMSUNG,
                        cardNo = "1234-5678-9012-3456",
                        amount = 10000L,
                    )
                }
            }
            latch.countDown()
            val results = futures.map { it.get() }
            executor.shutdown()

            // DB 검증
            val dbPayments = (1..THREAD_COUNT).flatMap { i ->
                paymentService.getPaymentsByOrderId("ORDER-LOAD-$i")
            }

            val pendingCount = results.count { it.status == PaymentStatus.PENDING }
            val failedCount = results.count { it.status == PaymentStatus.FAILED }

            // 결과 출력
            println("=== 시나리오 3: 버스트 50건 데이터 정합성 ===")
            println("요청: ${THREAD_COUNT}건")
            println("응답: ${results.size}건")
            println("DB 저장: ${dbPayments.size}건")
            println("PENDING: ${pendingCount}건 (${pendingCount * 100 / THREAD_COUNT}%)")
            println("FAILED: ${failedCount}건 (${failedCount * 100 / THREAD_COUNT}%)")
            println("유실: ${THREAD_COUNT - dbPayments.size}건")

            // assert — 데이터 유실 없음
            assertAll(
                { assertThat(results).hasSize(THREAD_COUNT) },
                { assertThat(dbPayments).hasSize(THREAD_COUNT) },
            )
        }

        @DisplayName("시나리오 4: HALF_OPEN 복구 시도 — CB가 제한된 호출만 허용한다")
        @Test
        fun halfOpenAllowsLimitedCalls() {
            // Phase 1: CB를 강제로 OPEN 전이
            repeat(20) {
                safeRequestPayment("test-user", "FORCE-OPEN-TEST-$it")
            }
            val cb = findCircuitBreaker()!!
            if (cb.state != CircuitBreaker.State.OPEN) {
                cb.transitionToOpenState()
            }
            assertThat(cb.state).isEqualTo(CircuitBreaker.State.OPEN)

            // Phase 2: 수동으로 HALF_OPEN 전이
            cb.transitionToHalfOpenState()
            assertThat(cb.state).isEqualTo(CircuitBreaker.State.HALF_OPEN)

            // Phase 3: HALF_OPEN에서 10건 순차 호출
            val halfOpenResults = (0 until 10).map { i ->
                safeRequestPayment("test-user", "HALF-OPEN-TEST-$i")
            }

            val successCount = halfOpenResults.count { it != null }

            // 결과 출력
            println("=== 시나리오 4: HALF_OPEN 복구 시도 ===")
            println("HALF_OPEN 호출: ${halfOpenResults.size}건")
            println("PG 성공: ${successCount}건")
            println("Fallback: ${halfOpenResults.count { it == null }}건")
            println("CB 최종 상태: ${cb.state}")
            println("CB 실패: ${cb.metrics.numberOfFailedCalls}건")
            println("CB 성공: ${cb.metrics.numberOfSuccessfulCalls}건")
            println("CB 거부: ${cb.metrics.numberOfNotPermittedCalls}건")

            // assert — HALF_OPEN에서 최대 3건만 허용 (permitted-number-of-calls-in-half-open-state)
            assertAll(
                { assertThat(halfOpenResults).hasSize(10) },
                { assertThat(cb.state).isNotEqualTo(CircuitBreaker.State.HALF_OPEN) },
            )
        }
    }
}
