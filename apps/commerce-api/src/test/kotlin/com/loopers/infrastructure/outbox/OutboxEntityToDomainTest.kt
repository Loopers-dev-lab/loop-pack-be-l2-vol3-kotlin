package com.loopers.infrastructure.outbox

import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class OutboxEntityToDomainTest {

    @Nested
    @DisplayName("CatalogOutboxEntity.toDomain()")
    inner class CatalogOutboxEntityToDomain {

        @Test
        @DisplayName("미지원 eventType 문자열이면 CoreException을 던진다")
        fun `미지원 eventType 문자열이면 CoreException을 던진다`() {
            val entity = CatalogOutboxEntity(
                eventId = "evt-1",
                eventType = "UNKNOWN_TYPE",
                productId = 1L,
                userId = 1L,
                published = false,
            )

            assertThatThrownBy { entity.toDomain() }
                .isInstanceOf(CoreException::class.java)
                .hasMessageContaining("지원하지 않는 이벤트 타입: UNKNOWN_TYPE")
        }
    }

    @Nested
    @DisplayName("CouponOutboxEntity.toDomain()")
    inner class CouponOutboxEntityToDomain {

        @Test
        @DisplayName("미지원 eventType 문자열이면 CoreException을 던진다")
        fun `미지원 eventType 문자열이면 CoreException을 던진다`() {
            val entity = CouponOutboxEntity(
                eventId = "evt-1",
                eventType = "UNKNOWN_TYPE",
                couponId = 1L,
                userId = 1L,
                published = false,
            )

            assertThatThrownBy { entity.toDomain() }
                .isInstanceOf(CoreException::class.java)
                .hasMessageContaining("지원하지 않는 이벤트 타입: UNKNOWN_TYPE")
        }
    }

    @Nested
    @DisplayName("OrderOutboxEntity.toDomain()")
    inner class OrderOutboxEntityToDomain {

        @Test
        @DisplayName("미지원 eventType 문자열이면 CoreException을 던진다")
        fun `미지원 eventType 문자열이면 CoreException을 던진다`() {
            val entity = OrderOutboxEntity(
                eventId = "evt-1",
                eventType = "UNKNOWN_TYPE",
                orderId = 1L,
                userId = 1L,
                totalAmount = null,
                reason = null,
                productId = null,
                quantity = null,
                published = false,
            )

            assertThatThrownBy { entity.toDomain() }
                .isInstanceOf(CoreException::class.java)
                .hasMessageContaining("지원하지 않는 이벤트 타입: UNKNOWN_TYPE")
        }
    }
}
