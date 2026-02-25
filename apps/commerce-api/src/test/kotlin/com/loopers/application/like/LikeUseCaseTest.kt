package com.loopers.application.like

import com.loopers.domain.catalog.brand.FakeBrandRepository
import com.loopers.domain.catalog.brand.model.Brand
import com.loopers.domain.catalog.brand.vo.BrandName
import com.loopers.domain.catalog.product.FakeProductRepository
import com.loopers.domain.catalog.product.model.Product
import com.loopers.domain.common.Money
import com.loopers.domain.like.FakeLikeRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class LikeUseCaseTest {

    private lateinit var brandRepository: FakeBrandRepository
    private lateinit var productRepository: FakeProductRepository
    private lateinit var likeRepository: FakeLikeRepository
    private lateinit var addLikeUseCase: AddLikeUseCase
    private lateinit var removeLikeUseCase: RemoveLikeUseCase
    private lateinit var getUserLikesUseCase: GetUserLikesUseCase

    @BeforeEach
    fun setUp() {
        brandRepository = FakeBrandRepository()
        productRepository = FakeProductRepository()
        likeRepository = FakeLikeRepository()
        addLikeUseCase = AddLikeUseCase(likeRepository, productRepository)
        removeLikeUseCase = RemoveLikeUseCase(likeRepository, productRepository)
        getUserLikesUseCase = GetUserLikesUseCase(likeRepository, productRepository)
    }

    private fun createBrand(name: String = "나이키"): Brand {
        return brandRepository.save(Brand(name = BrandName(name)))
    }

    private fun createProduct(brandId: Long, name: String = "에어맥스 90", price: BigDecimal = BigDecimal("129000"), stock: Int = 100): Product {
        return productRepository.save(
            Product(
                refBrandId = brandId,
                name = name,
                price = Money(price),
                stock = stock,
            ),
        )
    }

    private fun createBrandAndProduct(): Pair<Long, Long> {
        val brand = createBrand()
        val product = createProduct(brand.id)
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
            addLikeUseCase.execute(1L, productId)

            // assert
            val product = productRepository.findById(productId)!!
            assertThat(product.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("이미 좋아요한 상품에 다시 좋아요하면 likeCount가 증가하지 않는다 (멱등)")
        fun addLike_duplicate_doesNotIncreaseLikeCount() {
            // arrange
            val (_, productId) = createBrandAndProduct()

            // act
            addLikeUseCase.execute(1L, productId)
            addLikeUseCase.execute(1L, productId)

            // assert
            val product = productRepository.findById(productId)!!
            assertThat(product.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("동시 요청으로 DB unique constraint 위반이 발생해도 예외 없이 likeCount가 증가하지 않는다 (멱등)")
        fun addLike_concurrentDuplicateSave_doesNotIncreaseLikeCount() {
            // arrange
            val (_, productId) = createBrandAndProduct()
            // 동시성 상황 재현: find에는 보이지 않지만 save 시 unique constraint 위반 발생
            likeRepository.simulateConcurrentInsert(1L, productId)

            // act
            addLikeUseCase.execute(1L, productId)

            // assert: 예외가 전파되지 않고 likeCount도 증가하지 않는다
            val product = productRepository.findById(productId)!!
            assertThat(product.likeCount).isEqualTo(0)
        }

        @Test
        @DisplayName("삭제된 상품에 좋아요하면 NOT_FOUND 예외가 발생한다")
        fun addLike_deletedProduct_throwsNotFound() {
            // arrange
            val (_, productId) = createBrandAndProduct()
            val product = productRepository.findById(productId)!!
            product.delete()
            productRepository.save(product)

            // act
            val exception = assertThrows<CoreException> {
                addLikeUseCase.execute(1L, productId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("HIDDEN 상태인 상품에 좋아요하면 NOT_FOUND 예외가 발생한다")
        fun addLike_hiddenProduct_throwsNotFound() {
            // arrange
            val (_, productId) = createBrandAndProduct()
            val product = productRepository.findById(productId)!!
            product.update(null, null, null, Product.ProductStatus.HIDDEN)
            productRepository.save(product)

            // act
            val exception = assertThrows<CoreException> {
                addLikeUseCase.execute(1L, productId)
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
            addLikeUseCase.execute(1L, productId)

            // act
            removeLikeUseCase.execute(1L, productId)

            // assert
            val product = productRepository.findById(productId)!!
            assertThat(product.likeCount).isEqualTo(0)
        }

        @Test
        @DisplayName("삭제된 상품의 좋아요를 취소하면 likeCount를 갱신하지 않는다")
        fun removeLike_deletedProduct_doesNotUpdateLikeCount() {
            // arrange
            val (_, productId) = createBrandAndProduct()
            addLikeUseCase.execute(1L, productId)
            val product = productRepository.findById(productId)!!
            product.delete()
            productRepository.save(product)

            // act
            removeLikeUseCase.execute(1L, productId)

            // assert
            val found = productRepository.findByIdIncludeDeleted(productId)!!
            assertThat(found.likeCount).isEqualTo(1)
        }

        @Test
        @DisplayName("좋아요가 없는 상태에서 취소해도 예외가 발생하지 않는다 (멱등)")
        fun removeLike_noLike_isIdempotent() {
            // arrange
            val (_, productId) = createBrandAndProduct()

            // act & assert
            removeLikeUseCase.execute(1L, productId)
        }
    }

    @Nested
    @DisplayName("getLikes 시")
    inner class GetLikes {

        @Test
        @DisplayName("활성 상품만 포함하여 LikeInfo 목록을 반환한다")
        fun getLikes_returnsOnlyActiveProducts() {
            // arrange
            val brand = createBrand()
            val product1 = createProduct(brand.id, "상품1", BigDecimal("10000"), 10)
            val product2 = createProduct(brand.id, "상품2", BigDecimal("20000"), 10)
            addLikeUseCase.execute(1L, product1.id)
            addLikeUseCase.execute(1L, product2.id)
            val p2 = productRepository.findById(product2.id)!!
            p2.delete()
            productRepository.save(p2)

            // act
            val result = getUserLikesUseCase.execute(1L)

            // assert
            assertThat(result).hasSize(1)
            assertThat(result[0].productName).isEqualTo("상품1")
        }

        @Test
        @DisplayName("좋아요한 상품이 HIDDEN 상태로 변경되면 목록에서 제외된다")
        fun getLikes_hiddenProduct_isExcludedFromResult() {
            // arrange
            val brand = createBrand()
            val product1 = createProduct(brand.id, "상품1", BigDecimal("10000"), 10)
            val product2 = createProduct(brand.id, "상품2", BigDecimal("20000"), 10)
            addLikeUseCase.execute(1L, product1.id)
            addLikeUseCase.execute(1L, product2.id)
            // product2를 HIDDEN으로 변경
            val p2 = productRepository.findById(product2.id)!!
            p2.update(null, null, null, Product.ProductStatus.HIDDEN)
            productRepository.save(p2)

            // act
            val result = getUserLikesUseCase.execute(1L)

            // assert
            assertThat(result).hasSize(1)
            assertThat(result[0].productName).isEqualTo("상품1")
        }

        @Test
        @DisplayName("사용자의 좋아요 목록을 id 역순으로 반환한다")
        fun getLikes_returnsInReverseIdOrder() {
            // arrange
            val brand = createBrand()
            val product1 = createProduct(brand.id, "상품1", BigDecimal("10000"), 10)
            val product2 = createProduct(brand.id, "상품2", BigDecimal("20000"), 10)
            val product3 = createProduct(brand.id, "상품3", BigDecimal("30000"), 10)
            addLikeUseCase.execute(1L, product1.id)
            addLikeUseCase.execute(1L, product2.id)
            addLikeUseCase.execute(1L, product3.id)

            // act
            val result = getUserLikesUseCase.execute(1L)

            // assert
            assertThat(result).hasSize(3)
            assertThat(result[0].productName).isEqualTo("상품3")
            assertThat(result[1].productName).isEqualTo("상품2")
            assertThat(result[2].productName).isEqualTo("상품1")
        }

        @Test
        @DisplayName("좋아요가 없으면 빈 리스트를 반환한다")
        fun getLikes_noLikes_returnsEmptyList() {
            // act
            val result = getUserLikesUseCase.execute(1L)

            // assert
            assertThat(result).isEmpty()
        }
    }
}
