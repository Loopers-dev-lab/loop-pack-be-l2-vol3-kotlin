package com.loopers.config.rank

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ProductRankPropertiesTest {

    @Nested
    @DisplayName("ttlDays 검증")
    inner class TtlDays {

        @DisplayName("기본값 2로 정상 생성된다.")
        @Test
        fun shouldCreateWithDefaultTtl() {
            val props = ProductRankProperties()
            assertThat(props.ttlDays).isEqualTo(2L)
        }

        @DisplayName("0이면 IllegalArgumentException")
        @Test
        fun shouldFailWhenTtlIsZero() {
            assertThatThrownBy { ProductRankProperties(ttlDays = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("ttl-days")
        }

        @DisplayName("음수이면 IllegalArgumentException")
        @Test
        fun shouldFailWhenTtlIsNegative() {
            assertThatThrownBy { ProductRankProperties(ttlDays = -1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("ttl-days")
        }
    }

    @Nested
    @DisplayName("Weight 검증")
    inner class WeightValidation {

        @DisplayName("기본 가중치(0.1/0.2/0.7)로 정상 생성된다.")
        @Test
        fun shouldCreateWithDefaultWeights() {
            val weight = ProductRankProperties.Weight()
            assertThat(weight.view).isEqualTo(0.1)
            assertThat(weight.like).isEqualTo(0.2)
            assertThat(weight.order).isEqualTo(0.7)
        }

        @DisplayName("음수 view 가중치이면 IllegalArgumentException")
        @Test
        fun shouldFailWhenViewWeightIsNegative() {
            assertThatThrownBy { ProductRankProperties.Weight(view = -0.1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("view")
        }

        @DisplayName("음수 like 가중치이면 IllegalArgumentException")
        @Test
        fun shouldFailWhenLikeWeightIsNegative() {
            assertThatThrownBy { ProductRankProperties.Weight(like = -0.1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("like")
        }

        @DisplayName("음수 order 가중치이면 IllegalArgumentException")
        @Test
        fun shouldFailWhenOrderWeightIsNegative() {
            assertThatThrownBy { ProductRankProperties.Weight(order = -0.1) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("order")
        }
    }
}
