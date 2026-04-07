package com.loopers.application.event.handler

import com.loopers.application.event.LikeToggledEvent
import com.loopers.application.product.ProductService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class LikeCountEventHandlerTest {

    @Mock
    private lateinit var productService: ProductService

    @InjectMocks
    private lateinit var likeCountEventHandler: LikeCountEventHandler

    @DisplayName("LikeToggledEvent를 처리할 때,")
    @Nested
    inner class Handle {

        @DisplayName("liked=true이면, likeCount가 증가한다.")
        @Test
        fun incrementsLikeCount_whenLiked() {
            // arrange
            val event = LikeToggledEvent(userId = 1L, productId = 1L, liked = true)

            // act
            likeCountEventHandler.handle(event)

            // assert
            verify(productService).incrementLikeCount(1L)
        }

        @DisplayName("liked=false이면, likeCount가 감소한다.")
        @Test
        fun decrementsLikeCount_whenUnliked() {
            // arrange
            val event = LikeToggledEvent(userId = 1L, productId = 1L, liked = false)

            // act
            likeCountEventHandler.handle(event)

            // assert
            verify(productService).decrementLikeCount(1L)
        }
    }
}
