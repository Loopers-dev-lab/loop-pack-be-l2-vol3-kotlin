package com.loopers.interfaces.api.product

import com.loopers.domain.product.Product
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties

class ProductSortTypeTest {

    @Nested
    inner class SortFieldValidation {

        @Test
        @DisplayName("ProductSortType의 정렬 필드가 Product Entity에 존재한다")
        fun sortFieldsShouldMatchEntityFields() {
            // arrange
            val entityFields = Product::class.memberProperties.map { it.name }

            // act & assert
            ProductSortType.entries.forEach { sortType ->
                sortType.sort.forEach { order ->
                    assertThat(entityFields)
                        .describedAs("${sortType.name}의 정렬 필드 '${order.property}'가 Product Entity에 존재해야 한다")
                        .contains(order.property)
                }
            }
        }
    }

    @Nested
    inner class From {

        @Test
        @DisplayName("유효한 정렬 타입 문자열을 변환하면 해당 타입이 반환된다")
        fun validSortType() {
            // act & assert
            assertThat(ProductSortType.from("latest")).isEqualTo(ProductSortType.LATEST)
            assertThat(ProductSortType.from("price_asc")).isEqualTo(ProductSortType.PRICE_ASC)
            assertThat(ProductSortType.from("likes_desc")).isEqualTo(ProductSortType.LIKES_DESC)
        }

        @Test
        @DisplayName("대소문자를 구분하지 않고 변환한다")
        fun caseInsensitive() {
            // act & assert
            assertThat(ProductSortType.from("LATEST")).isEqualTo(ProductSortType.LATEST)
            assertThat(ProductSortType.from("Price_Asc")).isEqualTo(ProductSortType.PRICE_ASC)
        }

        @Test
        @DisplayName("유효하지 않은 문자열이면 LATEST로 fallback된다")
        fun invalidFallbackToLatest() {
            // act & assert
            assertThat(ProductSortType.from("invalid")).isEqualTo(ProductSortType.LATEST)
            assertThat(ProductSortType.from("")).isEqualTo(ProductSortType.LATEST)
        }
    }
}
