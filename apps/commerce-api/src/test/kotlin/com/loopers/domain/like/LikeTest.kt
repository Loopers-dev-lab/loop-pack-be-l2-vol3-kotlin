package com.loopers.domain.like

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class LikeTest {

    @DisplayName("좋아요 생성할 때,")
    @Nested
    inner class Create {
        private val userId = 1L
        private val productId = 1L

        @DisplayName("유효한 값이 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsLike_whenValidValuesProvided() {
            // arrange & act
            val like = Like(userId = userId, productId = productId)

            // assert
            assertAll(
                { assertThat(like.userId).isEqualTo(userId) },
                { assertThat(like.productId).isEqualTo(productId) },
            )
        }
    }
}
