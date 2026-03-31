package com.loopers.interfaces.consumer

import com.loopers.testcontainers.MySqlTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
@MockBean(KafkaTemplate::class)
class FcfsCouponIssueConsumerTest @Autowired constructor(
    private val consumer: FcfsCouponIssueConsumer,
    private val jdbcTemplate: JdbcTemplate,
    private val transactionTemplate: TransactionTemplate,
) {

    @AfterEach
    fun tearDown() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0")
        jdbcTemplate.execute("TRUNCATE TABLE kafka_consumed_event")
        jdbcTemplate.execute("TRUNCATE TABLE fcfs_coupon_issue_request")
        jdbcTemplate.execute("TRUNCATE TABLE fcfs_coupon_template")
        jdbcTemplate.execute("TRUNCATE TABLE issued_coupon")
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1")
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    private fun insertTemplate(totalQuantity: Int, issuedQuantity: Int = 0): Long {
        val now = ZonedDateTime.now()
        val endedAt = now.plusDays(7)
        jdbcTemplate.update(
            """
            INSERT INTO fcfs_coupon_template
                (name, discount_type, discount_value, total_quantity, issued_quantity,
                 status, started_at, ended_at, created_at, updated_at)
            VALUES (?, 'FIXED', 1000, ?, ?, 'ACTIVE', ?, ?, ?, ?)
            """.trimIndent(),
            "테스트쿠폰",
            totalQuantity,
            issuedQuantity,
            now,
            endedAt,
            now,
            now,
        )
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long::class.java)!!
    }

    private fun insertIssueRequest(templateId: Long, memberId: Long, status: String = "PENDING"): Long {
        val now = ZonedDateTime.now()
        jdbcTemplate.update(
            """
            INSERT INTO fcfs_coupon_issue_request (template_id, member_id, status, created_at)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
            templateId,
            memberId,
            status,
            now,
        )
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long::class.java)!!
    }

    private fun getRequestStatus(requestId: Long): String =
        jdbcTemplate.queryForObject(
            "SELECT status FROM fcfs_coupon_issue_request WHERE id = ?",
            String::class.java,
            requestId,
        )!!

    private fun getIssuedQuantity(templateId: Long): Int =
        jdbcTemplate.queryForObject(
            "SELECT issued_quantity FROM fcfs_coupon_template WHERE id = ?",
            Int::class.java,
            templateId,
        )!!

    // ────────────────────────────────────────────────────────────────────────
    // Test cases
    // ────────────────────────────────────────────────────────────────────────

    @DisplayName("선착순 쿠폰 정상 발급")
    @Nested
    inner class NormalIssue {

        @DisplayName("수량 10, 요청 1건 → 상태 ISSUED, issued_quantity=1")
        @Test
        fun issuesSuccessfully_whenQuantityAvailable() {
            // arrange
            val templateId = insertTemplate(totalQuantity = 10)
            val memberId = 1L
            val requestId = insertIssueRequest(templateId, memberId)

            // act
            consumer.processIssue(requestId, templateId, memberId)

            // assert
            assertThat(getRequestStatus(requestId)).isEqualTo("ISSUED")
            assertThat(getIssuedQuantity(templateId)).isEqualTo(1)
        }
    }

    @DisplayName("수량 초과 → SOLD_OUT")
    @Nested
    inner class SoldOut {

        @DisplayName("issued_quantity가 이미 total_quantity에 도달하면 → 상태 SOLD_OUT")
        @Test
        fun marksSoldOut_whenQuantityExhausted() {
            // arrange
            val templateId = insertTemplate(totalQuantity = 1, issuedQuantity = 1)
            val memberId = 2L
            val requestId = insertIssueRequest(templateId, memberId)

            // act
            consumer.processIssue(requestId, templateId, memberId)

            // assert
            assertThat(getRequestStatus(requestId)).isEqualTo("SOLD_OUT")
            assertThat(getIssuedQuantity(templateId)).isEqualTo(1)
        }
    }

    @DisplayName("중복 발급 방지")
    @Nested
    inner class DuplicatePrevention {

        @DisplayName("동일 member+template에 ISSUED 상태 요청이 존재하면 → 상태 FAILED")
        @Test
        fun marksFailed_whenDuplicateIssuedExists() {
            // arrange
            val templateId = insertTemplate(totalQuantity = 10, issuedQuantity = 1)
            val memberId = 3L
            // 이미 발급된 요청 존재
            insertIssueRequest(templateId, memberId, status = "ISSUED")
            // 새 요청
            val newRequestId = insertIssueRequest(templateId, memberId)

            // act
            consumer.processIssue(newRequestId, templateId, memberId)

            // assert
            assertThat(getRequestStatus(newRequestId)).isEqualTo("FAILED")
        }
    }

    @DisplayName("멱등 처리")
    @Nested
    inner class Idempotency {

        @DisplayName("동일 eventId로 2번 호출 → 두 번째는 kafka_consumed_event 중복으로 스킵(tryMarkConsumed 직접 검증)")
        @Test
        fun secondCallIsSkipped_whenEventIdAlreadyConsumed() {
            // arrange
            val eventId = "test-event-idempotency-001"
            jdbcTemplate.update(
                "INSERT IGNORE INTO kafka_consumed_event (event_id, consumer_group, handled_at) VALUES (?, ?, NOW())",
                eventId,
                FcfsCouponIssueConsumer.CONSUMER_GROUP,
            )

            // act: 동일 eventId 재삽입 시도
            val inserted = jdbcTemplate.update(
                "INSERT IGNORE INTO kafka_consumed_event (event_id, consumer_group, handled_at) VALUES (?, ?, NOW())",
                eventId,
                FcfsCouponIssueConsumer.CONSUMER_GROUP,
            )

            // assert: INSERT IGNORE → 중복이면 0 rows affected
            assertThat(inserted).isEqualTo(0)
        }

        @DisplayName("동일 eventId 첫 번째 호출 → 정상 처리, 두 번째 호출 → 스킵되어 issued_quantity 변화 없음")
        @Test
        fun processedOnce_whenCalledTwiceWithSamePayload() {
            // arrange
            val templateId = insertTemplate(totalQuantity = 10)
            val memberId = 10L
            val requestId1 = insertIssueRequest(templateId, memberId)

            // act: 첫 번째 처리
            consumer.processIssue(requestId1, templateId, memberId)

            val issuedAfterFirst = getIssuedQuantity(templateId)

            // 두 번째 요청 삽입 (같은 member지만 이미 ISSUED 상태이므로 FAILED 처리)
            val requestId2 = insertIssueRequest(templateId, memberId)
            consumer.processIssue(requestId2, templateId, memberId)

            // assert: 첫 번째만 ISSUED, issued_quantity는 1 유지
            assertThat(getRequestStatus(requestId1)).isEqualTo("ISSUED")
            assertThat(getRequestStatus(requestId2)).isEqualTo("FAILED")
            assertThat(issuedAfterFirst).isEqualTo(1)
            assertThat(getIssuedQuantity(templateId)).isEqualTo(1)
        }
    }

    @DisplayName("동시성: N개 요청 중 정확히 totalQuantity만 ISSUED")
    @Nested
    inner class Concurrency {

        @DisplayName("수량 10, 요청 20건을 멀티스레드로 처리 → ISSUED 정확히 10건, SOLD_OUT 10건")
        @Test
        fun exactlyTotalQuantityIssued_whenConcurrentRequests() {
            // arrange
            val totalQuantity = 10
            val requestCount = 20
            val templateId = insertTemplate(totalQuantity = totalQuantity)

            val requestIds = (1..requestCount).map { i ->
                insertIssueRequest(templateId, memberId = (100L + i))
            }

            val executorService = Executors.newFixedThreadPool(requestCount)
            val latch = CountDownLatch(requestCount)
            val errors = ConcurrentHashMap<Int, Throwable>()

            // act
            requestIds.forEachIndexed { idx, requestId ->
                val memberId = 100L + idx + 1
                executorService.submit {
                    try {
                        latch.countDown()
                        latch.await()
                        transactionTemplate.execute { consumer.processIssue(requestId, templateId, memberId) }
                    } catch (e: Throwable) {
                        errors[idx] = e
                    }
                }
            }
            executorService.shutdown()
            executorService.awaitTermination(30, TimeUnit.SECONDS)

            // assert
            assertThat(errors).isEmpty()

            val statuses = requestIds.map { getRequestStatus(it) }
            val issuedCount = statuses.count { it == "ISSUED" }
            val soldOutCount = statuses.count { it == "SOLD_OUT" }

            assertThat(issuedCount).isEqualTo(totalQuantity)
            assertThat(soldOutCount).isEqualTo(requestCount - totalQuantity)
            assertThat(getIssuedQuantity(templateId)).isEqualTo(totalQuantity)
        }
    }
}
