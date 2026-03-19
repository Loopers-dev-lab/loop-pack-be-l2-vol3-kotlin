package com.loopers.config.resilience4j

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Resilience4j CircuitBreaker 동작 검증 테스트.
 * Spring 없이 순수 Resilience4j API로 서킷 상태 전이를 검증한다.
 *
 * 이 테스트들은 k6 + Grafana로 수동 확인했던 동작을
 * 코드로 재현하여 자동 검증하는 목적이다.
 */
class CircuitBreakerBehaviorTest {

    private fun createCircuitBreaker(
        slidingWindowSize: Int = 5,
        minimumNumberOfCalls: Int = 3,
        failureRateThreshold: Float = 50f,
        slowCallDurationThreshold: Duration = Duration.ofMillis(300),
        slowCallRateThreshold: Float = 50f,
        waitDurationInOpenState: Duration = Duration.ofSeconds(1),
        permittedNumberOfCallsInHalfOpenState: Int = 5,
        automaticTransitionFromOpenToHalfOpenEnabled: Boolean = false,
    ): CircuitBreaker {
        val config = CircuitBreakerConfig.custom()
            .slidingWindowSize(slidingWindowSize)
            .minimumNumberOfCalls(minimumNumberOfCalls)
            .failureRateThreshold(failureRateThreshold)
            .slowCallDurationThreshold(slowCallDurationThreshold)
            .slowCallRateThreshold(slowCallRateThreshold)
            .waitDurationInOpenState(waitDurationInOpenState)
            .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
            .automaticTransitionFromOpenToHalfOpenEnabled(automaticTransitionFromOpenToHalfOpenEnabled)
            .build()
        return CircuitBreaker.of("test", config)
    }

    private fun callSuccess(cb: CircuitBreaker) {
        cb.executeSupplier { "success" }
    }

    private fun callFailure(cb: CircuitBreaker) {
        try {
            cb.executeSupplier { throw RuntimeException("fail") }
        } catch (_: RuntimeException) {
            // expected
        }
    }

    private fun callSlow(cb: CircuitBreaker, delayMs: Long = 400) {
        cb.executeSupplier {
            Thread.sleep(delayMs)
            "slow but success"
        }
    }

    @Nested
    @DisplayName("CLOSED → OPEN 전이")
    inner class ClosedToOpen {

        @Test
        @DisplayName("minimumNumberOfCalls 도달 전에는 실패율이 높아도 CLOSED를 유지한다")
        fun staysClosedBeforeMinimumCalls() {
            // arrange
            val cb = createCircuitBreaker(minimumNumberOfCalls = 3)

            // act — 2건만 호출 (minimum 미달)
            callFailure(cb)
            callFailure(cb)

            // assert
            assertThat(cb.state).isEqualTo(CircuitBreaker.State.CLOSED)
            assertThat(cb.metrics.failureRate).isEqualTo(-1f) // 아직 판단 안 함
        }

        @Test
        @DisplayName("minimumNumberOfCalls 이후 실패율이 threshold를 초과하면 OPEN으로 전이된다")
        fun opensAfterThresholdExceeded() {
            // arrange
            val cb = createCircuitBreaker(
                minimumNumberOfCalls = 3,
                failureRateThreshold = 50f,
            )

            // act — 3건 중 2건 실패 = 66%
            callFailure(cb)
            callFailure(cb)
            callSuccess(cb)

            // assert
            assertThat(cb.state).isEqualTo(CircuitBreaker.State.OPEN)
            assertThat(cb.metrics.failureRate).isGreaterThan(50f)
        }

        @Test
        @DisplayName("실패율이 threshold 이하이면 CLOSED를 유지한다")
        fun staysClosedWhenBelowThreshold() {
            // arrange
            val cb = createCircuitBreaker(
                minimumNumberOfCalls = 3,
                failureRateThreshold = 50f,
            )

            // act — 3건 중 1건 실패 = 33%
            callFailure(cb)
            callSuccess(cb)
            callSuccess(cb)

            // assert
            assertThat(cb.state).isEqualTo(CircuitBreaker.State.CLOSED)
            assertThat(cb.metrics.failureRate).isLessThan(50f)
        }

        @Test
        @DisplayName("slowCall 비율이 threshold를 초과해도 OPEN으로 전이된다")
        fun opensOnSlowCallRate() {
            // arrange
            val cb = createCircuitBreaker(
                minimumNumberOfCalls = 3,
                failureRateThreshold = 90f, // 실패율은 높게 (실패로는 안 열리게)
                slowCallDurationThreshold = Duration.ofMillis(200),
                slowCallRateThreshold = 50f,
            )

            // act — 3건 중 2건 slow = 66%
            callSlow(cb, 300)
            callSlow(cb, 300)
            callSuccess(cb)

            // assert
            assertThat(cb.state).isEqualTo(CircuitBreaker.State.OPEN)
            assertThat(cb.metrics.slowCallRate).isGreaterThan(50f)
        }
    }

