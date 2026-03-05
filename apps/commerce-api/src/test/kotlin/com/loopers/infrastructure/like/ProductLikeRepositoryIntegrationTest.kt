package com.loopers.infrastructure.like

import com.loopers.domain.like.ProductLike
import com.loopers.domain.like.ProductLikeRepository
import com.loopers.support.page.PageRequest
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@DisplayName("ProductLikeRepository 통합 테스트")
@SpringBootTest
class ProductLikeRepositoryIntegrationTest
@Autowired
constructor(
    private val productLikeRepository: ProductLikeRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun saveLike(
        userId: Long = 1L,
        productId: Long = 100L,
    ) {
        productLikeRepository.save(ProductLike.register(userId = userId, productId = productId))
    }

    @Nested
    @DisplayName("save 시")
    inner class WhenSave {
        @Test
        @DisplayName("저장 후 existsByUserIdAndProductId로 조회하면 true를 반환한다")
        fun save_success() {
            // arrange
            val userId = 1L
            val productId = 100L

            // act
            saveLike(userId = userId, productId = productId)

            // assert
            assertThat(productLikeRepository.existsByUserIdAndProductId(userId, productId)).isTrue()
        }

        @Test
        @DisplayName("동일한 userId, productId로 중복 저장해도 예외가 발생하지 않는다")
        fun save_duplicateIdempotent() {
            // arrange
            saveLike(userId = 1L, productId = 100L)

            // act & assert (no exception)
            saveLike(userId = 1L, productId = 100L)
        }
    }

    @Nested
    @DisplayName("existsByUserIdAndProductId 시")
    inner class WhenExistsByUserIdAndProductId {
        @Test
        @DisplayName("존재하는 좋아요를 조회하면 true를 반환한다")
        fun exists_true() {
            // arrange
            saveLike(userId = 1L, productId = 100L)

            // act & assert
            assertThat(productLikeRepository.existsByUserIdAndProductId(1L, 100L)).isTrue()
        }

        @Test
        @DisplayName("존재하지 않는 좋아요를 조회하면 false를 반환한다")
        fun exists_false() {
            // act & assert
            assertThat(productLikeRepository.existsByUserIdAndProductId(1L, 100L)).isFalse()
        }
    }

    @Nested
    @DisplayName("deleteByUserIdAndProductId 시")
    inner class WhenDeleteByUserIdAndProductId {
        @Test
        @Transactional
        @DisplayName("삭제 후 existsByUserIdAndProductId로 조회하면 false를 반환한다")
        fun delete_hardDelete() {
            // arrange
            saveLike(userId = 1L, productId = 100L)

            // act
            productLikeRepository.deleteByUserIdAndProductId(1L, 100L)

            // assert
            assertThat(productLikeRepository.existsByUserIdAndProductId(1L, 100L)).isFalse()
        }

        @Test
        @Transactional
        @DisplayName("존재하지 않는 데이터를 삭제해도 예외가 발생하지 않는다")
        fun delete_nonExistent() {
            // act & assert (no exception)
            productLikeRepository.deleteByUserIdAndProductId(999L, 999L)
        }
    }

    @Nested
    @DisplayName("findAllByUserId 시")
    inner class WhenFindAllByUserId {
        @Test
        @DisplayName("사용자의 좋아요 목록을 페이징하여 조회한다")
        fun findAll_paging() {
            // arrange
            val userId = 1L
            repeat(15) { saveLike(userId = userId, productId = (it + 1).toLong()) }

            // act
            val pageRequest = PageRequest().apply { size = 10 }
            val result = productLikeRepository.findAllByUserId(userId, pageRequest)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(10) },
                { assertThat(result.totalElements).isEqualTo(15L) },
                { assertThat(result.page).isEqualTo(0) },
                { assertThat(result.size).isEqualTo(10) },
            )
        }

        @Test
        @DisplayName("id 기준 내림차순으로 정렬된다")
        fun findAll_sortedByIdDesc() {
            // arrange
            val userId = 1L
            saveLike(userId = userId, productId = 1L)
            saveLike(userId = userId, productId = 2L)
            saveLike(userId = userId, productId = 3L)

            // act
            val result = productLikeRepository.findAllByUserId(userId, PageRequest())

            // assert
            val productIds = result.content.map { it.productId }
            assertThat(productIds).isEqualTo(listOf(3L, 2L, 1L))
        }

        @Test
        @DisplayName("좋아요가 없으면 빈 목록을 반환한다")
        fun findAll_empty() {
            // act
            val result = productLikeRepository.findAllByUserId(999L, PageRequest())

            // assert
            assertAll(
                { assertThat(result.content).isEmpty() },
                { assertThat(result.totalElements).isEqualTo(0L) },
            )
        }
    }

    @Nested
    @DisplayName("countByProductId 시")
    inner class WhenCountByProductId {
        @Test
        @DisplayName("상품의 좋아요 수를 정확히 반환한다")
        fun count_multiple() {
            // arrange
            val productId = 100L
            saveLike(userId = 1L, productId = productId)
            saveLike(userId = 2L, productId = productId)
            saveLike(userId = 3L, productId = productId)

            // act & assert
            assertThat(productLikeRepository.countByProductId(productId)).isEqualTo(3)
        }

        @Test
        @DisplayName("좋아요가 없는 상품은 0을 반환한다")
        fun count_zero() {
            // act & assert
            assertThat(productLikeRepository.countByProductId(999L)).isEqualTo(0)
        }
    }
}
