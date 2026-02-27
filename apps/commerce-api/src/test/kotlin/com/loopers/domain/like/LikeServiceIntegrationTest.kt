package com.loopers.domain.like

import com.loopers.infrastructure.like.ProductLikeJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class LikeServiceIntegrationTest @Autowired constructor(
    private val likeService: LikeService,
    private val productLikeJpaRepository: ProductLikeJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("좋아요를 조회할 때, ")
    @Nested
    inner class FindLike {
        @DisplayName("해당 유저와 상품의 좋아요가 존재하면, 좋아요 정보를 반환한다.")
        @Test
        fun returnsLike_whenLikeExists() {
            // arrange
            productLikeJpaRepository.save(ProductLike(userId = 1L, productId = 1L))

            // act
            val result = likeService.findLike(1L, 1L)

            // assert
            assertAll(
                { assertThat(result).isNotNull() },
                { assertThat(result?.userId).isEqualTo(1L) },
                { assertThat(result?.productId).isEqualTo(1L) },
            )
        }

        @DisplayName("해당 유저와 상품의 좋아요가 존재하지 않으면, null을 반환한다.")
        @Test
        fun returnsNull_whenLikeNotExists() {
            // act
            val result = likeService.findLike(1L, 1L)

            // assert
            assertThat(result).isNull()
        }
    }

    @DisplayName("좋아요를 생성할 때, ")
    @Nested
    inner class CreateLike {
        @DisplayName("유저 ID와 상품 ID가 주어지면, 좋아요가 생성된다.")
        @Test
        fun createsLike_whenUserIdAndProductIdAreProvided() {
            // arrange & act
            val result = likeService.createLike(1L, 1L)

            // assert
            val savedLike = productLikeJpaRepository.findById(result.id).get()
            assertAll(
                { assertThat(savedLike.userId).isEqualTo(1L) },
                { assertThat(savedLike.productId).isEqualTo(1L) },
            )
        }
    }

    @DisplayName("좋아요를 삭제할 때, ")
    @Nested
    inner class DeleteLike {
        @DisplayName("좋아요 엔티티가 주어지면, 좋아요가 삭제된다.")
        @Test
        fun deletesLike_whenProductLikeIsProvided() {
            // arrange
            val like = productLikeJpaRepository.save(ProductLike(userId = 1L, productId = 1L))

            // act
            likeService.deleteLike(like)

            // assert
            assertThat(productLikeJpaRepository.findById(like.id)).isEmpty()
        }
    }

    @DisplayName("유저의 좋아요 목록을 조회할 때, ")
    @Nested
    inner class GetUserLikes {
        @DisplayName("좋아요한 상품이 있으면, 목록을 반환한다.")
        @Test
        fun returnsLikeList_whenUserHasLikes() {
            // arrange
            productLikeJpaRepository.save(ProductLike(userId = 1L, productId = 1L))
            productLikeJpaRepository.save(ProductLike(userId = 1L, productId = 2L))
            productLikeJpaRepository.save(ProductLike(userId = 2L, productId = 1L))

            // act
            val result = likeService.getUserLikes(1L)

            // assert
            assertThat(result).hasSize(2)
        }

        @DisplayName("좋아요한 상품이 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyList_whenUserHasNoLikes() {
            // act
            val result = likeService.getUserLikes(1L)

            // assert
            assertThat(result).isEmpty()
        }
    }
}
