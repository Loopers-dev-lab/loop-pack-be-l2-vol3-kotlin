package com.loopers.domain.like

import com.loopers.domain.like.LikeModel
import com.loopers.domain.like.LikeRepository
import com.loopers.domain.product.Description
import com.loopers.domain.product.ImageUrl
import com.loopers.domain.product.Name
import com.loopers.domain.product.Price
import com.loopers.domain.product.ProductModel
import io.mockk.every
import io.mockk.justRun
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockKExtension::class)
class LikeServiceTest {

    private val likeRepository: LikeRepository = mockk()

    private lateinit var likeService: LikeService

    @BeforeEach
    fun setUp() {
        likeService = LikeService(likeRepository)
    }

    private fun createProduct() =
        ProductModel(brandId = 1L, name = Name("뉴발란스 991"), imageUrl = ImageUrl("test.png"), description = Description("신발"), price = Price(299_000L))

    @Nested
    inner class Like {

        @Test
        fun `좋아요 성공`() {
            // given
            val product = createProduct()
            val like = LikeModel(userId = 1L, productId = product.id)
            every { likeRepository.findByUserIdAndProductId(1L, product.id) } returns null
            every { likeRepository.save(any()) } returns like

            // when
            val result = likeService.like(userId = 1L, product = product)

            // then
            assertNotNull(result)
            assertEquals(1L, product.likeCount.value)
            verify(exactly = 1) { likeRepository.save(any()) }
        }

        @Test
        fun `이미 좋아요한 경우 기존 좋아요를 그대로 반환`() {
            // given - 첫 번째 좋아요로 likeCount가 이미 1인 상태
            val product = createProduct()
            val existingLike = LikeModel(userId = 1L, productId = product.id)
            every { likeRepository.findByUserIdAndProductId(1L, product.id) } returns null
            every { likeRepository.save(any()) } returns existingLike
            likeService.like(userId = 1L, product = product)

            // 두 번째 좋아요 시도
            every { likeRepository.findByUserIdAndProductId(1L, product.id) } returns existingLike

            // when
            val result = likeService.like(userId = 1L, product = product)

            // then
            assertEquals(existingLike, result)
            assertEquals(1L, product.likeCount.value)
            verify(exactly = 1) { likeRepository.save(any()) }
        }
    }

    @Nested
    inner class Unlike {

        @Test
        fun `좋아요 취소 성공`() {
            // given
            val product = createProduct()
            product.increaseLikeCount()
            val like = LikeModel(userId = 1L, productId = product.id)
            every { likeRepository.findByUserIdAndProductId(1L, product.id) } returns like
            justRun { likeRepository.delete(like) }

            // when
            likeService.unlike(userId = 1L, product = product)

            // then
            assertEquals(0L, product.likeCount.value)
            verify(exactly = 1) { likeRepository.delete(like) }
        }

        @Test
        fun `좋아요가 없는 경우 무시`() {
            // given
            val product = createProduct()
            every { likeRepository.findByUserIdAndProductId(1L, product.id) } returns null

            // when
            likeService.unlike(userId = 1L, product = product)

            // then
            assertEquals(0L, product.likeCount.value)
            verify(exactly = 0) { likeRepository.delete(any()) }
        }
    }
}
