package com.loopers.application.coupon

import com.loopers.domain.idempotency.EventHandled
import com.loopers.domain.idempotency.EventHandledRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class CouponIssueProcessor(
    private val eventHandledRepository: EventHandledRepository,
    private val jdbcTemplate: JdbcTemplate,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun process(
        eventId: String,
        eventType: String,
        requestId: String,
        couponId: Long,
        userId: Long,
    ) {
        // 1. 멱등성 체크
        if (eventHandledRepository.existsByEventId(eventId)) {
            log.info("중복 이벤트 skip [eventId={}]", eventId)
            return
        }

        // 2. 쿠폰 정보 조회 (maxQuantity, expiredAt)
        val couponInfo = findCouponInfo(couponId)
        if (couponInfo == null) {
            rejectRequest(requestId, "쿠폰이 존재하지 않습니다")
            markHandled(eventId, eventType)
            return
        }

        // 3. DB COUNT 기반 ground truth 검증
        val issuedCount = countIssuedCoupons(couponId)
        if (issuedCount >= couponInfo.maxQuantity) {
            rejectRequest(requestId, "수량 소진")
            markHandled(eventId, eventType)
            return
        }

        // 4. UserCoupon 생성 (unique constraint로 중복 방어)
        try {
            createUserCoupon(couponId, userId, couponInfo.expiredAt)
        } catch (e: DataIntegrityViolationException) {
            rejectRequest(requestId, "이미 발급된 쿠폰")
            markHandled(eventId, eventType)
            return
        }

        // 5. 요청 상태 업데이트
        approveRequest(requestId)
        markHandled(eventId, eventType)
        log.info("쿠폰 발급 완료 [couponId={}, userId={}, requestId={}]", couponId, userId, requestId)
    }

    private fun findCouponInfo(couponId: Long): CouponQueryResult? {
        val results = jdbcTemplate.query(
            "SELECT max_quantity, expired_at FROM coupons WHERE id = ? AND deleted_at IS NULL",
            { rs, _ ->
                CouponQueryResult(
                    maxQuantity = rs.getInt("max_quantity"),
                    expiredAt = rs.getTimestamp("expired_at").toInstant().atZone(ZonedDateTime.now().zone),
                )
            },
            couponId,
        )
        return results.firstOrNull()
    }

    private fun countIssuedCoupons(couponId: Long): Long {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_coupons WHERE coupon_id = ?",
            Long::class.java,
            couponId,
        ) ?: 0L
    }

    private fun createUserCoupon(couponId: Long, userId: Long, expiredAt: ZonedDateTime) {
        jdbcTemplate.update(
            "INSERT INTO user_coupons (coupon_id, user_id, status, expired_at, created_at) VALUES (?, ?, 'AVAILABLE', ?, NOW(6))",
            couponId,
            userId,
            java.sql.Timestamp.from(expiredAt.toInstant()),
        )
    }

    private fun approveRequest(requestId: String) {
        jdbcTemplate.update(
            "UPDATE coupon_issue_requests SET status = 'ISSUED', processed_at = NOW(6), updated_at = NOW(6) WHERE request_id = ?",
            requestId,
        )
    }

    private fun rejectRequest(requestId: String, reason: String) {
        jdbcTemplate.update(
            "UPDATE coupon_issue_requests SET status = 'REJECTED', reject_reason = ?, processed_at = NOW(6), updated_at = NOW(6) WHERE request_id = ?",
            reason,
            requestId,
        )
    }

    private fun markHandled(eventId: String, eventType: String) {
        eventHandledRepository.save(EventHandled.create(eventId, eventType))
    }

    private data class CouponQueryResult(
        val maxQuantity: Int,
        val expiredAt: ZonedDateTime,
    )
}
