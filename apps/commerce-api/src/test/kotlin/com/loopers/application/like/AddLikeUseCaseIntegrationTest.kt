package com.loopers.application.like

import com.loopers.application.brand.RegisterBrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.product.DeleteProductUseCase
import com.loopers.application.product.RegisterProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserCommand
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.testcontainers.MySqlTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql

@SpringBootTest
@Import(MySqlTestContainersConfig::class)
@Sql(
    statements = [
        "DELETE FROM likes",
        "DELETE FROM product_image",
        "DELETE FROM product",
        "DELETE FROM brand",
        "DELETE FROM users",
    ],
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class AddLikeUseCaseIntegrationTest {

    @Autowired
    private lateinit var addLikeUseCase: AddLikeUseCase

    @Autowired
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @Autowired
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @Autowired
    private lateinit var registerProductUseCase: RegisterProductUseCase

    @Autowired
    private lateinit var deleteProductUseCase: DeleteProductUseCase

    @Autowired
    private lateinit var productRepository: ProductRepository

    private var userId: Long = 0
    private var productId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = registerUserUseCase.register(createUserCommand())
        val brandId = registerBrandUseCase.register(
            RegisterBrandCommand(name = "테스트브랜드", description = null, logoUrl = null),
        )
        productId = registerProductUseCase.register(createProductCommand(brandId))
    }

    @Test
    fun `정상적인 경우 좋아요가 추가되어야 한다`() {
        addLikeUseCase.add(userId, productId)

        val product = productRepository.findById(productId)
        assertThat(product).isNotNull
        assertThat(product!!.likeCount).isEqualTo(1)
    }

    @Test
    fun `중복 좋아요는 멱등하게 처리되어 likeCount가 1을 유지한다`() {
        addLikeUseCase.add(userId, productId)
        addLikeUseCase.add(userId, productId)

        val product = productRepository.findById(productId)
        assertThat(product!!.likeCount).isEqualTo(1)
    }

    @Test
    fun `삭제된 상품에 좋아요를 추가하면 NOT_FOUND 예외가 발생한다`() {
        deleteProductUseCase.delete(productId)

        assertThatThrownBy { addLikeUseCase.add(userId, productId) }
            .isInstanceOfSatisfying(CoreException::class.java) { ex ->
                assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
            }
    }

    private fun createUserCommand() = RegisterUserCommand(
        loginId = "testuser",
        password = "Password1!",
        name = "테스트",
        birthDate = "1993-04-01",
        email = "test@example.com",
        gender = "MALE",
    )

    private fun createProductCommand(brandId: Long) = RegisterProductCommand(
        brandId = brandId,
        name = "테스트상품",
        description = "설명",
        price = 10000L,
        stock = 100,
        thumbnailUrl = null,
        images = emptyList(),
    )
}
