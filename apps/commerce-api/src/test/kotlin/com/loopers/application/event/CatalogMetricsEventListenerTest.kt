package com.loopers.application.event

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CatalogMetricsEventListenerTest {

    private val listener = CatalogMetricsEventListener()

    @Nested
    @DisplayName("CatalogEvent 수신 시")
    inner class HandleCatalogEvent {

        @Test
        @DisplayName("LikeAdded 이벤트를 처리한다")
        fun handleLikeAdded() {
            val event = CatalogEvent.LikeAdded(productId = 1L, userId = 2L)

            listener.handleCatalogEvent(event)
        }

        @Test
        @DisplayName("LikeRemoved 이벤트를 처리한다")
        fun handleLikeRemoved() {
            val event = CatalogEvent.LikeRemoved(productId = 1L, userId = 2L)

            listener.handleCatalogEvent(event)
        }

        @Test
        @DisplayName("ProductViewed 이벤트를 처리한다")
        fun handleProductViewed() {
            val event = CatalogEvent.ProductViewed(productId = 1L, userId = 2L)

            listener.handleCatalogEvent(event)
        }

        @Test
        @DisplayName("비인증 사용자의 ProductViewed 이벤트를 처리한다")
        fun handleProductViewed_nullUserId() {
            val event = CatalogEvent.ProductViewed(productId = 1L, userId = null)

            listener.handleCatalogEvent(event)
        }
    }
}
