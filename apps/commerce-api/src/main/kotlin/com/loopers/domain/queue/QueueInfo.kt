package com.loopers.domain.queue

/** 대기열 진입 결과 */
data class QueueEntryInfo(
    /** 현재 대기 순번 (1-based). 즉시 토큰이 발급된 경우 0. */
    val position: Long,
    /** 예상 대기 시간 (초). 순번 / 초당 처리량으로 계산. 즉시 발급이면 0. */
    val estimatedWaitSeconds: Long,
    /** 클라이언트 폴링 권장 주기 (밀리초). 서버가 순번 구간별로 계산해서 내려줌. */
    val retryAfterMs: Long,
    /** 이미 대기열에 있던 유저 여부. true면 재진입 시도 (순번 유지). */
    val alreadyInQueue: Boolean,
)

/** 순번 조회 결과 */
data class QueuePositionInfo(
    /** 현재 대기 순번 (1-based). 토큰이 발급된 경우 0. */
    val position: Long,
    /** 예상 대기 시간 (초). 순번 / 초당 처리량으로 계산. 토큰 발급이면 0. */
    val estimatedWaitSeconds: Long,
    /** 클라이언트 폴링 권장 주기 (밀리초). position=0이면 1_000ms 고정. */
    val retryAfterMs: Long,
    /** 발급된 입장 토큰. 대기 중이면 null, 순번이 됐으면 UUID 값. 주문 API 호출 시 사용. */
    val token: String? = null,
    /** 현재 전체 대기 인원. 클라이언트에 대기열 혼잡도를 표시하는 용도. */
    val totalWaiting: Long,
)
