package com.loopers.interfaces.consumer

import com.loopers.config.kafka.KafkaConfig
import com.loopers.config.kafka.KafkaTopicConfig
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime

@Component
class FcfsCouponIssueConsumer(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopicConfig.COUPON_ISSUE_REQUEST_TOPIC],
        containerFactory = KafkaConfig.SINGLE_LISTENER,
        groupId = CONSUMER_GROUP,
    )
    fun onCouponIssueRequest(
        record: ConsumerRecord<String, ByteArray>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            val eventId = record.headers().lastHeader("event-id")?.value()?.let { String(it) }
            if (eventId != null && !tryMarkConsumed(eventId)) {
                log.debug("이미 처리된 이벤트 스킵: eventId={}", eventId)
                acknowledgment.acknowledge()
                return
            }

            val payload = objectMapper.readValue(record.value(), Map::class.java)
            val requestId = (payload["requestId"] as Number).toLong()
            val templateId = (payload["templateId"] as Number).toLong()
            val memberId = (payload["memberId"] as Number).toLong()

            log.info("선착순 쿠폰 발급 요청 수신: requestId={}, templateId={}, memberId={}", requestId, templateId, memberId)

            transactionTemplate.execute { processIssue(requestId, templateId, memberId) }

            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.error("선착순 쿠폰 발급 처리 실패: error={}", e.message, e)
        }
    }

    private fun processIssue(requestId: Long, templateId: Long, memberId: Long) {
        // 1. 수량 확인 (SELECT FOR UPDATE — 같은 TX 내에서 락 유지)
        val template = jdbcTemplate.queryForMap(
            "SELECT total_quantity, issued_quantity FROM fcfs_coupon_template WHERE id = ? FOR UPDATE",
            templateId,
        )
        val totalQuantity = (template["total_quantity"] as Number).toInt()
        val issuedQuantity = (template["issued_quantity"] as Number).toInt()

        if (issuedQuantity >= totalQuantity) {
            updateRequestStatus(requestId, "SOLD_OUT")
            log.info("쿠폰 소진: requestId={}, templateId={}", requestId, templateId)
            return
        }

        // 2. 중복 발급 체크
        val duplicateCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM fcfs_coupon_issue_request WHERE template_id = ? AND member_id = ? AND status = 'ISSUED'",
            Int::class.java,
            templateId,
            memberId,
        )!!
        if (duplicateCount > 0) {
            updateRequestStatus(requestId, "FAILED")
            log.info("중복 발급 요청: requestId={}, templateId={}, memberId={}", requestId, templateId, memberId)
            return
        }

        // 3. 쿠폰 발급 (issued_coupon 테이블에 INSERT)
        val now = ZonedDateTime.now()
        val expiredAt = jdbcTemplate.queryForObject(
            "SELECT ended_at FROM fcfs_coupon_template WHERE id = ?",
            ZonedDateTime::class.java,
            templateId,
        )!!

        jdbcTemplate.update(
            """
            INSERT INTO issued_coupon (coupon_template_id, member_id, status, expired_at, version, created_at, updated_at)
            VALUES (?, ?, 'AVAILABLE', ?, 0, ?, ?)
            """.trimIndent(),
            templateId,
            memberId,
            expiredAt,
            now,
            now,
        )

        // 4. issued_quantity 증가
        jdbcTemplate.update(
            "UPDATE fcfs_coupon_template SET issued_quantity = issued_quantity + 1, updated_at = ? WHERE id = ?",
            now,
            templateId,
        )

        // 5. 요청 상태 업데이트
        updateRequestStatus(requestId, "ISSUED")
        log.info("선착순 쿠폰 발급 완료: requestId={}, templateId={}, memberId={}", requestId, templateId, memberId)
    }

    private fun updateRequestStatus(requestId: Long, status: String) {
        jdbcTemplate.update(
            "UPDATE fcfs_coupon_issue_request SET status = ?, processed_at = ? WHERE id = ?",
            status,
            ZonedDateTime.now(),
            requestId,
        )
    }

    private fun tryMarkConsumed(eventId: String): Boolean {
        val inserted = jdbcTemplate.update(
            "INSERT IGNORE INTO kafka_consumed_event (event_id, consumer_group, handled_at) VALUES (?, ?, NOW())",
            eventId,
            CONSUMER_GROUP,
        )
        return inserted > 0
    }

    companion object {
        const val CONSUMER_GROUP = "commerce-streamer-fcfs-coupon"
    }
}
