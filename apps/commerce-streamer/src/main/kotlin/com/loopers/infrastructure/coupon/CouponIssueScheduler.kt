package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponIssueService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@EnableScheduling
class CouponIssueScheduler(
    private val couponQueueRedisRepository: CouponQueueRedisRepository,
    private val couponIssueService: CouponIssueService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 1000)
    fun pollAndProcess() {
        val activeCouponIds = couponQueueRedisRepository.getActiveCouponIds()
        if (activeCouponIds.isEmpty()) return

        for (couponIdStr in activeCouponIds) {
            val couponId = couponIdStr.toLongOrNull() ?: continue
            val userIds = couponQueueRedisRepository.dequeue(couponId, 10)

            for (userIdStr in userIds) {
                val userId = userIdStr.toLongOrNull() ?: continue
                try {
                    couponIssueService.processIssue(couponId, userId)
                } catch (e: Exception) {
                    log.error("쿠폰 발급 처리 실패 - couponId: {}, userId: {}", couponId, userId, e)
                }
            }
        }
    }
}
