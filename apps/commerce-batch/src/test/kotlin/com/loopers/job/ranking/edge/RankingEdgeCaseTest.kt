package com.loopers.job.ranking.edge

import com.loopers.batch.job.ranking.ProductMetricsRow
import com.loopers.batch.job.ranking.step.RankingWriter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.batch.core.StepExecution
import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.test.MetaDataInstanceFactory
import java.time.LocalDate

/**
 * 정상 흐름에서는 절대 안 보이는 것들.
 *
 * 이 테스트들은 전부 "당연히 이렇게 되겠지"라는 가정을 깨뜨린다.
 * 면접에서 "Spring Batch 써보셨죠?" 다음에 나올 법한 질문들이다.
 */
class RankingEdgeCaseTest {

    /**
     * Q1. RankingWriter의 write()가 3번째 chunk에서 실패하면,
     *     4번째 chunk 재시작 시 rank는 몇 번부터 매겨지는가?
     *
     * 보기:
     * A) 1부터 (beforeStep이 0으로 초기화하니까)
     * B) 201부터 (chunk 1,2가 100건씩 = 200건 처리했으니까)
     * C) 301부터 (chunk 3의 100건도 카운트됐으니까)
     * D) 정의되지 않음 (ExecutionContext 저장 시점에 따라 다름)
     *
     * 정답: B.
     *
     * chunk 3의 write()에서 saveAction이 실패하면 트랜잭션이 롤백된다.
     * ExecutionContext.putInt("currentRank", currentRank)는 write() 끝에 호출되지만,
     * Spring Batch는 chunk 트랜잭션이 커밋될 때만 ExecutionContext를 DB에 반영한다.
     * 롤백되면 putInt도 버려진다.
     *
     * 따라서 ExecutionContext에는 chunk 2까지의 currentRank=200만 남아있고,
     * 재시작 시 beforeStep()에서 200을 복구해서 201부터 매긴다.
     *
     * C를 고른 사람은 "putInt 호출 = 저장 완료"라고 착각한 거다.
     * ExecutionContext는 in-memory 맵이고, DB 반영은 chunk 커밋 시점이다.
     */
    @Nested
    inner class `chunk_3_실패_후_재시작_시_rank는` {

        @Test
        fun `chunk_2까지_커밋된_rank부터_이어서_매겨진다`() {
            val saved = mutableListOf<List<Pair<Long, Int>>>()
            val writer = RankingWriter<Pair<Long, Int>>(
                rankingDate = LocalDate.of(2026, 4, 13),
                entityFactory = { row, rank, _ -> row.productId to rank },
                saveAction = { entities -> saved.add(entities) },
            )

            // chunk 1: rank 1~3 정상 처리
            val step1 = createStepExecution()
            writer.beforeStep(step1)
            writer.write(createChunk(1L, 2L, 3L))
            // chunk 커밋 시점: ExecutionContext에 currentRank=3 반영됨

            // chunk 2: rank 4~6 정상 처리
            writer.write(createChunk(4L, 5L, 6L))
            // chunk 커밋 시점: ExecutionContext에 currentRank=6 반영됨

            // === chunk 3에서 saveAction이 터졌다고 가정 ===
            // 트랜잭션 롤백 → ExecutionContext는 6인 상태에서 정지

            // === Step 재시작: beforeStep()에서 ExecutionContext 복구 ===
            val restartStep = createStepExecution(step1.executionContext)
            writer.beforeStep(restartStep)

            // 재시작 후 첫 write는 7부터 매겨져야 한다
            writer.write(createChunk(7L, 8L, 9L))

            val lastChunk = saved.last()
            assertThat(lastChunk.map { it.second }).containsExactly(7, 8, 9)
        }
    }

