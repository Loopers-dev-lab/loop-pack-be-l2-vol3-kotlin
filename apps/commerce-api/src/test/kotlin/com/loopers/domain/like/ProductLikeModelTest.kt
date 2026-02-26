package com.loopers.domain.like

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class ProductLikeModelTest {
    @DisplayName("좋아요 모델을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("memberId와 productId가 주어지면, 정상적으로 생성된다.")
        @Test
        fun createsProductLike_whenValidIdsProvided() {
            // act
            val like = ProductLikeModel(memberId = 1L, productId = 2L)

            // assert
            assertAll(
                { assertThat(like.memberId).isEqualTo(1L) },
                { assertThat(like.productId).isEqualTo(2L) },
            )
        }
    }
}
