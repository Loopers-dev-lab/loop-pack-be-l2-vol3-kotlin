package com.loopers.application.like

import com.loopers.domain.like.LikeService
import com.loopers.domain.like.LikeModel
import com.loopers.domain.product.ProductService
import com.loopers.domain.user.UserService
import com.loopers.domain.user.BirthDate
import com.loopers.domain.user.Email
import com.loopers.domain.user.LoginId
import com.loopers.domain.user.UserModel
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest

@ExtendWith(MockitoExtension::class)
class LikeFacadeTest {

    @Mock
    private lateinit var likeService: LikeService

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var productService: ProductService

    @InjectMocks
    private lateinit var likeFacade: LikeFacade

    private fun createTestUserModel(loginId: String = "testuser"): UserModel =
        UserModel(
            loginId = LoginId(loginId),
            encryptedPassword = "encrypted",
            name = com.loopers.domain.user.Name("홍길동"),
            birthDate = BirthDate("1990-01-01"),
            email = Email("test@example.com"),
        )

    private fun createTestLikeModel(userId: Long = 1L, productId: Long = 1L): LikeModel =
        LikeModel(userId = userId, productId = productId)

    @Nested
    inner class Like {

        @Test
        fun `like() 호출 시 LikeInfo를 반환한다`() {
            val userModel = createTestUserModel()
            val likeModel = createTestLikeModel()
            whenever(userService.getUserByLoginId(any())).thenReturn(userModel)
            whenever(likeService.like(any(), any())).thenReturn(Pair(true, likeModel))

            val result = likeFacade.like("testuser", 1L)

            assertThat(result).isInstanceOf(LikeInfo::class.java)
            assertThat(result.userId).isEqualTo(likeModel.userId)
            assertThat(result.productId).isEqualTo(likeModel.productId)
        }
    }

    @Nested
    inner class Unlike {

        @Test
        fun `unlike() 호출 시 정상적으로 완료된다`() {
            val userModel = createTestUserModel()
            whenever(userService.getUserByLoginId(any())).thenReturn(userModel)
            whenever(likeService.unlike(any(), any())).thenReturn(false)

            val result = likeFacade.unlike("testuser", 1L)

            assertThat(result).isEqualTo(Unit)
        }
    }

    @Nested
    inner class GetLikedProducts {

        @Test
        fun `본인이 좋아요한 상품 목록 조회 시 LikeInfo 목록을 반환한다`() {
            // given
            val userModel = createTestUserModel()  // user.id = 0 (기본값)
            val likeModels = listOf(
                createTestLikeModel(userId = 0L, productId = 1L),
                createTestLikeModel(userId = 0L, productId = 2L),
            )
            val pageable = PageRequest.of(0, 10)

            whenever(userService.getUserByLoginId(any())).thenReturn(userModel)
            whenever(likeService.getLikedProducts(0L, pageable)).thenReturn(likeModels)

            // when
            val result = likeFacade.getLikedProducts("testuser", 0L, pageable)

            // then
            assertThat(result).hasSize(2)
            assertThat(result[0]).isInstanceOf(LikeInfo::class.java)
            assertThat(result[0].productId).isEqualTo(1L)
            assertThat(result[1].productId).isEqualTo(2L)
        }

        @Test
        fun `타인의 좋아요 목록 조회 시 FORBIDDEN 예외가 발생한다`() {
            // given
            val userModel = createTestUserModel()  // user.id = 0 (기본값)

            whenever(userService.getUserByLoginId(any())).thenReturn(userModel)

            // when
            val exception = assertThrows<CoreException> {
                likeFacade.getLikedProducts("testuser", 999L, PageRequest.of(0, 10))
            }

            // then
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }
    }
}
