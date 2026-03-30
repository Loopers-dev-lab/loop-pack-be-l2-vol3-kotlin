package com.loopers.domain.outbox

import com.loopers.domain.common.vo.ProductId
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.outbox.model.CatalogOutbox
import com.loopers.domain.outbox.model.CatalogOutboxEventType
import com.loopers.domain.outbox.repository.CatalogOutboxRepository
import com.loopers.support.error.CoreException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CatalogOutboxTest {

    @Nested
    @DisplayName("CatalogOutbox 생성")
    inner class Create {

        @Test
        fun `eventType, productId가 유효하면 생성된다`() {
            val outbox = CatalogOutbox(
                eventType = CatalogOutboxEventType.LIKE_ADDED,
                productId = ProductId(1L),
                userId = UserId(1L),
            )

            assertThat(outbox.eventType).isEqualTo(CatalogOutboxEventType.LIKE_ADDED)
            assertThat(outbox.productId).isEqualTo(ProductId(1L))
            assertThat(outbox.userId).isEqualTo(UserId(1L))
            assertThat(outbox.published).isFalse()
            assertThat(outbox.eventId).isNotBlank()
        }

        @Test
        fun `userId가 null이어도 생성된다`() {
            val outbox = CatalogOutbox(
                eventType = CatalogOutboxEventType.PRODUCT_VIEWED,
                productId = ProductId(1L),
                userId = null,
            )

            assertThat(outbox.userId).isNull()
        }

        @Test
        fun `productId가 0 이하이면 예외가 발생한다`() {
            assertThatThrownBy {
                CatalogOutbox(eventType = CatalogOutboxEventType.LIKE_ADDED, productId = ProductId(0L), userId = UserId(1L))
            }.isInstanceOf(CoreException::class.java)
        }

        @Test
        fun `userId가 0이면 예외가 발생한다`() {
            assertThatThrownBy {
                CatalogOutbox(eventType = CatalogOutboxEventType.LIKE_ADDED, productId = ProductId(1L), userId = UserId(0L))
            }.isInstanceOf(CoreException::class.java)
        }

        @Test
        fun `userId가 음수이면 예외가 발생한다`() {
            assertThatThrownBy {
                CatalogOutbox(eventType = CatalogOutboxEventType.LIKE_ADDED, productId = ProductId(1L), userId = UserId(-1L))
            }.isInstanceOf(CoreException::class.java)
        }
    }

    @Nested
    @DisplayName("markPublished")
    inner class MarkPublished {

        @Test
        fun `발행 완료로 상태가 변경된다`() {
            val outbox = CatalogOutbox(
                eventType = CatalogOutboxEventType.LIKE_ADDED,
                productId = ProductId(1L),
                userId = UserId(1L),
            )

            outbox.markPublished()

            assertThat(outbox.published).isTrue()
        }
    }

    @Nested
    @DisplayName("CatalogOutboxRepository")
    inner class RepositoryTest {

        private lateinit var repository: CatalogOutboxRepository

        @BeforeEach
        fun setUp() {
            repository = FakeCatalogOutboxRepository()
        }

        @Test
        fun `미발행 메시지만 조회된다`() {
            val unpublished = repository.save(
                CatalogOutbox(eventType = CatalogOutboxEventType.LIKE_ADDED, productId = ProductId(1L), userId = UserId(1L)),
            )
            val published = repository.save(
                CatalogOutbox(eventType = CatalogOutboxEventType.PRODUCT_VIEWED, productId = ProductId(2L), userId = null),
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
                CatalogOutbox(eventType = CatalogOutboxEventType.LIKE_ADDED, productId = ProductId(1L), userId = UserId(1L)),
            )

            outbox.markPublished()
            repository.save(outbox)

            assertThat(repository.findAllUnpublished()).isEmpty()
        }
    }
}
