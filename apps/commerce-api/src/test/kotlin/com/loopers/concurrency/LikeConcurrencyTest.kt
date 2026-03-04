package com.loopers.concurrency

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.like.AddLikeUseCase
import com.loopers.application.like.LikeCommand
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.application.user.UserCommand
import com.loopers.domain.product.ProductRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.LocalDate

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
class LikeConcurrencyTest @Autowired constructor(
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

    private fun registerUser(loginId: String): Long {
        registerUserUseCase.execute(
            UserCommand.Register(
                loginId = loginId,
                rawPassword = "Test123!",
                name = "테스트",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "$loginId@example.com",
            ),
        )
        return userJpaRepository.findByLoginId(loginId)!!.id
    }

    private fun registerBrand(): Long {
        return registerBrandUseCase.execute(BrandCommand.Register(name = "테스트브랜드")).id
    }

    private fun registerProduct(brandId: Long): Long {
        return registerProductUseCase.execute(
            ProductCommand.Register(
                brandId = brandId,
                name = "테스트상품",
                description = "설명",
                price = 1000,
                stock = 100,
                imageUrl = "https://example.com/image.jpg",
            ),
        ).id
    }

    @DisplayName("50명이 동시에 좋아요를 누르면 likeCount가 정확히 50이어야 한다")
    @Test
    fun likeCountShouldBeExactAfterConcurrentLikes() {
        // arrange
        val threadCount = 50
        val userIds = (1..threadCount).map { registerUser("user$it") }
        val brandId = registerBrand()
        val productId = registerProduct(brandId)

        // act
        val actions = userIds.map { userId ->
            { addLikeUseCase.execute(LikeCommand.Create(userId = userId, productId = productId)) }
        }
        val results = ConcurrencyTestHelper.executeConcurrently(actions)

        val successes = results.filter { it.isSuccess }

        // assert
        val product = productRepository.findByIdOrNull(productId)!!

        assertAll(
            { assertThat(successes).`as`("모든 좋아요가 성공해야 한다").hasSize(threadCount) },
            { assertThat(product.likeCount).`as`("likeCount가 정확히 일치해야 한다").isEqualTo(threadCount.toLong()) },
        )
    }
}
