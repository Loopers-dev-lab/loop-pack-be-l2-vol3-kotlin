package com.loopers.support.page

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PageRequest")
class PageRequestTest {

    @Nested
    @DisplayName("page 설정 시")
    inner class WhenSetPage {
        @Test
        @DisplayName("음수 값은 0으로 자동 조정된다")
        fun setPage_negative_adjustsToZero() {
            val pageRequest = PageRequest()
            pageRequest.page = -1

            assertThat(pageRequest.page).isEqualTo(0)
        }

        @Test
        @DisplayName("0 이상 값은 그대로 유지된다")
        fun setPage_valid_keepsValue() {
            val pageRequest = PageRequest()
            pageRequest.page = 5

            assertThat(pageRequest.page).isEqualTo(5)
        }
    }

    @Nested
    @DisplayName("size 설정 시")
    inner class WhenSetSize {
        @Test
        @DisplayName("MAX_SIZE 초과 시 MAX_SIZE로 클램핑된다")
        fun setSize_exceedsMax_clampsToMax() {
            val pageRequest = PageRequest()
            pageRequest.size = 101

            assertThat(pageRequest.size).isEqualTo(PageRequest.MAX_SIZE)
        }

        @Test
        @DisplayName("MIN_SIZE 미만 값은 MIN_SIZE로 클램핑된다")
        fun setSize_belowMin_clampsToMin() {
            val pageRequest = PageRequest()
            pageRequest.size = 5

            assertThat(pageRequest.size).isEqualTo(PageRequest.MIN_SIZE)
        }

        @Test
        @DisplayName("음수 값은 MIN_SIZE로 클램핑된다")
        fun setSize_negative_clampsToMin() {
            val pageRequest = PageRequest()
            pageRequest.size = -5

            assertThat(pageRequest.size).isEqualTo(PageRequest.MIN_SIZE)
        }

        @Test
        @DisplayName("MIN_SIZE~MAX_SIZE 사이 값은 그대로 유지된다")
        fun setSize_valid_keepsValue() {
            val pageRequest = PageRequest()
            pageRequest.size = 50

            assertThat(pageRequest.size).isEqualTo(50)
        }

        @Test
        @DisplayName("MAX_SIZE(100)는 유효한 값이다")
        fun setSize_maxBoundary_keepsValue() {
            val pageRequest = PageRequest()
            pageRequest.size = 100

            assertThat(pageRequest.size).isEqualTo(100)
        }

        @Test
        @DisplayName("MIN_SIZE(10)는 유효한 값이다")
        fun setSize_minBoundary_keepsValue() {
            val pageRequest = PageRequest()
            pageRequest.size = 10

            assertThat(pageRequest.size).isEqualTo(10)
        }
    }

    @Nested
    @DisplayName("기본값")
    inner class Defaults {
        @Test
        @DisplayName("page 기본값은 0이다")
        fun defaultPage_isZero() {
            assertThat(PageRequest().page).isEqualTo(0)
        }

        @Test
        @DisplayName("size 기본값은 20이다")
        fun defaultSize_isDefaultSize() {
            assertThat(PageRequest().size).isEqualTo(20)
        }
    }
}
