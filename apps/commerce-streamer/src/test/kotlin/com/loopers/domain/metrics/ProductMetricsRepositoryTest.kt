package com.loopers.domain.metrics

import com.loopers.domain.metrics.model.ProductMetrics
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ProductMetricsRepositoryTest {

    private lateinit var repository: FakeProductMetricsRepository

    @BeforeEach
    fun setUp() {
        repository = FakeProductMetricsRepository()
    }

    @Nested
    @DisplayName("save 시")
    inner class Save {

        @Test
        fun `새 ProductMetrics를 저장하면 id가 할당된다`() {
            val metrics = ProductMetrics(productId = 1L)

            val saved = repository.save(metrics)

            assertThat(saved.id).isGreaterThan(0)
        }

        @Test
        fun `기존 ProductMetrics를 저장하면 업데이트된다`() {
            val metrics = ProductMetrics(productId = 1L)
            val saved = repository.save(metrics)
            saved.incrementViewCount()

            repository.save(saved)

            val found = repository.findByProductId(1L)
            assertThat(found?.viewCount).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("findByProductId 시")
    inner class FindByProductId {

        @Test
        fun `존재하는 productId로 조회하면 ProductMetrics를 반환한다`() {
            repository.save(ProductMetrics(productId = 1L))

            val found = repository.findByProductId(1L)

            assertThat(found).isNotNull
            assertThat(found!!.productId).isEqualTo(1L)
        }

        @Test
        fun `존재하지 않는 productId로 조회하면 null을 반환한다`() {
            val found = repository.findByProductId(999L)

            assertThat(found).isNull()
        }
    }
}
