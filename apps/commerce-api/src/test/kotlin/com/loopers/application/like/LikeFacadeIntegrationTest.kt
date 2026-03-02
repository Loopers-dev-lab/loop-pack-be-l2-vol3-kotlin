package com.loopers.application.like

import com.loopers.application.product.ProductFacade
import com.loopers.domain.brand.BrandService
import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Email
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.Name
import com.loopers.domain.user.PasswordEncryptor
import com.loopers.domain.user.UserModel
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest

@SpringBootTest
class LikeFacadeIntegrationTest @Autowired constructor(
    private val likeFacade: LikeFacade,
    private val brandService: BrandService,
    private val productFacade: ProductFacade,
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncryptor: PasswordEncryptor,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private val testPassword = "Password123!"

    private fun createUser(loginId: String = "testuser") = userJpaRepository.save(
        UserModel(
            loginId = LoginId(loginId),
            encryptedPassword = passwordEncryptor.encrypt(testPassword),
            name = Name("홍길동"),
            birthDate = BirthDate("1990-01-01"),
            email = Email("$loginId@example.com"),
        )
    )

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

    private fun createProduct(brandId: Long) = productFacade.createProduct(
        brandId = brandId,
        name = "Air Max",
        imageUrl = "image.png",
        description = "설명",
        price = 50_000L,
        quantity = 100L,
    )

    @Nested
    inner class Like {

        @Test
        fun `좋아요 등록 시 LikeInfo를 반환한다`() {
            val user = createUser()
            val brand = createBrand()
            val product = createProduct(brand.id)

            val result = likeFacade.like("testuser", product.id)

            assertThat(result.userId).isEqualTo(user.id)
            assertThat(result.productId).isEqualTo(product.id)
            assertThat(result.id).isGreaterThan(0)
        }
    }

    @Nested
    inner class Unlike {

        @Test
        fun `좋아요 취소 시 Unit을 반환하고 예외가 발생하지 않는다`() {
            createUser()
            val brand = createBrand()
            val product = createProduct(brand.id)
            likeFacade.like("testuser", product.id)

            likeFacade.unlike("testuser", product.id)

            val likedProducts = likeFacade.getLikedProducts("testuser", userJpaRepository.findByLoginId(LoginId("testuser"))!!.id, PageRequest.of(0, 10))
            assertThat(likedProducts).isEmpty()
        }
    }

    @Nested
    inner class GetLikedProducts {

        @Test
        fun `본인 좋아요 목록 조회 시 LikeInfo 목록을 반환한다`() {
            val user = createUser()
            val brand = createBrand()
            val product1 = createProduct(brand.id)
            val product2 = productFacade.createProduct(
                brandId = brand.id,
                name = "Air Force",
                imageUrl = "af.png",
                description = "설명",
                price = 30_000L,
                quantity = 100L,
            )

            likeFacade.like("testuser", product1.id)
            likeFacade.like("testuser", product2.id)

            val result = likeFacade.getLikedProducts("testuser", user.id, PageRequest.of(0, 10))

            assertThat(result).hasSize(2)
            assertThat(result.map { it.userId }).containsOnly(user.id)
        }

        @Test
        fun `타인의 좋아요 목록 조회 시 FORBIDDEN 예외가 발생한다`() {
            createUser("testuser")
            val otherUser = createUser("otheruser")

            val exception = assertThrows<CoreException> {
                likeFacade.getLikedProducts("testuser", otherUser.id, PageRequest.of(0, 10))
            }

            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }
    }
}
