package com.loopers.interfaces.api

import com.loopers.domain.user.Email
import com.loopers.domain.user.Password
import com.loopers.domain.user.UserModel
import com.loopers.domain.user.Username
import com.loopers.infrastructure.orderqueue.OrderQueueRedisRepository
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime

@SpringBootTest
@AutoConfigureMockMvc
class OrderQueueV1ApiE2ETest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userJpaRepository: UserJpaRepository,
    private val orderQueueRedisRepository: OrderQueueRedisRepository,
    private val passwordEncoder: PasswordEncoder,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    companion object {
        private const val ENDPOINT_BASE = "/api/v1/order-queue"
        private const val DEFAULT_USERNAME = "testuser"
        private const val DEFAULT_PASSWORD = "password1234"
        private const val DEFAULT_NAME = "테스트유저"
        private const val DEFAULT_EMAIL = "test@example.com"
        private val DEFAULT_BIRTH_DATE: ZonedDateTime = ZonedDateTime.parse("1990-01-01T00:00:00+09:00")
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisTemplate.delete("order:queue")
        redisTemplate.delete("order:queue:counter")
        redisTemplate.keys("order:token:*")?.forEach { redisTemplate.delete(it) }
    }

    private fun createUser(username: String = DEFAULT_USERNAME): UserModel {
        val user = UserModel(
            username = Username.of(username),
            password = Password.of(DEFAULT_PASSWORD, DEFAULT_BIRTH_DATE),
            name = DEFAULT_NAME,
            email = Email.of(DEFAULT_EMAIL),
            birthDate = DEFAULT_BIRTH_DATE,
        )
        user.applyEncodedPassword(passwordEncoder.encode(DEFAULT_PASSWORD))
        return userJpaRepository.save(user)
    }

    @DisplayName("POST /api/v1/order-queue/enter")
    @Nested
    inner class Enter {
        @DisplayName("대기열 진입에 성공하면 200과 순번을 반환한다.")
        @Test
        fun returnsOkWithPosition() {
            // arrange
            createUser()

            // act & assert
            mockMvc.perform(
                post("$ENDPOINT_BASE/enter")
                    .header("X-Loopers-LoginId", DEFAULT_USERNAME)
                    .header("X-Loopers-LoginPw", DEFAULT_PASSWORD),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.position").value(1))
                .andExpect(jsonPath("$.data.totalWaiting").value(1))
                .andExpect(jsonPath("$.data.estimatedWaitSeconds").isNumber)
                .andExpect(jsonPath("$.data.pollingIntervalSeconds").isNumber)
        }

        @DisplayName("중복 진입 시 409 CONFLICT를 반환한다.")
        @Test
        fun returnsConflictOnDuplicateEntry() {
            // arrange
            val user = createUser()
            orderQueueRedisRepository.enqueue(user.id)

            // act & assert
            mockMvc.perform(
                post("$ENDPOINT_BASE/enter")
                    .header("X-Loopers-LoginId", DEFAULT_USERNAME)
                    .header("X-Loopers-LoginPw", DEFAULT_PASSWORD),
            )
                .andExpect(status().isConflict)
        }
    }

    @DisplayName("GET /api/v1/order-queue/position")
    @Nested
    inner class GetPosition {
        @DisplayName("대기열에 있으면 WAITING 상태를 반환한다.")
        @Test
        fun returnsWaitingStatusWhenInQueue() {
            // arrange
            val user = createUser()
            orderQueueRedisRepository.enqueue(user.id)

            // act & assert
            mockMvc.perform(
                get("$ENDPOINT_BASE/position")
                    .header("X-Loopers-LoginId", DEFAULT_USERNAME)
                    .header("X-Loopers-LoginPw", DEFAULT_PASSWORD),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("WAITING"))
                .andExpect(jsonPath("$.data.position").value(1))
        }

        @DisplayName("토큰이 발급된 상태면 ACTIVE를 반환한다.")
        @Test
        fun returnsActiveStatusWhenTokenIssued() {
            // arrange
            val user = createUser()
            orderQueueRedisRepository.issueToken(user.id, 300)

            // act & assert
            mockMvc.perform(
                get("$ENDPOINT_BASE/position")
                    .header("X-Loopers-LoginId", DEFAULT_USERNAME)
                    .header("X-Loopers-LoginPw", DEFAULT_PASSWORD),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.tokenExpireSeconds").isNumber)
        }

        @DisplayName("대기열에도 없고 토큰도 없으면 NOT_IN_QUEUE를 반환한다.")
        @Test
        fun returnsNotInQueueWhenNeitherInQueueNorToken() {
            // arrange
            createUser()

            // act & assert
            mockMvc.perform(
                get("$ENDPOINT_BASE/position")
                    .header("X-Loopers-LoginId", DEFAULT_USERNAME)
                    .header("X-Loopers-LoginPw", DEFAULT_PASSWORD),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("NOT_IN_QUEUE"))
        }
    }
}
