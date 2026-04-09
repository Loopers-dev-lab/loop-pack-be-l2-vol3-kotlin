package com.loopers.application.ranking

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandName
import com.loopers.domain.brand.fixture.FakeBrandRepository
import com.loopers.domain.product.Money
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductName
import com.loopers.domain.product.Stock
import com.loopers.domain.product.fixture.FakeProductRepository
import com.loopers.domain.ranking.fixture.FakeRankingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GetRankingUseCaseTest {

    private lateinit var rankingRepository: FakeRankingRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var getRankingUseCase: GetRankingUseCase

    @BeforeEach
    fun setUp() {
        rankingRepository = FakeRankingRepository()
        productRepository = FakeProductRepository()
        brandRepository = FakeBrandRepository()
        getRankingUseCase = GetRankingUseCase(rankingRepository, productRepository, brandRepository)
    }

    @Nested
    inner class GetRankingPage {

        @Test
        fun `랭킹 페이지를 조회하면 상품 정보가 포함된 결과를 반환한다`() {
            val brandId = brandRepository.save(createBrand())
            val productId1 = productRepository.save(createProduct(brandId, "상품A"))
            val productId2 = productRepository.save(createProduct(brandId, "상품B"))

            rankingRepository.addScore(DATE, productId1, 15.0)
            rankingRepository.addScore(DATE, productId2, 10.0)

            val result = getRankingUseCase.getRankingPage(DATE, 1, 20)

            assertThat(result.rankings).hasSize(2)
            assertThat(result.rankings[0].rank).isEqualTo(1)
            assertThat(result.rankings[0].productId).isEqualTo(productId1)
            assertThat(result.rankings[0].productName).isEqualTo("상품A")
            assertThat(result.rankings[0].brandName).isEqualTo(BRAND_NAME)
            assertThat(result.rankings[1].rank).isEqualTo(2)
            assertThat(result.rankings[1].productId).isEqualTo(productId2)
            assertThat(result.totalCount).isEqualTo(2)
        }

        @Test
        fun `가중치 적용이 랭킹 순서에 반영된다`() {
            val brandId = brandRepository.save(createBrand())
            val productId1 = productRepository.save(createProduct(brandId, "주문1건상품"))
            val productId2 = productRepository.save(createProduct(brandId, "좋아요3건상품"))

            // 주문 1건: 0.7 * 1 = 0.7
            rankingRepository.addScore(DATE, productId1, 0.7)
            // 좋아요 3건: 0.2 * 3 = 0.6
            rankingRepository.addScore(DATE, productId2, 0.6)

            val result = getRankingUseCase.getRankingPage(DATE, 1, 20)

            assertThat(result.rankings[0].productId).isEqualTo(productId1)
            assertThat(result.rankings[1].productId).isEqualTo(productId2)
        }

        @Test
        fun `랭킹 데이터가 없으면 빈 목록을 반환한다`() {
            val result = getRankingUseCase.getRankingPage(DATE, 1, 20)

            assertThat(result.rankings).isEmpty()
            assertThat(result.totalCount).isEqualTo(0)
        }

        @Test
        fun `페이지네이션이 정상 동작한다`() {
            val brandId = brandRepository.save(createBrand())
            val productIds = (1..5).map {
                productRepository.save(createProduct(brandId, "상품$it"))
            }
            productIds.forEachIndexed { index, id ->
                rankingRepository.addScore(DATE, id, (50 - index * 10).toDouble())
            }

            val page1 = getRankingUseCase.getRankingPage(DATE, 1, 2)
            val page2 = getRankingUseCase.getRankingPage(DATE, 2, 2)

            assertThat(page1.rankings).hasSize(2)
            assertThat(page1.rankings[0].rank).isEqualTo(1)
            assertThat(page1.rankings[1].rank).isEqualTo(2)
            assertThat(page2.rankings).hasSize(2)
            assertThat(page2.rankings[0].rank).isEqualTo(3)
            assertThat(page2.rankings[1].rank).isEqualTo(4)
        }

        @Test
        fun `삭제된 상품은 랭킹 결과에서 제외된다`() {
            val brandId = brandRepository.save(createBrand())
            val activeProductId = productRepository.save(createProduct(brandId, "활성상품"))
            val deletedProduct = createProduct(brandId, "삭제상품").delete()
            val deletedProductId = productRepository.save(deletedProduct)

            rankingRepository.addScore(DATE, activeProductId, 10.0)
            rankingRepository.addScore(DATE, deletedProductId, 20.0)

            val result = getRankingUseCase.getRankingPage(DATE, 1, 20)

            assertThat(result.rankings).hasSize(1)
            assertThat(result.rankings[0].productId).isEqualTo(activeProductId)
        }

        @Test
        fun `다른 날짜의 랭킹은 서로 독립적이다`() {
            val brandId = brandRepository.save(createBrand())
            val productId = productRepository.save(createProduct(brandId))

            rankingRepository.addScore("20260408", productId, 100.0)
            rankingRepository.addScore("20260409", productId, 50.0)

            val yesterday = getRankingUseCase.getRankingPage("20260408", 1, 20)
            val today = getRankingUseCase.getRankingPage("20260409", 1, 20)

            assertThat(yesterday.rankings[0].score).isEqualTo(100.0)
            assertThat(today.rankings[0].score).isEqualTo(50.0)
        }
    }

    @Nested
    inner class GetProductRank {

        @Test
        fun `상품의 순위를 정상 조회한다`() {
            rankingRepository.addScore(DATE, 1L, 100.0)
            rankingRepository.addScore(DATE, 2L, 50.0)

            val result = getRankingUseCase.getProductRank(DATE, 2L)

            assertThat(result).isNotNull
            assertThat(result!!.rank).isEqualTo(2)
            assertThat(result.score).isEqualTo(50.0)
        }

        @Test
        fun `랭킹에 없는 상품은 null을 반환한다`() {
            val result = getRankingUseCase.getProductRank(DATE, 999L)

            assertThat(result).isNull()
        }
    }

    private fun createBrand() = Brand.create(
        name = BrandName(BRAND_NAME),
        description = null,
        logoUrl = null,
    )

    private fun createProduct(brandId: Long, name: String = PRODUCT_NAME) = Product.create(
        brandId = brandId,
        name = ProductName(name),
        description = null,
        price = Money(PRICE),
        stock = Stock(STOCK),
        thumbnailUrl = null,
        images = emptyList(),
    )

    companion object {
        private const val DATE = "20260409"
        private const val BRAND_NAME = "테스트브랜드"
        private const val PRODUCT_NAME = "테스트상품"
        private const val PRICE = 10000L
        private const val STOCK = 100
    }
}
