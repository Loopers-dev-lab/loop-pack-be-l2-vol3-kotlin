package com.loopers.support.page

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PageResponse")
class PageResponseTest {

    @Nested
    @DisplayName("totalPages 계산 시")
    inner class WhenCalculateTotalPages {
        @Test
        @DisplayName("totalElements가 size로 나누어떨어지면 정확한 페이지 수를 반환한다")
        fun totalPages_exactDivision() {
            val response = PageResponse(content = listOf("a", "b"), totalElements = 10, page = 0, size = 5)

            assertThat(response.totalPages).isEqualTo(2)
        }

        @Test
        @DisplayName("totalElements가 size로 나누어떨어지지 않으면 올림한다")
        fun totalPages_withRemainder() {
            val response = PageResponse(content = listOf("a", "b"), totalElements = 11, page = 0, size = 5)

            assertThat(response.totalPages).isEqualTo(3)
        }

        @Test
        @DisplayName("totalElements가 0이면 0을 반환한다")
        fun totalPages_zeroElements() {
            val response = PageResponse(content = emptyList<String>(), totalElements = 0, page = 0, size = 20)

            assertThat(response.totalPages).isEqualTo(0)
        }

        @Test
        @DisplayName("size가 0이면 0을 반환한다")
        fun totalPages_zeroSize() {
            val response = PageResponse(content = emptyList<String>(), totalElements = 10, page = 0, size = 0)

            assertThat(response.totalPages).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("map 변환 시")
    inner class WhenMap {
        @Test
        @DisplayName("content를 변환하고 메타데이터는 유지한다")
        fun map_transformsContentKeepsMeta() {
            val original = PageResponse(content = listOf(1, 2, 3), totalElements = 100, page = 2, size = 10)

            val mapped = original.map { it.toString() }

            assertThat(mapped.content).containsExactly("1", "2", "3")
            assertThat(mapped.totalElements).isEqualTo(100)
            assertThat(mapped.page).isEqualTo(2)
            assertThat(mapped.size).isEqualTo(10)
            assertThat(mapped.totalPages).isEqualTo(10)
        }
    }
}
