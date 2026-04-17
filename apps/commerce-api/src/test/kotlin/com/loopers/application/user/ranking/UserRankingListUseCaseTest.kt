package com.loopers.application.user.ranking

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.ranking.MonthlyRankQueryRepository
import com.loopers.domain.ranking.ProductRankingQueryRepository
import com.loopers.domain.ranking.RankedProduct
import com.loopers.domain.ranking.WeeklyRankQueryRepository
import com.loopers.support.page.PageRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@DisplayName("UserRankingListUseCase")
class UserRankingListUseCaseTest {
    private val productRankingQueryRepository: ProductRankingQueryRepository = mock()
    private val weeklyRankQueryRepository: WeeklyRankQueryRepository = mock()
    private val monthlyRankQueryRepository: MonthlyRankQueryRepository = mock()
    private val productRepository: ProductRepository = mock()
    private val brandRepository: BrandRepository = mock()
    private val useCase = UserRankingListUseCase(
        productRankingQueryRepository,
        weeklyRankQueryRepository,
        monthlyRankQueryRepository,
        productRepository,
        brandRepository,
    )

    private val date = LocalDate.of(2026, 4, 10)

    @Nested
    @DisplayName("ZSET에서 Top-N 조회 후 DB에서 상품 정보를 합성하여 반환한다")
    inner class GetList {

        @Test
        @DisplayName("ZSET 순서대로 상품 정보가 합성되어 반환된다")
        fun getList_success() {
            val pageRequest = PageRequest().apply {
                page = 0
                size = 10
            }
            whenever(productRankingQueryRepository.getTopRanked(date, 0, 10))
                .thenReturn(
                    listOf(
                        RankedProduct(productId = 100, score = 10.5, rank = 1),
                        RankedProduct(productId = 101, score = 8.3, rank = 2),
                    ),
                )
            whenever(productRankingQueryRepository.getTotalCount(date)).thenReturn(50)
            whenever(productRepository.findAllByIdIn(listOf(100L, 101L)))
                .thenReturn(
                    listOf(
                        activeProduct(id = 100, name = "상품A"),
                        activeProduct(id = 101, name = "상품B"),
                    ),
                )
            whenever(brandRepository.findAllByIdIn(any()))
                .thenReturn(listOf(activeBrand(id = 1L)))

            val result = useCase.getList(date, RankingPeriod.DAILY, pageRequest)

            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].rank).isEqualTo(1)
            assertThat(result.content[0].productName).isEqualTo("상품A")
            assertThat(result.content[1].rank).isEqualTo(2)
            assertThat(result.content[1].productName).isEqualTo("상품B")
            assertThat(result.totalElements).isEqualTo(50)
        }
    }

    @Nested
    @DisplayName("page=0&size=10이면 offset=0, page=1이면 offset=10으로 조회한다")
    inner class Pagination {

        @Test
        @DisplayName("page=1, size=10 → offset=10으로 ZSET 조회")
        fun getList_secondPage() {
            val pageRequest = PageRequest().apply {
                page = 1
                size = 10
            }
            whenever(productRankingQueryRepository.getTopRanked(date, 10, 10))
                .thenReturn(emptyList())
            whenever(productRankingQueryRepository.getTotalCount(date)).thenReturn(5)

            val result = useCase.getList(date, RankingPeriod.DAILY, pageRequest)

            assertThat(result.content).isEmpty()
            assertThat(result.page).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("ZSET에는 존재하지만 DB에 없거나 비활성인 상품은 목록에서 제외된다")
    inner class FilterDeletedProducts {

        @Test
        @DisplayName("삭제된 상품은 결과에서 제외 (짧은 페이지 허용)")
        fun getList_filterDeletedProducts() {
            val pageRequest = PageRequest().apply {
                page = 0
                size = 10
            }
            whenever(productRankingQueryRepository.getTopRanked(date, 0, 10))
                .thenReturn(
                    listOf(
                        RankedProduct(productId = 100, score = 10.5, rank = 1),
                        RankedProduct(productId = 999, score = 8.3, rank = 2),
                    ),
                )
            whenever(productRankingQueryRepository.getTotalCount(date)).thenReturn(2)
            whenever(productRepository.findAllByIdIn(listOf(100L, 999L)))
                .thenReturn(listOf(activeProduct(id = 100, name = "상품A")))
            whenever(brandRepository.findAllByIdIn(any()))
                .thenReturn(listOf(activeBrand(id = 1L)))

            val result = useCase.getList(date, RankingPeriod.DAILY, pageRequest)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].productId).isEqualTo(100)
        }

        @Test
        @DisplayName("비활성 상품은 결과에서 제외")
        fun getList_filterInactiveProducts() {
            val pageRequest = PageRequest().apply {
                page = 0
                size = 10
            }
            whenever(productRankingQueryRepository.getTopRanked(date, 0, 10))
                .thenReturn(
                    listOf(
                        RankedProduct(productId = 100, score = 10.5, rank = 1),
                        RankedProduct(productId = 101, score = 8.3, rank = 2),
                    ),
                )
            whenever(productRankingQueryRepository.getTotalCount(date)).thenReturn(2)
            whenever(productRepository.findAllByIdIn(listOf(100L, 101L)))
                .thenReturn(
                    listOf(
                        activeProduct(id = 100, name = "상품A"),
                        inactiveProduct(id = 101, name = "상품B"),
                    ),
                )
            whenever(brandRepository.findAllByIdIn(any()))
                .thenReturn(listOf(activeBrand(id = 1L)))

            val result = useCase.getList(date, RankingPeriod.DAILY, pageRequest)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].productId).isEqualTo(100)
        }

        @Test
        @DisplayName("비활성 브랜드의 상품은 결과에서 제외")
        fun getList_filterInactiveBrand() {
            val pageRequest = PageRequest().apply {
                page = 0
                size = 10
            }
            whenever(productRankingQueryRepository.getTopRanked(date, 0, 10))
                .thenReturn(
                    listOf(
                        RankedProduct(productId = 100, score = 10.5, rank = 1),
                        RankedProduct(productId = 101, score = 8.3, rank = 2),
                    ),
                )
            whenever(productRankingQueryRepository.getTotalCount(date)).thenReturn(2)
            whenever(productRepository.findAllByIdIn(listOf(100L, 101L)))
                .thenReturn(
                    listOf(
                        activeProduct(id = 100, name = "상품A", brandId = 1L),
                        activeProduct(id = 101, name = "상품B", brandId = 2L),
                    ),
                )
            whenever(brandRepository.findAllByIdIn(listOf(1L, 2L)))
                .thenReturn(
                    listOf(
                        activeBrand(id = 1L),
                        inactiveBrand(id = 2L),
                    ),
                )

            val result = useCase.getList(date, RankingPeriod.DAILY, pageRequest)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].productId).isEqualTo(100)
        }
    }

    @Nested
    @DisplayName("데이터 없는 날짜 조회 시 빈 목록이 반환된다")
    inner class EmptyResult {

        @Test
        @DisplayName("ZSET이 비어있으면 빈 PageResponse 반환")
        fun getList_emptyZset() {
            val pageRequest = PageRequest()
            whenever(productRankingQueryRepository.getTopRanked(any(), any(), any()))
                .thenReturn(emptyList())
            whenever(productRankingQueryRepository.getTotalCount(any())).thenReturn(0)

            val result = useCase.getList(date, RankingPeriod.DAILY, pageRequest)

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("period에 따라 적절한 rank source로 분기한다")
    inner class PeriodRouting {

        @Test
        @DisplayName("period=WEEKLY → weeklyRankQueryRepository만 호출")
        fun getList_weeklyRoutesToWeeklyRepo() {
            val pageRequest = PageRequest().apply {
                page = 0
                size = 10
            }
            whenever(weeklyRankQueryRepository.getTopRanked(date, 0, 10)).thenReturn(emptyList())
            whenever(weeklyRankQueryRepository.getTotalCount(date)).thenReturn(0)

            useCase.getList(date, RankingPeriod.WEEKLY, pageRequest)

            verify(weeklyRankQueryRepository).getTopRanked(date, 0, 10)
            verify(weeklyRankQueryRepository).getTotalCount(date)
            verify(productRankingQueryRepository, never()).getTopRanked(any(), any(), any())
            verify(monthlyRankQueryRepository, never()).getTopRanked(any(), any(), any())
        }

        @Test
        @DisplayName("period=MONTHLY → monthlyRankQueryRepository만 호출")
        fun getList_monthlyRoutesToMonthlyRepo() {
            val pageRequest = PageRequest().apply {
                page = 0
                size = 10
            }
            whenever(monthlyRankQueryRepository.getTopRanked(date, 0, 10)).thenReturn(emptyList())
            whenever(monthlyRankQueryRepository.getTotalCount(date)).thenReturn(0)

            useCase.getList(date, RankingPeriod.MONTHLY, pageRequest)

            verify(monthlyRankQueryRepository).getTopRanked(date, 0, 10)
            verify(monthlyRankQueryRepository).getTotalCount(date)
            verify(productRankingQueryRepository, never()).getTopRanked(any(), any(), any())
            verify(weeklyRankQueryRepository, never()).getTopRanked(any(), any(), any())
        }

        @Test
        @DisplayName("period=DAILY → productRankingQueryRepository(Redis)만 호출")
        fun getList_dailyRoutesToRedisRepo() {
            val pageRequest = PageRequest().apply {
                page = 0
                size = 10
            }
            whenever(productRankingQueryRepository.getTopRanked(date, 0, 10)).thenReturn(emptyList())
            whenever(productRankingQueryRepository.getTotalCount(date)).thenReturn(0)

            useCase.getList(date, RankingPeriod.DAILY, pageRequest)

            verify(productRankingQueryRepository).getTopRanked(date, 0, 10)
            verify(weeklyRankQueryRepository, never()).getTopRanked(any(), any(), any())
            verify(monthlyRankQueryRepository, never()).getTopRanked(any(), any(), any())
        }
    }

    private fun activeProduct(
        id: Long,
        name: String,
        brandId: Long = 1L,
    ): Product =
        Product.retrieve(
            id = id,
            name = name,
            regularPrice = com.loopers.domain.common.Money(java.math.BigDecimal.valueOf(20000)),
            sellingPrice = com.loopers.domain.common.Money(java.math.BigDecimal.valueOf(15000)),
            brandId = brandId,
            imageUrl = null,
            thumbnailUrl = "thumb.jpg",
            likeCount = 0,
            status = Product.Status.ACTIVE,
        )

    private fun inactiveProduct(
        id: Long,
        name: String,
        brandId: Long = 1L,
    ): Product =
        Product.retrieve(
            id = id,
            name = name,
            regularPrice = com.loopers.domain.common.Money(java.math.BigDecimal.valueOf(20000)),
            sellingPrice = com.loopers.domain.common.Money(java.math.BigDecimal.valueOf(15000)),
            brandId = brandId,
            imageUrl = null,
            thumbnailUrl = "thumb.jpg",
            likeCount = 0,
            status = Product.Status.INACTIVE,
        )

    private fun activeBrand(id: Long): Brand =
        Brand.retrieve(id = id, name = "브랜드$id", status = Brand.Status.ACTIVE)

    private fun inactiveBrand(id: Long): Brand =
        Brand.retrieve(id = id, name = "브랜드$id", status = Brand.Status.INACTIVE)
}
