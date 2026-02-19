package com.loopers.domain.productlike

import com.loopers.domain.brand.Brand
import com.loopers.domain.product.Product
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.user.User
import com.loopers.domain.user.vo.BirthDate
import com.loopers.domain.user.vo.Email
import com.loopers.domain.user.vo.LoginId
import com.loopers.domain.user.vo.Name
import com.loopers.domain.user.vo.Password
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.productlike.ProductLikeJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal

@SpringBootTest
class ProductLikeServiceIntegrationTest @Autowired constructor(
    private val productLikeService: ProductLikeService,
    private val productLikeJpaRepository: ProductLikeJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(): Brand {
        return Brand.create(name = "Test Brand", description = "Test Description")
            .let { brandJpaRepository.save(it) }
    }

    private fun createProduct(brand: Brand = createBrand()): Product {
        return Product.create(
            brand = brand,
            name = "Test Product",
            price = BigDecimal("10000.00"),
            stock = 100,
            status = ProductStatus.ACTIVE,
        ).let { productJpaRepository.save(it) }
    }

    private fun createUser(): User {
        return User.create(
            loginId = LoginId.of("testuser"),
            password = Password.ofEncrypted("encodedPassword"),
            name = Name.of("테스트 사용자"),
            birthDate = BirthDate.of("20000101"),
            email = Email.of("test@example.com"),
        ).let { userJpaRepository.save(it) }
    }

    @DisplayName("상품 좋아요 등록")
    @Nested
    inner class LikeProduct {
        @DisplayName("유효한 사용자와 상품이 주어지면 좋아요를 등록하고 카운트를 증가시킨다")
        @Test
        fun likesProduct_whenValidUserAndProductAreProvided() {
            // arrange
            val user = createUser()
            val product = createProduct()
            val initialLikeCount = product.likeCount

            // act
            productLikeService.addProductLike(user, product)

            // assert
            val savedProductLike = productLikeJpaRepository.findByUserIdAndProductId(user.id, product.id)
            val updatedProduct = productJpaRepository.findById(product.id).get()

            assertAll(
                { assertThat(savedProductLike).isNotNull() },
                { assertThat(updatedProduct.likeCount).isEqualTo(initialLikeCount + 1) },
            )
        }

        @DisplayName("이미 좋아요한 상품을 다시 좋아요하면 멱등성을 보장한다 (아무것도 안 함)")
        @Test
        fun isIdempotent_whenProductIsAlreadyLiked() {
            // arrange
            val user = createUser()
            val product = createProduct()
            productLikeService.addProductLike(user, product)
            val productAfterFirstLike = productJpaRepository.findById(product.id).get()
            val likeCountAfterFirstLike = productAfterFirstLike.likeCount

            // act
            productLikeService.addProductLike(user, product)

            // assert
            val productAfterSecondLike = productJpaRepository.findById(product.id).get()
            assertAll(
                { assertThat(productLikeJpaRepository.findByUserIdAndProductId(user.id, product.id)).isNotNull() },
                { assertThat(productAfterSecondLike.likeCount).isEqualTo(likeCountAfterFirstLike) },
            )
        }
    }

    @DisplayName("상품 좋아요 취소")
    @Nested
    inner class UnlikeProduct {
        @DisplayName("좋아요 기록이 있으면 취소하고 카운트를 감소시킨다")
        @Test
        fun unlikesProduct_whenLikeRecordExists() {
            // arrange
            val user = createUser()
            val product = createProduct()
            productLikeService.addProductLike(user, product)
            val productAfterLike = productJpaRepository.findById(product.id).get()
            val likeCountAfterLike = productAfterLike.likeCount

            // act
            productLikeService.removeProductLike(user, product)

            // assert
            val savedProductLike = productLikeJpaRepository.findByUserIdAndProductId(user.id, product.id)
            val productAfterUnlike = productJpaRepository.findById(product.id).get()

            assertAll(
                { assertThat(savedProductLike).isNull() },
                { assertThat(productAfterUnlike.likeCount).isEqualTo(likeCountAfterLike - 1) },
            )
        }

        @DisplayName("좋아요 기록이 없으면 멱등성을 보장한다 (아무것도 안 함)")
        @Test
        fun isIdempotent_whenLikeRecordDoesNotExist() {
            // arrange
            val user = createUser()
            val product = createProduct()

            // act & assert (예외 발생 없음)
            productLikeService.removeProductLike(user, product)
            assertThat(productLikeJpaRepository.findByUserIdAndProductId(user.id, product.id)).isNull()
        }
    }

    @DisplayName("내가 좋아요한 상품 목록 조회")
    @Nested
    inner class GetMyLikedProducts {
        @DisplayName("사용자가 좋아요한 상품 목록을 최신순으로 반환한다")
        @Test
        fun returnsMyLikedProducts_inDescendingOrderByCreatedAt() {
            // arrange
            val user = createUser()
            val product1 = createProduct()
            val product2 = createProduct()
            val product3 = createProduct()

            productLikeService.addProductLike(user, product1)
            productLikeService.addProductLike(user, product2)
            productLikeService.addProductLike(user, product3)

            // act
            val result = productLikeService.getMyLikedProducts(user.id, PageRequest.of(0, 10))

            // assert
            assertAll(
                { assertThat(result.totalElements).isEqualTo(3) },
                { assertThat(result.content[0].id).isEqualTo(product3.id) },
                { assertThat(result.content[1].id).isEqualTo(product2.id) },
                { assertThat(result.content[2].id).isEqualTo(product1.id) },
            )
        }

        @DisplayName("좋아요한 상품이 없으면 빈 목록을 반환한다")
        @Test
        fun returnsEmptyPage_whenUserHasNoLikedProducts() {
            // arrange
            val user = createUser()

            // act
            val result = productLikeService.getMyLikedProducts(user.id, PageRequest.of(0, 10))

            // assert
            assertThat(result.totalElements).isZero()
        }
    }
}
