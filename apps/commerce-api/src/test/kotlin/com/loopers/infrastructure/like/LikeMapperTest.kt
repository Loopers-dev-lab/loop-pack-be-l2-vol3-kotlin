package com.loopers.infrastructure.like

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class LikeMapperTest {

    @Test
    fun `entity id가 null이면 IllegalArgumentException이 발생한다`() {
        val entity = LikeEntity(
            id = null,
            userId = 1L,
            productId = 1L,
            createdAt = ZonedDateTime.now(),
        )

        assertThatThrownBy { LikeMapper.toDomain(entity) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("LikeEntity.id가 null입니다")
    }
}
