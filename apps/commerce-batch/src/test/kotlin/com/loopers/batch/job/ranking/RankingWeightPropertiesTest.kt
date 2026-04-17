package com.loopers.batch.job.ranking

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.bind.validation.ValidationBindHandler
import org.springframework.boot.context.properties.source.ConfigurationPropertySource
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

@DisplayName("RankingWeightProperties 검증")
class RankingWeightPropertiesTest {

    @DisplayName("생성자 검증 시,")
    @Nested
    inner class Constructor {

        @DisplayName("모든 가중치가 0이면 IllegalArgumentException이 발생한다.")
        @Test
        fun rejectsAllZeroWeights() {
            assertThatThrownBy { RankingWeightProperties(0.0, 0.0, 0.0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("가중치 합은 0보다 커야")
        }

        @DisplayName("일부 가중치가 0이어도 합이 0보다 크면 허용된다.")
        @Test
        fun allowsPartialZeroWeights() {
            val properties = RankingWeightProperties(0.0, 0.0, 0.7)
            assertThat(properties.salesWeight).isEqualTo(0.7)
        }
    }

    @DisplayName("ConfigurationProperties 바인딩 시,")
    @Nested
    inner class Binding {

        private fun bind(source: Map<String, Any?>): RankingWeightProperties {
            val propertySource: ConfigurationPropertySource = MapConfigurationPropertySource(source)
            val binder = Binder(propertySource)
            val validator = LocalValidatorFactoryBean().apply { afterPropertiesSet() }
            val handler = ValidationBindHandler(validator)
            return binder.bind("ranking.weight", Bindable.of(RankingWeightProperties::class.java), handler).get()
        }

        @DisplayName("음수 가중치로 바인딩하면 viewWeight 필드의 PositiveOrZero 검증 실패로 예외가 발생한다.")
        @Test
        fun rejectsNegativeWeight() {
            val source = mapOf(
                "ranking.weight.view-weight" to "-0.1",
                "ranking.weight.like-weight" to "0.2",
                "ranking.weight.sales-weight" to "0.7",
            )
            // 내부 예외 타입에 결합하지 않고, 검증 실패의 실체(필드명 + 제약)가 root cause 메시지에 드러남을 확인한다
            assertThatThrownBy { bind(source) }
                .rootCause()
                .hasMessageContaining("viewWeight")
                .hasMessageContaining("PositiveOrZero")
        }

        @DisplayName("모든 가중치가 0으로 바인딩되면 생성자 검증에서 실패한다.")
        @Test
        fun rejectsAllZeroOnBinding() {
            val source = mapOf(
                "ranking.weight.view-weight" to "0.0",
                "ranking.weight.like-weight" to "0.0",
                "ranking.weight.sales-weight" to "0.0",
            )
            assertThatThrownBy { bind(source) }
                .hasRootCauseInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
