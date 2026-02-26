package com.loopers.application.like

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.application.user.UserCommand
import com.loopers.domain.product.ProductRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class GetMyLikesUseCaseTest @Autowired constructor(
    private val getMyLikesUseCase: GetMyLikesUseCase,
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

    private fun registerProduct(name: String = "테스트 상품", brandName: String = "나이키"): Long {
        val brandId = registerBrandUseCase.execute(BrandCommand.Register(name = brandName)).id
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

    @DisplayName("내 좋아요 목록 조회")
    @Nested
    inner class Execute {

        @DisplayName("좋아요한 활성 상품 목록을 반환한다")
        @Test
        fun success() {
            // arrange
            val userId = registerUser()
            val productId = registerProduct("상품A", "브랜드A")
            addLikeUseCase.execute(LikeCommand.Create(userId = userId, productId = productId))

            // act
            val result = getMyLikesUseCase.execute(userId, page = 0, size = 20)

            // assert
            assertThat(result.content).hasSize(1)
        }

        @DisplayName("삭제된 상품은 목록에서 필터링된다")
        @Test
        fun filtersDeletedProducts() {
            // arrange
            val userId = registerUser()
            val activeProductId = registerProduct("활성 상품", "브랜드A")
            val deletedProductId = registerProduct("삭제 상품", "브랜드B")
            addLikeUseCase.execute(LikeCommand.Create(userId = userId, productId = activeProductId))
            addLikeUseCase.execute(LikeCommand.Create(userId = userId, productId = deletedProductId))

            val deletedProduct = productRepository.findByIdOrNull(deletedProductId)!!
            deletedProduct.delete()
            productRepository.save(deletedProduct)

            // act
            val result = getMyLikesUseCase.execute(userId, page = 0, size = 20)

            // assert
            assertThat(result.content).hasSize(1)
        }

        @DisplayName("좋아요한 상품이 없으면 빈 목록을 반환한다")
        @Test
        fun returnsEmptyWhenNoLikes() {
            // arrange
            val userId = registerUser()

            // act
            val result = getMyLikesUseCase.execute(userId, page = 0, size = 20)

            // assert
            assertThat(result.content).isEmpty()
        }
    }
}
