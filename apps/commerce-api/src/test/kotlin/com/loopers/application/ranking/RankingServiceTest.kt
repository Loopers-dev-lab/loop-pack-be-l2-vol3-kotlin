package com.loopers.application.ranking

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.ranking.ProductRankingScore
import com.loopers.domain.ranking.RankingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class RankingServiceTest {

    @Mock
    private lateinit var rankingRepository: RankingRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var brandRepository: BrandRepository

    @InjectMocks
    private lateinit var rankingService: RankingService

    private val testDate = LocalDate.of(2026, 4, 8)

    @DisplayName("랭킹을 조회할 때,")
    @Nested
    inner class GetRankings {

        @DisplayName("ZSET 점수 순으로 상품 정보가 포함된 랭킹을 반환한다.")
        @Test
        fun returnsRankingsWithProductInfo_whenDataExists() {
            // arrange
            val rankingScores = listOf(
                ProductRankingScore(productId = 1L, score = 85.5),
                ProductRankingScore(productId = 2L, score = 72.3),
            )
            val product1 = createProduct(id = 1L, brandId = 10L, name = "에어맥스 90", price = BigDecimal("129000"))
            val product2 = createProduct(id = 2L, brandId = 20L, name = "울트라부스트", price = BigDecimal("189000"))
            val brand1 = createBrand(id = 10L, name = "나이키")
            val brand2 = createBrand(id = 20L, name = "아디다스")

            whenever(rankingRepository.getTopRankings(testDate, 0, 19)).thenReturn(rankingScores)
            whenever(rankingRepository.getTotalCount(testDate)).thenReturn(2L)
            whenever(productRepository.findAllByIds(listOf(1L, 2L))).thenReturn(listOf(product1, product2))
            whenever(brandRepository.findAllByIdIncludingDeleted(listOf(10L, 20L))).thenReturn(listOf(brand1, brand2))

            // act
            val result = rankingService.getRankings(testDate, 1, 20)

            // assert
            assertAll(
                { assertThat(result.rankings).hasSize(2) },
                { assertThat(result.rankings[0].rank).isEqualTo(1) },
                { assertThat(result.rankings[0].productName).isEqualTo("에어맥스 90") },
                { assertThat(result.rankings[0].brandName).isEqualTo("나이키") },
                { assertThat(result.rankings[0].score).isEqualTo(85.5) },
                { assertThat(result.rankings[1].rank).isEqualTo(2) },
                { assertThat(result.rankings[1].productName).isEqualTo("울트라부스트") },
                { assertThat(result.totalCount).isEqualTo(2L) },
            )
        }

        @DisplayName("ZSET에 데이터가 없으면, 빈 랭킹을 반환한다.")
        @Test
        fun returnsEmptyRankings_whenNoData() {
            // arrange
            whenever(rankingRepository.getTopRankings(testDate, 0, 19)).thenReturn(emptyList())
            whenever(rankingRepository.getTotalCount(testDate)).thenReturn(0L)

            // act
            val result = rankingService.getRankings(testDate, 1, 20)

            // assert
            assertAll(
                { assertThat(result.rankings).isEmpty() },
                { assertThat(result.totalCount).isEqualTo(0L) },
            )
        }

        @DisplayName("삭제된 상품은 랭킹에서 제외한다.")
        @Test
        fun excludesDeletedProducts_whenProductNotFound() {
            // arrange
            val rankingScores = listOf(
                ProductRankingScore(productId = 1L, score = 85.5),
                ProductRankingScore(productId = 999L, score = 72.3),
            )
            val product1 = createProduct(id = 1L, brandId = 10L, name = "에어맥스 90", price = BigDecimal("129000"))
            val brand1 = createBrand(id = 10L, name = "나이키")

            whenever(rankingRepository.getTopRankings(testDate, 0, 19)).thenReturn(rankingScores)
            whenever(rankingRepository.getTotalCount(testDate)).thenReturn(2L)
            whenever(productRepository.findAllByIds(listOf(1L, 999L))).thenReturn(listOf(product1))
            whenever(brandRepository.findAllByIdIncludingDeleted(listOf(10L))).thenReturn(listOf(brand1))

            // act
            val result = rankingService.getRankings(testDate, 1, 20)

            // assert
            assertAll(
                { assertThat(result.rankings).hasSize(1) },
                { assertThat(result.rankings[0].productId).isEqualTo(1L) },
            )
        }
    }

    @DisplayName("상품 순위를 조회할 때,")
    @Nested
    inner class GetProductRank {

        @DisplayName("순위가 있으면, 1-based 순위를 반환한다.")
        @Test
        fun returnsOneBased_whenRankExists() {
            // arrange
            whenever(rankingRepository.getProductRank(1L, testDate)).thenReturn(0L)

            // act
            val result = rankingService.getProductRank(1L, testDate)

            // assert
            assertThat(result).isEqualTo(1)
        }

        @DisplayName("순위가 없으면, null을 반환한다.")
        @Test
        fun returnsNull_whenRankNotExists() {
            // arrange
            whenever(rankingRepository.getProductRank(999L, testDate)).thenReturn(null)

            // act
            val result = rankingService.getProductRank(999L, testDate)

            // assert
            assertThat(result).isNull()
        }
    }

    private fun createProduct(
        id: Long,
        brandId: Long,
        name: String,
        price: BigDecimal,
    ): Product {
        val product = Product(
            brandId = brandId,
            name = name,
            price = price,
            stock = 100,
            description = null,
            imageUrl = null,
        )
        ReflectionTestUtils.setField(product, "id", id)
        return product
    }

    private fun createBrand(id: Long, name: String): Brand {
        val brand = Brand(name = name, description = null)
        ReflectionTestUtils.setField(brand, "id", id)
        return brand
    }
}
