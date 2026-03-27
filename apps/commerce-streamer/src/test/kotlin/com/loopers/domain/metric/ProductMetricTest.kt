package com.loopers.domain.metric

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ProductMetric")
class ProductMetricTest {
    @Test
    @DisplayName("catalog event version이 증가할 때 조회 수치와 like_count 스냅샷을 반영한다")
    fun synchronizeCatalogMetrics() {
        val metric = ProductMetric.register(PRODUCT_ID)

        val viewed = metric.recordDetailViewed(1L)!!
        val synced = viewed.synchronizeLikeCount(2L, 7)!!

        assertThat(viewed.viewCount).isEqualTo(1)
        assertThat(synced.likeCount).isEqualTo(7)
        assertThat(synced.catalogEventVersion).isEqualTo(2L)
    }

    @Test
    @DisplayName("판매 수량은 order event 순서와 무관하게 누적한다")
    fun recordOrderEvents() {
        val metric = ProductMetric.register(PRODUCT_ID)

        val sold = metric.recordUnitsSold(2)
        val moreSold = sold.recordUnitsSold(3)

        assertThat(moreSold.unitsSold).isEqualTo(5)
        assertThat(moreSold.orderEventVersion).isEqualTo(0L)
    }

    @Test
    @DisplayName("catalog event version이 오래되면 like_count 스냅샷 반영도 무시한다")
    fun skipStaleSnapshotEvent() {
        val metric = ProductMetric.retrieve(
            productId = PRODUCT_ID,
            viewCount = 1,
            likeCount = 1,
            unitsSold = 2,
            catalogEventVersion = 5L,
            orderEventVersion = 7L,
        )

        assertThat(metric.recordDetailViewed(4L)).isNull()
        assertThat(metric.synchronizeLikeCount(4L, 9)).isNull()
        assertThat(metric.recordUnitsSold(1).unitsSold).isEqualTo(3)
    }

    companion object {
        private const val PRODUCT_ID = 100L
    }
}