    @Nested
    @DisplayName("OPEN 상태 동작")
    inner class OpenState {

        @Test
        @DisplayName("OPEN 상태에서 호출하면 CallNotPermittedException이 발생한다")
        fun rejectsCallsWhenOpen() {
            // arrange
            val cb = createCircuitBreaker(minimumNumberOfCalls = 3, failureRateThreshold = 50f)
            callFailure(cb)
            callFailure(cb)
            callSuccess(cb) // 3건째 → OPEN

            // act & assert
            assertThrows<CallNotPermittedException> {
                cb.executeSupplier { "should not reach" }
            }
            assertThat(cb.metrics.numberOfNotPermittedCalls).isGreaterThan(0)
        }

        @Test
        @DisplayName("OPEN 상태에서는 실제 함수가 실행되지 않는다")
        fun functionNotExecutedWhenOpen() {
            // arrange
            val cb = createCircuitBreaker(minimumNumberOfCalls = 3, failureRateThreshold = 50f)
            callFailure(cb)
            callFailure(cb)
            callSuccess(cb) // OPEN

            var executed = false

            // act
            try {
                cb.executeSupplier {
                    executed = true
                    "should not reach"
                }
            } catch (_: CallNotPermittedException) {
                // expected
            }

            // assert
            assertThat(executed).isFalse()
        }
    }

    @Nested
    @DisplayName("HALF_OPEN 상태 동작")
    inner class HalfOpenState {

        @Test
        @DisplayName("OPEN → waitDuration 경과 후 자동 전이가 켜져있으면 HALF_OPEN이 된다")
        fun transitionsToHalfOpenAutomatically() {
            // arrange
            val cb = createCircuitBreaker(
                minimumNumberOfCalls = 3,
                failureRateThreshold = 50f,
                waitDurationInOpenState = Duration.ofMillis(100),
                automaticTransitionFromOpenToHalfOpenEnabled = true,
            )
            callFailure(cb)
            callFailure(cb)
            callSuccess(cb) // OPEN

            // act
            Thread.sleep(200) // waitDuration 경과

            // assert
            assertThat(cb.state).isEqualTo(CircuitBreaker.State.HALF_OPEN)
        }

        @Test
        @DisplayName("HALF_OPEN에서 시험 호출이 모두 성공하면 CLOSED로 복구된다")
        fun recoversToClosedOnSuccess() {
            // arrange
            val cb = createCircuitBreaker(
                minimumNumberOfCalls = 3,
                failureRateThreshold = 50f,
                waitDurationInOpenState = Duration.ofMillis(100),
                permittedNumberOfCallsInHalfOpenState = 3,
            )
            callFailure(cb)
            callFailure(cb)
            callSuccess(cb) // OPEN
            Thread.sleep(200)
            // 수동 전이 (automatic=false이므로 호출로 트리거)

            // act — HALF_OPEN에서 3건 모두 성공
            callSuccess(cb) // 이 호출이 HALF_OPEN 트리거 + 첫 시험 호출
            callSuccess(cb)
            callSuccess(cb)

            // assert
            assertThat(cb.state).isEqualTo(CircuitBreaker.State.CLOSED)
        }

        @Test
        @DisplayName("HALF_OPEN에서 시험 호출이 실패하면 다시 OPEN으로 돌아간다")
        fun returnsToOpenOnFailure() {
            // arrange
            val cb = createCircuitBreaker(
                minimumNumberOfCalls = 3,
                failureRateThreshold = 50f,
                waitDurationInOpenState = Duration.ofMillis(100),
                permittedNumberOfCallsInHalfOpenState = 3,
            )
            callFailure(cb)
            callFailure(cb)
            callSuccess(cb) // OPEN
            Thread.sleep(200)

            // act — HALF_OPEN에서 2건 실패
            callFailure(cb) // HALF_OPEN 트리거 + 실패
            callFailure(cb)
            callSuccess(cb)

            // assert
            assertThat(cb.state).isEqualTo(CircuitBreaker.State.OPEN)
        }
    }

