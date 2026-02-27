package com.loopers.application.like

import com.loopers.domain.like.LikeModel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LikeInfoTest {

    @Test
    fun `LikeModel에서 LikeInfo로 변환 시 모든 필드가 올바르게 매핑된다`() {
        // given
        val likeModel = LikeModel(userId = 42L, productId = 99L)

        // when
        val likeInfo = LikeInfo.from(likeModel)

        // then
        assertThat(likeInfo.id).isEqualTo(likeModel.id)
        assertThat(likeInfo.userId).isEqualTo(42L)
        assertThat(likeInfo.productId).isEqualTo(99L)
    }

    @Test
    fun `다른 userId와 productId 조합도 올바르게 매핑된다`() {
        // given
        val likeModel = LikeModel(userId = 1L, productId = 1L)

        // when
        val likeInfo = LikeInfo.from(likeModel)

        // then
        assertThat(likeInfo.userId).isEqualTo(1L)
        assertThat(likeInfo.productId).isEqualTo(1L)
    }
}
