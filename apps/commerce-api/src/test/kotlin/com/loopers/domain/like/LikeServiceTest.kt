package com.loopers.domain.like

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@DisplayName("LikeService")
class LikeServiceTest {

    private val likeRepository: LikeRepository = mockk()
    private val likeService = LikeService(likeRepository)

    companion object {
        private const val USER_ID = 1L
        private const val PRODUCT_ID = 100L
    }

    @DisplayName("like")
    @Nested
    inner class Like {
        @DisplayName("기록이 없으면 새로운 좋아요를 생성하고 true를 반환한다")
        @Test
        fun createsNewLike_whenNoRecordExists() {
            // arrange
            every { likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID) } returns null
            every { likeRepository.save(any()) } answers { firstArg() }

            // act
            val isNewLike = likeService.like(USER_ID, PRODUCT_ID)

            // assert
            assertThat(isNewLike).isTrue()
            verify(exactly = 1) { likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID) }
            verify(exactly = 1) { likeRepository.save(any()) }
        }

        @DisplayName("삭제된 기록이 있으면 restore하고 true를 반환한다")
        @Test
        fun restoresLike_whenDeletedRecordExists() {
            // arrange
            val deletedLike = LikeModel(userId = USER_ID, productId = PRODUCT_ID)
            deletedLike.delete() // 소프트 삭제 상태로 만듦

            every { likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID) } returns deletedLike
            every { likeRepository.save(any()) } answers { firstArg() }

            // act
            val isNewLike = likeService.like(USER_ID, PRODUCT_ID)

            // assert
            assertThat(isNewLike).isTrue()
            assertThat(deletedLike.deletedAt).isNull()
            verify(exactly = 1) { likeRepository.save(deletedLike) }
        }

        @DisplayName("이미 활성 기록이 있으면 아무 작업 없이 false를 반환한다")
        @Test
        fun returnsExistingLike_whenActiveRecordExists() {
            // arrange
            val activeLike = LikeModel(userId = USER_ID, productId = PRODUCT_ID)

            every { likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID) } returns activeLike

            // act
            val isNewLike = likeService.like(USER_ID, PRODUCT_ID)

            // assert
            assertThat(isNewLike).isFalse()
            verify(exactly = 1) { likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID) }
            verify(exactly = 0) { likeRepository.save(any()) }
        }
    }

    @DisplayName("unlike")
    @Nested
    inner class Unlike {
        @DisplayName("활성 기록이 있으면 소프트 삭제하고 true를 반환한다")
        @Test
        fun softDeletesLike_whenActiveRecordExists() {
            // arrange
            val activeLike = LikeModel(userId = USER_ID, productId = PRODUCT_ID)

            every { likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID) } returns activeLike
            every { likeRepository.save(any()) } answers { firstArg() }

            // act
            val wasActive = likeService.unlike(USER_ID, PRODUCT_ID)

            // assert
            assertThat(wasActive).isTrue()
            assertThat(activeLike.deletedAt).isNotNull()
            verify(exactly = 1) { likeRepository.save(activeLike) }
        }

        @DisplayName("기록이 없으면 아무 작업 없이 false를 반환한다")
        @Test
        fun doesNothing_whenNoRecordExists() {
            // arrange
            every { likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID) } returns null

            // act
            val wasActive = likeService.unlike(USER_ID, PRODUCT_ID)

            // assert
            assertThat(wasActive).isFalse()
            verify(exactly = 1) { likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID) }
            verify(exactly = 0) { likeRepository.save(any()) }
        }

        @DisplayName("이미 삭제된 기록이면 아무 작업 없이 false를 반환한다")
        @Test
        fun doesNothing_whenAlreadyDeleted() {
            // arrange
            val deletedLike = LikeModel(userId = USER_ID, productId = PRODUCT_ID)
            deletedLike.delete()

            every { likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID) } returns deletedLike

            // act
            val wasActive = likeService.unlike(USER_ID, PRODUCT_ID)

            // assert
            assertThat(wasActive).isFalse()
            verify(exactly = 1) { likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID) }
            verify(exactly = 0) { likeRepository.save(any()) }
        }
    }

    @DisplayName("findByUserId")
    @Nested
    inner class FindByUserId {
        @DisplayName("유저의 활성 좋아요 목록을 페이징으로 조회한다")
        @Test
        fun returnsActiveLikes_whenUserHasLikes() {
            // arrange
            val likes = listOf(
                LikeModel(userId = USER_ID, productId = 1L),
                LikeModel(userId = USER_ID, productId = 2L),
                LikeModel(userId = USER_ID, productId = 3L),
            )
            val pageable = PageRequest.of(0, 10)
            every { likeRepository.findAllByUserIdAndDeletedAtIsNull(USER_ID, pageable) } returns
                PageImpl(likes, pageable, 3)

            // act
            val result = likeService.findByUserId(USER_ID, pageable)

            // assert
            assertThat(result.content).hasSize(3)
            assertThat(result.content.map { it.productId }).containsExactly(1L, 2L, 3L)
            verify(exactly = 1) { likeRepository.findAllByUserIdAndDeletedAtIsNull(USER_ID, pageable) }
        }
    }
}
