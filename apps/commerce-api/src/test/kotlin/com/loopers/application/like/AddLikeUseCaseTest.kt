package com.loopers.application.like

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.application.user.UserCommand
import com.loopers.domain.product.ProductRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.LikeErrorCode
import com.loopers.support.error.ProductErrorCode
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class AddLikeUseCaseTest @Autowired constructor(
    private val addLikeUseCase: AddLikeUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val registerProductUseCase: RegisterProductUseCase,
    private val productRepository: ProductRepository,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerUser(loginId: String = "testuser"): Long {
        registerUserUseCase.execute(
            UserCommand.Register(
                loginId = loginId,
                rawPassword = "Test123!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            ),
        )
        return userJpaRepository.findByLoginId(loginId)!!.id
    }

    private fun registerProduct(name: String = "테스트 상품"): Long {
        val brandId = registerBrandUseCase.execute(BrandCommand.Register(name = "나이키")).id
        return registerProductUseCase.execute(
            ProductCommand.Register(
                brandId = brandId,
                name = name,
                description = "상품 설명",
                price = 10000,
                stock = 100,
                imageUrl = "https://example.com/image.jpg",
            ),
        ).id
    }

    @DisplayName("좋아요 등록")
    @Nested
    inner class Execute {

        @DisplayName("활성 상품에 좋아요를 등록하면 likeCount가 1 증가한다")
        @Test
        fun success() {
            // arrange
            val userId = registerUser()
            val productId = registerProduct()

            // act
            addLikeUseCase.execute(LikeCommand.Create(userId = userId, productId = productId))

            // assert
            val product = productRepository.findByIdOrNull(productId)
            assertThat(product?.likeCount).isEqualTo(1)
        }

        @DisplayName("이미 좋아요한 상품에 다시 좋아요하면 ALREADY_LIKED 예외가 발생한다")
        @Test
        fun failWhenAlreadyLiked() {
            // arrange
            val userId = registerUser()
            val productId = registerProduct()
            addLikeUseCase.execute(LikeCommand.Create(userId = userId, productId = productId))

            // act & assert
            val exception = assertThrows<CoreException> {
                addLikeUseCase.execute(LikeCommand.Create(userId = userId, productId = productId))
            }
            assertThat(exception.errorCode).isEqualTo(LikeErrorCode.ALREADY_LIKED)
        }

        @DisplayName("존재하지 않는 상품에 좋아요하면 PRODUCT_NOT_FOUND 예외가 발생한다")
        @Test
        fun failWhenProductNotFound() {
            // arrange
            val userId = registerUser()

            // act & assert
            val exception = assertThrows<CoreException> {
                addLikeUseCase.execute(LikeCommand.Create(userId = userId, productId = 999L))
            }
            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND)
        }

        @DisplayName("삭제된 상품에 좋아요하면 PRODUCT_NOT_FOUND 예외가 발생한다")
        @Test
        fun failWhenProductDeleted() {
            // arrange
            val userId = registerUser()
            val productId = registerProduct()
            val product = productRepository.findByIdOrNull(productId)!!
            product.delete()
            productRepository.save(product)

            // act & assert
            val exception = assertThrows<CoreException> {
                addLikeUseCase.execute(LikeCommand.Create(userId = userId, productId = productId))
            }
            assertThat(exception.errorCode).isEqualTo(ProductErrorCode.PRODUCT_NOT_FOUND)
        }
    }
}
