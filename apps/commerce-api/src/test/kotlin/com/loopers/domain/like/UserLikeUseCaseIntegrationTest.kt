package com.loopers.domain.like

import com.loopers.application.catalog.AdminRegisterProductUseCase
import com.loopers.application.catalog.RegisterProductCriteria
import com.loopers.domain.catalog.BrandService
import com.loopers.domain.catalog.RegisterBrandCommand
import com.loopers.application.like.GetLikedProductsCriteria
import com.loopers.application.like.LikeProductCriteria
import com.loopers.application.like.UnlikeProductCriteria
import com.loopers.application.like.UserGetLikedProductsUseCase
import com.loopers.application.like.UserLikeProductUseCase
import com.loopers.application.like.UserUnlikeProductUseCase
import com.loopers.domain.user.RegisterCommand
import com.loopers.domain.user.UserService
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class UserLikeUseCaseIntegrationTest @Autowired constructor(
    private val userLikeProductUseCase: UserLikeProductUseCase,
    private val userUnlikeProductUseCase: UserUnlikeProductUseCase,
    private val userGetLikedProductsUseCase: UserGetLikedProductsUseCase,
    private val brandService: BrandService,
    private val adminRegisterProductUseCase: AdminRegisterProductUseCase,
    private val userService: UserService,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val DEFAULT_USERNAME = "testuser"
        private const val DEFAULT_PASSWORD = "password1234!"
        private const val DEFAULT_NAME = "테스트유저"
        private const val DEFAULT_EMAIL = "test@loopers.com"
        private val DEFAULT_BIRTH_DATE = ZonedDateTime.of(1995, 5, 29, 0, 0, 0, 0, ZoneId.of("Asia/Seoul"))

        private const val DEFAULT_BRAND_NAME = "나이키"
        private const val DEFAULT_PRODUCT_NAME = "에어맥스 90"
        private const val DEFAULT_QUANTITY = 100
        private val DEFAULT_PRICE = BigDecimal("129000")
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun registerUser(username: String = DEFAULT_USERNAME): Long {
        val user = userService.register(
            RegisterCommand(
                username = username,
                password = DEFAULT_PASSWORD,
                name = DEFAULT_NAME,
                email = DEFAULT_EMAIL,
                birthDate = DEFAULT_BIRTH_DATE,
            ),
        )
        return user.id
    }

    private fun registerBrand(name: String = DEFAULT_BRAND_NAME): Long {
        return brandService.register(RegisterBrandCommand(name = name)).id
    }

    private fun registerProduct(
        brandId: Long,
        name: String = DEFAULT_PRODUCT_NAME,
        price: BigDecimal = DEFAULT_PRICE,
    ): Long {
        return adminRegisterProductUseCase.execute(
            RegisterProductCriteria(
                brandId = brandId,
                name = name,
                quantity = DEFAULT_QUANTITY,
                price = price,
            ),
        ).id
    }

    @DisplayName("좋아요 등록")
    @Nested
    inner class LikeProduct {

        @DisplayName("정상적으로 좋아요를 등록한다.")
        @Test
        fun likesProductSuccessfully() {
            // arrange
            registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId)
            val criteria = LikeProductCriteria(loginId = DEFAULT_USERNAME, productId = productId)

            // act
            userLikeProductUseCase.execute(criteria)

            // assert
            val result = userGetLikedProductsUseCase.execute(
                GetLikedProductsCriteria(loginId = DEFAULT_USERNAME, userId = 1L, page = 0, size = 10),
            )
            assertThat(result.content).hasSize(1)
        }

        @DisplayName("존재하지 않는 상품이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenProductDoesNotExist() {
            // arrange
            registerUser()
            val criteria = LikeProductCriteria(loginId = DEFAULT_USERNAME, productId = 999L)

            // act
            val result = assertThrows<CoreException> {
                userLikeProductUseCase.execute(criteria)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("이미 좋아요한 상품이면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflictExceptionWhenAlreadyLiked() {
            // arrange
            registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId)
            userLikeProductUseCase.execute(LikeProductCriteria(loginId = DEFAULT_USERNAME, productId = productId))
            val criteria = LikeProductCriteria(loginId = DEFAULT_USERNAME, productId = productId)

            // act
            val result = assertThrows<CoreException> {
                userLikeProductUseCase.execute(criteria)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @DisplayName("좋아요 취소")
    @Nested
    inner class UnlikeProduct {

        @DisplayName("정상적으로 좋아요를 취소한다.")
        @Test
        fun unlikesProductSuccessfully() {
            // arrange
            registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId)
            userLikeProductUseCase.execute(LikeProductCriteria(loginId = DEFAULT_USERNAME, productId = productId))
            val criteria = UnlikeProductCriteria(loginId = DEFAULT_USERNAME, productId = productId)

            // act
            userUnlikeProductUseCase.execute(criteria)

            // assert
            val result = userGetLikedProductsUseCase.execute(
                GetLikedProductsCriteria(loginId = DEFAULT_USERNAME, userId = 1L, page = 0, size = 10),
            )
            assertThat(result.content).isEmpty()
        }

        @DisplayName("좋아요하지 않은 상품이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundExceptionWhenLikeDoesNotExist() {
            // arrange
            registerUser()
            val criteria = UnlikeProductCriteria(loginId = DEFAULT_USERNAME, productId = 999L)

            // act
            val result = assertThrows<CoreException> {
                userUnlikeProductUseCase.execute(criteria)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("좋아요 목록 조회")
    @Nested
    inner class GetLikedProducts {

        @DisplayName("좋아요한 상품이 없으면, 빈 목록을 반환한다.")
        @Test
        fun returnsEmptyListWhenNoLikes() {
            // arrange
            val userId = registerUser()
            val criteria = GetLikedProductsCriteria(
                loginId = DEFAULT_USERNAME,
                userId = userId,
                page = 0,
                size = 10,
            )

            // act
            val result = userGetLikedProductsUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).isEmpty() },
                { assertThat(result.hasNext).isFalse() },
            )
        }

        @DisplayName("좋아요한 상품 목록을 정상적으로 조회한다.")
        @Test
        fun returnsLikedProductsWithProductInfo() {
            // arrange
            val userId = registerUser()
            val brandId = registerBrand()
            val productId = registerProduct(brandId)
            userLikeProductUseCase.execute(LikeProductCriteria(loginId = DEFAULT_USERNAME, productId = productId))
            val criteria = GetLikedProductsCriteria(
                loginId = DEFAULT_USERNAME,
                userId = userId,
                page = 0,
                size = 10,
            )

            // act
            val result = userGetLikedProductsUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(1) },
                { assertThat(result.content[0].name).isEqualTo(DEFAULT_PRODUCT_NAME) },
                { assertThat(result.content[0].brandName).isEqualTo(DEFAULT_BRAND_NAME) },
                { assertThat(result.content[0].price).isEqualByComparingTo(DEFAULT_PRICE) },
            )
        }

        @DisplayName("페이지 크기보다 좋아요가 많으면, hasNext가 true이다.")
        @Test
        fun returnsHasNextTrueWhenMoreLikesExist() {
            // arrange
            val userId = registerUser()
            val brandId = registerBrand()
            val product1 = registerProduct(brandId, name = "상품1")
            val product2 = registerProduct(brandId, name = "상품2")
            val product3 = registerProduct(brandId, name = "상품3")
            userLikeProductUseCase.execute(LikeProductCriteria(loginId = DEFAULT_USERNAME, productId = product1))
            userLikeProductUseCase.execute(LikeProductCriteria(loginId = DEFAULT_USERNAME, productId = product2))
            userLikeProductUseCase.execute(LikeProductCriteria(loginId = DEFAULT_USERNAME, productId = product3))
            val criteria = GetLikedProductsCriteria(
                loginId = DEFAULT_USERNAME,
                userId = userId,
                page = 0,
                size = 2,
            )

            // act
            val result = userGetLikedProductsUseCase.execute(criteria)

            // assert
            assertAll(
                { assertThat(result.content).hasSize(2) },
                { assertThat(result.hasNext).isTrue() },
            )
        }

        @DisplayName("다른 사용자의 좋아요 목록을 조회하면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorizedExceptionWhenUserIdMismatch() {
            // arrange
            registerUser()
            val criteria = GetLikedProductsCriteria(
                loginId = DEFAULT_USERNAME,
                userId = 999L,
                page = 0,
                size = 10,
            )

            // act
            val result = assertThrows<CoreException> {
                userGetLikedProductsUseCase.execute(criteria)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }
    }
}
