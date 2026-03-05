package com.loopers.domain.like

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.dao.DataIntegrityViolationException

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

        @DisplayName("저장 시 UNIQUE 제약 위반이 발생하면, 예외가 전파된다. (TOCTOU — Facade에서 처리)")
        @Test
        fun throwsException_whenUniqueConstraintViolation() {
            // arrange
            val userId = 1L
            val productId = 1L

            whenever(likeRepository.existsByUserIdAndProductId(userId, productId)).thenReturn(false)
            whenever(likeRepository.save(any())).thenThrow(DataIntegrityViolationException("Duplicate entry"))

            // act & assert
            assertThrows<DataIntegrityViolationException> {
                likeService.like(userId, productId)
            }
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

    @DisplayName("좋아요를 취소할 때,")
    @Nested
    inner class UnlikeProduct {

        @DisplayName("좋아요가 존재하면, 삭제 후 true를 반환한다.")
        @Test
        fun returnsTrue_whenLikeExists() {
            // arrange
            val userId = 1L
            val productId = 1L

            whenever(likeRepository.deleteByUserIdAndProductId(userId, productId)).thenReturn(true)

            // act
            val result = likeService.unlike(userId, productId)

            // assert
            assertThat(result).isTrue()
            verify(likeRepository).deleteByUserIdAndProductId(userId, productId)
        }

        @DisplayName("좋아요가 존재하지 않으면, false를 반환한다.")
        @Test
        fun returnsFalse_whenLikeNotExists() {
            // arrange
            val userId = 1L
            val productId = 1L

            whenever(likeRepository.deleteByUserIdAndProductId(userId, productId)).thenReturn(false)

            // act
            val result = likeService.unlike(userId, productId)

            // assert
            assertThat(result).isFalse()
        }
    }

    @DisplayName("좋아요 목록을 조회할 때,")
    @Nested
    inner class GetLikedProductIds {

        @DisplayName("사용자의 좋아요 상품 ID 목록을 반환한다.")
        @Test
        fun returnsLikedProductIds() {
            // arrange
            val userId = 1L
            whenever(likeRepository.findProductIdsByUserId(userId)).thenReturn(listOf(1L, 2L, 3L))

            // act
            val result = likeService.getLikedProductIds(userId)

            // assert
            assertThat(result).containsExactly(1L, 2L, 3L)
        }

        @DisplayName("좋아요한 상품이 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyList_whenNoLikes() {
            // arrange
            val userId = 1L
            whenever(likeRepository.findProductIdsByUserId(userId)).thenReturn(emptyList())

            // act
            val result = likeService.getLikedProductIds(userId)

            // assert
            assertThat(result).isEmpty()
        }
    }
}
