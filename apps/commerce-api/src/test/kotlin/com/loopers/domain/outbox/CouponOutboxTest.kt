package com.loopers.domain.outbox

import com.loopers.domain.outbox.model.CouponOutbox
import com.loopers.domain.outbox.repository.CouponOutboxRepository
import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CouponOutboxTest {

    @Nested
    @DisplayName("CouponOutbox 생성")
    inner class Create {

        @Test
        fun `유효한 필드로 생성된다`() {
            val outbox = CouponOutbox(
                eventType = "COUPON_ISSUE_REQUESTED",
                couponId = 1L,
                userId = 1L,
            )

            assertThat(outbox.eventType).isEqualTo("COUPON_ISSUE_REQUESTED")
            assertThat(outbox.couponId).isEqualTo(1L)
            assertThat(outbox.userId).isEqualTo(1L)
            assertThat(outbox.published).isFalse()
            assertThat(outbox.eventId).isNotBlank()
        }

        @Test
        fun `eventType이 비어있으면 예외가 발생한다`() {
            assertThatThrownBy {
                CouponOutbox(eventType = "", couponId = 1L, userId = 1L)
            }.isInstanceOf(CoreException::class.java)
        }

        @Test
        fun `couponId가 0 이하이면 예외가 발생한다`() {
            assertThatThrownBy {
                CouponOutbox(eventType = "COUPON_ISSUE_REQUESTED", couponId = 0L, userId = 1L)
            }.isInstanceOf(CoreException::class.java)
        }

        @Test
        fun `userId가 0 이하이면 예외가 발생한다`() {
            assertThatThrownBy {
                CouponOutbox(eventType = "COUPON_ISSUE_REQUESTED", couponId = 1L, userId = 0L)
            }.isInstanceOf(CoreException::class.java)
        }

        @Test
        fun `eventId가 blank이면 예외가 발생한다`() {
            assertThatThrownBy {
                CouponOutbox(eventId = "   ", eventType = "COUPON_ISSUE_REQUESTED", couponId = 1L, userId = 1L)
            }.isInstanceOf(CoreException::class.java)
        }

        @Test
        fun `eventId가 빈 문자열이면 예외가 발생한다`() {
            assertThatThrownBy {
                CouponOutbox(eventId = "", eventType = "COUPON_ISSUE_REQUESTED", couponId = 1L, userId = 1L)
            }.isInstanceOf(CoreException::class.java)
        }
    }

    @Nested
    @DisplayName("markPublished")
    inner class MarkPublished {

        @Test
        fun `발행 완료로 상태가 변경된다`() {
            val outbox = CouponOutbox(
                eventType = "COUPON_ISSUE_REQUESTED",
                couponId = 1L,
                userId = 1L,
            )

            outbox.markPublished()

            assertThat(outbox.published).isTrue()
        }
    }

    @Nested
    @DisplayName("CouponOutboxRepository")
    inner class RepositoryTest {

        private lateinit var repository: CouponOutboxRepository

        @BeforeEach
        fun setUp() {
            repository = FakeCouponOutboxRepository()
        }

        @Test
        fun `미발행 메시지만 조회된다`() {
            val unpublished = repository.save(
                CouponOutbox(eventType = "COUPON_ISSUE_REQUESTED", couponId = 1L, userId = 1L),
            )
            val published = repository.save(
                CouponOutbox(eventType = "COUPON_ISSUE_REQUESTED", couponId = 2L, userId = 2L),
            )
            published.markPublished()
            repository.save(published)

            val result = repository.findAllUnpublished()

            assertThat(result).hasSize(1)
            assertThat(result[0].id).isEqualTo(unpublished.id)
        }

        @Test
        fun `발행 완료 마킹 후 미발행 목록에서 제외된다`() {
            val outbox = repository.save(
                CouponOutbox(eventType = "COUPON_ISSUE_REQUESTED", couponId = 1L, userId = 1L),
            )

            outbox.markPublished()
            repository.save(outbox)

            assertThat(repository.findAllUnpublished()).isEmpty()
        }
    }
}
