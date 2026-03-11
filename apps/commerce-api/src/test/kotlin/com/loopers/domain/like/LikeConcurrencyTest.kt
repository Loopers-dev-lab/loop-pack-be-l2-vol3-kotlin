package com.loopers.domain.like

import com.loopers.infrastructure.like.LikeJpaRepository
import com.loopers.infrastructure.product.ProductEntity
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class LikeConcurrencyTest @Autowired constructor(
    private val likeRegister: LikeRegister,
    private val likeJpaRepository: LikeJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @Test
    fun `동시에_여러_회원이_같은_상품에_좋아요를_해도_정확한_좋아요_수가_유지된다`() {
        // arrange
        val product = productJpaRepository.save(
            ProductEntity(
                brandId = 1L,
                name = "테스트상품",
                price = 10000L,
                description = "테스트 상품 설명",
                stock = 100,
                status = "SELLING",
            ),
        )

        val threadCount = 20
        val executorService = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)

        // act — 각 스레드가 서로 다른 memberId로 좋아요
        repeat(threadCount) { index ->
            executorService.submit {
                try {
                    likeRegister.register(memberId = (index + 1).toLong(), productId = product.id!!)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failCount.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executorService.shutdown()

        // assert
        val likeCount = likeJpaRepository.countByProductId(product.id!!)
        assertThat(successCount.get()).isEqualTo(threadCount)
        assertThat(likeCount).isEqualTo(threadCount.toLong())
    }
}
