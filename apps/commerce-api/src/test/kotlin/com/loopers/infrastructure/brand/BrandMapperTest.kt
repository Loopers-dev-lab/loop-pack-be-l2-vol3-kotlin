package com.loopers.infrastructure.brand

import com.loopers.domain.brand.BrandStatus
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class BrandMapperTest {

    @Test
    fun `entity id가 null이면 IllegalArgumentException이 발생한다`() {
        val entity = BrandEntity(
            id = null,
            name = "테스트브랜드",
            description = "설명",
            logoUrl = "https://example.com/logo.png",
            status = BrandStatus.ACTIVE,
            deletedAt = null,
        )

        assertThatThrownBy { BrandMapper.toDomain(entity) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("BrandEntity.id가 null입니다")
    }
}
