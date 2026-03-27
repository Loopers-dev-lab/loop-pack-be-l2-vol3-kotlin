package com.loopers.application.outbox

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.like.AddLikeUseCase
import com.loopers.application.like.CancelLikeUseCase
import com.loopers.application.like.LikeCommand
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.application.user.UserCommand
import com.loopers.domain.outbox.OutboxEventRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.testcontainers.KafkaTestContainersConfig
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.LocalDate

@SpringBootTest
@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class, KafkaTestContainersConfig::class)
class OutboxEventIntegrationTest @Autowired constructor(
    private val addLikeUseCase: AddLikeUseCase,
    private val cancelLikeUseCase: CancelLikeUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val registerProductUseCase: RegisterProductUseCase,
    private val outboxEventRepository: OutboxEventRepository,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    private fun registerUser(loginId: String = "testuser"): Long {
        registerUserUseCase.execute(
            UserCommand.Register(
                loginId = loginId,
                rawPassword = "Test123!",
                name = "홍길동",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "test@example.com",
            ),
        )
        return userJpaRepository.findByLoginId(loginId)!!.id
    }

    private fun registerProduct(name: String = "테스트 상품"): Long {
        val brandId = registerBrandUseCase.execute(BrandCommand.Register(name = "나이키")).id
        return registerProductUseCase.execute(
            ProductCommand.Register(
                brandId = brandId,
                name = name,
                description = "상품 설명",
                price = 10000,
                stock = 100,
                imageUrl = "https://example.com/image.jpg",
            ),
        ).id
    }

    @DisplayName("Outbox 기록 통합 테스트")
    @Nested
    inner class OutboxWrite {

        @DisplayName("좋아요 등록 시 LIKE_CREATED 이벤트가 outbox에 기록된다")
        @Test
        fun likeCreatedOutbox() {
            // arrange
            val userId = registerUser()
            val productId = registerProduct()

            // act
            addLikeUseCase.execute(LikeCommand.Create(userId = userId, productId = productId))

            // assert
            val events = outboxEventRepository.findUnpublished(100)
            val likeEvent = events.find { it.eventType == "LIKE_CREATED" }

            assertAll(
                { assertThat(likeEvent).isNotNull },
                { assertThat(likeEvent?.topic).isEqualTo("catalog-events") },
                { assertThat(likeEvent?.partitionKey).isEqualTo(productId.toString()) },
                { assertThat(likeEvent?.published).isFalse() },
                { assertThat(likeEvent?.payload).contains("\"productId\"") },
            )
        }

        @DisplayName("좋아요 취소 시 LIKE_CANCELLED 이벤트가 outbox에 기록된다")
        @Test
        fun likeCancelledOutbox() {
            // arrange
            val userId = registerUser()
            val productId = registerProduct()
            addLikeUseCase.execute(LikeCommand.Create(userId = userId, productId = productId))

            // act
            cancelLikeUseCase.execute(userId, productId)

            // assert
            val events = outboxEventRepository.findUnpublished(100)
            val cancelEvent = events.find { it.eventType == "LIKE_CANCELLED" }

            assertAll(
                { assertThat(cancelEvent).isNotNull },
                { assertThat(cancelEvent?.topic).isEqualTo("catalog-events") },
                { assertThat(cancelEvent?.partitionKey).isEqualTo(productId.toString()) },
            )
        }
    }
}
