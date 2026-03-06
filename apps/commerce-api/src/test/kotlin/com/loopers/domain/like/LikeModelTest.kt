package com.loopers.domain.like

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("LikeModel")
class LikeModelTest {

    companion object {
        private const val USER_ID = 1L
        private const val PRODUCT_ID = 100L
    }

    @DisplayName("유효한 userId와 productId로 LikeModel이 정상 생성된다")
    @Test
    fun createsLikeModel_whenValidFieldsProvided() {
        // arrange & act
        val like = LikeModel(userId = USER_ID, productId = PRODUCT_ID)

        // assert
        assertThat(like.userId).isEqualTo(USER_ID)
        assertThat(like.productId).isEqualTo(PRODUCT_ID)
        assertThat(like.deletedAt).isNull()
    }
}
