package com.loopers.application.product

import com.loopers.application.event.ProductViewedEvent
import com.loopers.application.event.UserActionLogEvent
import com.loopers.domain.brand.BrandReader
import com.loopers.domain.product.ProductChanger
import com.loopers.domain.product.ProductReader
import com.loopers.domain.product.ProductRegister
import com.loopers.domain.product.ProductRemover
import com.loopers.infrastructure.ranking.RankingRedisReader
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.ZonedDateTime

class ProductUseCaseTest {
    private val productRegister = mockk<ProductRegister>(relaxed = true)
    private val productReader = mockk<ProductReader>(relaxed = true)
    private val productChanger = mockk<ProductChanger>(relaxed = true)
    private val productRemover = mockk<ProductRemover>(relaxed = true)
    private val brandReader = mockk<BrandReader>(relaxed = true)
    private val productCacheStore = mockk<ProductCacheStore>()
    private val applicationEventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val rankingRedisReader = mockk<RankingRedisReader>()

    private val productUseCase = ProductUseCase(
        productRegister = productRegister,
        productReader = productReader,
        productChanger = productChanger,
        productRemover = productRemover,
        brandReader = brandReader,
        productCacheStore = productCacheStore,
        applicationEventPublisher = applicationEventPublisher,
        rankingRedisReader = rankingRedisReader,
    )

    @Test
    fun `상품_상세는_캐시된_상품정보에_현재_랭킹을_덧붙여_반환한다`() {
        val cached = ProductInfo.Detail(
            id = 1L,
            brandId = 10L,
            brandName = "브랜드",
            name = "상품",
            price = 1000L,
            description = "설명",
            stock = 10,
            status = "SELLING",
            likeCount = 3L,
            ranking = null,
        )
        every { productCacheStore.getDetail(1L, any()) } returns cached
        every { rankingRedisReader.getRank(any<ZonedDateTime>(), 1L) } returns 2L

        val result = productUseCase.getById(1L)

        assertThat(result.ranking).isEqualTo(2L)
        verify(exactly = 1) { applicationEventPublisher.publishEvent(match<UserActionLogEvent> { it.targetId == "1" }) }
        verify(exactly = 1) { applicationEventPublisher.publishEvent(match<ProductViewedEvent> { it.productId == 1L }) }
    }
}
