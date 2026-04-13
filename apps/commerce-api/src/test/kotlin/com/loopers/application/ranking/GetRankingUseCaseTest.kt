package com.loopers.application.ranking

import com.loopers.domain.catalog.product.FakeProductRepository
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.vo.Stock
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.ranking.FakeRankingRepository
import com.loopers.domain.ranking.FakeWeeklyRankingRepository
import com.loopers.domain.ranking.RankingPeriod
import com.loopers.domain.ranking.model.WeeklyProductRank
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class GetRankingUseCaseTest {

    private lateinit var rankingRepository: FakeRankingRepository
    private lateinit var weeklyRankingRepository: FakeWeeklyRankingRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var useCase: GetRankingUseCase

    private val clock: Clock = Clock.fixed(
        LocalDate.of(2026, 4, 7).atTime(12, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(),
        ZoneId.of("Asia/Seoul"),
    )
    private val today: LocalDate = LocalDate.now(clock)

    @Suppress("EmptyFunctionBlock")
    private val noOpTxManager = object : AbstractPlatformTransactionManager() {
        override fun doGetTransaction() = Any()
        override fun doBegin(transaction: Any, definition: TransactionDefinition) {}
        override fun doCommit(status: DefaultTransactionStatus) {}
        override fun doRollback(status: DefaultTransactionStatus) {}
    }

    @BeforeEach
    fun setUp() {
        rankingRepository = FakeRankingRepository()
        weeklyRankingRepository = FakeWeeklyRankingRepository()
        productRepository = FakeProductRepository()
        useCase = GetRankingUseCase(rankingRepository, productRepository, weeklyRankingRepository, clock, noOpTxManager)

        listOf(1L, 2L, 3L, 4L, 5L).forEach { id ->
            productRepository.save(
                Product(
                    id = ProductId(id),
                    refBrandId = BrandId(1L),
                    name = "상품$id",
                    price = Money(BigDecimal.valueOf(id * 1000)),
                    stock = Stock(10),
                ),
            )
        }
    }

    @Nested
    @DisplayName("Top-N 조회")
    inner class TopN {

        @Test
        @DisplayName("상품 ID로 상품 상세 정보를 Aggregation하여 반환한다")
        fun `랭킹 항목에 상품 정보가 포함된다`() {
            // Arrange
            rankingRepository.addEntry(today, 2L, 3.0)
            rankingRepository.addEntry(today, 1L, 1.0)
            rankingRepository.addEntry(today, 3L, 2.0)

            // Act
            val result = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)

            // Assert
            assertThat(result.page.content).hasSize(3)
            val first = result.page.content[0]
            assertThat(first.rank).isEqualTo(1)
            assertThat(first.productId).isEqualTo(2L)
            assertThat(first.productName).isEqualTo("상품2")
            assertThat(first.price).isEqualByComparingTo(BigDecimal.valueOf(2000))
            assertThat(first.score).isCloseTo(3.0, Offset.offset(0.001))
        }

        @Test
        @DisplayName("ZSET 순서가 보존되어 점수 내림차순으로 반환된다")
        fun `점수 내림차순으로 반환된다`() {
            // Arrange
            rankingRepository.addEntry(today, 1L, 1.0)
            rankingRepository.addEntry(today, 2L, 3.0)
            rankingRepository.addEntry(today, 3L, 2.0)

            // Act
            val result = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)

            // Assert
            assertThat(result.page.content.map { it.productId }).containsExactly(2L, 3L, 1L)
            assertThat(result.page.content.map { it.rank }).containsExactly(1, 2, 3)
        }

        @Test
        @DisplayName("랭킹 데이터가 없으면 빈 결과를 반환한다")
        fun `랭킹 데이터가 없으면 빈 결과를 반환한다`() {
            // Act
            val result = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)

            // Assert
            assertThat(result.page.content).isEmpty()
            assertThat(result.page.totalElements).isEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("필터링")
    inner class Filtering {

        @Test
        @DisplayName("삭제된 상품은 결과에서 제외된다")
        fun `삭제된 상품은 제외된다`() {
            // Arrange
            productRepository.save(
                Product(
                    id = ProductId(10L),
                    refBrandId = BrandId(1L),
                    name = "삭제상품",
                    price = Money(BigDecimal.valueOf(10000)),
                    stock = Stock(10),
                    deletedAt = ZonedDateTime.now(),
                ),
            )
            rankingRepository.addEntry(today, 10L, 5.0)
            rankingRepository.addEntry(today, 1L, 3.0)

            // Act
            val result = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)

            // Assert
            assertThat(result.page.content).hasSize(1)
            assertThat(result.page.content[0].productId).isEqualTo(1L)
        }

        @Test
        @DisplayName("비활성(HIDDEN) 상품은 결과에서 제외된다")
        fun `비활성 상품은 제외된다`() {
            // Arrange
            productRepository.save(
                Product(
                    id = ProductId(10L),
                    refBrandId = BrandId(1L),
                    name = "숨김상품",
                    price = Money(BigDecimal.valueOf(10000)),
                    stock = Stock(10),
                    status = Product.ProductStatus.HIDDEN,
                ),
            )
            rankingRepository.addEntry(today, 10L, 5.0)
            rankingRepository.addEntry(today, 1L, 3.0)

            // Act
            val result = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)

            // Assert
            assertThat(result.page.content).hasSize(1)
            assertThat(result.page.content[0].productId).isEqualTo(1L)
        }

        @Test
        @DisplayName("score가 0 이하인 상품은 결과에서 제외된다")
        fun `score 0 이하는 제외된다`() {
            // Arrange
            rankingRepository.addEntry(today, 1L, 2.0)
            rankingRepository.addEntry(today, 2L, 0.0)
            rankingRepository.addEntry(today, 3L, -0.2)

            // Act
            val result = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)

            // Assert
            assertThat(result.page.content).hasSize(1)
            assertThat(result.page.content[0].productId).isEqualTo(1L)
        }

        @Test
        @DisplayName("DB에 존재하지 않는 상품은 결과에서 제외된다")
        fun `DB 미존재 상품은 제외된다`() {
            // Arrange — productId=999는 DB에 없음
            rankingRepository.addEntry(today, 999L, 10.0)
            rankingRepository.addEntry(today, 1L, 5.0)
            rankingRepository.addEntry(today, 2L, 3.0)

            // Act
            val result = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)

            // Assert
            assertThat(result.page.content).hasSize(2)
            assertThat(result.page.content.map { it.productId }).containsExactly(1L, 2L)
        }
    }

    @Nested
    @DisplayName("커서 정합성 (파싱 드랍 시뮬레이션)")
    inner class CursorIntegrity {

        @Test
        @DisplayName("파싱 드랍 발생 시 rawFetchCount 기준으로 offset이 전진하여 2번째 배치 항목도 누락 없이 반환된다")
        fun `파싱 드랍이 발생해도 offset이 rawFetchCount 기준으로 전진한다`() {
            // Arrange — 505개 항목 (FETCH_BATCH_SIZE=500 초과), parseDropCount=2
            // 배치 1: 500개 fetch → 2개 드랍 → 498개 entries, rawFetchCount=500
            //   버그(entries.size 기준): 498 < 500 → 조기 break, 배치 2 도달 불가
            //   수정(rawFetchCount 기준): 500 == 500 → continue, 배치 2 진행
            // 배치 2: 5개 fetch → 2개 드랍 → 3개 entries, rawFetchCount=5
            for (i in 6L..505L) {
                productRepository.save(
                    Product(
                        id = ProductId(i),
                        refBrandId = BrandId(1L),
                        name = "상품$i",
                        price = Money(BigDecimal.valueOf(i * 1000)),
                        stock = Stock(10),
                    ),
                )
            }
            for (i in 1L..505L) {
                rankingRepository.addEntry(today, i, i.toDouble())
            }
            rankingRepository.parseDropCount = 2

            // Act
            val result = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 600)

            // Assert — 배치 2의 3개 항목(3, 2, 1)이 포함되어야 한다
            val productIds = result.page.content.map { it.productId }
            assertThat(productIds).doesNotHaveDuplicates()
            assertThat(productIds).hasSize(501) // 498 (배치1) + 3 (배치2)
            assertThat(productIds).contains(3L, 2L, 1L) // 배치 2에서 온 항목
        }

        @Test
        @DisplayName("rawFetchCount가 0이면 루프를 종료한다")
        fun `rawFetchCount가 0이면 루프를 종료한다`() {
            // Arrange — 데이터 없음 → rawFetchCount=0

            // Act
            val result = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)

            // Assert — 빈 결과, 무한루프 없음
            assertThat(result.page.content).isEmpty()
            assertThat(result.page.totalElements).isEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("예외 처리")
    inner class ErrorHandling {

        @Test
        @DisplayName("Redis 장애 시 예외가 전파된다")
        fun `Redis 장애 시 예외가 전파된다`() {
            // Arrange
            rankingRepository.shouldThrow = true

            // Act & Assert
            org.junit.jupiter.api.assertThrows<RuntimeException> {
                useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)
            }
        }
    }

    @Nested
    @DisplayName("날짜 기본값")
    inner class DefaultDate {

        @Test
        @DisplayName("date가 null이면 오늘(KST) 날짜로 조회한다")
        fun `date가 null이면 오늘 날짜를 사용한다`() {
            // Arrange
            rankingRepository.addEntry(today, 1L, 5.0)

            // Act
            val result = useCase.execute(date = null, period = RankingPeriod.DAILY, page = 0, size = 10)

            // Assert
            assertThat(result.page.content).hasSize(1)
            assertThat(result.page.content[0].productId).isEqualTo(1L)
        }
    }

    @Nested
    @DisplayName("totalElements 캐시")
    inner class TotalCountCache {

        @Test
        @DisplayName("캐시 유효 기간 내 항목이 추가되어도 totalElements는 캐시값을 반환한다")
        fun `캐시 유효 기간 내 재호출 시 totalElements가 갱신되지 않는다`() {
            // Arrange — 2개 항목으로 1차 호출 → count=2 캐시
            rankingRepository.addEntry(today, 1L, 1.0)
            rankingRepository.addEntry(today, 2L, 2.0)
            val first = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)
            assertThat(first.page.totalElements).isEqualTo(2L)

            // 캐시 세팅 이후 새 항목 추가
            rankingRepository.addEntry(today, 3L, 3.0)

            // Act — TTL 내 재호출
            val second = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)

            // Assert — totalElements는 캐시값(2), content는 최신(3) → 캐시 존재 증거
            assertThat(second.page.totalElements).isEqualTo(2L)
            assertThat(second.page.content).hasSize(3)
        }

        @Test
        @DisplayName("날짜가 다르면 캐시를 공유하지 않고 독립적으로 계산한다")
        fun `날짜가 다르면 캐시를 공유하지 않는다`() {
            // Arrange — 오늘 2개 항목으로 캐시 세팅
            val yesterday = today.minusDays(1)
            rankingRepository.addEntry(today, 1L, 1.0)
            rankingRepository.addEntry(today, 2L, 2.0)
            useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10) // today count=2 캐시

            // 어제 항목은 캐시 이후에 추가 (today 캐시와 독립인지 확인)
            rankingRepository.addEntry(yesterday, 3L, 3.0)
            rankingRepository.addEntry(yesterday, 4L, 4.0)

            // Act — 어제 날짜로 호출
            val yesterdayResult = useCase.execute(date = yesterday, period = RankingPeriod.DAILY, page = 0, size = 10)

            // Assert — 어제는 캐시 없이 새로 계산 → 2개
            assertThat(yesterdayResult.page.totalElements).isEqualTo(2L)
        }

        @Test
        @DisplayName("TTL 만료 후 재호출 시 totalElements가 새 값으로 재계산된다")
        fun `TTL 만료 후 totalElements가 재계산된다`() {
            // 별도 MutableClock + UseCase 생성 (TTL 시간 조작을 위해 clock 분리)
            val mutableClock = MutableClock(clock.instant(), clock.zone)
            val localRepo = FakeRankingRepository()
            val ttlUseCase = GetRankingUseCase(localRepo, productRepository, weeklyRankingRepository, mutableClock, noOpTxManager)

            // step 1: 첫 호출로 totalElements 캐시 생성 (count=2)
            localRepo.addEntry(today, 1L, 1.0)
            localRepo.addEntry(today, 2L, 2.0)
            assertThat(ttlUseCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10).page.totalElements).isEqualTo(2L)

            // step 2: 같은 날짜에 항목 추가
            localRepo.addEntry(today, 3L, 3.0)

            // step 3: TTL 내 재호출 → stale 캐시 반환 (2)
            assertThat(ttlUseCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10).page.totalElements).isEqualTo(2L)

            // step 4: clock을 31초 전진 (TTL=30s 초과)
            mutableClock.advance(31)

            // step 5: TTL 만료 후 재호출 → 재계산된 새 값 (3)
            assertThat(ttlUseCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10).page.totalElements).isEqualTo(3L)
        }

        @Test
        @DisplayName("TTL 만료 후 다른 만료 엔트리가 누적되어도 동일 날짜 재조회 시 재계산값이 반환된다 (opportunistic cleanup)")
        fun `opportunistic cleanup이 일어나도 동일 날짜 재조회와 후속 경로가 정상 동작한다`() {
            // Arrange — MutableClock + 별도 UseCase 구성
            val mutableClock = MutableClock(clock.instant(), clock.zone)
            val localRepo = FakeRankingRepository()
            val cleanupUseCase = GetRankingUseCase(localRepo, productRepository, weeklyRankingRepository, mutableClock, noOpTxManager)
            val yesterday = today.minusDays(1)

            // step 1: yesterday + today 두 날짜로 캐시를 누적
            localRepo.addEntry(yesterday, 1L, 1.0)
            localRepo.addEntry(today, 1L, 1.0)
            localRepo.addEntry(today, 2L, 2.0)
            assertThat(cleanupUseCase.execute(date = yesterday, period = RankingPeriod.DAILY, page = 0, size = 10).page.totalElements).isEqualTo(1L)
            assertThat(cleanupUseCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10).page.totalElements).isEqualTo(2L)

            // step 2: 캐시 세팅 이후 today 항목 추가 → 재계산 시 새 값 검증용
            localRepo.addEntry(today, 3L, 3.0)

            // step 3: clock을 31초 전진 → yesterday/today 두 캐시 모두 만료 상태
            mutableClock.advance(31)

            // step 4: today 재조회 → opportunistic cleanup 트리거 + 동일 날짜는 재계산값(3) 반환
            val todayResult = cleanupUseCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)
            assertThat(todayResult.page.totalElements).isEqualTo(3L)

            // step 5: cleanup 이후 yesterday 재조회 경로도 정상 동작 (만료된 엔트리가 정리된 상태에서 새로 계산)
            localRepo.addEntry(yesterday, 2L, 2.0)
            val yesterdayResult = cleanupUseCase.execute(date = yesterday, period = RankingPeriod.DAILY, page = 0, size = 10)
            assertThat(yesterdayResult.page.totalElements).isEqualTo(2L)
        }

        @Test
        @DisplayName("캐시 만료 직후 연속 2회 호출해도 scanTotalVisibleCount는 단 1회만 실행된다 (compute 원자화 간접 검증)")
        fun `캐시 만료 후 연속 호출 시 scan은 1회만 실행된다`() {
            // Arrange — 데이터 1건만 있는 환경 (rawFetchCount < FETCH_BATCH_SIZE → 한 배치에 종료)
            val mutableClock = MutableClock(clock.instant(), clock.zone)
            val localRepo = FakeRankingRepository()
            val atomicUseCase = GetRankingUseCase(localRepo, productRepository, weeklyRankingRepository, mutableClock, noOpTxManager)
            localRepo.addEntry(today, 1L, 5.0)

            // step 1: 첫 호출로 캐시 세팅 (fetchRankings 1회 + scan 1회 = 2회)
            atomicUseCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)
            val countAfterFirst = localRepo.getTopNCallCount

            // step 2: TTL 만료
            mutableClock.advance(31)

            // step 3: 만료 직후 2차 호출 — compute 내부에서 scan 실행 (fetchRankings 1회 + scan 1회 = +2회)
            atomicUseCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)
            val countAfterSecond = localRepo.getTopNCallCount

            // step 4: 곧이어 3차 호출 — 캐시 hit 예상 (fetchRankings 1회만 = +1회)
            atomicUseCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)
            val countAfterThird = localRepo.getTopNCallCount

            // Assert
            assertThat(countAfterSecond - countAfterFirst)
                .`as`("2차 호출은 cache miss → scan 1회 + fetchRankings 1회 = 총 2회")
                .isEqualTo(2)
            assertThat(countAfterThird - countAfterSecond)
                .`as`("3차 호출은 cache hit → fetchRankings 1회만 = 총 1회 (scan skip)")
                .isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("페이지네이션")
    inner class Pagination {

        @Test
        @DisplayName("offset 기반으로 페이지가 나뉜다")
        fun `페이지가 올바르게 나뉜다`() {
            // Arrange
            rankingRepository.addEntry(today, 5L, 5.0)
            rankingRepository.addEntry(today, 4L, 4.0)
            rankingRepository.addEntry(today, 3L, 3.0)
            rankingRepository.addEntry(today, 2L, 2.0)
            rankingRepository.addEntry(today, 1L, 1.0)

            // Act
            val page0 = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 2)
            val page1 = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 1, size = 2)

            // Assert — page 0: rank 1,2 / page 1: rank 3,4
            assertThat(page0.page.content.map { it.productId }).containsExactly(5L, 4L)
            assertThat(page0.page.content.map { it.rank }).containsExactly(1, 2)

            assertThat(page1.page.content.map { it.productId }).containsExactly(3L, 2L)
            assertThat(page1.page.content.map { it.rank }).containsExactly(3, 4)
        }

        @Test
        @DisplayName("비활성 상품이 필터링되어도 페이지 경계가 정확하다")
        fun `비활성 상품 필터링 시 페이지 경계가 정확하다`() {
            // Arrange — 상품 6을 HIDDEN으로 등록 (비활성)
            productRepository.save(
                Product(
                    id = ProductId(6L),
                    refBrandId = BrandId(1L),
                    name = "비활성상품",
                    price = Money(BigDecimal.valueOf(6000)),
                    stock = Stock(10),
                    status = Product.ProductStatus.HIDDEN,
                ),
            )
            // 랭킹 순서: 6(hidden,6.0) > 5(5.0) > 4(4.0) > 3(3.0) > 2(2.0) > 1(1.0)
            rankingRepository.addEntry(today, 6L, 6.0)
            rankingRepository.addEntry(today, 5L, 5.0)
            rankingRepository.addEntry(today, 4L, 4.0)
            rankingRepository.addEntry(today, 3L, 3.0)
            rankingRepository.addEntry(today, 2L, 2.0)
            rankingRepository.addEntry(today, 1L, 1.0)

            // Act
            val page0 = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 2)
            val page1 = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 1, size = 2)

            // Assert — 비활성 상품 6은 제외, visible 순서: 5→4→3→2→1
            assertThat(page0.page.content.map { it.productId }).containsExactly(5L, 4L)
            assertThat(page1.page.content.map { it.productId }).containsExactly(3L, 2L)
            // 페이지 간 중복 없음
            val allIds = page0.page.content.map { it.productId } + page1.page.content.map { it.productId }
            assertThat(allIds).doesNotHaveDuplicates()
        }

        @Test
        @DisplayName("totalElements는 score > 0인 active 상품의 전체 수를 반환한다")
        fun `totalElements는 active 상품만 카운트한다`() {
            // Arrange — 5개 등록, size=2로 조회
            rankingRepository.addEntry(today, 5L, 5.0)
            rankingRepository.addEntry(today, 4L, 4.0)
            rankingRepository.addEntry(today, 3L, 3.0)
            rankingRepository.addEntry(today, 2L, 2.0)
            rankingRepository.addEntry(today, 1L, 1.0)

            // Act
            val result = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 2)

            // Assert — content는 2개지만 totalElements는 전체 active 5개
            assertThat(result.page.content).hasSize(2)
            assertThat(result.page.totalElements).isEqualTo(5L)
        }

        @Test
        @DisplayName("비활성/DB미존재 상품은 totalElements에서 제외된다")
        fun `비활성 상품은 totalElements에서 제외된다`() {
            // Arrange — 상품6=HIDDEN, 상품999=DB 미존재
            productRepository.save(
                Product(
                    id = ProductId(6L),
                    refBrandId = BrandId(1L),
                    name = "비활성상품",
                    price = Money(BigDecimal.valueOf(6000)),
                    stock = Stock(10),
                    status = Product.ProductStatus.HIDDEN,
                ),
            )
            rankingRepository.addEntry(today, 6L, 10.0)
            rankingRepository.addEntry(today, 999L, 8.0)
            rankingRepository.addEntry(today, 3L, 3.0)
            rankingRepository.addEntry(today, 2L, 2.0)
            rankingRepository.addEntry(today, 1L, 1.0)

            // Act
            val result = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 0, size = 10)

            // Assert — active 상품 3개만 카운트 (6=HIDDEN, 999=미존재 제외)
            assertThat(result.page.content).hasSize(3)
            assertThat(result.page.totalElements).isEqualTo(3L)
        }

        @Test
        @DisplayName("page > 0에서 빈 결과여도 totalElements는 전체 수를 유지한다")
        fun `빈 페이지에서도 totalElements를 유지한다`() {
            // Arrange — 3개 등록, page=5(offset=10)으로 범위 초과 조회
            rankingRepository.addEntry(today, 3L, 3.0)
            rankingRepository.addEntry(today, 2L, 2.0)
            rankingRepository.addEntry(today, 1L, 1.0)

            // Act
            val result = useCase.execute(date = today, period = RankingPeriod.DAILY, page = 5, size = 2)

            // Assert — content는 비었지만 totalElements는 3 유지
            assertThat(result.page.content).isEmpty()
            assertThat(result.page.totalElements).isEqualTo(3L)
        }
    }

    @Nested
    @DisplayName("period")
    inner class Period {

        @Test
        @DisplayName("DAILY 조회 시 periodKey는 요청 date의 ISO 날짜 포맷이다")
        fun `DAILY periodKey는 요청 date의 ISO 날짜 포맷이다`() {
            // Arrange
            val targetDate = LocalDate.of(2026, 4, 7)
            rankingRepository.addEntry(targetDate, 1L, 5.0)

            // Act
            val result = useCase.execute(date = targetDate, period = RankingPeriod.DAILY, page = 0, size = 10)

            // Assert
            assertThat(result.period).isEqualTo(RankingPeriod.DAILY)
            assertThat(result.periodKey).isEqualTo("2026-04-07")
        }

        @Test
        @DisplayName("DAILY date가 null이면 periodKey는 오늘 날짜의 ISO 포맷이다")
        fun `DAILY date null 시 periodKey는 오늘 날짜이다`() {
            // Act
            val result = useCase.execute(date = null, period = RankingPeriod.DAILY, page = 0, size = 10)

            // Assert
            assertThat(result.periodKey).isEqualTo(today.toString())
        }

        @Test
        @DisplayName("MONTHLY 조회 시 빈 RankingPageResult를 반환한다")
        fun `MONTHLY 조회 시 빈 결과를 반환한다`() {
            // Arrange
            rankingRepository.addEntry(today, 1L, 5.0)

            // Act
            val result = useCase.execute(date = today, period = RankingPeriod.MONTHLY, page = 0, size = 10)

            // Assert
            assertThat(result.period).isEqualTo(RankingPeriod.MONTHLY)
            assertThat(result.page.content).isEmpty()
            assertThat(result.page.totalElements).isEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("Weekly 주간 조회")
    inner class Weekly {

        private val periodKey2026W15 = "2026-W15"
        private val startDate = LocalDate.of(2026, 4, 6)
        private val endDate = LocalDate.of(2026, 4, 12)

        private fun weeklyEntry(
            rank: Int,
            productId: Long,
            score: Double = 1.0,
            pk: String = periodKey2026W15,
        ) = WeeklyProductRank(
            rank = rank,
            productId = productId,
            score = score,
            viewCount = 10L,
            likeCount = 5L,
            salesCount = 3L,
            periodKey = pk,
            periodStartDate = startDate,
            periodEndDate = endDate,
        )

        @Test
        @DisplayName("date가 속한 ISO 주의 periodKey(YYYY-Www 포맷)를 반환한다")
        fun `periodKey는 YYYY-Www 포맷이다`() {
            // Arrange
            weeklyRankingRepository.addEntry(weeklyEntry(1, 1L))
            val targetDate = LocalDate.of(2026, 4, 7) // 2026-W15

            // Act
            val result = useCase.execute(date = targetDate, period = RankingPeriod.WEEKLY, page = 0, size = 10)

            // Assert
            assertThat(result.period).isEqualTo(RankingPeriod.WEEKLY)
            assertThat(result.periodKey).isEqualTo("2026-W15")
        }

        @Test
        @DisplayName("2025-12-29는 ISO 주 기준 2026-W01에 속한다 (연말 경계)")
        fun `ISO Week 경계 - 연말일이 다음 해 1주차에 속한다`() {
            // Act
            val result = useCase.execute(date = LocalDate.of(2025, 12, 29), period = RankingPeriod.WEEKLY, page = 0, size = 10)

            // Assert
            assertThat(result.periodKey).isEqualTo("2026-W01")
        }

        @Test
        @DisplayName("date가 null이면 clock 기준 오늘 날짜의 주로 조회한다")
        fun `date null 시 오늘 날짜를 사용한다`() {
            // Arrange
            weeklyRankingRepository.addEntry(weeklyEntry(1, 1L))

            // Act
            val result = useCase.execute(date = null, period = RankingPeriod.WEEKLY, page = 0, size = 10)

            // Assert
            assertThat(result.periodKey).isEqualTo("2026-W15") // clock = 2026-04-07
        }

        @Test
        @DisplayName("MV의 active 상품 rank, productName, price, score를 조합해 반환한다")
        fun `active 상품의 랭킹 정보를 반환한다`() {
            // Arrange
            weeklyRankingRepository.addEntry(weeklyEntry(1, 1L, 10.0))
            weeklyRankingRepository.addEntry(weeklyEntry(2, 2L, 8.0))

            // Act
            val result = useCase.execute(date = LocalDate.of(2026, 4, 7), period = RankingPeriod.WEEKLY, page = 0, size = 10)

            // Assert
            assertThat(result.page.content).hasSize(2)
            assertThat(result.page.totalElements).isEqualTo(2L)
            with(result.page.content[0]) {
                assertThat(rank).isEqualTo(1)
                assertThat(productId).isEqualTo(1L)
                assertThat(productName).isEqualTo("상품1")
                assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(1000))
                assertThat(score).isCloseTo(10.0, Offset.offset(0.001))
            }
        }

        @Test
        @DisplayName("비활성(HIDDEN) 상품은 결과와 totalElements에서 제외된다")
        fun `비활성 상품은 제외된다`() {
            // Arrange
            productRepository.save(
                Product(
                    id = ProductId(6L),
                    refBrandId = BrandId(1L),
                    name = "숨김상품",
                    price = Money(BigDecimal.valueOf(6000)),
                    stock = Stock(10),
                    status = Product.ProductStatus.HIDDEN,
                ),
            )
            weeklyRankingRepository.addEntry(weeklyEntry(1, 1L, 10.0))
            weeklyRankingRepository.addEntry(weeklyEntry(2, 6L, 8.0)) // HIDDEN

            // Act
            val result = useCase.execute(date = LocalDate.of(2026, 4, 7), period = RankingPeriod.WEEKLY, page = 0, size = 10)

            // Assert
            assertThat(result.page.content).hasSize(1)
            assertThat(result.page.content[0].productId).isEqualTo(1L)
            assertThat(result.page.totalElements).isEqualTo(1L)
        }

        @Test
        @DisplayName("MV 5건 중 2건이 비활성이면 totalElements == 3이다")
        fun `totalElements는 active 필터링 후 남은 행 수이다`() {
            // Arrange
            listOf(6L, 7L).forEach { id ->
                productRepository.save(
                    Product(
                        id = ProductId(id),
                        refBrandId = BrandId(1L),
                        name = "숨김$id",
                        price = Money(BigDecimal.valueOf(id * 1000)),
                        stock = Stock(10),
                        status = Product.ProductStatus.HIDDEN,
                    ),
                )
            }
            listOf(1L, 2L, 3L).forEachIndexed { i, productId ->
                weeklyRankingRepository.addEntry(weeklyEntry(i + 1, productId))
            }
            weeklyRankingRepository.addEntry(weeklyEntry(4, 6L))
            weeklyRankingRepository.addEntry(weeklyEntry(5, 7L))

            // Act
            val result = useCase.execute(date = LocalDate.of(2026, 4, 7), period = RankingPeriod.WEEKLY, page = 0, size = 10)

            // Assert
            assertThat(result.page.totalElements).isEqualTo(3L)
            assertThat(result.page.content).hasSize(3)
        }

        @Test
        @DisplayName("page=1, size=2 요청 시 3~4번째 항목을 반환한다")
        fun `페이지네이션이 정상 동작한다`() {
            // Arrange
            (1..4).forEach { rank -> weeklyRankingRepository.addEntry(weeklyEntry(rank, rank.toLong())) }

            // Act
            val result = useCase.execute(date = LocalDate.of(2026, 4, 7), period = RankingPeriod.WEEKLY, page = 1, size = 2)

            // Assert
            assertThat(result.page.content).hasSize(2)
            assertThat(result.page.content[0].rank).isEqualTo(3)
            assertThat(result.page.content[1].rank).isEqualTo(4)
            assertThat(result.page.totalElements).isEqualTo(4L)
        }

        @Test
        @DisplayName("과거 날짜를 지정하면 해당 날짜가 속한 주의 periodKey로 조회한다")
        fun `과거 날짜는 해당 주의 periodKey로 조회된다`() {
            // Arrange - 2026-03-02 = 2026-W10
            val pastDate = LocalDate.of(2026, 3, 2)
            weeklyRankingRepository.addEntry(weeklyEntry(1, 1L, pk = "2026-W10"))

            // Act
            val result = useCase.execute(date = pastDate, period = RankingPeriod.WEEKLY, page = 0, size = 10)

            // Assert
            assertThat(result.periodKey).isEqualTo("2026-W10")
            assertThat(result.page.content).hasSize(1)
        }
    }
}

private class MutableClock(
    private var current: Instant,
    private val zone: ZoneId,
) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)
    override fun instant(): Instant = current
    fun advance(seconds: Long) {
        current = current.plusSeconds(seconds)
    }
}
