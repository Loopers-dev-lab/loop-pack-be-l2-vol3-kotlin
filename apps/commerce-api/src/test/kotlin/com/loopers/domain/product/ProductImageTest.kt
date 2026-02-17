package com.loopers.domain.product

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ProductImageTest {

    @Test
    fun `create로 생성한 ProductImage의 persistenceId는 null이어야 한다`() {
        val image = ProductImage.create(
            imageUrl = "https://example.com/image.png",
            displayOrder = 0,
        )

        assertThat(image.persistenceId).isNull()
        assertThat(image.imageUrl).isEqualTo("https://example.com/image.png")
        assertThat(image.displayOrder).isEqualTo(0)
    }

    @Test
    fun `빈 이미지URL의 경우 create가 실패해야 한다`() {
        assertThatThrownBy {
            ProductImage.create(imageUrl = "", displayOrder = 0)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `음수 표시순서의 경우 create가 실패해야 한다`() {
        assertThatThrownBy {
            ProductImage.create(imageUrl = "https://example.com/image.png", displayOrder = -1)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `reconstitute로 생성한 ProductImage는 persistenceId를 가져야 한다`() {
        val image = ProductImage.reconstitute(
            persistenceId = 1L,
            imageUrl = "https://example.com/image.png",
            displayOrder = 0,
        )

        assertThat(image.persistenceId).isEqualTo(1L)
    }
}
