package com.loopers.application.like

import com.loopers.domain.like.LikeModel
import com.loopers.domain.like.LikeRepository
import com.loopers.domain.product.Description
import com.loopers.domain.product.ImageUrl
import com.loopers.domain.product.Name
import com.loopers.domain.product.Price
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.justRun
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
class LikeServiceTest {

    private val likeRepository: LikeRepository = mockk()
    private val productRepository: ProductRepository = mockk()

    private lateinit var likeService: LikeService

    @BeforeEach
    fun setUp() {
        likeService = LikeService(likeRepository, productRepository)
    }

    @Nested
    inner class Like {

        @Test
        fun `좋아요 성공`() {
            // given
            val product = ProductModel(1L, Name("뉴발란스 991"), ImageUrl("test.png"), Description("신발"), Price(299_000L))
            val like = LikeModel(userId = 1L, productId = 1L)
            every { productRepository.findById(1L) } returns product
            every { likeRepository.findByUserIdAndProductId(1L, 1L) } returns null
            every { likeRepository.save(any()) } returns like

            // when
            val result = likeService.like(userId = 1L, productId = 1L)

            // then
            assertNotNull(result)
            assertEquals(1L, product.likeCount.value)
            verify(exactly = 1) { likeRepository.save(any()) }
        }

        @Test
        fun `이미 좋아요한 경우 기존 좋아요를 그대로 반환`() {
            // given - 첫 번째 좋아요로 likeCount가 이미 1인 상태
            val product = ProductModel(1L, Name("뉴발란스 991"), ImageUrl("test.png"), Description("신발"), Price(299_000L))
            val existingLike = LikeModel(userId = 1L, productId = 1L)
            every { productRepository.findById(1L) } returns product
            every { likeRepository.findByUserIdAndProductId(1L, 1L) } returns null
            every { likeRepository.save(any()) } returns existingLike
            likeService.like(userId = 1L, productId = 1L)

            // 두 번째 좋아요 시도
            every { likeRepository.findByUserIdAndProductId(1L, 1L) } returns existingLike

            // when
            val result = likeService.like(userId = 1L, productId = 1L)

            // then
            assertEquals(existingLike, result)
            assertEquals(1L, product.likeCount.value)
            verify(exactly = 1) { likeRepository.save(any()) }
        }

        @Test
        fun `존재하지 않는 상품에 좋아요 시 예외 반환`() {
            // given
            every { productRepository.findById(99L) } returns null

            // when
            val exception = assertThrows<CoreException> {
                likeService.like(userId = 1L, productId = 99L)
            }

            // then
            assertEquals(ErrorType.NOT_FOUND, exception.errorType)
            verify(exactly = 0) { likeRepository.save(any()) }
        }
    }

    @Nested
    inner class Unlike {

        @Test
        fun `좋아요 취소 성공`() {
            // given
            val product = ProductModel(1L, Name("뉴발란스 991"), ImageUrl("test.png"), Description("신발"), Price(299_000L))
            product.increaseLikeCount()
            val like = LikeModel(userId = 1L, productId = 1L)
            every { productRepository.findById(1L) } returns product
            every { likeRepository.findByUserIdAndProductId(1L, 1L) } returns like
            justRun { likeRepository.delete(like) }

            // when
            likeService.unlike(userId = 1L, productId = 1L)

            // then
            assertEquals(0L, product.likeCount.value)
            verify(exactly = 1) { likeRepository.delete(like) }
        }

        @Test
        fun `좋아요가 없는 경우 무시`() {
            // given
            val product = ProductModel(1L, Name("뉴발란스 991"), ImageUrl("test.png"), Description("신발"), Price(299_000L))
            every { productRepository.findById(1L) } returns product
            every { likeRepository.findByUserIdAndProductId(1L, 1L) } returns null

            // when
            likeService.unlike(userId = 1L, productId = 1L)

            // then
            assertEquals(0L, product.likeCount.value)
            verify(exactly = 0) { likeRepository.delete(any()) }
        }

        @Test
        fun `존재하지 않는 상품에 좋아요 취소 시 예외 반환`() {
            // given
            every { productRepository.findById(99L) } returns null

            // when
            val exception = assertThrows<CoreException> {
                likeService.unlike(userId = 1L, productId = 99L)
            }

            // then
            assertEquals(ErrorType.NOT_FOUND, exception.errorType)
            verify(exactly = 0) { likeRepository.delete(any()) }
        }
    }
}
