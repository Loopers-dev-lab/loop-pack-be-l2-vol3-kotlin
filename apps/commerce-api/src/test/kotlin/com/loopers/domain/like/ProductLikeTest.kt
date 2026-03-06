package com.loopers.domain.like

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class ProductLikeTest {
    @DisplayName("좋아요를 생성할 때, ")
    @Nested
    inner class Create {
        @DisplayName("userId와 productId가 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsProductLike_whenUserIdAndProductIdAreProvided() {
            // arrange & act
            val like = ProductLike(userId = 1L, productId = 2L)

            // assert
            assertAll(
                { assertThat(like.userId).isEqualTo(1L) },
                { assertThat(like.productId).isEqualTo(2L) },
            )
        }
    }
}
