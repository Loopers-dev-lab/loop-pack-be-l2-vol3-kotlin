package com.loopers.domain.productlike.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ProductUnlikedEvent")
class ProductUnlikedEventTest {

    @Test
    @DisplayName("동일한 userId와 productId로 생성한 두 이벤트의 dedupeKey는 동일해야 한다")
    fun dedupeKeyIsDeterministic() {
        // Arrange
        val userId = 123L
        val productId = 456L

        // Act
        val event1 = ProductUnlikedEvent(
            productId = productId,
            userId = userId,
        )
        val event2 = ProductUnlikedEvent(
            productId = productId,
            userId = userId,
        )

        // Assert
        assertThat(event1.dedupeKey).isEqualTo(event2.dedupeKey)
        assertThat(event1.dedupeKey).isEqualTo("product.unliked:$userId:$productId")
    }

    @Test
    @DisplayName("다른 userId의 이벤트는 다른 dedupeKey를 가져야 한다")
    fun dedupeKeyDifferentForDifferentUserId() {
        // Arrange
        val productId = 456L

        // Act
        val event1 = ProductUnlikedEvent(productId = productId, userId = 123L)
        val event2 = ProductUnlikedEvent(productId = productId, userId = 124L)

        // Assert
        assertThat(event1.dedupeKey).isNotEqualTo(event2.dedupeKey)
    }

    @Test
    @DisplayName("다른 productId의 이벤트는 다른 dedupeKey를 가져야 한다")
    fun dedupeKeyDifferentForDifferentProductId() {
        // Arrange
        val userId = 123L

        // Act
        val event1 = ProductUnlikedEvent(productId = 456L, userId = userId)
        val event2 = ProductUnlikedEvent(productId = 457L, userId = userId)

        // Assert
        assertThat(event1.dedupeKey).isNotEqualTo(event2.dedupeKey)
    }
}
