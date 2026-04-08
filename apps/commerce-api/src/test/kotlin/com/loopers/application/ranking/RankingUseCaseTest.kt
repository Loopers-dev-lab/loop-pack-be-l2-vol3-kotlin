package com.loopers.application.ranking

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandReader
import com.loopers.domain.brand.vo.BrandName
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductReader
import com.loopers.domain.product.vo.ProductDescription
import com.loopers.domain.product.vo.ProductName
import com.loopers.domain.product.vo.ProductPrice
import com.loopers.domain.product.vo.Stock
import com.loopers.infrastructure.ranking.RankedProductScore
import com.loopers.infrastructure.ranking.RankingRedisReader
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RankingUseCaseTest {
    private val rankingRedisReader = mockk<RankingRedisReader>()
    private val productReader = mockk<ProductReader>()
    private val brandReader = mockk<BrandReader>()
    private val rankingUseCase = RankingUseCase(rankingRedisReader, productReader, brandReader)

    @Test
    fun `랭킹_페이지는_레디스_순서를_기준으로_상품정보를_조합한다`() {
        every { rankingRedisReader.getPage("20260408", 0L, 19L) } returns listOf(
            RankedProductScore(rank = 1L, productId = 2L, score = 3.0),
            RankedProductScore(rank = 2L, productId = 1L, score = 1.0),
        )
        every { productReader.getAllByIds(listOf(2L, 1L)) } returns listOf(product(id = 1L), product(id = 2L, likeCount = 5L))
        every { brandReader.getAllByIds(listOf(10L)) } returns listOf(Brand(id = 10L, name = BrandName("브랜드")))

        val result = rankingUseCase.getPage(date = "20260408", size = 20, page = 1)

        assertThat(result).hasSize(2)
        assertThat(result[0].productId).isEqualTo(2L)
        assertThat(result[0].rank).isEqualTo(1L)
        assertThat(result[0].likeCount).isEqualTo(5L)
        assertThat(result[1].productId).isEqualTo(1L)
        assertThat(result[1].rank).isEqualTo(2L)
    }

    private fun product(id: Long, likeCount: Long = 0L): Product {
        return Product(
            id = id,
            brandId = 10L,
            name = ProductName("상품$id"),
            price = ProductPrice(1000L),
            description = ProductDescription("설명$id"),
            stock = Stock(10),
            likeCount = likeCount,
        )
    }
}
