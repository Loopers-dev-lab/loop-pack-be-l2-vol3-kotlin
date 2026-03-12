package com.loopers.domain.product

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.data.domain.Sort

class ProductSortTypeTest {

    @DisplayName("toSort()를 호출할 때,")
    @Nested
    inner class ToSort {

        @DisplayName("LATEST이면, createdAt DESC Sort를 반환한다.")
        @Test
        fun returnsCreatedAtDesc_whenLatest() {
            // act
            val sort = ProductSortType.LATEST.toSort()

            // assert
            val order = sort.getOrderFor("createdAt")
            assertAll(
                { assertThat(order).isNotNull() },
                { assertThat(order!!.direction).isEqualTo(Sort.Direction.DESC) },
            )
        }

        @DisplayName("PRICE_ASC이면, price ASC Sort를 반환한다.")
        @Test
        fun returnsPriceAsc_whenPriceAsc() {
            // act
            val sort = ProductSortType.PRICE_ASC.toSort()

            // assert
            val order = sort.getOrderFor("price")
            assertAll(
                { assertThat(order).isNotNull() },
                { assertThat(order!!.direction).isEqualTo(Sort.Direction.ASC) },
            )
        }

        @DisplayName("LIKES_DESC이면, likeCount DESC Sort를 반환한다.")
        @Test
        fun returnsLikeCountDesc_whenLikesDesc() {
            // act
            val sort = ProductSortType.LIKES_DESC.toSort()

            // assert
            val order = sort.getOrderFor("likeCount")
            assertAll(
                { assertThat(order).isNotNull() },
                { assertThat(order!!.direction).isEqualTo(Sort.Direction.DESC) },
            )
        }
    }
}
