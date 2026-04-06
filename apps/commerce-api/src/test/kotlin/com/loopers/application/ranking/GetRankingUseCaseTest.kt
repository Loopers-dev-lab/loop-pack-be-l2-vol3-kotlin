package com.loopers.application.ranking

import com.loopers.domain.catalog.product.FakeProductRepository
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.catalog.product.vo.Stock
import com.loopers.domain.common.vo.BrandId
import com.loopers.domain.common.vo.Money
import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.ranking.FakeRankingRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class GetRankingUseCaseTest {

    private lateinit var rankingRepository: FakeRankingRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var useCase: GetRankingUseCase

    private val today = LocalDate.now(ZoneId.of("Asia/Seoul"))

    @BeforeEach
    fun setUp() {
        rankingRepository = FakeRankingRepository()
        productRepository = FakeProductRepository()
        useCase = GetRankingUseCase(rankingRepository, productRepository)

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
            val result = useCase.execute(date = today, page = 0, size = 10)

            // Assert
            assertThat(result.content).hasSize(3)
            val first = result.content[0]
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
            val result = useCase.execute(date = today, page = 0, size = 10)

            // Assert
            assertThat(result.content.map { it.productId }).containsExactly(2L, 3L, 1L)
            assertThat(result.content.map { it.rank }).containsExactly(1, 2, 3)
        }

        @Test
        @DisplayName("랭킹 데이터가 없으면 빈 결과를 반환한다")
        fun `랭킹 데이터가 없으면 빈 결과를 반환한다`() {
            // Act
            val result = useCase.execute(date = today, page = 0, size = 10)

            // Assert
            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0L)
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
            val result = useCase.execute(date = today, page = 0, size = 10)

            // Assert
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].productId).isEqualTo(1L)
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
            val result = useCase.execute(date = today, page = 0, size = 10)

            // Assert
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].productId).isEqualTo(1L)
        }

        @Test
        @DisplayName("score가 0 이하인 상품은 결과에서 제외된다")
        fun `score 0 이하는 제외된다`() {
            // Arrange
            rankingRepository.addEntry(today, 1L, 2.0)
            rankingRepository.addEntry(today, 2L, 0.0)
            rankingRepository.addEntry(today, 3L, -0.2)

            // Act
            val result = useCase.execute(date = today, page = 0, size = 10)

            // Assert
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].productId).isEqualTo(1L)
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
            val result = useCase.execute(date = null, page = 0, size = 10)

            // Assert
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].productId).isEqualTo(1L)
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
            val page0 = useCase.execute(date = today, page = 0, size = 2)
            val page1 = useCase.execute(date = today, page = 1, size = 2)

            // Assert — page 0: rank 1,2 / page 1: rank 3,4
            assertThat(page0.content.map { it.productId }).containsExactly(5L, 4L)
            assertThat(page0.content.map { it.rank }).containsExactly(1, 2)

            assertThat(page1.content.map { it.productId }).containsExactly(3L, 2L)
            assertThat(page1.content.map { it.rank }).containsExactly(3, 4)
        }

        @Test
        @DisplayName("totalElements는 ZCARD 기반으로 전체 랭킹 수를 반환한다")
        fun `totalElements는 전체 랭킹 수를 반환한다`() {
            // Arrange — 5개 등록, size=2로 조회
            rankingRepository.addEntry(today, 5L, 5.0)
            rankingRepository.addEntry(today, 4L, 4.0)
            rankingRepository.addEntry(today, 3L, 3.0)
            rankingRepository.addEntry(today, 2L, 2.0)
            rankingRepository.addEntry(today, 1L, 1.0)

            // Act
            val result = useCase.execute(date = today, page = 0, size = 2)

            // Assert — content는 2개지만 totalElements는 전체 5개
            assertThat(result.content).hasSize(2)
            assertThat(result.totalElements).isEqualTo(5L)
        }

        @Test
        @DisplayName("page > 0에서 빈 결과여도 totalElements는 전체 수를 유지한다")
        fun `빈 페이지에서도 totalElements를 유지한다`() {
            // Arrange — 3개 등록, page=5(offset=10)으로 범위 초과 조회
            rankingRepository.addEntry(today, 3L, 3.0)
            rankingRepository.addEntry(today, 2L, 2.0)
            rankingRepository.addEntry(today, 1L, 1.0)

            // Act
            val result = useCase.execute(date = today, page = 5, size = 2)

            // Assert — content는 비었지만 totalElements는 3 유지
            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(3L)
        }
    }
}
