package com.loopers.domain.like

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class LikeServiceTest {

    @Mock
    private lateinit var likeRepository: LikeRepository

    private lateinit var likeService: LikeService

    @BeforeEach
    fun setUp() {
        likeService = LikeService(likeRepository)
    }

    @DisplayName("좋아요를 등록할 때,")
    @Nested
    inner class LikeProduct {

        @DisplayName("좋아요가 존재하지 않으면, 저장 후 true를 반환한다.")
        @Test
        fun returnsTrue_whenLikeNotExists() {
            // arrange
            val userId = 1L
            val productId = 1L

            whenever(likeRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false)
            whenever(likeRepository.save(any())).thenReturn(Like(userId = userId, productId = productId))

            // act
            val result = likeService.like(userId, productId)

            // assert
            assertThat(result).isTrue()
            verify(likeRepository).save(any())
        }

        @DisplayName("이미 좋아요가 존재하면, false를 반환하고 저장하지 않는다.")
        @Test
        fun returnsFalse_whenLikeAlreadyExists() {
            // arrange
            val userId = 1L
            val productId = 1L

            whenever(likeRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(true)

            // act
            val result = likeService.like(userId, productId)

            // assert
            assertThat(result).isFalse()
            verify(likeRepository, never()).save(any())
        }
    }
}
