package com.loopers.domain.like

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LikeTest {

    @Test
    fun `create로 생성한 Like의 persistenceId는 null이어야 한다`() {
        val like = Like.create(userId = USER_ID, productId = PRODUCT_ID)

        assertThat(like.persistenceId).isNull()
    }

    @Test
    fun `create로 생성한 Like의 userId와 productId가 설정되어야 한다`() {
        val like = Like.create(userId = USER_ID, productId = PRODUCT_ID)

        assertThat(like.userId).isEqualTo(USER_ID)
        assertThat(like.productId).isEqualTo(PRODUCT_ID)
    }

    @Test
    fun `create로 생성한 Like의 createdAt이 설정되어야 한다`() {
        val like = Like.create(userId = USER_ID, productId = PRODUCT_ID)

        assertThat(like.createdAt).isNotNull()
    }

    companion object {
        private const val USER_ID = 1L
        private const val PRODUCT_ID = 10L
    }
}
