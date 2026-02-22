package com.loopers.domain.like

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LikeServiceTest {
    private lateinit var likeService: LikeService
    private lateinit var fakeRepository: FakeProductLikeRepository

    @BeforeEach
    fun setUp() {
        fakeRepository = FakeProductLikeRepository()
        likeService = LikeService(fakeRepository)
    }

    @DisplayName("좋아요를 등록할 때,")
    @Nested
    inner class Like {
        @DisplayName("처음 좋아요하면, 정상적으로 등록된다.")
        @Test
        fun savesLike_whenFirstTime() {
            // act
            likeService.like(memberId = 1L, productId = 100L)

            // assert
            val result = fakeRepository.findByMemberIdAndProductId(1L, 100L)
            assertThat(result).isNotNull
        }

        @DisplayName("이미 좋아요한 상태에서 다시 좋아요하면, 중복 저장되지 않는다 (멱등).")
        @Test
        fun doesNotDuplicate_whenAlreadyLiked() {
            // arrange
            likeService.like(memberId = 1L, productId = 100L)

            // act
            likeService.like(memberId = 1L, productId = 100L)

            // assert
            val all = fakeRepository.findAllByMemberId(1L)
            assertThat(all).hasSize(1)
        }
    }

    @DisplayName("좋아요를 취소할 때,")
    @Nested
    inner class Unlike {
        @DisplayName("좋아요가 있으면, 정상적으로 삭제된다.")
        @Test
        fun deletesLike_whenExists() {
            // arrange
            likeService.like(memberId = 1L, productId = 100L)

            // act
            likeService.unlike(memberId = 1L, productId = 100L)

            // assert
            val result = fakeRepository.findByMemberIdAndProductId(1L, 100L)
            assertThat(result).isNull()
        }

        @DisplayName("좋아요가 없는 상태에서 취소해도, 예외가 발생하지 않는다 (멱등).")
        @Test
        fun doesNotThrow_whenNotLiked() {
            // act & assert — no exception
            likeService.unlike(memberId = 1L, productId = 100L)
        }
    }

    @DisplayName("좋아요한 상품 ID 목록을 조회할 때,")
    @Nested
    inner class GetLikedProductIds {
        @DisplayName("좋아요한 상품이 있으면, 상품 ID 목록을 반환한다.")
        @Test
        fun returnsProductIds_whenLikesExist() {
            // arrange
            likeService.like(memberId = 1L, productId = 100L)
            likeService.like(memberId = 1L, productId = 200L)

            // act
            val result = likeService.getLikedProductIds(1L)

            // assert
            assertThat(result).containsExactlyInAnyOrder(100L, 200L)
        }

        @DisplayName("좋아요한 상품이 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyList_whenNoLikes() {
            // act
            val result = likeService.getLikedProductIds(1L)

            // assert
            assertThat(result).isEmpty()
        }

        @DisplayName("다른 회원의 좋아요는 포함하지 않는다.")
        @Test
        fun excludesOtherMemberLikes() {
            // arrange
            likeService.like(memberId = 1L, productId = 100L)
            likeService.like(memberId = 2L, productId = 200L)

            // act
            val result = likeService.getLikedProductIds(1L)

            // assert
            assertThat(result).containsExactly(100L)
        }
    }
}
