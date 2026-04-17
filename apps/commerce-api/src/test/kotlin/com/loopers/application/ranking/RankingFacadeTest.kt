package com.loopers.application.ranking

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductService
import com.loopers.domain.ranking.RankedProduct
import com.loopers.domain.ranking.RankingPage
import com.loopers.domain.ranking.RankingPeriod
import com.loopers.domain.ranking.RankingService
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("RankingFacade")
class RankingFacadeTest {

    private val rankingService: RankingService = mockk()
    private val productService: ProductService = mockk()
    private val brandService: BrandService = mockk()
    private val rankingFacade = RankingFacade(rankingService, productService, brandService)

    private val date = LocalDate.of(2026, 4, 10)

    @DisplayName("랭킹 조회 시 상품 정보가 Aggregation 되어 반환된다")
    @Test
    fun returnsRankingWithProductInfo() {
        // arrange
        every { rankingService.getTopRankings(RankingPeriod.DAILY, date, 1, 20) } returns RankingPage(
            entries = listOf(
                RankedProduct(rank = 1, productId = 10L, score = 5.0),
                RankedProduct(rank = 2, productId = 20L, score = 3.0),
            ),
            totalElements = 2,
            page = 1,
            size = 20,
        )
        every { productService.findAllByIds(listOf(10L, 20L)) } returns listOf(
            createProduct(id = 10L, name = "감성 티셔츠", price = 25000L, brandId = 1L),
            createProduct(id = 20L, name = "캔버스백", price = 5000L, brandId = 1L),
        )
        every { brandService.findAllByIds(listOf(1L)) } returns listOf(
            createBrand(id = 1L, name = "루프팩"),
        )

        // act
        val result = rankingFacade.getRankings(RankingPeriod.DAILY, date, page = 1, size = 20)

        // assert
        assertThat(result.content).hasSize(2)
        assertThat(result.content[0].rank).isEqualTo(1)
        assertThat(result.content[0].score).isEqualTo(5.0)
        assertThat(result.content[0].product.name).isEqualTo("감성 티셔츠")
        assertThat(result.content[0].product.brandName).isEqualTo("루프팩")
        assertThat(result.content[1].rank).isEqualTo(2)
        assertThat(result.content[1].product.name).isEqualTo("캔버스백")
        assertThat(result.totalElements).isEqualTo(2)
    }

    @DisplayName("랭킹이 비어있으면 빈 결과를 반환한다")
    @Test
    fun returnsEmptyWhenNoRankings() {
        every { rankingService.getTopRankings(RankingPeriod.DAILY, date, 1, 20) } returns RankingPage(
            entries = emptyList(),
            totalElements = 0,
            page = 1,
            size = 20,
        )

        val result = rankingFacade.getRankings(RankingPeriod.DAILY, date, page = 1, size = 20)

        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isEqualTo(0)
    }

    @DisplayName("삭제된 상품은 랭킹 결과에서 제외된다")
    @Test
    fun excludesDeletedProducts() {
        every { rankingService.getTopRankings(RankingPeriod.DAILY, date, 1, 20) } returns RankingPage(
            entries = listOf(
                RankedProduct(rank = 1, productId = 10L, score = 5.0),
                RankedProduct(rank = 2, productId = 999L, score = 3.0),
            ),
            totalElements = 2,
            page = 1,
            size = 20,
        )
        // productId=999 is deleted so not returned
        every { productService.findAllByIds(listOf(10L, 999L)) } returns listOf(
            createProduct(id = 10L, name = "감성 티셔츠", price = 25000L, brandId = 1L),
        )
        every { brandService.findAllByIds(listOf(1L)) } returns listOf(
            createBrand(id = 1L, name = "루프팩"),
        )

        val result = rankingFacade.getRankings(RankingPeriod.DAILY, date, page = 1, size = 20)

        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].product.id).isEqualTo(10L)
    }

    private fun createProduct(id: Long, name: String, price: Long, brandId: Long): ProductModel {
        val product = ProductModel(name = name, price = price, brandId = brandId, stockQuantity = 100)
        return spyk(product) { every { this@spyk.id } returns id }
    }

    private fun createBrand(id: Long, name: String): BrandModel {
        val brand = BrandModel(name = name)
        return spyk(brand) { every { this@spyk.id } returns id }
    }
}
