package com.loopers.domain.like

import com.loopers.domain.like.LikeModel
import com.loopers.domain.like.LikeRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class LikeServiceTest {

    private val likeRepository: LikeRepository = mockk()

    private lateinit var likeService: LikeService

    @BeforeEach
    fun setUp() {
        likeService = LikeService(likeRepository)
    }

    @Nested
    inner class Like {

        @Test
        fun `좋아요 성공`() {
            // given
            val like = LikeModel(userId = 1L, productId = 1L)
            every { likeRepository.findByUserIdAndProductId(1L, 1L) } returns null
            every { likeRepository.save(any()) } returns like

            // when
            val (isNew, result) = likeService.like(userId = 1L, productId = 1L)

            // then
            assertTrue(isNew)
            assertNotNull(result)
            verify(exactly = 1) { likeRepository.save(any()) }
        }

        @Test
        fun `이미 좋아요한 경우 기존 좋아요를 그대로 반환`() {
            // given
            val existingLike = LikeModel(userId = 1L, productId = 1L)
            every { likeRepository.findByUserIdAndProductId(1L, 1L) } returns existingLike

            // when
            val (isNew, result) = likeService.like(userId = 1L, productId = 1L)

            // then
            assertFalse(isNew)
            assertEquals(existingLike, result)
            verify(exactly = 0) { likeRepository.save(any()) }
        }
    }

    @Nested
    inner class Unlike {

        @Test
        fun `좋아요 취소 성공`() {
            // given
            every { likeRepository.deleteByUserIdAndProductId(1L, 1L) } returns 1

            // when
            val deleted = likeService.unlike(userId = 1L, productId = 1L)

            // then
            assertTrue(deleted)
            verify(exactly = 1) { likeRepository.deleteByUserIdAndProductId(1L, 1L) }
        }

        @Test
        fun `좋아요가 없는 경우 무시`() {
            // given
            every { likeRepository.deleteByUserIdAndProductId(1L, 1L) } returns 0

            // when
            val deleted = likeService.unlike(userId = 1L, productId = 1L)

            // then
            assertFalse(deleted)
        }
    }
}