    @Nested
    @DisplayName("HALF_OPEN 조기 차단 (Short-circuiting)")
    inner class HalfOpenShortCircuiting {

        @Test
        @DisplayName("permitted=5, minimum=3, threshold=15%일 때 minimum 충족 후 1/3 실패로 즉시 OPEN (남은 2건 성공해도 1/5=20% > 15%)")

        fun shortCircuitsWhenRecoveryImpossible() {
            // arrange
            // threshold 15% → 5건 중 0.75건 → 1건이라도 실패하면 복구 불가
            val cb = createCircuitBreaker(
                minimumNumberOfCalls = 3,
                failureRateThreshold = 15f,
                waitDurationInOpenState = Duration.ofMillis(100),
                permittedNumberOfCallsInHalfOpenState = 5,
            )
            // OPEN 만들기
            callFailure(cb)
            callFailure(cb)
            callSuccess(cb)
            assertThat(cb.state).isEqualTo(CircuitBreaker.State.OPEN)
            Thread.sleep(200)

            // act — HALF_OPEN에서 3건 호출 (minimum 충족), 1건 실패
            callFailure(cb) // HALF_OPEN 트리거 + 실패
            callSuccess(cb)
            callSuccess(cb)

            // assert — 5건 안 채웠는데 3건에서 바로 OPEN (조기 차단)
            assertThat(cb.state).isEqualTo(CircuitBreaker.State.OPEN)
            assertThat(cb.metrics.numberOfBufferedCalls).isLessThan(5)
        }

        @Test
        @DisplayName("permitted=5, minimum=3, threshold=50%일 때 1건 실패로는 HALF_OPEN 유지 (남은 4건 성공하면 1/5=20% < 50%)")
        fun doesNotShortCircuitWhenRecoveryStillPossible() {
            // arrange
            // threshold 50% → 5건 중 2.5건 → 2건까지는 실패해도 복구 가능
            val cb = createCircuitBreaker(
                minimumNumberOfCalls = 3,
                failureRateThreshold = 50f,
                waitDurationInOpenState = Duration.ofMillis(100),
                permittedNumberOfCallsInHalfOpenState = 5,
            )
            // OPEN 만들기
            callFailure(cb)
            callFailure(cb)
            callSuccess(cb)
            assertThat(cb.state).isEqualTo(CircuitBreaker.State.OPEN)
            Thread.sleep(200)

            // act — HALF_OPEN에서 1건 실패 (아직 복구 가능)
            callFailure(cb) // HALF_OPEN 트리거 + 실패

            // assert — 아직 HALF_OPEN 유지 (남은 4건이 다 성공하면 1/5=20% < 50%)
            assertThat(cb.state).isEqualTo(CircuitBreaker.State.HALF_OPEN)
        }
    }

    @Nested
    @DisplayName("윈도우 크기와 판단 시점")
    inner class SlidingWindow {

        @Test
        @DisplayName("윈도우 크기를 초과하면 가장 오래된 호출이 밀려난다")
        fun oldestCallsSlidOut() {
            // arrange
            val cb = createCircuitBreaker(
                slidingWindowSize = 5,
                minimumNumberOfCalls = 3,
                failureRateThreshold = 50f,
            )

            // act — 5건 실패 후 5건 성공 (윈도우에 최근 5건만 남음)
            repeat(5) { callFailure(cb) } // OPEN됨
            cb.transitionToClosedState() // 수동 리셋

            repeat(5) { callSuccess(cb) }

            // assert — 윈도우에 성공 5건만 남아서 실패율 0%
            assertThat(cb.state).isEqualTo(CircuitBreaker.State.CLOSED)
            assertThat(cb.metrics.failureRate).isEqualTo(0f)
            assertThat(cb.metrics.numberOfBufferedCalls).isEqualTo(5)
        }
    }

    @Nested
    @DisplayName("서킷 인스턴스 분리")
    inner class InstanceIsolation {

        @Test
        @DisplayName("서로 다른 이름의 서킷은 독립적으로 동작한다")
        fun separateInstancesAreIndependent() {
            // arrange
            val config = CircuitBreakerConfig.custom()
                .slidingWindowSize(5)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(50f)
                .build()

            val pgRequest = CircuitBreaker.of("pg-request", config)
            val pgQuery = CircuitBreaker.of("pg-query", config)

            // act — pg-request만 실패시켜서 OPEN
            callFailure(pgRequest)
            callFailure(pgRequest)
            callSuccess(pgRequest)

            // assert — pg-request는 OPEN, pg-query는 CLOSED
            assertThat(pgRequest.state).isEqualTo(CircuitBreaker.State.OPEN)
            assertThat(pgQuery.state).isEqualTo(CircuitBreaker.State.CLOSED)
        }
    }

