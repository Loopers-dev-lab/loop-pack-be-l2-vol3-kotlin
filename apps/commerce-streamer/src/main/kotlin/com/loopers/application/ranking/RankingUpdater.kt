package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingEvent
import com.loopers.domain.ranking.RankingKeyPolicy
import com.loopers.domain.ranking.RankingRepository
import com.loopers.domain.ranking.ScoreCalculator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

/**
 * Consumer 가 호출하는 랭킹 갱신 진입점.
 *
 * 책임:
 *  1. 이벤트 → 점수 델타 변환 (ScoreCalculator 위임)
 *  2. 오늘 일자 키 결정 (RankingKeyPolicy 위임)
 *  3. ZINCRBY 호출 (RankingRepository 위임)
 *  4. 새 키에 대한 TTL 설정 (멱등, JVM 내 캐싱으로 EXPIRE 호출 최소화)
 *
 * Phase A 모드를 위한 [applyEvent] 만 우선 제공한다. Phase B 의 배치 모드는 후속 커밋에서 추가.
 *
 * 실패 정책 (멘토 노트):
 *  - Redis 호출은 DB 트랜잭션 밖에서 일어남 (consumer 가 metrics → ranking 순서로 호출)
 *  - 본 컴포넌트에서 던진 예외는 호출자(Consumer) 의 try/catch 로 흡수되며, 메인 트랜잭션에 영향 없음
 */
@Component
class RankingUpdater(
    private val rankingRepository: RankingRepository,
    private val scoreCalculator: ScoreCalculator,
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 이미 TTL 을 적용한 키를 캐싱해 매 호출마다 EXPIRE 가 발생하는 것을 막는다.
     * - 일자가 바뀌면 새 키가 들어와 자연스레 EXPIRE 가 1회 실행됨
     * - JVM 재시작 시 캐시는 비워지지만 EXPIRE 는 멱등이므로 무해
     * - 메모리 사용량은 상수에 가까움 (1 entry / day)
     */
    private val ttlAppliedKeys: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /**
     * 이벤트 1건을 ZSET 에 반영한다 (Phase A).
     *
     * 호출 시점에 [LocalDate.now] 를 기준으로 키를 결정한다. 즉, 이벤트가 발생한 시각이
     * 아니라 처리되는 시각을 기준으로 분류된다 (윈도우 경계 처리 단순화).
     */
    fun applyEvent(event: RankingEvent) {
        val key = currentKey()
        val delta = scoreCalculator.scoreFor(event)

        if (delta == 0.0) {
            log.debug("[RankingUpdater] zero delta skipped: event={}", event)
            return
        }

        rankingRepository.incrementScore(key, event.productId, delta)
        ensureTtl(key)
    }

    private fun currentKey(): String = RankingKeyPolicy.dailyKey(LocalDate.now(clock))

    private fun ensureTtl(key: String) {
        if (ttlAppliedKeys.add(key)) {
            rankingRepository.expire(key, RankingKeyPolicy.TTL)
            log.info("[RankingUpdater] TTL applied to new key: {}", key)
        }
    }
}
