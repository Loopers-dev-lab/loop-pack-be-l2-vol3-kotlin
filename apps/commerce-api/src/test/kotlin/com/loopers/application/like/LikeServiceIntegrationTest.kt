package com.loopers.application.like

import com.loopers.domain.brand.Brand
import com.loopers.domain.like.Like
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
import org.junit.jupiter.api.BeforeEach
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
 * LikeService 통합 테스트
 * - 좋아요 등록(addLike), 취소(cancelLike), 목록 조회(getUserLikes) 검증
 * - 실제 DB(TestContainers)와 연동
 */
@SpringBootTest
class LikeServiceIntegrationTest @Autowired constructor(
    private val likeService: LikeService,
    private val likeJpaRepository: LikeJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val brandJpaRepository: BrandJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    private lateinit var savedUser: User
    private lateinit var savedProduct: Product
    private lateinit var savedProduct2: Product

    @BeforeEach
    fun setUp() {
        savedUser = userJpaRepository.save(
            User(
                loginId = "testuser1",
                password = "encodedPassword",
                name = "테스트유저",
                birthDate = LocalDate.of(1990, 1, 15),
                email = "test@example.com",
            ),
        )
        val brand = brandJpaRepository.save(Brand(name = "나이키", description = "스포츠 브랜드"))
        savedProduct = productJpaRepository.save(
            Product(
                brandId = brand.id,
                name = "에어맥스 90",
                price = BigDecimal("129000"),
                stock = 100,
                description = null,
                imageUrl = null,
            ),
        )
        savedProduct2 = productJpaRepository.save(
            Product(
                brandId = brand.id,
                name = "울트라부스트",
                price = BigDecimal("199000"),
                stock = 50,
                description = null,
                imageUrl = null,
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("좋아요를 등록할 때,")
    @Nested
    inner class AddLike {

        @DisplayName("정상적인 요청이면, DB에 좋아요가 저장된다.")
        @Test
        fun savesLikeToDatabase_whenValidRequest() {
            // act
            val result = likeService.addLike(savedUser.id, savedProduct.id)

            // assert
            val saved = likeJpaRepository.findByUserIdAndProductIdAndDeletedAtIsNull(savedUser.id, savedProduct.id)!!
            assertAll(
                { assertThat(saved.userId).isEqualTo(savedUser.id) },
                { assertThat(saved.productId).isEqualTo(savedProduct.id) },
                { assertThat(result.productId).isEqualTo(savedProduct.id) },
            )
        }

        @DisplayName("이미 좋아요한 상품에 다시 좋아요하면, 기존 좋아요가 유지된다.")
        @Test
        fun returnsExistingLike_whenAlreadyLiked() {
            // arrange
            val first = likeService.addLike(savedUser.id, savedProduct.id)

            // act
            val second = likeService.addLike(savedUser.id, savedProduct.id)

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
            val like = likeJpaRepository.save(Like(userId = savedUser.id, productId = savedProduct.id))
            like.delete()
            likeJpaRepository.save(like)

            // act
            val result = likeService.addLike(savedUser.id, savedProduct.id)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(like.id) },
                { assertThat(result.productId).isEqualTo(savedProduct.id) },
                { assertThat(likeJpaRepository.findAll()).hasSize(1) },
            )
        }

        @DisplayName("존재하지 않는 상품이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenProductNotExists() {
            // act & assert
            val exception = assertThrows<CoreException> {
                likeService.addLike(savedUser.id, 999L)
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("삭제된 상품에도 좋아요를 등록할 수 있다.")
        @Test
        fun addsLike_whenProductIsSoftDeleted() {
            // arrange
            savedProduct.delete()
            productJpaRepository.save(savedProduct)

            // act
            val result = likeService.addLike(savedUser.id, savedProduct.id)

            // assert
            assertThat(result.productId).isEqualTo(savedProduct.id)
        }
    }

    @DisplayName("좋아요를 취소할 때,")
    @Nested
    inner class CancelLike {

        @DisplayName("활성 좋아요가 있으면, Soft Delete된다.")
        @Test
        fun softDeletesLike_whenActiveLikeExists() {
            // arrange
            val like = likeJpaRepository.save(Like(userId = savedUser.id, productId = savedProduct.id))

            // act
            likeService.cancelLike(savedUser.id, savedProduct.id)

            // assert
            val deleted = likeJpaRepository.findById(like.id).get()
            assertAll(
                { assertThat(deleted.isDeleted()).isTrue() },
                { assertThat(likeJpaRepository.findByUserIdAndProductIdAndDeletedAtIsNull(savedUser.id, savedProduct.id)).isNull() },
            )
        }

        @DisplayName("좋아요가 없으면, 아무 일도 일어나지 않는다.")
        @Test
        fun doesNothing_whenNoLikeExists() {
            // act
            likeService.cancelLike(savedUser.id, savedProduct.id)

            // assert
            assertThat(likeJpaRepository.findAll()).isEmpty()
        }

        @DisplayName("이미 Soft Delete된 좋아요는, 다시 취소해도 아무 일도 일어나지 않는다.")
        @Test
        fun doesNothing_whenAlreadySoftDeleted() {
            // arrange
            val like = likeJpaRepository.save(Like(userId = savedUser.id, productId = savedProduct.id))
            like.delete()
            likeJpaRepository.save(like)

            // act
            likeService.cancelLike(savedUser.id, savedProduct.id)

            // assert
            val result = likeJpaRepository.findById(like.id).get()
            assertThat(result.isDeleted()).isTrue()
        }
    }

    @DisplayName("좋아요 목록을 조회할 때,")
    @Nested
    inner class GetUserLikes {

        @DisplayName("활성 좋아요만 반환된다.")
        @Test
        fun returnsOnlyActiveLikes() {
            // arrange
            likeJpaRepository.save(Like(userId = savedUser.id, productId = savedProduct.id))
            val deletedLike = likeJpaRepository.save(Like(userId = savedUser.id, productId = savedProduct2.id))
            deletedLike.delete()
            likeJpaRepository.save(deletedLike)

            // act
            val result = likeService.getUserLikes(savedUser.id)

            // assert
            assertAll(
                { assertThat(result).hasSize(1) },
                { assertThat(result[0].productId).isEqualTo(savedProduct.id) },
            )
        }

        @DisplayName("좋아요가 없으면, 빈 목록이 반환된다.")
        @Test
        fun returnsEmptyList_whenNoLikes() {
            // act
            val result = likeService.getUserLikes(savedUser.id)

            // assert
            assertThat(result).isEmpty()
        }

        @DisplayName("다른 유저의 좋아요는 포함되지 않는다.")
        @Test
        fun excludesOtherUserLikes() {
            // arrange
            val otherUser = userJpaRepository.save(
                User(
                    loginId = "otheruser",
                    password = "encodedPassword",
                    name = "다른유저",
                    birthDate = LocalDate.of(1995, 5, 20),
                    email = "other@example.com",
                ),
            )
            likeJpaRepository.save(Like(userId = savedUser.id, productId = savedProduct.id))
            likeJpaRepository.save(Like(userId = otherUser.id, productId = savedProduct2.id))

            // act
            val result = likeService.getUserLikes(savedUser.id)

            // assert
            assertAll(
                { assertThat(result).hasSize(1) },
                { assertThat(result[0].productId).isEqualTo(savedProduct.id) },
            )
        }
    }
}
