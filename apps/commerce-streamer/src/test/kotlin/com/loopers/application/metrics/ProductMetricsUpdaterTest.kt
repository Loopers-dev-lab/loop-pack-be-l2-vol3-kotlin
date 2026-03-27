package com.loopers.application.metrics

import com.loopers.infrastructure.metrics.ProductMetricsEntity
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.Optional

class ProductMetricsUpdaterTest {

    @Test
    fun `오래된_좋아요_이벤트는_무시한다`() {
        val repository = mockk<ProductMetricsJpaRepository>()
        val updater = ProductMetricsUpdater(repository)
        val saved = slot<ProductMetricsEntity>()
        val metrics = ProductMetricsEntity(
            productId = 1L,
            likeCount = 5L,
            lastLikeEventAt = ZonedDateTime.parse("2026-03-27T10:00:00+09:00[Asia/Seoul]"),
        )

        every { repository.findById(1L) } returns Optional.of(metrics)
        every { repository.save(capture(saved)) } answers { saved.captured }

        updater.increaseLikeCount(
            productId = 1L,
            occurredAt = ZonedDateTime.parse("2026-03-27T09:59:59+09:00[Asia/Seoul]"),
        )

        assertThat(saved.isCaptured).isFalse()
        assertThat(metrics.likeCount).isEqualTo(5L)
    }

    @Test
    fun `더_최근_판매_이벤트면_집계를_반영한다`() {
        val repository = mockk<ProductMetricsJpaRepository>()
        val updater = ProductMetricsUpdater(repository)
        val saved = slot<ProductMetricsEntity>()
        val metrics = ProductMetricsEntity(
            productId = 1L,
            salesCount = 2L,
            lastSalesEventAt = ZonedDateTime.parse("2026-03-27T10:00:00+09:00[Asia/Seoul]"),
        )

        every { repository.findById(1L) } returns Optional.of(metrics)
        every { repository.save(capture(saved)) } answers { saved.captured }

        updater.increaseSalesCount(
            productId = 1L,
            quantity = 3L,
            occurredAt = ZonedDateTime.parse("2026-03-27T10:00:01+09:00[Asia/Seoul]"),
        )

        assertThat(saved.captured.salesCount).isEqualTo(5L)
        assertThat(saved.captured.lastSalesEventAt)
            .isEqualTo(ZonedDateTime.parse("2026-03-27T10:00:01+09:00[Asia/Seoul]"))
    }
}