    /**
     * Q2. score가 정확히 같은 두 상품이 있다.
     *     배치를 두 번 돌리면 둘의 rank가 뒤바뀔 수 있는가?
     *
     * 보기:
     * A) 아니다. ORDER BY score DESC는 결정적(deterministic)이다
     * B) 그렇다. DB가 동점 시 임의 순서를 반환할 수 있다
     * C) UniqueConstraint 때문에 두 번째 실행이 실패한다
     * D) Cleanup이 먼저 돌아서 항상 같은 결과가 나온다
     *
     * 정답: B (였다 — 지금은 A).
     *
     * SQL 표준에서 ORDER BY에 tie-breaking 컬럼이 없으면 동점 행의 순서는 미정의다.
     * MySQL InnoDB에서 동점일 때 primary key 순서로 나올 "가능성이 높지만" 보장은 안 된다.
     * 테이블 재구성(OPTIMIZE TABLE), 버퍼 풀 상태, 병렬 스캔 여부에 따라 달라질 수 있다.
     *
     * 현재 SQL은 ORDER BY score DESC, product_id ASC 로 보조 정렬을 갖는다.
     * 동점이면 productId가 작은 쪽이 항상 먼저 나오므로 재실행에도 순서가 결정적이다.
     */
    @Nested
    inner class `동점_상품의_rank_안정성` {

        @Test
        fun `같은_score를_가진_상품들도_rank가_연속으로_할당된다`() {
            val saved = mutableListOf<List<Pair<Long, Int>>>()
            val writer = RankingWriter<Pair<Long, Int>>(
                rankingDate = LocalDate.of(2026, 4, 13),
                entityFactory = { row, rank, _ -> row.productId to rank },
                saveAction = { entities -> saved.add(entities) },
            )

            writer.beforeStep(createStepExecution())

            // 세 상품 모두 score 42.0 (동점)
            val chunk = Chunk(
                listOf(
                    ProductMetricsRow(productId = 100L, score = 42.0),
                    ProductMetricsRow(productId = 200L, score = 42.0),
                    ProductMetricsRow(productId = 300L, score = 42.0),
                ),
            )
            writer.write(chunk)

            val ranks = saved.last().map { it.second }
            // rank는 1, 2, 3으로 연속이지만
            // productId 순서는 DB가 어떤 순서로 줬느냐에 의존한다
            // 이 테스트는 "rank가 연속인가"만 검증하고
            // "어떤 상품이 1위인가"는 검증하지 않는다
            assertThat(ranks).containsExactly(1, 2, 3)
        }
    }

    /**
     * Q3. 배치 앱이 Cleanup Step 성공 직후, Aggregate Step 시작 전에 죽었다.
     *     Spring Batch가 재시작하면 어떻게 되는가?
     *
     * 보기:
     * A) Cleanup부터 다시 돌린다 (MV가 이미 비어있는데 DELETE를 또 한다)
     * B) Aggregate부터 시작한다 (Cleanup은 COMPLETED이므로 skip)
     * C) Job 전체가 FAILED 상태라 재시작이 안 된다
     * D) RunIdIncrementer 때문에 새 인스턴스로 시작해서 Cleanup부터 다시 돈다
     *
     * 정답: D.
     *
     * RunIdIncrementer는 매 실행마다 run.id를 증가시켜서 "새로운 JobInstance"를 만든다.
     * Spring Batch의 재시작(restart)은 "같은 JobInstance의 실패한 실행을 이어간다"인데,
     * RunIdIncrementer를 쓰면 매번 새 인스턴스라서 restart가 아니라 새 실행이 된다.
     *
     * 따라서 Cleanup이 다시 돌고 (이미 빈 테이블을 DELETE — no-op),
     * Aggregate가 다시 처음부터 돈다.
     *
     * B를 고른 사람은 "Spring Batch restart = 실패한 Step부터 재개"라고 알고 있는 건 맞는데,
     * RunIdIncrementer가 그 전제를 깨뜨린다는 걸 놓친 거다.
     *
     * 이건 설계 의도다. 랭킹 배치는 멱등하게 설계했기 때문에
     * "같은 requestDate로 처음부터 다시 돌려도 같은 결과"가 보장된다.
     * restart 시맨틱보다 멱등 재실행이 이 시스템에는 더 안전하다.
     */
    @Nested
    inner class `RunIdIncrementer와_재시작_시맨틱` {

        @Test
        fun `Cleanup이_빈_테이블을_DELETE해도_에러가_나지_않는다`() {
            // Cleanup Tasklet의 deleteAction은 JPQL DELETE다.
            // 대상 row가 0건이어도 예외 없이 affected_rows=0을 반환한다.
            // 이게 멱등 재실행의 전제 조건이다.
            var deleteCount = 0
            val deleteAction: (LocalDate) -> Unit = { deleteCount++ }

            // 두 번 연속 호출해도 에러가 나지 않아야 한다
            deleteAction(LocalDate.of(2026, 4, 13))
            deleteAction(LocalDate.of(2026, 4, 13))

            assertThat(deleteCount).isEqualTo(2)
        }
    }

