package com.loopers.infrastructure.like

import com.loopers.domain.like.LikeModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class LikeRepositoryImplTest {

    private val likeJpaRepository: LikeJpaRepository = mockk()
    private val likeRepositoryImpl = LikeRepositoryImpl(likeJpaRepository)

    @DisplayName("좋아요를 저장할 때,")
    @Nested
    inner class Save {
        @DisplayName("JpaRepository에 위임하여 저장하고 결과를 반환한다.")
        @Test
        fun delegatesToJpaRepository() {
            // arrange
            val like = LikeModel(userId = 1L, productId = 10L)
            every { likeJpaRepository.save(like) } returns like

            // act
            val result = likeRepositoryImpl.save(like)

            // assert
            assertThat(result).isEqualTo(like)
            verify(exactly = 1) { likeJpaRepository.save(like) }
        }
    }

    @DisplayName("userId와 productId로 좋아요를 조회할 때,")
    @Nested
    inner class FindByUserIdAndProductId {
        @DisplayName("기록이 존재하면 반환한다 (삭제 여부 무관).")
        @Test
        fun returnsLike_whenExists() {
            // arrange
            val like = LikeModel(userId = 1L, productId = 10L)
            every { likeJpaRepository.findByUserIdAndProductId(1L, 10L) } returns like

            // act
            val result = likeRepositoryImpl.findByUserIdAndProductId(1L, 10L)

            // assert
            assertThat(result).isNotNull
            assertThat(result!!.userId).isEqualTo(1L)
            assertThat(result.productId).isEqualTo(10L)
            verify(exactly = 1) { likeJpaRepository.findByUserIdAndProductId(1L, 10L) }
        }

        @DisplayName("기록이 없으면 null을 반환한다.")
        @Test
        fun returnsNull_whenNotExists() {
            // arrange
            every { likeJpaRepository.findByUserIdAndProductId(1L, 999L) } returns null

            // act
            val result = likeRepositoryImpl.findByUserIdAndProductId(1L, 999L)

            // assert
            assertThat(result).isNull()
        }
    }

    @DisplayName("유저의 활성 좋아요 목록을 조회할 때,")
    @Nested
    inner class FindAllByUserId {
        @DisplayName("JpaRepository에 위임하여 삭제되지 않은 좋아요만 페이징 반환한다.")
        @Test
        fun returnsActivelikesForUser() {
            // arrange
            val pageable = PageRequest.of(0, 10)
            val likes = listOf(
                LikeModel(userId = 1L, productId = 10L),
                LikeModel(userId = 1L, productId = 20L),
            )
            val page = PageImpl(likes, pageable, 2)
            every { likeJpaRepository.findAllByUserIdAndDeletedAtIsNull(1L, pageable) } returns page

            // act
            val result = likeRepositoryImpl.findAllByUserIdAndDeletedAtIsNull(1L, pageable)

            // assert
            assertThat(result.content).hasSize(2)
            assertThat(result.content.map { it.productId }).containsExactly(10L, 20L)
            verify(exactly = 1) { likeJpaRepository.findAllByUserIdAndDeletedAtIsNull(1L, pageable) }
        }
    }
}
