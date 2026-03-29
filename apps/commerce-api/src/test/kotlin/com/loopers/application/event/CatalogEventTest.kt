package com.loopers.application.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CatalogEventTest {

    @Nested
    @DisplayName("ProductViewed 이벤트")
    inner class ProductViewedTest {
        @Test
        @DisplayName("productId와 userId를 포함한다")
        fun productViewed_hasRequiredFields() {
            val event = CatalogEvent.ProductViewed(productId = 1L, userId = 2L)

            assertThat(event.productId).isEqualTo(1L)
            assertThat(event.userId).isEqualTo(2L)
            assertThat(event).isInstanceOf(CatalogEvent::class.java)
        }

        @Test
        @DisplayName("비인증 사용자의 경우 userId가 null이다")
        fun productViewed_allowsNullUserId() {
            val event = CatalogEvent.ProductViewed(productId = 1L, userId = null)

            assertThat(event.productId).isEqualTo(1L)
            assertThat(event.userId).isNull()
        }
    }
}
