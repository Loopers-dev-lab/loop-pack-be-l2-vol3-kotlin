package com.loopers.application.product

import com.loopers.domain.brand.Brand
import com.loopers.domain.like.Like
import com.loopers.domain.product.CreateProductCommand
import com.loopers.domain.product.Product
import com.loopers.domain.user.User
import com.loopers.infrastructure.brand.BrandJpaRepository
import com.loopers.infrastructure.like.LikeJpaRepository
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate

/**
 * ProductFacade 통합 테스트
 * - 실제 DB(TestContainers)와 연동하여 Facade → Service → Repository 레이어 통합 테스트
 * - createProduct: 브랜드 검증 + 상품 생성
 * - addLike: 상품 존재 검증 + 좋아요 등록
 */
@SpringBootTest
class ProductFacadeIntegrationTest @Autowired constructor(
    private val productFacade: ProductFacade,
    private val productJpaRepository: ProductJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val likeJpaRepository: LikeJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createBrand(name: String = "나이키"): Brand {
        return brandJpaRepository.save(Brand(name = name, description = "스포츠 브랜드"))
    }

    private fun createUser(): User {
        return userJpaRepository.save(
            User(
                loginId = "testuser1",
                password = "encodedPassword",
                name = "테스트유저",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            ),
        )
    }

    private fun createProduct(brand: Brand = createBrand()): Product {
        return productJpaRepository.save(
            Product(
                brandId = brand.id,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
                description = null,
                imageUrl = null,
            ),
        )
    }

    @DisplayName("상품을 등록할 때,")
    @Nested
    inner class CreateProduct {

        @DisplayName("정상적인 정보가 주어지면, 상품이 DB에 저장된다.")
        @Test
        fun savesProductToDatabase_whenValidInfoProvided() {
            // arrange
            val brand = createBrand()
            val command = CreateProductCommand(
                brandId = brand.id,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
                description = "나이키 에어맥스 90",
                imageUrl = "https://example.com/airmax90.jpg",
            )

            // act
            val result = productFacade.createProduct(command)

            // assert
            val saved = productJpaRepository.findByIdAndDeletedAtIsNull(result.id)!!
            assertAll(
                { assertThat(saved.name).isEqualTo("에어맥스 90") },
                { assertThat(saved.brandId).isEqualTo(brand.id) },
                { assertThat(saved.price).isEqualByComparingTo(BigDecimal("129000")) },
                { assertThat(saved.stock).isEqualTo(100) },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID로 등록하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandNotExists() {
            // arrange
            val command = CreateProductCommand(
                brandId = 999L,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
                description = null,
                imageUrl = null,
            )

            // act & assert
            val exception = assertThrows<CoreException> {
                productFacade.createProduct(command)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품에 좋아요를 등록할 때,")
    @Nested
    inner class AddLike {

        @DisplayName("정상적인 요청이면, DB에 좋아요가 저장된다.")
        @Test
        fun savesLikeToDatabase_whenValidRequest() {
            // arrange
            val user = createUser()
            val product = createProduct()

            // act
            val result = productFacade.addLike(user.id, product.id)

            // assert
            val saved = likeJpaRepository.findByUserIdAndProductIdAndDeletedAtIsNull(user.id, product.id)!!
            assertAll(
                { assertThat(saved.userId).isEqualTo(user.id) },
                { assertThat(saved.productId).isEqualTo(product.id) },
                { assertThat(result.isDeleted()).isFalse() },
            )
        }

        @DisplayName("이미 좋아요한 상품에 다시 좋아요하면, 기존 좋아요가 유지된다.")
        @Test
        fun returnsExistingLike_whenAlreadyLiked() {
            // arrange
            val user = createUser()
            val product = createProduct()
            val first = productFacade.addLike(user.id, product.id)

            // act
            val second = productFacade.addLike(user.id, product.id)

            // assert
            assertAll(
                { assertThat(second.id).isEqualTo(first.id) },
                { assertThat(likeJpaRepository.findAll()).hasSize(1) },
            )
        }

        @DisplayName("Soft Delete된 좋아요가 있으면, 복원된다.")
        @Test
        fun restoresLike_whenSoftDeletedLikeExists() {
            // arrange
            val user = createUser()
            val product = createProduct()
            val like = likeJpaRepository.save(Like(userId = user.id, productId = product.id))
            like.delete()
            likeJpaRepository.save(like)

            // act
            val result = productFacade.addLike(user.id, product.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(like.id) },
                { assertThat(result.isDeleted()).isFalse() },
                { assertThat(likeJpaRepository.findAll()).hasSize(1) },
            )
        }

        @DisplayName("존재하지 않는 상품이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotExists() {
            // arrange
            val user = createUser()

            // act & assert
            val exception = assertThrows<CoreException> {
                productFacade.addLike(user.id, 999L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("삭제된 상품에도 좋아요를 등록할 수 있다.")
        @Test
        fun addsLike_whenProductIsSoftDeleted() {
            // arrange
            val user = createUser()
            val product = createProduct()
            product.delete()
            productJpaRepository.save(product)

            // act
            val result = productFacade.addLike(user.id, product.id)

            // assert
            assertAll(
                { assertThat(result.userId).isEqualTo(user.id) },
                { assertThat(result.productId).isEqualTo(product.id) },
            )
        }
    }
}
