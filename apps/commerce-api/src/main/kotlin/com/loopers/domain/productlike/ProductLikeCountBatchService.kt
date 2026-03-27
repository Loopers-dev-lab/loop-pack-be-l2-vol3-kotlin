package com.loopers.domain.productlike

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductLikeCountBatchService(
    private val productLikeRepository: ProductLikeRepository,
    private val productLikeCountRepository: ProductLikeCountRepository,
) {
    companion object {
        private val log = LoggerFactory.getLogger(ProductLikeCountBatchService::class.java)
    }

    /**
     * 좋아요 집계 일일 재계산 배치.
     * 비동기 이벤트 처리 실패나 서버 크래시로 인한 집계 불일치를 자동으로 복구한다.
     * 매일 새벽 2시에 실행된다.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    fun reconcileProductLikeCount() {
        log.info("좋아요 집계 일일 재계산 배치 시작")

        try {
            // 모든 상품별 좋아요 개수 재계산
            val productLikeCounts = productLikeRepository.countByProductId()

            if (productLikeCounts.isEmpty()) {
                log.info("좋아요 기록이 없어 배치를 스킵합니다")
                return
            }

            var updatedCount = 0
            var skippedCount = 0

            productLikeCounts.forEach { dto ->
                try {
                    productLikeCountRepository.updateCount(dto.productId, dto.count)
                    updatedCount++
                } catch (e: Exception) {
                    log.error("상품별 좋아요 개수 업데이트 실패: productId=${dto.productId}, count=${dto.count}", e)
                    skippedCount++
                }
            }

            log.info(
                "좋아요 집계 일일 재계산 완료: 성공=${updatedCount}건, 실패=${skippedCount}건, 총=${productLikeCounts.size}건",
            )
        } catch (e: Exception) {
            log.error("좋아요 집계 일일 재계산 배치 실패", e)
            // 모니터링 알람 트리거 지점 (필요시 별도 구현)
        }
    }
}
