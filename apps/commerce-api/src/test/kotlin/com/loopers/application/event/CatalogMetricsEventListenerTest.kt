package com.loopers.application.event

import com.loopers.domain.outbox.FakeCatalogOutboxRepository
import com.loopers.domain.outbox.repository.CatalogOutboxRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CatalogMetricsEventListenerTest {

    private lateinit var catalogOutboxRepository: CatalogOutboxRepository
    private lateinit var listener: CatalogMetricsEventListener

    @BeforeEach
    fun setUp() {
        catalogOutboxRepository = FakeCatalogOutboxRepository()
        listener = CatalogMetricsEventListener(catalogOutboxRepository)
    }

    @Nested
    @DisplayName("ProductViewed 이벤트 수신 시")
    inner class HandleProductViewed {

        @Test
        @DisplayName("CatalogOutbox에 PRODUCT_VIEWED로 기록된다")
        fun handleProductViewed() {
            val event = CatalogEvent.ProductViewed(productId = 1L, userId = 2L)

            listener.handleProductViewed(event)

            val outboxList = catalogOutboxRepository.findAllUnpublished()
            assertThat(outboxList).hasSize(1)
            assertThat(outboxList[0].eventType).isEqualTo("PRODUCT_VIEWED")
            assertThat(outboxList[0].productId).isEqualTo(1L)
            assertThat(outboxList[0].userId).isEqualTo(2L)
        }

        @Test
        @DisplayName("비인증 사용자의 ProductViewed → userId가 null로 기록된다")
        fun handleProductViewed_nullUserId() {
            val event = CatalogEvent.ProductViewed(productId = 1L, userId = null)

            listener.handleProductViewed(event)

            val outboxList = catalogOutboxRepository.findAllUnpublished()
            assertThat(outboxList).hasSize(1)
            assertThat(outboxList[0].userId).isNull()
        }
    }
}
