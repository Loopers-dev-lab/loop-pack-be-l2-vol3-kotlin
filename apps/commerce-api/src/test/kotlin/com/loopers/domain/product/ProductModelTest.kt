package com.loopers.domain.product

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class ProductModelTest {

    @Nested
    inner class Create {

        @Test
        fun `유효한 파라미터 입력 시 생성한다`() {
            val brandId = 1L
            val name = Name("뉴발란스 991")
            val imageUrl = ImageUrl("/tmp/test.png")
            val description = Description("뉴발란스 신발")
            val price = Price(299_000)

            assertDoesNotThrow {
                ProductModel(brandId, name, imageUrl, description, price)
            }
        }
    }

    @Nested
    inner class LikeCount {

        @Test
        fun `좋아요 시 1 추가된다`() {
            val product = ProductModel(
                brandId = 1L,
                name = Name("뉴발란스 991"),
                imageUrl = ImageUrl("/tmp/test.png"),
                description = Description("뉴발란스 신발"),
                price = Price(299_000),
            )

            assertThat(product.likeCount.value).isEqualTo(0)

            product.increaseLikeCount()

            assertThat(product.likeCount.value).isEqualTo(1)
        }

        @Test
        fun `좋아요 취소 시 1 감소한다`() {
            val product = ProductModel(
                brandId = 1L,
                name = Name("뉴발란스 991"),
                imageUrl = ImageUrl("/tmp/test.png"),
                description = Description("뉴발란스 신발"),
                price = Price(299_000),
            )

            product.increaseLikeCount()
            product.increaseLikeCount()

            assertThat(product.likeCount.value).isEqualTo(2)

            product.decreaseLikeCount()

            assertThat(product.likeCount.value).isEqualTo(1)
        }

        @Test
        fun `좋아요 0 일 때 좋아요 취소 시 0을 유지한다`() {
            val product = ProductModel(
                brandId = 1L,
                name = Name("뉴발란스 991"),
                imageUrl = ImageUrl("/tmp/test.png"),
                description = Description("뉴발란스 신발"),
                price = Price(299_000),
            )

            product.decreaseLikeCount()

            assertThat(product.likeCount.value).isEqualTo(0)
        }
    }
}
