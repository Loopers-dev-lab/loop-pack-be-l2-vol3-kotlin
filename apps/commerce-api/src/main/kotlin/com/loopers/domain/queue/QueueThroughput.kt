package com.loopers.domain.queue

import kotlin.math.ceil

/**
 * 대기열 처리량 정책. 산정 근거: docs/design/06-waiting-queue.md
 *
 * - DB 커넥션 풀 40 (modules/jpa/jpa.yml maximum-pool-size)
 * - 주문 1건 평균 처리 시간 200ms
 * - 이론적 최대 TPS: 40 / 0.2 = 200
 * - 안전 마진 70%: 설계 처리량 140 TPS
 *
 * 스케줄러 배치 크기와 예상 대기 시간이 모두 이 값에서 파생된다.
 * 커넥션 풀이나 처리 시간이 바뀌면 이 파일만 수정한다.
 */
object QueueThroughput {
    const val DESIGN_TPS = 140.0
    const val SCHEDULER_INTERVAL_MS = 100L

    val BATCH_SIZE: Long = (DESIGN_TPS * SCHEDULER_INTERVAL_MS / 1000).toLong()

    /**
     * 순번 기준 예상 대기 시간(초). 스케줄러가 100ms 단위로 이산 처리하므로
     * 내림이 아니라 올림으로 계산해 과소 안내를 막는다.
     */
    fun estimateWaitSeconds(position: Long): Long {
        return ceil(position / DESIGN_TPS).toLong().coerceAtLeast(1)
    }
}
