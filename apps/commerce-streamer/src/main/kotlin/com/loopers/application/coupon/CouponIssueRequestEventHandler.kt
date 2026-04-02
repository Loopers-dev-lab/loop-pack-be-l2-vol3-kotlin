package com.loopers.application.coupon

import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class CouponIssueRequestEventHandler(
    private val jdbcTemplate: JdbcTemplate,
) {
    @Transactional
    fun handle(requestId: Long) {
        val request = findRequestForUpdate(requestId) ?: return
        if (request.isFinalStatus()) {
            return
        }

        markProcessing(requestId)

        val duplicateCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM coupon_issue
            WHERE coupon_id = ? AND user_id = ? AND deleted_at IS NULL
            """.trimIndent(),
            Long::class.java,
            request.couponId,
            request.userId,
        ) ?: 0L
        if (duplicateCount > 0) {
            markDuplicate(requestId)
            return
        }

        val coupon = findCouponForUpdate(request.couponId) ?: run {
            markSoldOut(requestId)
            return
        }

        if (coupon.isExpired) {
            markExpired(requestId)
            return
        }

        if (coupon.quantity != null && coupon.issuedQuantity >= coupon.quantity) {
            markSoldOut(requestId)
            return
        }

        try {
            jdbcTemplate.update(
                """
                INSERT INTO coupon_issue (coupon_id, user_id, status, created_at, updated_at, deleted_at)
                VALUES (?, ?, 'AVAILABLE', NOW(), NOW(), NULL)
                """.trimIndent(),
                request.couponId,
                request.userId,
            )
        } catch (_: DuplicateKeyException) {
            markDuplicate(requestId)
            return
        }

        jdbcTemplate.update(
            """
            UPDATE coupon
            SET issued_quantity = issued_quantity + 1, updated_at = NOW()
            WHERE id = ?
            """.trimIndent(),
            request.couponId,
        )

        val couponIssueId = jdbcTemplate.queryForObject(
            """
            SELECT id
            FROM coupon_issue
            WHERE coupon_id = ? AND user_id = ? AND deleted_at IS NULL
            ORDER BY id DESC
            LIMIT 1
            """.trimIndent(),
            Long::class.java,
            request.couponId,
            request.userId,
        )

        jdbcTemplate.update(
            """
            UPDATE coupon_issue_request
            SET status = 'COMPLETED', coupon_issue_id = ?, updated_at = NOW()
            WHERE id = ?
            """.trimIndent(),
            couponIssueId,
            requestId,
        )
    }

    private fun findRequestForUpdate(requestId: Long): CouponIssueRequestRow? {
        return jdbcTemplate.query(
            """
            SELECT id, coupon_id, user_id, status
            FROM coupon_issue_request
            WHERE id = ? AND deleted_at IS NULL
            FOR UPDATE
            """.trimIndent(),
            { rs, _ ->
                CouponIssueRequestRow(
                    id = rs.getLong("id"),
                    couponId = rs.getLong("coupon_id"),
                    userId = rs.getLong("user_id"),
                    status = rs.getString("status"),
                )
            },
            requestId,
        ).firstOrNull()
    }

    private fun findCouponForUpdate(couponId: Long): CouponRow? {
        return jdbcTemplate.query(
            """
            SELECT id, quantity, issued_quantity, expired_at
            FROM coupon
            WHERE id = ? AND deleted_at IS NULL
            FOR UPDATE
            """.trimIndent(),
            { rs, _ ->
                CouponRow(
                    id = rs.getLong("id"),
                    quantity = rs.getLong("quantity").takeIf { !rs.wasNull() },
                    issuedQuantity = rs.getLong("issued_quantity"),
                    isExpired = rs.getTimestamp("expired_at").toInstant().isBefore(java.time.Instant.now()),
                )
            },
            couponId,
        ).firstOrNull()
    }

    private fun markProcessing(requestId: Long) {
        jdbcTemplate.update(
            "UPDATE coupon_issue_request SET status = 'PROCESSING', updated_at = NOW() WHERE id = ?",
            requestId,
        )
    }

    private fun markDuplicate(requestId: Long) {
        jdbcTemplate.update(
            "UPDATE coupon_issue_request SET status = 'DUPLICATE', updated_at = NOW() WHERE id = ?",
            requestId,
        )
    }

    private fun markSoldOut(requestId: Long) {
        jdbcTemplate.update(
            "UPDATE coupon_issue_request SET status = 'SOLD_OUT', updated_at = NOW() WHERE id = ?",
            requestId,
        )
    }

    private fun markExpired(requestId: Long) {
        jdbcTemplate.update(
            "UPDATE coupon_issue_request SET status = 'EXPIRED', updated_at = NOW() WHERE id = ?",
            requestId,
        )
    }
}

private data class CouponIssueRequestRow(
    val id: Long,
    val couponId: Long,
    val userId: Long,
    val status: String,
) {
    fun isFinalStatus(): Boolean {
        return status == "COMPLETED" || status == "DUPLICATE" || status == "SOLD_OUT" || status == "EXPIRED"
    }
}

private data class CouponRow(
    val id: Long,
    val quantity: Long?,
    val issuedQuantity: Long,
    val isExpired: Boolean,
)
