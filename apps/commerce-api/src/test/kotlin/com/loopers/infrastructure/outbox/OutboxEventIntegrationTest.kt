package com.loopers.infrastructure.outbox

import com.loopers.application.like.LikeFacade
import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.LikeCount
import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class OutboxEventIntegrationTest @Autowired constructor(
    private val likeFacade: LikeFacade,
    private val outboxEventRepository: OutboxEventRepository,
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createProduct(): Product {
        val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        return productRepository.save(
            Product(
                name = "에어맥스",
                description = "러닝화",
                price = Money.of(159000L),
                likes = LikeCount.of(0),
                stockQuantity = StockQuantity.of(100),
                brandId = brand.id,
            ),
        )
    }

    @DisplayName("Outbox INSERT가 비즈니스 트랜잭션과 함께 저장될 때,")
    @Nested
    inner class OutboxWithTransaction {

        @DisplayName("좋아요 트랜잭션 커밋 시 outbox_events에 LIKED 이벤트가 함께 저장된다.")
        @Test
        fun savesOutboxEventWithLikeTransaction() {
            // arrange
            val product = createProduct()

            // act
            likeFacade.like(userId = 1L, productId = product.id)

            // assert
            val outboxEvents = outboxEventRepository.findByPublishedAtIsNull()
            assertThat(outboxEvents).hasSize(1)
            assertThat(outboxEvents[0].aggregateType).isEqualTo("CATALOG")
            assertThat(outboxEvents[0].eventType).isEqualTo("LIKED")
            assertThat(outboxEvents[0].aggregateId).isEqualTo(product.id.toString())
            assertThat(outboxEvents[0].publishedAt).isNull()
        }
    }
}
