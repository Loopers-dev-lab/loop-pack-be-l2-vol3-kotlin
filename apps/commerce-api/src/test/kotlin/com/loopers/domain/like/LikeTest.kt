package com.loopers.domain.like

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class LikeTest {

    @Nested
    inner class Create {
        @Test
        fun `유효한_정보로_좋아요를_생성할_수_있다`() {
            // act
            val like = Like(memberId = 1L, productId = 2L)

            // assert
            assertAll(
                { assertThat(like.memberId).isEqualTo(1L) },
                { assertThat(like.productId).isEqualTo(2L) },
                { assertThat(like.id).isNull() },
            )
        }
    }
}
