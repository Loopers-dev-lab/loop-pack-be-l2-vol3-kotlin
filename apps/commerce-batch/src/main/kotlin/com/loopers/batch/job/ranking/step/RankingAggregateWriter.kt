package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.RankingAggregateTemp
import com.loopers.infrastructure.ranking.RankingAggregateTempJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

/**
 * 중간 테이블 적재 Writer.
 *
 * Processor가 변환한 RankingAggregateTemp 엔티티를 배치 단위로 저장한다.
 * 상태가 없고 JobParameters에도 의존하지 않으므로 @StepScope 없이 싱글톤 @Component.
 */
@Component
class RankingAggregateWriter(
    private val tempJpaRepository: RankingAggregateTempJpaRepository,
) : ItemWriter<RankingAggregateTemp> {

    companion object {
        private val log = LoggerFactory.getLogger(RankingAggregateWriter::class.java)
    }

    override fun write(chunk: Chunk<out RankingAggregateTemp>) {
        val items = chunk.items.toList()
        if (items.isEmpty()) return

        tempJpaRepository.saveAll(items)
        log.info("[랭킹 집계] 중간 테이블 {}건 적재", items.size)
    }
}
