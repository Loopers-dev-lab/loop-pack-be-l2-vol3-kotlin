package com.loopers.application.like

import com.loopers.domain.catalog.CatalogCommand
import com.loopers.domain.catalog.CatalogService
import com.loopers.domain.catalog.brand.FakeBrandRepository
import com.loopers.domain.catalog.product.FakeProductRepository
import com.loopers.domain.like.FakeLikeRepository
import com.loopers.domain.like.LikeService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class LikeFacadeTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var likeRepository: FakeLikeRepository
    private lateinit var catalogService: CatalogService
    private lateinit var likeService: LikeService
    private lateinit var likeFacade: LikeFacade

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        productRepository = FakeProductRepository()
        likeRepository = FakeLikeRepository()
        catalogService = CatalogService(brandRepository, productRepository)
        likeService = LikeService(likeRepository)
        likeFacade = LikeFacade(likeService, catalogService)
    }

    private fun createBrandAndProduct(): Pair<Long, Long> {
        val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = "나이키"))
        val product = catalogService.createProduct(
            CatalogCommand.CreateProduct(
                brandId = brand.id,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
            ),
        )
        return brand.id to product.id
    }

    @Nested
    @DisplayName("addLike 시")
    inner class AddLike {

        @Test
        @DisplayName("활성 상품에 좋아요를 등록하면 likeCount가 증가한다")
        fun addLike_activeProduct_increasesLikeCount() {
            // arrange
            val (_, productId) = createBrandAndProduct()

            // act
            likeFacade.addLike(1L, productId)

            // assert
            val product = catalogService.getProduct(productId)
            assertThat(product.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("이미 좋아요한 상품에 다시 좋아요하면 likeCount가 증가하지 않는다 (멱등)")
        fun addLike_duplicate_doesNotIncreaseLikeCount() {
            // arrange
            val (_, productId) = createBrandAndProduct()

            // act
            likeFacade.addLike(1L, productId)
            likeFacade.addLike(1L, productId)

            // assert
            val product = catalogService.getProduct(productId)
            assertThat(product.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("삭제된 상품에 좋아요하면 NOT_FOUND 예외가 발생한다")
        fun addLike_deletedProduct_throwsNotFound() {
            // arrange
            val (_, productId) = createBrandAndProduct()
            catalogService.deleteProduct(productId)

            // act
            val exception = assertThrows<CoreException> {
                likeFacade.addLike(1L, productId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("removeLike 시")
    inner class RemoveLike {

        @Test
        @DisplayName("좋아요를 취소하면 likeCount가 감소한다")
        fun removeLike_activeProduct_decreasesLikeCount() {
            // arrange
            val (_, productId) = createBrandAndProduct()
            likeFacade.addLike(1L, productId)

            // act
            likeFacade.removeLike(1L, productId)

            // assert
            val product = catalogService.getProduct(productId)
            assertThat(product.likeCount).isEqualTo(0)
        }

        @Test
        @DisplayName("삭제된 상품의 좋아요를 취소하면 likeCount를 갱신하지 않는다")
        fun removeLike_deletedProduct_doesNotUpdateLikeCount() {
            // arrange
            val (_, productId) = createBrandAndProduct()
            likeFacade.addLike(1L, productId)
            catalogService.deleteProduct(productId)

            // act
            likeFacade.removeLike(1L, productId)

            // assert
            val product = catalogService.getProduct(productId)
            assertThat(product.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("좋아요가 없는 상태에서 취소해도 예외가 발생하지 않는다 (멱등)")
        fun removeLike_noLike_isIdempotent() {
            // arrange
            val (_, productId) = createBrandAndProduct()

            // act & assert
            likeFacade.removeLike(1L, productId)
        }
    }

    @Nested
    @DisplayName("getLikes 시")
    inner class GetLikes {

        @Test
        @DisplayName("활성 상품만 포함하여 LikeInfo 목록을 반환한다")
        fun getLikes_returnsOnlyActiveProducts() {
            // arrange
            val brand = catalogService.createBrand(CatalogCommand.CreateBrand(name = "나이키"))
            val product1 = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "상품1",
                    price = BigDecimal("10000"),
                    stock = 10,
                ),
            )
            val product2 = catalogService.createProduct(
                CatalogCommand.CreateProduct(
                    brandId = brand.id,
                    name = "상품2",
                    price = BigDecimal("20000"),
                    stock = 10,
                ),
            )
            likeFacade.addLike(1L, product1.id)
            likeFacade.addLike(1L, product2.id)
            catalogService.deleteProduct(product2.id)

            // act
            val result = likeFacade.getLikes(1L)

            // assert
            assertThat(result).hasSize(1)
            assertThat(result[0].product.name).isEqualTo("상품1")
        }

        @Test
        @DisplayName("좋아요가 없으면 빈 리스트를 반환한다")
        fun getLikes_noLikes_returnsEmptyList() {
            // act
            val result = likeFacade.getLikes(1L)

            // assert
            assertThat(result).isEmpty()
        }
    }
}
