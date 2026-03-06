package com.loopers.domain.like

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest

@SpringBootTest
class LikeServiceIntegrationTest @Autowired constructor(
    private val likeService: LikeService,
    private val brandService: BrandService,
    private val productService: ProductService,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand() = brandService.createBrand(
        name = "Nike",
        logoImageUrl = "test.png",
        description = "테스트 브랜드",
        zipCode = "12345",
        roadAddress = "서울특별시 중구 테스트길 1",
        detailAddress = "1층",
        email = "nike@google.com",
        phoneNumber = "02-3783-4401",
        businessNumber = "123-45-67890",
    )

    private fun createProduct(brandId: Long, name: String = "Air Max") =
        productService.createProduct(brandId, name, "image.png", "설명", 50_000L)

    @Nested
    inner class Like {

        @Test
        fun `최초 좋아요 시 LikeModel이 저장된다`() {
            val brand = createBrand()
            val product = createProduct(brand.id)

            val (_, likeModel) = likeService.like(1L, product.id)

            assertThat(likeModel.id).isGreaterThan(0)
            assertThat(likeModel.userId).isEqualTo(1L)
            assertThat(likeModel.productId).isEqualTo(product.id)
        }

        @Test
        fun `중복 좋아요 시 기존 LikeModel을 반환하고 중복 저장되지 않는다`() {
            val brand = createBrand()
            val product = createProduct(brand.id)

            val (_, firstLike) = likeService.like(1L, product.id)
            val (_, secondLike) = likeService.like(1L, product.id)

            assertThat(secondLike.id).isEqualTo(firstLike.id)

            val allLikes = likeService.getLikedProducts(1L, PageRequest.of(0, 10))
            assertThat(allLikes).hasSize(1)
        }
    }

    @Nested
    inner class Unlike {

        @Test
        fun `좋아요 취소 시 해당 좋아요가 삭제된다`() {
            val brand = createBrand()
            val product = createProduct(brand.id)
            likeService.like(1L, product.id)

            likeService.unlike(1L, product.id)

            val result = likeService.getLikedProducts(1L, PageRequest.of(0, 10))
            assertThat(result).isEmpty()
        }

        @Test
        fun `존재하지 않는 좋아요 취소 시 에러 없이 종료된다`() {
            val brand = createBrand()
            val product = createProduct(brand.id)

            // 에러 없이 종료
            likeService.unlike(999L, product.id)
        }
    }

    @Nested
    inner class GetLikedProducts {

        @Test
        fun `좋아요한 상품 목록을 페이징 조회한다`() {
            val brand = createBrand()
            val product1 = createProduct(brand.id, "Product1")
            val product2 = createProduct(brand.id, "Product2")
            val product3 = createProduct(brand.id, "Product3")

            likeService.like(1L, product1.id)
            likeService.like(1L, product2.id)
            likeService.like(2L, product3.id)

            val result = likeService.getLikedProducts(1L, PageRequest.of(0, 10))

            assertThat(result).hasSize(2)
            assertThat(result.map { it.userId }).containsOnly(1L)
        }
    }
}
