package com.loopers.concurrency

import com.loopers.domain.coupon.CouponIssueProcessor
import com.loopers.domain.coupon.IssueRequestStatus
import com.loopers.infrastructure.coupon.CouponIssueJpaRepository
import com.loopers.infrastructure.coupon.CouponIssueRequestJpaRepository
import com.loopers.infrastructure.coupon.CouponQuantityJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 선착순 쿠폰 발급 동시성 테스트.
 *
 * CouponIssueProcessor를 직접 동시 호출하여
 * 수량 초과 발급이 발생하지 않는지 검증한다.
 *
 * 테스트 경계: Consumer → Application (CouponIssueProcessor)
 */
@SpringBootTest
class CouponIssueConcurrencyTest @Autowired constructor(
    private val couponIssueProcessor: CouponIssueProcessor,
    private val couponIssueJpaRepository: CouponIssueJpaRepository,
    private val issueRequestJpaRepository: CouponIssueRequestJpaRepository,
    private val couponQuantityJpaRepository: CouponQuantityJpaRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    @BeforeEach
    fun setUp() {
        // Coupon 엔티티가 streamer에 없어서 Hibernate DDL이 coupons 테이블을 안 만듦
        // 테스트에서만 직접 생성 (프로덕션에서는 commerce-api가 DDL 담당)
        jdbcTemplate.execute(
            """
            CREATE TABLE IF NOT EXISTS coupons (
                id BIGINT NOT NULL AUTO_INCREMENT,
                name VARCHAR(255) NOT NULL,
                type VARCHAR(50) NOT NULL,
                value BIGINT NOT NULL,
                expired_at DATETIME(6) NOT NULL,
                max_issue_count INT,
                issued_count INT NOT NULL DEFAULT 0,
                created_at DATETIME(6) NOT NULL,
                updated_at DATETIME(6) NOT NULL,
                deleted_at DATETIME(6),
                PRIMARY KEY (id)
            )
            """,
        )
    }

    @AfterEach
    fun tearDown() {
        couponIssueJpaRepository.deleteAll()
        issueRequestJpaRepository.deleteAll()
        jdbcTemplate.execute("DELETE FROM coupons")
    }
    private fun createCoupon(maxIssueCount: Int): Long {
        jdbcTemplate.update(
            """
            INSERT INTO coupons (name, type, value, expired_at, max_issue_count, issued_count, created_at, updated_at)
            VALUES ('선착순 테스트 쿠폰', 'FIXED', 5000, DATE_ADD(NOW(), INTERVAL 7 DAY), ?, 0, NOW(), NOW())
            """,
            maxIssueCount,
        )
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long::class.java)!!
    }

    private fun createIssueRequest(userId: Long, couponId: Long): String {
        val requestId = UUID.randomUUID().toString()
        jdbcTemplate.update(
            """
            INSERT INTO coupon_issue_request (request_id, user_id, coupon_id, status, created_at, updated_at)
            VALUES (?, ?, ?, 'PENDING', NOW(), NOW())
            """,
             requestId, userId, couponId,
        )
        return requestId
    }

    @Nested
    @DisplayName("선착순 쿠폰 수량 제한 동시성")
    inner class QuantityLimit {

        @Test
        @Timeout(300, unit = TimeUnit.SECONDS)
        @DisplayName("100장 한정 쿠폰에 10000명이 동시에 요청하면, 100명만 발급되고 나머지는 수량 소진으로 거절된다")
        fun onlyMaxIssueCountSucceeds() {
            // arrange
            val maxIssueCount = 100
            val totalRequests = 10_000
            val couponId = createCoupon(maxIssueCount)

            val requests = (1L..totalRequests.toLong()).map { userId ->
                val requestId = createIssueRequest(userId, couponId)
                Triple(requestId, userId, couponId)
            }

            val threadCount = 20
            val executorService = Executors.newFixedThreadPool(threadCount)
            val startLatch = CountDownLatch(1)
            val endLatch = CountDownLatch(totalRequests)
            val failCount = AtomicInteger(0)

            // act
            requests.forEach { (requestId, userId, cId) ->
                executorService.submit {
                    try {
                        startLatch.await()
                        couponIssueProcessor.process(requestId, userId, cId)
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                    } finally {
                        endLatch.countDown()
                    }
                }
            }
            startLatch.countDown()
            endLatch.await()
            executorService.shutdown()

            // assert
            val issuedCoupons = couponIssueJpaRepository.findAll()
            val allRequests = issueRequestJpaRepository.findAll()
            val successRequests = allRequests.filter { it.status == IssueRequestStatus.SUCCESS }
            val exhaustedRequests = allRequests.filter { it.status == IssueRequestStatus.EXHAUSTED }
            val issuedCount = jdbcTemplate.queryForObject(
                "SELECT issued_count FROM coupons WHERE id = ?",
                Int::class.java,
                couponId,
            )!!

            assertAll(
                { assertThat(issuedCoupons).hasSize(maxIssueCount) },
                { assertThat(successRequests).hasSize(maxIssueCount) },
                { assertThat(exhaustedRequests).hasSize(totalRequests - maxIssueCount) },
                { assertThat(issuedCount).isEqualTo(maxIssueCount) },
                { assertThat(failCount.get()).isEqualTo(0) },
            )
        }
    }

    @Nested
    @DisplayName("중복 발급 방지")
    inner class DuplicatePrevention {

        @Test
        @Timeout(10, unit = TimeUnit.SECONDS)
        @DisplayName("같은 유저가 같은 쿠폰을 발급받은 상태에서 재요청하면, 중복으로 거절된다")
        fun preventsDuplicateIssue() {
            // arrange
            val couponId = createCoupon(100)
            val userId = 1L
            val requestId1 = createIssueRequest(userId, couponId)

            // 첫 번째 발급
            couponIssueProcessor.process(requestId1, userId, couponId)

            // 두 번째 요청 (requestId만 다름, userId + couponId 동일)
            val requestId2 = UUID.randomUUID().toString()
            jdbcTemplate.update(
                """
                INSERT INTO coupon_issue_request (request_id, user_id, coupon_id, status, created_at, updated_at)
                VALUES (?, ?, ?, 'PENDING', NOW(), NOW())
                """,
                requestId2, userId, couponId,
            )

            // act — 두 번째 발급 시도
            couponIssueProcessor.process(requestId2, userId, couponId)

            // assert
            val issuedCoupons = couponIssueJpaRepository.findAll()
            val request2 = issueRequestJpaRepository.findByRequestId(requestId2)

            assertAll(
                { assertThat(issuedCoupons).hasSize(1) },
                { assertThat(request2?.status).isEqualTo(IssueRequestStatus.FAILED) },
                { assertThat(request2?.failureReason).isEqualTo("이미 발급된 쿠폰") },
            )
        }
    }
}
