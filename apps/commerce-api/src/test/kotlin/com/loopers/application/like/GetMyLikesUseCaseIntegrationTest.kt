package com.loopers.application.like

import com.loopers.application.brand.RegisterBrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.product.RegisterProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserCommand
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.testcontainers.MySqlTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
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
class GetMyLikesUseCaseIntegrationTest {

    @Autowired
    private lateinit var getMyLikesUseCase: GetMyLikesUseCase

    @Autowired
    private lateinit var addLikeUseCase: AddLikeUseCase

    @Autowired
    private lateinit var registerUserUseCase: RegisterUserUseCase

    @Autowired
    private lateinit var registerBrandUseCase: RegisterBrandUseCase

    @Autowired
    private lateinit var registerProductUseCase: RegisterProductUseCase

    private var userId: Long = 0
    private var brandId: Long = 0

    @BeforeEach
    fun setUp() {
        userId = registerUserUseCase.register(createUserCommand())
        brandId = registerBrandUseCase.register(
            RegisterBrandCommand(name = "테스트브랜드", description = null, logoUrl = null),
        )
    }

    @Test
    fun `좋아요한 목록을 조회할 수 있다`() {
        val productId1 = registerProductUseCase.register(createProductCommand(brandId, "상품A"))
        val productId2 = registerProductUseCase.register(createProductCommand(brandId, "상품B"))
        addLikeUseCase.add(userId, productId1)
        addLikeUseCase.add(userId, productId2)

        val result = getMyLikesUseCase.getMyLikes(userId)

        assertThat(result).hasSize(2)
    }

    @Test
    fun `좋아요가 없으면 빈 목록을 반환한다`() {
        val result = getMyLikesUseCase.getMyLikes(userId)

        assertThat(result).isEmpty()
    }

    private fun createUserCommand() = RegisterUserCommand(
        loginId = "testuser",
        password = "Password1!",
        name = "테스트",
        birthDate = "1993-04-01",
        email = "test@example.com",
        gender = "MALE",
    )

    private fun createProductCommand(brandId: Long, name: String) = RegisterProductCommand(
        brandId = brandId,
        name = name,
        description = "설명",
        price = 10000L,
        stock = 100,
        thumbnailUrl = null,
        images = emptyList(),
    )
}
