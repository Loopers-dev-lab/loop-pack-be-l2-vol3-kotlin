package com.loopers.application.orderqueue

import com.loopers.domain.orderqueue.QueueStatus
import com.loopers.domain.user.RegisterCommand
import com.loopers.domain.user.UserService
import com.loopers.infrastructure.orderqueue.OrderQueueRedisRepository
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
import com.loopers.config.redis.RedisConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.time.ZoneId
import java.time.ZonedDateTime

@SpringBootTest
class UserOrderQueueUseCaseIntegrationTest @Autowired constructor(
    private val userEnterOrderQueueUseCase: UserEnterOrderQueueUseCase,
    private val userGetQueuePositionUseCase: UserGetQueuePositionUseCase,
    private val orderQueueRedisRepository: OrderQueueRedisRepository,
    private val userService: UserService,
    private val databaseCleanUp: DatabaseCleanUp,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER) private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val DEFAULT_USERNAME = "testuser"
        private const val DEFAULT_PASSWORD = "password1234!"
        private const val DEFAULT_NAME = "테스트유저"
        private const val DEFAULT_EMAIL = "test@loopers.com"
        private val DEFAULT_BIRTH_DATE = ZonedDateTime.of(1995, 5, 29, 0, 0, 0, 0, ZoneId.of("Asia/Seoul"))
    }

    @BeforeEach
    fun setUp() {
        cleanUpRedis()
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        cleanUpRedis()
    }

    private fun cleanUpRedis() {
        redisTemplate.delete("order:queue")
        redisTemplate.delete("order:queue:counter")
        redisTemplate.keys("order:token:*")?.forEach { redisTemplate.delete(it) }
    }

    private fun registerUser(username: String = DEFAULT_USERNAME) {
        userService.register(
            RegisterCommand(
                username = username,
                password = DEFAULT_PASSWORD,
                name = DEFAULT_NAME,
                email = DEFAULT_EMAIL,
                birthDate = DEFAULT_BIRTH_DATE,
            ),
        )
    }

    @DisplayName("UserEnterOrderQueueUseCase")
    @Nested
    inner class EnterQueue {
        @DisplayName("유저가 대기열에 진입하면 순번을 반환한다.")
        @Test
        fun returnsPositionWhenUserEntersQueue() {
            // arrange
            registerUser()

            // act
            val result = userEnterOrderQueueUseCase.execute(EnterQueueCriteria(loginId = DEFAULT_USERNAME))

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(1L) },
                { assertThat(result.totalWaiting).isEqualTo(1L) },
                { assertThat(result.estimatedWaitSeconds).isGreaterThan(0L) },
                { assertThat(result.pollingIntervalSeconds).isGreaterThan(0) },
            )
        }

        @DisplayName("존재하지 않는 유저면 NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFoundWhenUserDoesNotExist() {
            // act & assert
            val exception = assertThrows<CoreException> {
                userEnterOrderQueueUseCase.execute(EnterQueueCriteria(loginId = "nonexistent"))
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("중복 진입 시 CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflictOnDuplicateEntry() {
            // arrange
            registerUser()
            userEnterOrderQueueUseCase.execute(EnterQueueCriteria(loginId = DEFAULT_USERNAME))

            // act & assert
            val exception = assertThrows<CoreException> {
                userEnterOrderQueueUseCase.execute(EnterQueueCriteria(loginId = DEFAULT_USERNAME))
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @DisplayName("UserGetQueuePositionUseCase")
    @Nested
    inner class GetPosition {
        @DisplayName("대기열에 있으면 WAITING 상태와 순번을 반환한다.")
        @Test
        fun returnsWaitingStatusWhenInQueue() {
            // arrange
            registerUser()
            userEnterOrderQueueUseCase.execute(EnterQueueCriteria(loginId = DEFAULT_USERNAME))

            // act
            val result = userGetQueuePositionUseCase.execute(GetQueuePositionCriteria(loginId = DEFAULT_USERNAME))

            // assert
            assertAll(
                { assertThat(result.status).isEqualTo(QueueStatus.WAITING) },
                { assertThat(result.position).isEqualTo(1L) },
            )
        }

        @DisplayName("토큰이 발급된 상태면 ACTIVE를 반환한다.")
        @Test
        fun returnsActiveStatusWhenTokenIssued() {
            // arrange
            registerUser()
            val user = userService.getUser(DEFAULT_USERNAME)
            orderQueueRedisRepository.issueToken(user.id, 300)

            // act
            val result = userGetQueuePositionUseCase.execute(GetQueuePositionCriteria(loginId = DEFAULT_USERNAME))

            // assert
            assertAll(
                { assertThat(result.status).isEqualTo(QueueStatus.ACTIVE) },
                { assertThat(result.tokenExpireSeconds).isGreaterThan(0L) },
            )
        }

        @DisplayName("대기열에도 없고 토큰도 없으면 NOT_IN_QUEUE를 반환한다.")
        @Test
        fun returnsNotInQueueWhenNeitherInQueueNorToken() {
            // arrange
            registerUser()

            // act
            val result = userGetQueuePositionUseCase.execute(GetQueuePositionCriteria(loginId = DEFAULT_USERNAME))

            // assert
            assertThat(result.status).isEqualTo(QueueStatus.NOT_IN_QUEUE)
        }
    }
}
