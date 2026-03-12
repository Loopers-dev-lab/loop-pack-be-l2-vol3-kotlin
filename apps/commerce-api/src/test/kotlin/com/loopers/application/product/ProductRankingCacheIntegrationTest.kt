package com.loopers.application.product

import com.loopers.application.brand.BrandUseCase
import com.loopers.application.like.LikeUseCase
import com.loopers.domain.product.ProductSortType
import com.loopers.infrastructure.brand.BrandEntity
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.like.LikeJpaRepository
import com.loopers.infrastructure.product.ProductEntity
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ProductRankingCacheIntegrationTest @Autowired constructor(
    private val productUseCase: ProductUseCase,
    private val brandUseCase: BrandUseCase,
    private val likeUseCase: LikeUseCase,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val likeJpaRepository: LikeJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
        databaseCleanUp.truncateAllTables()
    }

    @Test
    fun `좋아요_등록과_취소_시_상품_like_count가_동기화된다`() {
        // arrange
        val brand = saveBrand()
        val product = saveProduct(brandId = brand.id!!)

        // act
        val firstLike = likeUseCase.register(memberId = 1L, productId = product.id!!)
        likeUseCase.register(memberId = 2L, productId = product.id!!)

        // assert
        assertThat(productJpaRepository.findById(product.id!!).orElseThrow().likeCount).isEqualTo(2L)
        assertThat(likeJpaRepository.countByProductId(product.id!!)).isEqualTo(2L)

        // act
        likeUseCase.remove(likeId = firstLike.id, memberId = 1L)

        // assert
        assertThat(productJpaRepository.findById(product.id!!).orElseThrow().likeCount).isEqualTo(1L)
        assertThat(likeJpaRepository.countByProductId(product.id!!)).isEqualTo(1L)
    }

    @Test
    fun `상품_상세_캐시는_좋아요_등록_후_무효화되어_최신_like_count를_반환한다`() {
        // arrange
        val brand = saveBrand()
        val product = saveProduct(brandId = brand.id!!)

        // act
        val cachedBeforeLike = productUseCase.getById(product.id!!)
        likeUseCase.register(memberId = 1L, productId = product.id!!)
        val refreshedAfterLike = productUseCase.getById(product.id!!)

        // assert
        assertThat(cachedBeforeLike.likeCount).isZero()
        assertThat(refreshedAfterLike.likeCount).isEqualTo(1L)
    }

    @Test
    fun `브랜드_필터_좋아요순_목록은_좋아요_변경_후_최신_순서를_반환한다`() {
        // arrange
        val brand = saveBrand()
        val lowRankProduct = saveProduct(brandId = brand.id!!, name = "저순위 상품")
        val highRankProduct = saveProduct(brandId = brand.id!!, name = "고순위 상품")

        // act
        val cachedBeforeLike = productUseCase.getAll(ProductSortType.LIKES_DESC, brand.id)
        likeUseCase.register(memberId = 1L, productId = lowRankProduct.id!!)
        val refreshedAfterLike = productUseCase.getAll(ProductSortType.LIKES_DESC, brand.id)

        // assert
        assertThat(cachedBeforeLike.map { it.id }).containsExactly(highRankProduct.id!!, lowRankProduct.id!!)
        assertThat(refreshedAfterLike.first().id).isEqualTo(lowRankProduct.id!!)
    }

    @Test
    fun `브랜드명_변경_후_상품_상세_캐시는_무효화된다`() {
        // arrange
        val brand = saveBrand(name = "변경전 브랜드")
        val product = saveProduct(brandId = brand.id!!)

        // act
        val cachedBeforeBrandChange = productUseCase.getById(product.id!!)
        brandUseCase.changeName(
            brand.id!!,
            BrandUseCase.ChangeNameCommand(name = "변경후 브랜드"),
        )
        val refreshedAfterBrandChange = productUseCase.getById(product.id!!)

        // assert
        assertThat(cachedBeforeBrandChange.brandName).isEqualTo("변경전 브랜드")
        assertThat(refreshedAfterBrandChange.brandName).isEqualTo("변경후 브랜드")
    }

    private fun saveBrand(name: String = "테스트 브랜드"): BrandEntity {
        return brandJpaRepository.save(
            BrandEntity(
                name = name,
                status = "ACTIVE",
            ),
        )
    }

    private fun saveProduct(
        brandId: Long,
        name: String = "테스트 상품",
        price: Long = 10_000L,
    ): ProductEntity {
        return productJpaRepository.save(
            ProductEntity(
                brandId = brandId,
                name = name,
                price = price,
                description = "$name 설명",
                stock = 100,
                status = "SELLING",
            ),
        )
    }
}