    @Nested
    @DisplayName("CircuitBreaker vs Bulkhead 동시성 제한")
    inner class BulkheadComparison {

        @Test
        @DisplayName("CircuitBreaker만으로는 동시 호출을 제한하지 않는다 — 20개 스레드가 전부 동시에 실행된다")
        fun circuitBreakerDoesNotLimitConcurrency() {
            // arrange
            val cb = createCircuitBreaker(slidingWindowSize = 100, minimumNumberOfCalls = 50)
            val concurrentCount = AtomicInteger(0)
            val maxConcurrent = AtomicInteger(0)
            val latch = CountDownLatch(20)

            // act — 20개 스레드 동시 실행
            val threads = (1..20).map {
                Thread {
                    cb.executeSupplier {
                        val current = concurrentCount.incrementAndGet()
                        maxConcurrent.updateAndGet { max -> maxOf(max, current) }
                        Thread.sleep(200) // PG 호출 시뮬레이션
                        concurrentCount.decrementAndGet()
                        latch.countDown()
                        "success"
                    }
                }
            }
            threads.forEach { it.start() }
            latch.await()

            // assert — 동시 실행이 제한되지 않음 (거의 20개가 동시에 실행됨)
            assertThat(maxConcurrent.get()).isGreaterThan(10)
        }

        @Test
        @DisplayName("Bulkhead를 추가하면 동시 호출이 maxConcurrentCalls로 제한된다")
        fun bulkheadLimitsConcurrency() {
            // arrange
            val bulkhead = Bulkhead.of("pg-request", BulkheadConfig.custom()
                .maxConcurrentCalls(3)
                .maxWaitDuration(Duration.ZERO) // 대기 없이 즉시 거부
                .build())

            val successCount = AtomicInteger(0)
            val rejectedCount = AtomicInteger(0)
            val latch = CountDownLatch(20)

            // act — 20개 스레드 동시 실행
            val threads = (1..20).map {
                Thread {
                    try {
                        bulkhead.executeSupplier {
                            Thread.sleep(200) // PG 호출 시뮬레이션
                            successCount.incrementAndGet()
                            "success"
                        }
                    } catch (_: BulkheadFullException) {
                        rejectedCount.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }
            threads.forEach { it.start() }
            latch.await()

            // assert — 동시 3개만 허용, 나머지는 거부
            assertThat(successCount.get()).isLessThanOrEqualTo(3)
            assertThat(rejectedCount.get()).isGreaterThan(0)
        }
    }

    @Nested
    @DisplayName("CircuitBreaker + Retry 조합")
    inner class CircuitBreakerWithRetry {

        private fun createRetry(): Retry {
            val config: RetryConfig = RetryConfig.custom<Any>()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(10))
                .retryExceptions(RuntimeException::class.java)
                .build()
            return Retry.of("test-retry", config)
        }

        @Test
        @DisplayName("Retry가 3회 소진된 후 CircuitBreaker에는 1건의 실패로만 기록된다")
        fun retryExhaustionCountsAsOneCBFailure() {
            // arrange
            val cb = createCircuitBreaker(
                slidingWindowSize = 10,
                minimumNumberOfCalls = 3,
                failureRateThreshold = 50f,
            )
            val retry = createRetry()
            val callCount = AtomicInteger(0)

            // act — CB(Outer) → Retry(Inner): Retry로 감싼 함수를 CB가 실행
            try {
                cb.executeSupplier {
                    retry.executeSupplier {
                        callCount.incrementAndGet()
                        throw RuntimeException("PG 장애")
                    }
                }
            } catch (_: RuntimeException) {
                // expected
            }

            // assert
            assertThat(callCount.get()).isEqualTo(3) // Retry가 3번 호출
            assertThat(cb.metrics.numberOfFailedCalls).isEqualTo(1) // CB에는 1건만 기록
            assertThat(cb.metrics.numberOfBufferedCalls).isEqualTo(1)
        }

        @Test
        @DisplayName("CircuitBreaker가 OPEN이면 Retry는 실행되지 않는다")
        fun retryIsSkippedWhenCircuitIsOpen() {
            // arrange
            val cb = createCircuitBreaker(
                minimumNumberOfCalls = 3,
                failureRateThreshold = 50f,
            )
            val retry = createRetry()

            // OPEN 만들기
            callFailure(cb)
            callFailure(cb)
            callSuccess(cb)
            assertThat(cb.state).isEqualTo(CircuitBreaker.State.OPEN)

            val callCount = AtomicInteger(0)

            // act — CB OPEN 상태에서 호출
            try {
                cb.executeSupplier {
                    retry.executeSupplier {
                        callCount.incrementAndGet()
                        "success"
                    }
                }
            } catch (_: CallNotPermittedException) {
                // expected
            }

            // assert — 실제 함수가 한 번도 실행되지 않음 (Retry도 안 탐)
            assertThat(callCount.get()).isEqualTo(0)
        }
    }
}
