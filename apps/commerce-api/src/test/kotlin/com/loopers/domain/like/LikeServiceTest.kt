package com.loopers.domain.like

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LikeServiceTest {

    private lateinit var likeRepository: FakeLikeRepository
    private lateinit var likeService: LikeService

    @BeforeEach
    fun setUp() {
        likeRepository = FakeLikeRepository()
        likeService = LikeService(likeRepository)
    }

    @Nested
    @DisplayName("addLike 시")
    inner class AddLike {

        @Test
        @DisplayName("좋아요가 등록되면 true를 반환한다")
        fun addLike_newLike_returnsTrue() {
            // act
            val result = likeService.addLike(1L, 1L)

            // assert
            assertThat(result).isTrue()
            assertThat(likeRepository.findByUserIdAndProductId(1L, 1L)).isNotNull
        }

        @Test
        @DisplayName("이미 좋아요가 존재하면 false를 반환한다 (멱등)")
        fun addLike_alreadyExists_returnsFalse() {
            // arrange
            likeService.addLike(1L, 1L)

            // act
            val result = likeService.addLike(1L, 1L)

            // assert
            assertThat(result).isFalse()
        }

        @Test
        @DisplayName("동시 요청으로 DB unique constraint 위반이 발생해도 예외 없이 false를 반환한다 (멱등)")
        fun addLike_concurrentDuplicateSave_returnsFalse() {
            // arrange
            // 동시성 상황 재현: findByUserIdAndProductId는 null을 반환하지만(체크 통과),
            // save 시점에 이미 DB에 레코드가 존재하여 DataIntegrityViolationException 발생
            likeRepository.simulateConcurrentInsert(1L, 1L)

            // act
            val result = likeService.addLike(1L, 1L)

            // assert: 예외가 전파되지 않고 false를 반환해야 한다
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("removeLike 시")
    inner class RemoveLike {

        @Test
        @DisplayName("좋아요가 삭제되면 true를 반환한다")
        fun removeLike_existing_returnsTrue() {
            // arrange
            likeService.addLike(1L, 1L)

            // act
            val result = likeService.removeLike(1L, 1L)

            // assert
            assertThat(result).isTrue()
            assertThat(likeRepository.findByUserIdAndProductId(1L, 1L)).isNull()
        }

        @Test
        @DisplayName("좋아요가 없으면 false를 반환한다 (멱등)")
        fun removeLike_notExists_returnsFalse() {
            // act
            val result = likeService.removeLike(1L, 1L)

            // assert
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("getLikesByUserId 시")
    inner class GetLikesByUserId {

        @Test
        @DisplayName("사용자의 좋아요 목록을 id 역순으로 반환한다")
        fun getLikesByUserId_returnsInReverseIdOrder() {
            // arrange
            likeService.addLike(1L, 10L)
            likeService.addLike(1L, 20L)
            likeService.addLike(1L, 30L)

            // act
            val result = likeService.getLikesByUserId(1L)

            // assert
            assertThat(result).hasSize(3)
            assertThat(result[0].refProductId).isEqualTo(30L)
            assertThat(result[1].refProductId).isEqualTo(20L)
            assertThat(result[2].refProductId).isEqualTo(10L)
        }

        @Test
        @DisplayName("좋아요가 없으면 빈 리스트를 반환한다")
        fun getLikesByUserId_noLikes_returnsEmptyList() {
            // act
            val result = likeService.getLikesByUserId(1L)

            // assert
            assertThat(result).isEmpty()
        }
    }
}