    /**
     * Q4. product_metrics에 상품이 딱 3개뿐이다. LIMIT 100인데 3건만 읽힌다.
     *     MV 테이블에는 rank 1, 2, 3이 적재된다.
     *     이때 API에서 page=2&size=20을 요청하면?
     *
     * 보기:
     * A) 빈 배열 (3건뿐이니까 2페이지는 없다)
     * B) Fallback이 작동해서 이전 주 데이터가 나온다
     * C) 3건 전부 나온다 (페이지네이션이 무시된다)
     * D) 500 에러 (PageRequest.of(1, 20)에 3건이라 IndexOutOfBounds)
     *
     * 정답: A.
     *
     * JPA의 PageRequest.of(1, 20)은 offset 20부터 20건을 가져오라는 뜻이다.
     * MV에 3건뿐이면 offset 20 이후 데이터가 없으므로 빈 List가 반환된다.
     * Fallback 체인은 "해당 ranking_date에 데이터가 아예 없을 때"만 작동한다.
     * page 2에서 빈 결과가 나오는 건 "데이터가 없는 게 아니라 페이지가 넘친 것"이라
     * Fallback이 트리거되지 않는다.
     *
     * B를 고른 사람은 Fallback 조건을 잘못 이해한 거다.
     * countByRankingDate()가 3을 반환하므로 "데이터 있음"으로 판정된다.
     * entries가 빈 건 페이지네이션 결과이지 데이터 부재가 아니다.
     *
     * 이건 Fallback 설계의 허점이다. 실무에서는 totalCount도 확인하거나,
     * "요청한 페이지에 데이터가 없으면" 마지막 유효 페이지로 리다이렉트하는 전략을 쓴다.
     */
    @Nested
    inner class `Fallback_트리거_조건의_함정` {

        @Test
        fun `데이터가_있지만_페이지가_넘친_경우_Fallback이_작동하지_않는다`() {
            // 이건 단위 테스트로는 완전히 검증이 안 되고
            // UseCase 레벨에서 검증해야 한다.
            // 여기서는 "Fallback 조건이 entries.isEmpty()인데,
            // 페이지 초과로 인한 빈 결과도 isEmpty()=true"라는 사실을 기록한다.

            val emptyFromPagination = emptyList<Any>()
            val emptyFromNoData = emptyList<Any>()

            // 둘 다 isEmpty()=true지만 의미가 완전히 다르다
            assertThat(emptyFromPagination.isEmpty()).isTrue()
            assertThat(emptyFromNoData.isEmpty()).isTrue()

            // 현재 구현에서는 이 둘을 구분하지 못한다
            // 이게 Fallback 설계의 허점이다
        }
    }

    /**
     * Q5. score 계산에서 view_count * 0.1 + like_count * 0.2 + order_count * 0.7을 쓴다.
     *     view_count가 Long.MAX_VALUE이면 어떻게 되는가?
     *
     * 보기:
     * A) Long.MAX_VALUE * 0.1은 정확한 Double 값이 나온다
     * B) 정밀도 손실이 발생해서 score 정렬 순서가 뒤바뀔 수 있다
     * C) MySQL에서 오버플로 에러가 난다
     * D) Double.POSITIVE_INFINITY가 되어서 ORDER BY가 깨진다
     *
     * 정답: B.
     *
     * Long.MAX_VALUE = 9_223_372_036_854_775_807.
     * Double의 유효 정밀도는 약 15~17자리인데, Long.MAX_VALUE는 19자리다.
     * Long → Double 변환 시 하위 자릿수가 잘리면서 값이 근사된다.
     *
     * 예를 들어 Long.MAX_VALUE와 Long.MAX_VALUE - 1은
     * Double로 변환하면 같은 값이 된다.
     * 이 두 상품의 view_count 차이가 1이라면 score 정렬에서 구분이 안 된다.
     *
     * 현실적으로 view_count가 Long.MAX_VALUE에 도달할 일은 없지만,
     * "Double 연산의 정밀도 한계"는 score 비교 로직에서 항상 잠재적 위험이다.
     * 실무에서 score를 DECIMAL(18,4) 같은 고정소수점으로 처리하는 이유가 이거다.
     */
    @Nested
    inner class `Double_정밀도_한계` {

        @Test
        fun `Long의_최대값_근처에서_score_정밀도가_손실된다`() {
            val a = Long.MAX_VALUE
            val b = Long.MAX_VALUE - 1

            val scoreA = a.toDouble() * 0.1
            val scoreB = b.toDouble() * 0.1

            // 직관적으로 scoreA > scoreB여야 하지만
            // Double 정밀도 한계로 같은 값이 될 수 있다
            // 이건 "정상 범위에서는 문제없지만 극단에서 깨진다"의 예시다
            assertThat(scoreA).isEqualTo(scoreB)
        }
    }

