package com.loopers.application.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.loopers.application.like.LikeFacade
import com.loopers.domain.like.LikeService
import com.loopers.domain.outbox.OutboxEventModel
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductService
import com.loopers.infrastructure.outbox.OutboxEventJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@SpringJUnitConfig(CatalogEventOutboxAppenderTest.TestConfig::class)
@DisplayName("CatalogEventOutboxAppender")
class CatalogEventOutboxAppenderTest {
    @Configuration
    class TestConfig {
        @Bean
        fun likeService(): LikeService = mockk()

        @Bean
        fun productService(): ProductService = mockk()

        @Bean
        fun outboxEventJpaRepository(): OutboxEventJpaRepository = mockk()

        @Bean
        fun objectMapper(): ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())

        @Bean
        fun likeFacade(
            likeService: LikeService,
            productService: ProductService,
            applicationEventPublisher: org.springframework.context.ApplicationEventPublisher,
        ): LikeFacade {
            return LikeFacade(likeService, productService, applicationEventPublisher)
        }

        @Bean
        fun catalogEventOutboxAppender(
            outboxEventJpaRepository: OutboxEventJpaRepository,
            objectMapper: ObjectMapper,
        ): CatalogEventOutboxAppender {
            return CatalogEventOutboxAppender(outboxEventJpaRepository, objectMapper, "catalog-events")
        }
    }

    @jakarta.annotation.Resource
    private lateinit var likeFacade: LikeFacade

    @jakarta.annotation.Resource
    private lateinit var likeService: LikeService

    @jakarta.annotation.Resource
    private lateinit var productService: ProductService

    @jakarta.annotation.Resource
    private lateinit var outboxEventJpaRepository: OutboxEventJpaRepository

    @DisplayName("좋아요 비즈니스 변경 중 이벤트가 발행되면 outbox 레코드가 저장된다")
    @Test
    fun savesOutboxRecordWhenLikeSucceeds() {
        // arrange
        every { productService.findById(10L) } returns createProductModel()
        every { likeService.like(1L, 10L) } returns true
        every { outboxEventJpaRepository.save(any<OutboxEventModel>()) } answers { firstArg() }

        // act
        likeFacade.likeProduct(userId = 1L, productId = 10L)

        // assert
        verify(exactly = 1) { likeService.like(1L, 10L) }
        verify(exactly = 1) { outboxEventJpaRepository.save(any<OutboxEventModel>()) }
    }

    @DisplayName("outbox 저장이 실패하면 비즈니스 변경도 함께 실패한다")
    @Test
    fun failsBusinessFlowWhenOutboxSaveFails() {
        // arrange
        every { productService.findById(10L) } returns createProductModel()
        every { likeService.like(1L, 10L) } returns true
        every { outboxEventJpaRepository.save(any<OutboxEventModel>()) } throws IllegalStateException("outbox save failed")

        assertThatThrownBy {
            likeFacade.likeProduct(userId = 1L, productId = 10L)
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("outbox save failed")
    }

    private fun createProductModel(): ProductModel {
        return ProductModel(
            name = "테스트상품",
            price = 1000,
            brandId = 1L,
            stockQuantity = 10,
        )
    }
}
