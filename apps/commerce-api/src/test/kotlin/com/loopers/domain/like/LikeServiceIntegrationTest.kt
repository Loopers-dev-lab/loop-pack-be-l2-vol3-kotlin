package com.loopers.domain.like

import com.loopers.domain.brand.Brand
import com.loopers.domain.brand.BrandRepository
import com.loopers.domain.common.Money
import com.loopers.domain.common.StockQuantity
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class LikeServiceIntegrationTest @Autowired constructor(
    private val likeService: LikeService,
    private val productRepository: ProductRepository,
    private val brandRepository: BrandRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createProduct(): Product {
        val brand = brandRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        return productRepository.save(
            Product(name = "에어맥스", description = "러닝화", price = Money.of(159000L), likes = 0, stockQuantity = StockQuantity.of(100), brandId = brand.id),
        )
    }

    @DisplayName("좋아요를 등록할 때,")
    @Nested
    inner class LikeProduct {

        @DisplayName("좋아요가 존재하지 않으면, 저장 후 true를 반환한다.")
        @Test
        fun returnsTrue_whenLikeNotExists() {
            // arrange
            val product = createProduct()
            val userId = 1L

            // act
            val result = likeService.like(userId, product.id)

            // assert
            assertThat(result).isTrue()
        }

        @DisplayName("이미 좋아요가 존재하면, false를 반환한다.")
        @Test
        fun returnsFalse_whenLikeAlreadyExists() {
            // arrange
            val product = createProduct()
            val userId = 1L
            likeService.like(userId, product.id) // 첫 번째 좋아요

            // act
            val result = likeService.like(userId, product.id) // 두 번째 좋아요 (중복)

            // assert
            assertThat(result).isFalse()
        }
    }

    @DisplayName("좋아요를 취소할 때,")
    @Nested
    inner class UnlikeProduct {

        @DisplayName("좋아요가 존재하면, 삭제 후 true를 반환한다.")
        @Test
        fun returnsTrue_whenLikeExists() {
            // arrange
            val product = createProduct()
            val userId = 1L
            likeService.like(userId, product.id) // 좋아요 등록

            // act
            val result = likeService.unlike(userId, product.id)

            // assert
            assertThat(result).isTrue()
        }

        @DisplayName("좋아요가 존재하지 않으면, false를 반환한다.")
        @Test
        fun returnsFalse_whenLikeNotExists() {
            // arrange
            val product = createProduct()
            val userId = 1L

            // act
            val result = likeService.unlike(userId, product.id)

            // assert
            assertThat(result).isFalse()
        }
    }

    @DisplayName("좋아요 목록을 조회할 때,")
    @Nested
    inner class GetLikedProductIds {

        @DisplayName("사용자가 좋아요한 상품 ID 목록을 반환한다.")
        @Test
        fun returnsLikedProductIds() {
            // arrange
            val product1 = createProduct()
            val product2 = createProduct()
            val userId = 1L
            likeService.like(userId, product1.id)
            likeService.like(userId, product2.id)

            // act
            val result = likeService.getLikedProductIds(userId)

            // assert
            assertThat(result).containsExactlyInAnyOrder(product1.id, product2.id)
        }

        @DisplayName("삭제된 상품 ID도 포함하여 반환한다.")
        @Test
        fun includesDeletedProductIds() {
            // arrange
            val product1 = createProduct()
            val product2 = createProduct()
            product2.delete()
            productRepository.save(product2)
            val userId = 1L
            likeService.like(userId, product1.id)
            likeService.like(userId, product2.id)

            // act
            val result = likeService.getLikedProductIds(userId)

            // assert
            assertThat(result).containsExactlyInAnyOrder(product1.id, product2.id)
        }

        @DisplayName("좋아요한 상품이 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyList_whenNoLikes() {
            // arrange
            val userId = 1L

            // act
            val result = likeService.getLikedProductIds(userId)

            // assert
            assertThat(result).isEmpty()
        }
    }
}
