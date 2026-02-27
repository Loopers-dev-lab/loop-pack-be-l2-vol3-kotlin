package com.loopers.application.like

import com.loopers.application.product.ProductService
import com.loopers.domain.like.Like
import com.loopers.domain.like.LikeRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.test.util.ReflectionTestUtils
import java.time.ZonedDateTime

@ExtendWith(MockitoExtension::class)
class LikeServiceTest {

    @Mock
    private lateinit var likeRepository: LikeRepository

    @Mock
    private lateinit var productService: ProductService

    @InjectMocks
    private lateinit var likeService: LikeService

    @DisplayName("좋아요를 등록할 때,")
    @Nested
    inner class AddLike {

        @DisplayName("좋아요가 없으면, 신규 생성된다.")
        @Test
        fun createsNewLike_whenNoExistingLike() {
            // arrange
            val userId = 1L
            val productId = 1L
            val now = ZonedDateTime.now()

            doNothing().whenever(productService).validateProductExistsIncludingDeleted(productId)
            whenever(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(null)
            whenever(likeRepository.save(any())).thenAnswer {
                val like = it.arguments[0] as Like
                ReflectionTestUtils.setField(like, "createdAt", now)
                like
            }

            // act
            val result = likeService.addLike(userId, productId)

            // assert
            assertAll(
                { assertThat(result.productId).isEqualTo(productId) },
            )
            verify(likeRepository).save(any())
        }

        @DisplayName("이미 활성 좋아요가 있으면, 기존 좋아요를 반환한다.")
        @Test
        fun returnsExistingLike_whenActiveLikeExists() {
            // arrange
            val userId = 1L
            val productId = 1L
            val now = ZonedDateTime.now()
            val existingLike = Like(userId = userId, productId = productId)
            ReflectionTestUtils.setField(existingLike, "createdAt", now)

            doNothing().whenever(productService).validateProductExistsIncludingDeleted(productId)
            whenever(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(existingLike)

            // act
            val result = likeService.addLike(userId, productId)

            // assert
            assertThat(result.productId).isEqualTo(productId)
            verify(likeRepository, never()).save(any())
        }

        @DisplayName("Soft Delete된 좋아요가 있으면, 복원된다.")
        @Test
        fun restoresLike_whenSoftDeletedLikeExists() {
            // arrange
            val userId = 1L
            val productId = 1L
            val now = ZonedDateTime.now()
            val deletedLike = Like(userId = userId, productId = productId)
            deletedLike.delete()

            doNothing().whenever(productService).validateProductExistsIncludingDeleted(productId)
            whenever(likeRepository.findByUserIdAndProductId(userId, productId)).thenReturn(deletedLike)
            whenever(likeRepository.save(any())).thenAnswer {
                val like = it.arguments[0] as Like
                ReflectionTestUtils.setField(like, "createdAt", now)
                like
            }

            // act
            val result = likeService.addLike(userId, productId)

            // assert
            assertThat(result.productId).isEqualTo(productId)
            verify(likeRepository).save(any())
        }

        @DisplayName("상품이 존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsException_whenProductNotFound() {
            // arrange
            val userId = 1L
            val productId = 999L

            doThrow(CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."))
                .whenever(productService).validateProductExistsIncludingDeleted(productId)

            // act
            val exception = assertThrows<CoreException> {
                likeService.addLike(userId, productId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("좋아요를 취소할 때,")
    @Nested
    inner class CancelLike {

        @DisplayName("활성 좋아요가 있으면, Soft Delete된다.")
        @Test
        fun softDeletesLike_whenActiveLikeExists() {
            // arrange
            val userId = 1L
            val productId = 1L
            val like = Like(userId = userId, productId = productId)

            whenever(likeRepository.findActiveByUserIdAndProductId(userId, productId)).thenReturn(like)
            whenever(likeRepository.save(any())).thenAnswer { it.arguments[0] }

            // act
            likeService.cancelLike(userId, productId)

            // assert
            assertThat(like.isDeleted()).isTrue()
            verify(likeRepository).save(like)
        }

        @DisplayName("좋아요가 없으면, 아무 작업도 하지 않는다.")
        @Test
        fun doesNothing_whenNoActiveLikeExists() {
            // arrange
            val userId = 1L
            val productId = 1L

            whenever(likeRepository.findActiveByUserIdAndProductId(userId, productId)).thenReturn(null)

            // act
            likeService.cancelLike(userId, productId)

            // assert
            verify(likeRepository, never()).save(any())
        }
    }

    @DisplayName("좋아요 목록을 조회할 때,")
    @Nested
    inner class GetUserLikes {

        @DisplayName("활성 좋아요 목록이 반환된다.")
        @Test
        fun returnsActiveLikes() {
            // arrange
            val userId = 1L
            val now = ZonedDateTime.now()
            val like1 = Like(userId = userId, productId = 1L)
            ReflectionTestUtils.setField(like1, "createdAt", now)
            val like2 = Like(userId = userId, productId = 2L)
            ReflectionTestUtils.setField(like2, "createdAt", now)
            val likes = listOf(like1, like2)

            whenever(likeRepository.findAllActiveByUserId(userId)).thenReturn(likes)

            // act
            val result = likeService.getUserLikes(userId)

            // assert
            assertThat(result).hasSize(2)
        }

        @DisplayName("좋아요가 없으면, 빈 목록이 반환된다.")
        @Test
        fun returnsEmptyList_whenNoLikes() {
            // arrange
            val userId = 1L

            whenever(likeRepository.findAllActiveByUserId(userId)).thenReturn(emptyList())

            // act
            val result = likeService.getUserLikes(userId)

            // assert
            assertThat(result).isEmpty()
        }
    }
}
