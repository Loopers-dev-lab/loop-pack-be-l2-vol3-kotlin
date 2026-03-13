package com.loopers.application.like

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.BrandService
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.ProductService
import com.loopers.infrastructure.like.ProductLikeJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@Transactional
class LikeServiceIntegrationTest @Autowired constructor(
    private val likeService: LikeService,
    private val brandService: BrandService,
    private val productService: ProductService,
    private val productLikeJpaRepository: ProductLikeJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private var productId: Long = 0
    private val memberId: Long = 1L

    @BeforeEach
    fun setUp() {
        val brand = brandService.createBrand(
            BrandCommand.Create(name = "루퍼스", description = "테스트", imageUrl = "https://example.com/brand.jpg"),
        )
        val product = productService.createProduct(
            ProductCommand.Create(
                brandId = brand.id,
                name = "감성 티셔츠",
                description = "좋은 상품",
                price = 39000,
                stockQuantity = 100,
                imageUrl = "https://example.com/product.jpg",
            ),
        )
        productId = product.id
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("좋아요를 등록할 때,")
    @Nested
    inner class Like {
        @DisplayName("처음 좋아요하면, 정상적으로 등록된다.")
        @Test
        fun createsLike_whenFirstTime() {
            // act
            likeService.like(memberId, productId)

            // assert
            val likes = productLikeJpaRepository.findAllByMemberId(memberId)
            assertThat(likes).hasSize(1)
            assertThat(likes[0].productId).isEqualTo(productId)
        }

        // 중복 좋아요 멱등 처리는 Facade 책임 (exists 사전 조회 + UNIQUE Constraint)
        // E2E 테스트(LikeV1ApiE2ETest)와 동시성 테스트(ConcurrencyTest)에서 검증
    }

    @DisplayName("좋아요를 취소할 때,")
    @Nested
    inner class Unlike {
        @DisplayName("좋아요가 있으면, 정상적으로 삭제된다.")
        @Test
        fun deletesLike_whenLikeExists() {
            // arrange
            likeService.like(memberId, productId)

            // act
            likeService.unlike(memberId, productId)

            // assert
            val likes = productLikeJpaRepository.findAllByMemberId(memberId)
            assertThat(likes).isEmpty()
        }

        @DisplayName("좋아요가 없어도, 멱등하게 처리된다.")
        @Test
        fun handlesIdempotently_whenNoLikeExists() {
            // act & assert - no exception
            likeService.unlike(memberId, productId)
            val likes = productLikeJpaRepository.findAllByMemberId(memberId)
            assertThat(likes).isEmpty()
        }
    }

    @DisplayName("좋아요한 상품 ID를 조회할 때,")
    @Nested
    inner class GetLikedProductIds {
        @DisplayName("좋아요한 상품이 있으면, ID 목록을 반환한다.")
        @Test
        fun returnsProductIds_whenLikesExist() {
            // arrange
            likeService.like(memberId, productId)

            // act
            val result = likeService.getLikedProductIds(memberId)

            // assert
            assertThat(result).containsExactly(productId)
        }

        @DisplayName("좋아요한 상품이 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmpty_whenNoLikesExist() {
            // act
            val result = likeService.getLikedProductIds(memberId)

            // assert
            assertThat(result).isEmpty()
        }
    }
}
