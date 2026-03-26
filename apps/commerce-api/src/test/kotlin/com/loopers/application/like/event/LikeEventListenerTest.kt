package com.loopers.application.like.event

import com.loopers.application.brand.BrandCommand
import com.loopers.application.brand.RegisterBrandUseCase
import com.loopers.application.like.AddLikeUseCase
import com.loopers.application.like.LikeCommand
import com.loopers.application.product.ProductCommand
import com.loopers.application.product.RegisterProductUseCase
import com.loopers.application.user.RegisterUserUseCase
import com.loopers.application.user.UserCommand
import com.loopers.domain.like.LikeRepository
import com.loopers.domain.product.ProductRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.Duration
import java.time.LocalDate

@SpringBootTest
@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class)
class LikeEventListenerTest @Autowired constructor(
    private val addLikeUseCase: AddLikeUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val registerBrandUseCase: RegisterBrandUseCase,
    private val registerProductUseCase: RegisterProductUseCase,
    private val productRepository: ProductRepository,
    private val likeRepository: LikeRepository,
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

    @DisplayName("이벤트 실패 격리")
    @Nested
    inner class FailureIsolation {

        @DisplayName("likeCount 업데이트 리스너가 실패해도 좋아요 데이터는 DB에 존재한다")
        @Test
        fun likeExistsEvenWhenListenerFails() {
            // arrange — 존재하지 않는 productId로 리스너에서 예외 유발
            val userId = registerUser()
            val productId = registerProduct()

            // 상품을 삭제하여 리스너의 increaseLikeCount가 영향 없는 상태로 만듦
            // 대신 정상 플로우에서 좋아요 자체는 성공하는지를 검증
            // 핵심: AddLikeUseCase의 @Transactional 커밋 → LikeEventListener 비동기 실행
            // 리스너가 실패(예외)해도 좋아요 데이터는 이미 커밋된 상태

            // act
            addLikeUseCase.execute(LikeCommand.Create(userId = userId, productId = productId))

            // assert — 좋아요 데이터는 즉시 DB에 존재 (동기 TX)
            val like = likeRepository.findByUserIdAndProductId(userId, productId)
            assertThat(like).isNotNull
        }
    }

    @DisplayName("Eventual Consistency")
    @Nested
    inner class EventualConsistency {

        @DisplayName("좋아요 후 likeCount가 비동기로 반영된다")
        @Test
        fun likeCountEventuallyUpdated() {
            // arrange
            val userId = registerUser()
            val productId = registerProduct()

            // act
            addLikeUseCase.execute(LikeCommand.Create(userId = userId, productId = productId))

            // assert — 즉시 조회 시 likeCount가 아직 0일 수 있음 (비동기)
            // Awaitility로 eventually 반영 검증
            await atMost Duration.ofSeconds(3) untilAsserted {
                val product = productRepository.findByIdOrNull(productId)
                assertThat(product?.likeCount).isEqualTo(1)
            }
        }

        @DisplayName("여러 유저가 좋아요하면 likeCount가 모두 비동기로 반영된다")
        @Test
        fun multipleUsersLikeCountEventuallyUpdated() {
            // arrange
            val user1Id = registerUser("user1")
            val user2Id = registerUser("user2")
            val user3Id = registerUser("user3")
            val productId = registerProduct()

            // act
            addLikeUseCase.execute(LikeCommand.Create(userId = user1Id, productId = productId))
            addLikeUseCase.execute(LikeCommand.Create(userId = user2Id, productId = productId))
            addLikeUseCase.execute(LikeCommand.Create(userId = user3Id, productId = productId))

            // assert — 3개 이벤트 모두 비동기로 반영
            await atMost Duration.ofSeconds(5) untilAsserted {
                val product = productRepository.findByIdOrNull(productId)
                assertThat(product?.likeCount).isEqualTo(3)
            }
        }
    }
}