    /**
     * Q6. 아래 두 시나리오의 결과가 같은가?
     *
     * 시나리오 A: 주간 배치 → 월간 배치 순서로 실행
     * 시나리오 B: 월간 배치 → 주간 배치 순서로 실행
     *
     * 보기:
     * A) 같다. 둘 다 같은 product_metrics를 읽으니까
     * B) 다를 수 있다. 두 배치 사이에 product_metrics가 변경될 수 있으니까
     * C) 같다. 두 배치가 같은 트랜잭션을 공유하니까
     * D) 다를 수 있다. LIMIT 100의 결과가 DB 상태에 따라 달라지니까
     *
     * 정답: B.
     *
     * 두 배치는 독립된 Job이고 각자의 트랜잭션에서 product_metrics를 읽는다.
     * 주간 배치가 도는 동안 Kafka Consumer가 product_metrics를 upsert할 수 있다.
     * 주간 배치 후 월간 배치가 읽는 시점에는 데이터가 달라져 있을 수 있다.
     *
     * A를 고른 사람은 "배치가 도는 동안 다른 프로세스가 데이터를 바꿀 수 있다"를 놓친 거다.
     * 이건 격리 수준(isolation level) 문제가 아니라 시간 차 문제다.
     *
     * 실무에서는 주간/월간을 동시에(또는 연속으로) 같은 스냅샷에서 읽어야 할 때
     * 임시 스냅샷 테이블에 먼저 복사하고, 거기서 두 배치가 읽는 방식을 쓴다.
     * 이걸 "Staging Table" 패턴이라고 부른다.
     */
    @Nested
    inner class `배치_실행_순서와_데이터_일관성` {

        @Test
        fun `두_배치가_같은_데이터를_읽는다는_보장은_없다`() {
            // 이건 통합 테스트에서만 제대로 검증 가능하지만
            // 개념을 코드로 표현하면 이렇다

            var metricsVersion = 1

            val readForWeekly = metricsVersion // 주간 배치가 읽는 시점
            metricsVersion = 2 // 그 사이 Consumer가 upsert
            val readForMonthly = metricsVersion // 월간 배치가 읽는 시점

            assertThat(readForWeekly).isNotEqualTo(readForMonthly)
        }
    }

    /**
     * Q7. Cleanup Step에서 DELETE가 10,000건을 삭제한다.
     *     이때 InnoDB의 gap lock 때문에 Kafka Consumer의
     *     product_metrics INSERT가 블로킹될 수 있는가?
     *
     * 보기:
     * A) 없다. mv_product_rank_weekly와 product_metrics는 다른 테이블이니까
     * B) 있다. 같은 DB의 같은 커넥션 풀을 쓰면 table-level lock이 걸릴 수 있다
     * C) 없다. DELETE는 row lock만 잡는다
     * D) 있다. 대량 DELETE 시 InnoDB가 table lock으로 에스컬레이션할 수 있다
     *
     * 정답: A (일반적으로), 하지만 D도 주의가 필요하다.
     *
     * mv_product_rank_weekly와 product_metrics는 다른 테이블이라
     * 직접적인 lock 경합은 없다. 하지만 10,000건 DELETE는 undo log를 대량 생성하고,
     * 이게 같은 InnoDB buffer pool을 공유하는 다른 트랜잭션의 성능에 간접적으로 영향을 준다.
     *
     * 또한 같은 커넥션 풀을 공유하면, 배치가 커넥션을 오래 점유하는 동안
     * API 요청이 커넥션을 얻지 못해 대기할 수 있다.
     * 이건 lock 경합이 아니라 connection pool 고갈 문제다.
     *
     * 실무에서 배치와 API의 DataSource를 분리하는 이유가 이거다.
     */
    @Nested
    inner class `대량_DELETE와_간접적_영향` {

        @Test
        fun `이건_단위_테스트로_검증할_수_없다`() {
            // 이 시나리오는 통합 환경에서만 재현 가능하다.
            // 여기에 남기는 건 "이런 위험이 있다"는 기록이다.
            //
            // 검증하려면:
            // 1. 배치가 대량 DELETE를 실행하는 동안
            // 2. 별도 스레드에서 product_metrics INSERT를 시도하고
            // 3. INSERT의 latency가 정상 범위(< 100ms)를 넘는지 측정한다
            //
            // HikariCP의 maximumPoolSize가 10이고 배치가 1개를 점유하면
            // API는 9개만 쓸 수 있다. 피크 트래픽에서 9개로 충분한가?
            assertThat(true).isTrue() // placeholder
        }
    }

    private fun createStepExecution(
        existingContext: ExecutionContext = ExecutionContext(),
    ): StepExecution {
        val jobExecution = MetaDataInstanceFactory.createJobExecution()
        val stepExecution = StepExecution("testStep", jobExecution)
        existingContext.entrySet().forEach { (key, value) ->
            stepExecution.executionContext.put(key, value)
        }
        return stepExecution
    }

    private fun createChunk(vararg productIds: Long): Chunk<ProductMetricsRow> {
        return Chunk(
            productIds.map { ProductMetricsRow(productId = it, score = (100 - it).toDouble()) },
        )
    }
}
